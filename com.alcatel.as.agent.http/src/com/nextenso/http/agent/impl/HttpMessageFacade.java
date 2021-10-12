// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.ByteBuffer;
import alcatel.tess.hometop.gateways.utils.ByteOutputStream;
import alcatel.tess.hometop.gateways.utils.Charset;
import alcatel.tess.hometop.gateways.utils.Constants;
import alcatel.tess.hometop.gateways.utils.Multipart;
import alcatel.tess.hometop.gateways.utils.QuotedStringTokenizer;
import alcatel.tess.hometop.gateways.utils.RfcDateParser;
import alcatel.tess.hometop.gateways.utils.StringCaseHashtable;
import alcatel.tess.hometop.gateways.utils.Utils;

import com.nextenso.agent.event.AsynchronousEventListener;
import com.nextenso.http.agent.parser.CookieParser;
import com.nextenso.http.agent.parser.HttpHandler;
import com.nextenso.http.agent.parser.HttpHeaderDescriptor;
import com.nextenso.proxylet.Proxylet;
import com.nextenso.proxylet.engine.AsyncProxyletManager;
import com.nextenso.proxylet.event.ProxyletEvent;
import com.nextenso.proxylet.event.ProxyletEventListener;
import com.nextenso.proxylet.http.HttpBody;
import com.nextenso.proxylet.http.HttpCookie;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpMessage;
import com.nextenso.proxylet.http.HttpObject;
import com.nextenso.proxylet.http.HttpSession;
import com.nextenso.proxylet.http.HttpURL;
import com.nextenso.proxylet.http.HttpUtils;
import com.nextenso.proxylet.http.event.AbortEvent;
import com.nextenso.proxylet.http.event.DisconnectionEvent;
import com.nextenso.proxylet.http.event.ServiceTimeoutEvent;
import com.nextenso.proxylet.impl.ProxyletDataImpl;

public abstract class HttpMessageFacade extends ProxyletDataImpl implements HttpObject, HttpHeaders,
    HttpBody, HttpMessage, AsynchronousEventListener, HttpHandler {
  /**
   * Factory used to wrap http message output streams. 
   */
  protected static final int EVENT_DATA_CONSTRUCTED = 1;
  protected static final int EVENT_DATA_NOT_CONSTRUCTED = 2;
  protected static final int EVENT_CLIENT_DISC = 3;
  protected static final int EVENT_SERVER_DISC = 4;
  protected static final int EVENT_SERVICE_TO = 5;
  protected static final int EVENT_ABORT = 6;
  
  /**
   * protected Constructor
   */
  protected HttpMessageFacade() {
    // content must be instanciated in superclasses
  }
  
  /********************************************
   * Implementation of AsynchronousEventListener
   ********************************************/
  
  public void asynchronousEvent(Object data, int type) {
    boolean doResponse = false;
    ProxyletEventListener[] localListeners = listeners;
    int size = localListeners.length;
    switch (type) {
    case EVENT_DATA_CONSTRUCTED: {
      // the data is the event
      ProxyletEvent event = (ProxyletEvent) data;
      for (int i = 0; i < size; i++)
        localListeners[i].proxyletEvent(event);
      break;
    }
    case EVENT_DATA_NOT_CONSTRUCTED: {
      // the data is the event source
      if (size == 0)
        break;
      ProxyletEvent event = new ProxyletEvent(data, this);
      for (int i = 0; i < size; i++)
        localListeners[i].proxyletEvent(event);
      break;
    }
    case EVENT_CLIENT_DISC: {
      // the data is null
      doResponse = true;
      if (size == 0)
        break;
      DisconnectionEvent event = new DisconnectionEvent(DisconnectionEvent.CLIENT, this);
      for (int i = 0; i < size; i++)
        localListeners[i].proxyletEvent(event);
      break;
    }
    case EVENT_SERVER_DISC: {
      // the data is null
      doResponse = true;
      if (size == 0)
        break;
      DisconnectionEvent event = new DisconnectionEvent(DisconnectionEvent.SERVER, this);
      for (int i = 0; i < size; i++)
        localListeners[i].proxyletEvent(event);
      break;
    }
    case EVENT_SERVICE_TO: {
      // the data is null
      doResponse = true;
      if (size == 0)
        break;
      ServiceTimeoutEvent event = new ServiceTimeoutEvent(getProxyletContext(), this);
      for (int i = 0; i < size; i++)
        localListeners[i].proxyletEvent(event);
      break;
    }
    case EVENT_ABORT: {
      // the data is null
      doResponse = true;
      if (size == 0)
        break;
      AbortEvent event = new AbortEvent(getProxyletContext(), this);
      for (int i = 0; i < size; i++)
        localListeners[i].proxyletEvent(event);
      break;
    }
    }
    
    if (doResponse) {
      if (isRequest()) {
        // we notify the listeners registered on the response
        HttpResponseFacade response = (HttpResponseFacade) getResponse();
        response.asynchronousEvent(null, type);
      }
    }
  }
  
  /******************************************************
   * Implementation of com.nextenso.proxylet.http.HttpObject
   *******************************************************/
  
  public HttpSession getSession() {
    return (this.session);
  }
  
  public String getRemoteAddr() {
    return getSession().getRemoteAddr();
  }
  
  public String getRemoteHost() {
    return getSession().getRemoteHost();
  }
  
  /**
   *   Superclasses must implement:
   *   getRequest
   *   getResponse
   */
  
  /******************************************************
   * Implementation of com.nextenso.proxylet.http.HttpBody
   *******************************************************/
  
  public InputStream getInputStream() {
    return content.getInputStream();
  }
  
  public OutputStream getOutputStream() {
    return content.getOutputStream();
  }
  
  public void clearContent() {
    this.content.init();
  }
  
  public void setContent(byte[] data) {
    setContent(data, false);
  }
  
  public void setContent(byte[] data, boolean copy) {
    setContent(data, 0, data.length, copy);
  }
  
  public void setContent(byte[] data, int offset, int length) {
    setContent(data, offset, length, false);
  }
  
  public void setContent(byte[] data, int offset, int length, boolean copy) {
    this.content.init(data, offset, length, copy);
  }
  
  public void setContent(String s) {
    byte[] data = Charset.makeBytes(s);
    this.content.init(data, 0, data.length, false);
  }
  
  public void setContent(String s, String enc) throws UnsupportedEncodingException {
    byte[] data = Charset.makeBytes(s, enc);
    this.content.init(data, 0, data.length, false);
  }
  
  public void appendContent(int b) {
    this.content.append((byte) b);
  }
  
  public void appendContent(byte[] data) {
    this.content.append(data);
  }
  
  public void appendContent(byte[] data, int offset, int length) {
    this.content.append(data, offset, length);
  }
  
  public void appendContent(InputStream in) throws IOException {
    this.content.append(in);
  }
  
  public void appendContent(InputStream in, int n) throws IOException {
    this.content.append(in, n);
  }
  
  public void appendContent(String s) {
    byte[] data = Charset.makeBytes(s);
    this.content.append(data, 0, data.length);
  }
  
  public void appendContent(String s, String enc) throws UnsupportedEncodingException {
    byte[] data = Charset.makeBytes(s, enc);
    this.content.append(data, 0, data.length);
  }
  
  public void appendContent(java.nio.ByteBuffer ... bufs) throws IOException {
    for (java.nio.ByteBuffer buf : bufs) {
      if (buf.hasArray()) {
        appendContent(buf.array(), buf.position(), buf.remaining());
      } else {
        while (buf.hasRemaining()) {
          appendContent(buf.get());
        }
      }
    }
  }
  
  public byte[] getContent() {
    return this.content.toByteArray(true);
  }
  
  public byte[] getInternalContent() {
    return this.content.toByteArray(false);
  }
  
  public String getContentAsString() throws UnsupportedEncodingException {
    if (content.size() == 0) {
      return (Constants.EMPTY_STRING);
    }
    
    String charset = getCharacterEncoding();
    
    if (charset == null) {
      charset = Constants.CHARSET_ISO8859_1;
    }
    
    return (Charset.makeString(this.content.toByteArray(false), 0, this.content.size(), charset));
  }
  
  public int getSize() {
    return (this.content.size());
  }
  
  /*************************************************************
   * Implementation of com.nextenso.proxylet.http.HttpHeaders
   *************************************************************/
  
  public String getContentType() {
    String ctype = getHeader(HttpUtils.CONTENT_TYPE);
    
    if (ctype == null) {
      return (null);
    }
    
    int i = -1;
    for (int k = 0; k < ctype.length(); k++) {
      char c = ctype.charAt(k);
      if (c == ';' || c == ',') {
        i = k;
        break;
      }
    }
    
    if (i != -1)
      return (ctype.substring(0, i).trim());
    
    return (ctype);
  }
  
  public void setContentType(String ct) {
    setHeader(HttpUtils.CONTENT_TYPE, ct);
  }
  
  public Enumeration<?> getHeaderNames() {
    final Enumeration<?> enumer = hdrs.keys();
    return new Enumeration<Object>() {
      public boolean hasMoreElements() {
        return enumer.hasMoreElements();
      }
      
      public Object nextElement() {
        return Utils.capitalizeFirstLetter((String) enumer.nextElement());
      }
    };
  }
  
  public String getHeader(String k) {
    return hdrs.get(k);
  }
  
  public String getHeader(String k, String def) {
    String val = getHeader(k);
    return (val != null) ? val : def;
  }
  
  public Enumeration<?> getHeaders(String name) {
    String val = getHeader(name);
    if (val == null) {
      return new Enumeration<Object>() {
        public boolean hasMoreElements() {
          return false;
        }
        
        public Object nextElement() {
          throw new NoSuchElementException();
        }
      };
    }
    final QuotedStringTokenizer st = new QuotedStringTokenizer(val, ",", false);
    return new Enumeration<Object>() {
      public boolean hasMoreElements() {
        return st.hasMoreElements();
      }
      
      public Object nextElement() {
        return ((String) st.nextElement()).trim();
      }
    };
  }
  
  public void addCookie(HttpCookie cookie) {
    cookies.add(cookie);
  }
  
  public HttpCookie getCookie(String name) {
    for (int i = 0; i < cookies.size(); i++) {
      HttpCookie cookie = (HttpCookie) cookies.elementAt(i);
      if (name.equals(cookie.getName()))
        return cookie;
    }
    return null;
  }
  
  public Enumeration<HttpCookie> getCookies() {
    return cookies.size() == 0 ? Collections.emptyEnumeration() : cookies.elements();
  }
  
  public Enumeration<?> getCookieNames() {
    final Enumeration<HttpCookie> enumer = getCookies();
    return new Enumeration<Object>() {
      public boolean hasMoreElements() {
        return enumer.hasMoreElements();
      }
      
      public Object nextElement() {
        return ((HttpCookie) enumer.nextElement()).getName();
      }
    };
  }
  
  public void removeCookie(HttpCookie cookie) {
    cookies.removeElement(cookie);
  }
  
  public void removeCookies() {
    cookies.clear();
  }
  
  /**
   * private method to handle headers
   * the value is NOT null
   */
  private void setHeader(String key, String value, boolean remove) {
    if (key.equalsIgnoreCase("Set-Cookie")) {
      if (remove)
        removeCookies();
      // set-cookie contains 1 cookie per line
      value = value.trim();
      if (value.length() > 0) {
        try {
          //we do not apply defaults values for domain and path
          addCookie(CookieParser.parse(value));
        } catch (IllegalArgumentException e) {
          // the cookie is invalid
          // ignore it for now
          logger.warn("Ignoring invalid set-cookie header:" + value);
        }
      }
      return;
    }
    if (key.equalsIgnoreCase("Set-Cookie2")) {
      // we ignore it - we make sure we do not put it in the headers
      return;
    }
    if (key.equalsIgnoreCase("Cookie")) {
      if (remove)
        removeCookies();
      // cookie contains 1+ cookies per line separated by ';'
      StringTokenizer st = new StringTokenizer(value, ";");
      while (st.hasMoreTokens()) {
        String token = st.nextToken().trim();
        if (token.length() > 0) {
          try {
            addCookie(CookieParser.parse(token));
          } catch (IllegalArgumentException e) {
            // the cookie is invalid
            // ignore it for now
            logger.warn("Ignoring invalid cookie header:" + token);
          }
        }
      }
      return;
    }
    
    // Track if a proxylet is setting the header "Connection"
    if (key.equalsIgnoreCase("Connection")) {
      connectionHeaderModified = true;
    }
    
    // not a cookie-related header
    
    if (!remove) {
      String oldValue = getHeader(key);
      if (oldValue != null)
        value = oldValue + ',' + value;
    }
    hdrs.put(key, value);
  }
  
  public void addHeaderWithNoChange(String key, String value) {
    if (value != null) {
      String oldValue = getHeader(key);
      if (oldValue != null) {
        value = oldValue + ',' + value;
      }
      
      hdrs.put(key, value);
    }
  }
  
  public void addHeader(String key, String value) {
    if (value != null)
      setHeader(key, value, false);
  }
  
  public void addDateHeader(String name, long date) {
    addHeader(name, RfcDateParser.format(new Date(date)));
  }
  
  public void addIntHeader(String name, int value) {
    addHeader(name, String.valueOf(value));
  }
  
  public int getIntHeader(String k, int def) {
    String val = getHeader(k);
    
    if (val == null) {
      return (def);
    }
    
    try {
      return (Integer.parseInt(val));
    } catch (NumberFormatException e) {
      return (def);
    }
  }
  
  public long getDateHeader(String name) {
    String h = getHeader(name);
    
    if (h == null)
      return (-1);
    
    Date d = RfcDateParser.parse(h);
    if (d == null)
      return -1;
    
    return (d.getTime());
  }
  
  public void setDateHeader(String name, long date) {
    setHeader(name, RfcDateParser.format(new Date(date)));
  }
  
  public void setIntHeader(String name, int value) {
    setHeader(name, String.valueOf(value));
  }
  
  public void setHeader(String key, String val) {
    if (val == null)
      removeHeader(key);
    else
      setHeader(key, val, true);
  }
  
  public String removeHeader(String key) {
    if (key.equalsIgnoreCase("Connection")) {
      connectionHeaderModified = true;
    }
    return hdrs.remove(key);
  }
  
  public void removeHeaders() {
    hdrs.clear();
    cookies.clear();
  }
  
  public void addParameter(String headerName, String parameterName, String parameterValue) {
    String oldValue = getHeader(headerName);
    if (oldValue == null)
      return;
    StringBuilder buff = new StringBuilder(oldValue);
    buff.append(';').append(parameterName);
    if (parameterValue != null)
      // a parameter like 'secure' may have a null value
      buff.append('=').append(parameterValue);
    hdrs.put(headerName, buff.toString());
  }
  
  public String getCharacterEncoding() {
    String s = getHeader(HttpUtils.CONTENT_TYPE);
    
    if (s == null)
      return null;
    
    int i = s.indexOf("charset");
    if (i == -1)
      i = s.indexOf("Charset"); // frequent case
    if (i == -1 || i == s.length() - 7)
      return null;
    
    i = s.indexOf('=', i + 7);
    if (i == -1 || i == s.length() - 1)
      return null;
    int j = s.indexOf(';', i + 1);
    if (j == -1)
      j = s.length();
    s = s.substring(i + 1, j).trim();
    if (s.length() == 0)
      return null;
    if (s.length() == 1)
      return s;
    
    if (s.charAt(0) == '"')
      return s.substring(1, s.length() - 1);
    else
      return s;
  }
  
  public void setCharacterEncoding(String enc) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(getContentType());
    buffer.append(";charset=");
    buffer.append(enc);
    setContentType(buffer.toString());
  }
  
  public int getContentLength() {
    String l = getHeader(HttpUtils.CONTENT_LENGTH);
    try {
      return (l != null) ? Integer.parseInt(l) : -1;
    } catch (NumberFormatException e) {
      return -1;
    }
  }
  
  /*************************************************************
   * Implementation of com.nextenso.proxylet.http.HttpMessage
   *************************************************************/
  
  public HttpHeaders getHeaders() {
    return this;
  }
  
  public HttpBody getBody() {
    return this;
  }
  
  public int setContentLength() {
    int l = getSize();
    setIntHeader(HttpUtils.CONTENT_LENGTH, l);
    return l;
  }
  
  /**
   * Writes the headers and only the headers to the output stream, without any
   * modification (no headers are added).
   */
  public void writeHeadersTo(OutputStream out) throws IOException {
    StringBuilder buf = new StringBuilder();
    toString(buf, false, false);
    out.write(Charset.CHARSET_US_ASCII.getBytes(buf.toString()));
    out.flush();
  }
  
  /**
   * Writes the whole message to the OutputStream.
   * Assumes the message is buffered and does not use chunked data.
   */
  public void writeTo(OutputStream out, boolean usingProxy) throws IOException {
    out.write(Charset.CHARSET_US_ASCII.getBytes(toString(false, usingProxy)));
    if (content.size() > 0) {
      out.write(content.toByteArray(false), 0, content.size());
    }
    out.flush();
  }
  
  /**
   * Writes the headers to the OutputStream.
   * Handles chunked data.
   */
  public void writeHeadersTo(OutputStream out, boolean usingProxy) throws IOException {
    out.write(Charset.CHARSET_US_ASCII.getBytes(toString(false, usingProxy)));
    out.flush();
  }
  
  /**
   * Writes the body to the OutputStream.
   * Handles chunked data.
   */
  @SuppressWarnings("resource")
  public void writeBodyTo(OutputStream out, boolean lastChunk) throws IOException {
    if (content.size() > 0) {
      if (chunked) {
        ByteOutputStream tmp = new ByteOutputStream();
        tmp.write(Charset.makeBytes(Integer.toHexString(content.size()), Constants.ASCII));
        tmp.write(Constants.CRLF_B);
        tmp.write(content.toByteArray(false), 0, content.size());
        tmp.write(Constants.CRLF_B);
        out.write(tmp.toByteArray(false), 0, tmp.size());
        out.flush();
      } else {
        // no chunk
        out.write(content.toByteArray(false), 0, content.size());
        out.flush();
      }
    }
    if (lastChunk && chunked) {
      out.write(Constants._0_CRLF_CRLF_B);
      out.flush();
    }
  }
  
  /*************************************************************
   * Implementation of com.nextenso.http.agent.parser.HttpHandler
   *************************************************************/
  
  public void setHttpProtocol(String protocol) {
    setProtocol(protocol);
  }
  
  public void addHttpCookie(HttpCookie cookie) {
    addCookie(cookie);
  }
  
  public void addHttpHeader(String name, String val) {
    addHeader(name, val);
  }
  
  public void addHttpHeader(HttpHeaderDescriptor desc, String val) {
    addHeader(desc.name(), val);
  }
  
  public void addHttpBody(InputStream in, int size) throws IOException {
    content.append(in, size);
  }
  
  /*************************************************************
   * Common methods from Request and Response
   *************************************************************/
  
  public String getProtocol() {
    return (this.protocol);
  }
  
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }
  
  public boolean isSecure() {
    return getURL().isSecure();
  }
  
  public abstract HttpURL getURL();
  
  /*************************************************************
   * Misc. methods
   *************************************************************/
  
  public boolean connectionHeaderModified() {
    return connectionHeaderModified;
  }
  
  public abstract boolean isRequest();
  
  public int getListenersSize() {
    synchronized (listenersLock) {
      return listeners.length;
    }
  }
  
  public boolean setStreaming(boolean value) {
    if (value == false) {
      return (this.chunked = false);
    }
    // no chunk in http 1.0
    // TODO: send server_close for responses ???
    // Support https
    // always streaming irrespective of the protocol the version
    if (isSecure()) {
      // we do not write chunked data len while sending req/resp
      this.chunked = false;
      return true;
    }
    return (this.chunked = 
          (this.protocol.equals(HttpUtils.HTTP_11) || this.protocol.equals(HttpUtils.HTTP_20)));
  }
  
  public boolean isChunked() {
    return this.chunked;
  }
  
  public void removeChunkedHeader() {
    String te = removeHeader(HttpUtils.TRANSFER_ENCODING);
    if (te == null)
      return;
    int i = te.lastIndexOf(',');
    if (i == -1) {
      // frequent case (1 value) > fast process
      if (te.equalsIgnoreCase(Constants.PARAM_CHUNKED))
        return;
      else
        setHeader(HttpUtils.TRANSFER_ENCODING, te);
    } else {
      String tok = te.substring(i + 1);
      if (tok.equalsIgnoreCase(Constants.PARAM_CHUNKED))
        setHeader(HttpUtils.TRANSFER_ENCODING, te.substring(0, i));
      else
        setHeader(HttpUtils.TRANSFER_ENCODING, te);
    }
  }
  
  public void addChunkedHeader() {
    String te = (String) getHeader(HttpUtils.TRANSFER_ENCODING);
    if (te == null) {
      setHeader(HttpUtils.TRANSFER_ENCODING, Constants.PARAM_CHUNKED);
      return;
    }
    int i = te.lastIndexOf(',');
    if (i == -1) {
      // frequent case (1 value) > fast process
      if (te.equalsIgnoreCase(Constants.PARAM_CHUNKED))
        return;
      else
        setHeader(HttpUtils.TRANSFER_ENCODING, te + ',' + Constants.PARAM_CHUNKED);
    } else {
      String tok = te.substring(i + 1);
      if (tok.equalsIgnoreCase(Constants.PARAM_CHUNKED))
        return;
      else
        setHeader(HttpUtils.TRANSFER_ENCODING, te + ',' + Constants.PARAM_CHUNKED);
    }
  }
  
  public boolean hasChunkedHeader() {
    String te = (String) getHeader(HttpUtils.TRANSFER_ENCODING);
    if (te == null)
      return false;
    int i = te.lastIndexOf(',');
    if (i == -1)
      // frequent case (1 value) > fast process
      return (te.equalsIgnoreCase(Constants.PARAM_CHUNKED));
    else
      return (te.substring(i + 1).equalsIgnoreCase(Constants.PARAM_CHUNKED));
  }
  
  public void setSession(HttpSession session) {
    this.session = session;
  }
  
  public void addProxylet(Proxylet proxylet) {
    this.proxylets.add(proxylet);
  }
  
  public Proxylet getProxylet(int i) {
    return (Proxylet) this.proxylets.get(i);
  }
  
  public int getProxyletsSize() {
    return this.proxylets.size();
  }
  
  public String toString() {
    return (toString(true, true));
  }
  
  public String toString(boolean withBody, boolean usingProxy) {
    return (toString(withBody, false, usingProxy));
  }
  
  public String toString(boolean withBody, boolean noBinary, boolean usingProxy) {
    StringBuilder buf = new StringBuilder();
    firstLineToString(buf, usingProxy);
    toString(buf, withBody, noBinary);
    return (buf.toString());
  }
  
  @Override
  public void resume(int status) {
    AsyncProxyletManager.resume(this, status);
  }
  
  abstract protected void firstLineToString(StringBuilder buf, boolean usingProxy);
  
  /*************************************************************
   * Private declarations
   *************************************************************/
  
  /**
   * Fills a string buffer with headers with(out) body.
   * @param buf The string buffer to fill.
   * @param withBody true if body must be filled, false if not.
   * @param noBinary true if body must not display binary content.
   */
  private void toString(StringBuilder buf, boolean withBody, boolean noBinary) {
    Enumeration<?> keys = hdrs.keys();
    
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      String val = (String) hdrs.get(key);
      
      if (val != null) {
        buf.append(Utils.capitalizeFirstLetter(key)).append(": ").append(val);
        buf.append(Constants.CRLF);
      }
    }
    
    if (cookies.size() > 0) {
      toStringCookies(buf);
    }
    
    buf.append(Constants.CRLF);
    
    if (withBody && content.size() > 0) {
      toStringBody(buf, noBinary);
    }
  }

	private void toStringBody(StringBuilder buf, boolean noBinary) {
		if (noBinary) {
			//
			// We must not display binary content
			//
			String contentType = getHeaders().getContentType();

			if (contentType != null && contentType.regionMatches(true, 0, "multipart/", 0, "multipart/".length())) {
				Multipart mp = new Multipart(getHeaders().getHeader(HttpUtils.CONTENT_TYPE), content.toByteArray(false),
						0, content.size());
				buf.append(mp.toString(true));
				return;
			}

			if (!Utils.isBinaryType(contentType)) {
				//
				// Display textual content.
				//
				buf.append(Charset.makeString(content.toByteArray(false), 0, content.size()));
			}
		} else {
			//
			// We may display all kind of content
			//
			buf.append(Charset.makeString(content.toByteArray(false), 0, content.size()));
		}
	}

	private void toStringCookies(StringBuilder buf) {
		Enumeration<HttpCookie> c = cookies.elements();

		if (isRequest()) {
			buf.append("Cookie: ");
			boolean separator = false;
			while (c.hasMoreElements()) {
				HttpCookie cookie = (HttpCookie) c.nextElement();
				if (separator)
					buf.append("; ");
				separator = true;
				buf.append(cookie.getName()).append('=').append(cookie.getValue());
			}
			buf.append(Constants.CRLF);
		} else {
			while (c.hasMoreElements()) {
				HttpCookie cookie = (HttpCookie) c.nextElement();
				String val = CookieParser.toString(cookie, 0);
				buf.append("Set-Cookie").append(": ").append(val);
				buf.append(Constants.CRLF);
				val = CookieParser.toString(cookie, 1);
				buf.append("Set-Cookie2").append(": ").append(val);
				buf.append(Constants.CRLF);
			}
		}
	}
  
  protected ByteBuffer content;
  protected String protocol = HttpUtils.HTTP_11;
  protected StringCaseHashtable hdrs = new StringCaseHashtable();
  protected Vector<HttpCookie> cookies = new Vector<HttpCookie>(); // we use a Vector to be able to use the method "elements ()"
  protected HttpSession session;
  protected boolean chunked = false;
  protected ArrayList<Proxylet> proxylets = new ArrayList<Proxylet>();
  private boolean connectionHeaderModified; // the appli has set a "Connection: close" header.
  private final static Logger logger = com.nextenso.http.agent.Utils.logger;
  
}
