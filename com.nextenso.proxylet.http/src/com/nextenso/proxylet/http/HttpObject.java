package com.nextenso.proxylet.http;


/**
 * The basic Http Object that all the elements of an HttpMessage extend.
 * <p/>When a request arrives, both the request and the response objects are instanciated.
 * <br/>It is possible to access them at any time, but one should be careful when doing it
 * and consider their lifecycles to avoid inconsistent data.
 */
public interface HttpObject {
        
    /**
     * Returns the request associated to this object.
     * @return the request.
     */
    public HttpRequest getRequest ();
  
    /**
     * Returns the response associated to this object.
     * @return the response.
     */
    public HttpResponse getResponse ();
  
    /**
     * Returns the session associated to this object.
     * @return the session.
     */
    public HttpSession getSession ();
  
    /**
     * Returns the Internet Protocol (IP) address of the client that sent the request.
     * @return the client IP address, or <code>null</code> if unknown.
     */
    public String getRemoteAddr ();

    /**
     * Returns the host name of the client that sent the request.
     * <br/>If the engine cannot or chooses not to resolve the hostname (to improve performance), this method returns the IP address.
     * @return	the client host name or IP address; <code>null</code> if unknown.
     */
    public String getRemoteHost ();
}

