package com.nextenso.proxylet.http;

import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

import com.nextenso.proxylet.ProxyletData;

/**
 * This Class defines a basic http message.
 * <p/>It provides access to the headers and the body.
 * <br/>It extends ProxyletData and therefore makes it possible for requests and responses
 * to store attributes or to handle events (via ProxyletEvent).
 */
public interface HttpMessage extends HttpObject, ProxyletData {
  
    /**
     * Returns the Headers.
     * @return the headers
     */
    public HttpHeaders getHeaders ();
  
    /**
     * Returns the Body.
     * @return the body
     */
    public HttpBody getBody ();

    /**
     * Sets the content-length header to the body size and returns the value.
     * <br/>This method can be only be called for a buffered message.
     * @return the body size
     */
    public int setContentLength ();
  
    /**
     * Writes this http message into an output stream.
     * @param out the output stream where this message must be written to.
     * @param usingProxy specifies if the message should be written in a proxy-compliant way (only relevant for a request).
     * @throws IOException if any I/O error occurs.
     */
    public void writeTo (OutputStream out, boolean usingProxy) throws IOException;
  
}
