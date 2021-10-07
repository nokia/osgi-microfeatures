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
  
  // the initial size for content
  public static int CONTENT_INIT_SIZE = 1024; // 1K
  // the max size for content when reseting
//  public static int CONTENT_MAX_SIZE = 4096; // 4K
  
  private HttpChannel channel;
  private MuxHeaderV0 header = new MuxHeaderV0();
  private boolean hasAvailableInput;
  private ByteBuffer buffer;
  private HttpParser parser;
  private String reqMethod;
  private boolean pre;
  private volatile int bytesSent = 0;
  private final ByteOutputStream out = new ByteOutputStream();

  public HttpSocket() {
    this.buffer = new ByteBuffer(CONTENT_INIT_SIZE);
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
  
//  public void release() {
//    parser.init();
//    if (buffer.bufferSize() > CONTENT_MAX_SIZE)
//      buffer.init(CONTENT_INIT_SIZE);
//    else
//      buffer.init();
//    channel = null;
//    hasAvailableInput = false;
//    reqMethod = null;
//  }
  
  public OutputStream getOutputStream() {
    return this;
  }
  
  public synchronized int readRequest(HttpRequestFacade req) throws HttpParserException {
    hasAvailableInput = false;
    try {
      return parser.parseRequest(buffer.getInputStream(), req);
    } finally {
      buffer.init();
    }
  }
  
  public synchronized int readRequestHeaders(HttpRequestFacade req) throws HttpParserException {
    try {
      buffer.mark(buffer.available());
    } catch (IOException ignored) {
    }
    try {
      return parser.parseRequestHeaders(buffer.getInputStream(), req);
    } finally {
      try {
        buffer.reset();
        parser.init();
      } catch (IOException ignored) {
      }
    }
  }
  
  public synchronized int readResponse(HttpResponseFacade rsp) throws HttpParserException {
    hasAvailableInput = false;
    try {
      return parser.parseResponse(reqMethod, buffer.getInputStream(), rsp);
    } finally {
      buffer.init();
    }
  }
  
  public synchronized boolean flushRequestBody() throws HttpParserException {
    hasAvailableInput = false;
    try {
      return parser.flushRequestBody(buffer.getInputStream());
    } finally {
      buffer.init();
    }
  }
  
  public synchronized boolean flushResponseBody() throws HttpParserException {
    hasAvailableInput = false;
    try {
      return parser.flushResponseBody(buffer.getInputStream());
    } finally {
      buffer.init();
    }
  }
  
  public synchronized int getRemainingBytes() {
    return parser.getRemainingBytes();
  }
  
  public synchronized void fillsInput(byte[] data, int offset, int length, boolean close) {
    if (data != null) {
      buffer.append(data, offset, length);
      hasAvailableInput = true;
    }
    
    if (close) {
      buffer.close();
    }
  }
  
  public synchronized boolean needsInput() {
    return parser.needsInput();
  }
  
  public synchronized boolean hasAvailableInput() {
    return hasAvailableInput;
  }
  
  public byte[] getInternalBuffer() {
    return buffer.toByteArray(false);
  }
  
  public int getBytesSent() {
    return bytesSent;
  }
  
  /******** OutputStream ********/
  
  public void write(int b) {
    write(new byte[] { (byte) b }, 0, 1);
  }
  
  public void write(byte[] data, int off, int len) {
	out.write(data, off, len);
    bytesSent += len;
  }
  
  public void flush() throws IOException {
	if (out.size() > 0) {
      setFlags();
      channel.sendMuxData(header, false, java.nio.ByteBuffer.wrap(out.toByteArray(false), 0, out.size()));
      out.reset(); // reallocate a new fresh buffer.
    }
  }

  /**************************************/
  @Override
  public String toString() {
    return "HttpSocket [hasAvailableInput=" + hasAvailableInput + ", " + parser + "]";
  }
  
}