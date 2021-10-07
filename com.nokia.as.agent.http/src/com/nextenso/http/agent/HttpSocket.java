package com.nextenso.http.agent;

import java.io.IOException;
import java.io.OutputStream;

import alcatel.tess.hometop.gateways.utils.ByteBuffer;
import alcatel.tess.hometop.gateways.utils.ByteOutputStream;

import com.nextenso.http.agent.impl.HttpRequestFacade;
import com.nextenso.http.agent.impl.HttpResponseFacade;
import com.nextenso.http.agent.parser.HttpParser;
import com.nextenso.http.agent.parser.HttpParserException;
import com.nextenso.mux.MuxHeaderV0;

public class HttpSocket extends OutputStream {
  
  // max input buf size to recycle.
  private static int MAX_INPUT_BUFSIZE = 1024;
  private final static byte[] EMPTY_BUFFER = new byte[0];
  
  private HttpChannel channel;
  private MuxHeaderV0 header = new MuxHeaderV0();
  private boolean hasAvailableInput;
  private ByteBuffer input;
  private HttpParser parser;
  private String reqMethod;
  private boolean pre;
  private volatile int bytesSent = 0;
  private final ByteOutputStream output = new ByteOutputStream();

  public HttpSocket() {
    this.input = new ByteBuffer(EMPTY_BUFFER, 0);
    this.parser = new HttpParser();
  }
  
  public void init(boolean pre, HttpChannel channel) {
    this.channel = channel;
    this.pre = pre;
  }
  
  private void setFlags() {
    int flags = Utils.DATA;
    flags |= (pre) ? Utils.PRE_FILTER_FLAGS : Utils.POST_FILTER_FLAGS;
    header.set(channel.getSessionId(), channel.getId(), flags);
  }
  
  public void setRequestMethod(String method) {
    reqMethod = method;
  }
  
  public void recycled() {
    parser.init();
    if (input.bufferSize() > MAX_INPUT_BUFSIZE) {
      input.init(EMPTY_BUFFER, 0, 0, false);
    } else {
      input.init();
    }
    channel = null;
    hasAvailableInput = false;
    reqMethod = null;
    bytesSent = 0;
  }
  
  public OutputStream getOutputStream() {
    return this;
  }
  
  public synchronized int readRequest(HttpRequestFacade req) throws HttpParserException {
    hasAvailableInput = false;
    try {
      return parser.parseRequest(input.getInputStream(), req);
    } finally {
      input.init();
    }
  }
  
  public synchronized int readRequestHeaders(HttpRequestFacade req) throws HttpParserException {
    try {
      input.mark(input.available());
    } catch (IOException ignored) {
    }
    try {
      return parser.parseRequestHeaders(input.getInputStream(), req);
    } finally {
      try {
        input.reset();
        parser.init();
      } catch (IOException ignored) {
      }
    }
  }
  
  public synchronized int readResponse(HttpResponseFacade rsp) throws HttpParserException {
    hasAvailableInput = false;
    try {
      return parser.parseResponse(reqMethod, input.getInputStream(), rsp);
    } finally {
      input.init();
    }
  }
  
  public synchronized boolean flushRequestBody() throws HttpParserException {
    hasAvailableInput = false;
    try {
      return parser.flushRequestBody(input.getInputStream());
    } finally {
      input.init();
    }
  }
  
  public synchronized boolean flushResponseBody() throws HttpParserException {
    hasAvailableInput = false;
    try {
      return parser.flushResponseBody(input.getInputStream());
    } finally {
      input.init();
    }
  }
  
  public synchronized int getRemainingBytes() {
    return parser.getRemainingBytes();
  }
  
  public synchronized void fillsInput(byte[] data, int offset, int length, boolean close) {
    if (data != null) {
      input.append(data, offset, length);
      hasAvailableInput = true;
    }
    
    if (close) {
      input.close();
    }
  }
  
  public synchronized boolean needsInput() {
    return parser.needsInput();
  }
  
  public synchronized boolean hasAvailableInput() {
    return hasAvailableInput;
  }
  
  public byte[] getInternalBuffer() {
    return input.toByteArray(false);
  }
  
  public int getBytesSent() {
    return bytesSent;
  }
  
  /******** OutputStream ********/
  
  public void write(int b) {
    write(new byte[] { (byte) b }, 0, 1);
  }
  
  public void write(byte[] data, int off, int len) {
	output.write(data, off, len);
    bytesSent += len;
  }
  
  public void flush() throws IOException {
	if (output.size() > 0) {
      setFlags();
      channel.sendMuxData(header, false, java.nio.ByteBuffer.wrap(output.toByteArray(false), 0, output.size()));
      output.reset(); // reallocate a new fresh buffer.
    }
  }

  /**************************************/
  @Override
  public String toString() {
    return "HttpSocket [hasAvailableInput=" + hasAvailableInput + ", " + parser + "]";
  }
  
}
