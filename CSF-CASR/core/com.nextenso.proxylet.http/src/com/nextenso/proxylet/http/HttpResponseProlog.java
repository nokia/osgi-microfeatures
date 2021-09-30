package com.nextenso.proxylet.http;

/**
 * This Class encapsulates the first line of the response.
 * <p/>It includes the status, the reason and the http version.
 */
public interface HttpResponseProlog extends HttpObject {
  
    /**
     * Specifies if the connection is secure.
     * @return true if secure; false otherwise.
     */
    public boolean isSecure ();
  
    /**
     * Returns the HTTP version used by the response.
     * <br/>The usual values are HttpUtils.HTTP_10 and HttpUtils.HTTP_11.
     * @return the http version.
     */
    public String getProtocol ();
  
    /**
     * Sets the HTTP version to use for this response.
     * <br/>This method should be used by the Engine only.
     * <br/>The usual values are HttpUtils.HTTP_10 and HttpUtils.HTTP_11.
     * @param protocol the http version.
     */
    public void setProtocol (String protocol);
  
    /**
     * Returns the status code.
     * @return the status.
     */
    public int getStatus ();
  
    /**
     * Sets the status code.
     * @param status the status.
     */
    public void setStatus (int status);
  
    /**
     * Returns the reason.
     * @return the reason.
     */
    public String getReason ();
  
    /**
     * Sets the reason.
     * @param reason the reason.
     */
    public void setReason (String reason);
  
    /**
     * Returns the HttpURL specified in the request.
     * @return the HttpURL.
     */
    public HttpURL getURL ();
}
