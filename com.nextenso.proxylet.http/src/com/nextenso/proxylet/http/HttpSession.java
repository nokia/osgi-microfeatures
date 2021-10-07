package com.nextenso.proxylet.http;

import java.util.Enumeration;
import com.nextenso.proxylet.*;

/**
 * This Class identifies a client across multiple requests.
 */
public interface HttpSession {
        
    /**
     * Returns a unique long that identifies the session.
     * @return the session Id.
     */
    public long getId();
  
    /**
     * Returns the ProxyletContext the session belongs to.
     * @return the ProxyletContext.
     */
    public ProxyletContext getProxyletContext();

    /**
     * Returns the time when this session was created.
     * <br/>It is measured in milliseconds since midnight January 1, 1970 GMT.
     * @return				a <code>long</code> specifying
     * 					when this session was created,
     *					expressed in 
     *					milliseconds since 1/1/1970 GMT
     */
    public long getCreationTime();
    
    /**
     *
     * Returns the last time the client sent a request.
     * <br/>It is indicated as the number of milliseconds since midnight
     * January 1, 1970 GMT.
     * @return				a <code>long</code>
     *					representing the last time 
     *					the client sent a request associated
     *					with this session, expressed in 
     *					milliseconds since 1/1/1970 GMT
     */
    public long getLastAccessedTime();
    
    /**
     * Returns the specified attribute.
     * @param name the attribute name.
     * @return the attribute value or <code>null</code> if not set.
     */
    public Object getAttribute(Object name);
    
    /**
     * Returns an Enumeration of the attribute names.
     * @return an Enumeration of the attribute names.
     */
    public Enumeration getAttributeNames();

    /**
     * Returns the Internet Protocol (IP) address of the client.
     * @return the client IP address, or <code>null</code> if unknown.
     */
    public String getRemoteAddr ();

    /**
     * Returns the host name of the client.
     * <br/>If the engine cannot or chooses not to resolve the hostname (to improve performance), this method returns the IP address.
     * @return	the client host name or IP address; <code>null</code> if unknown.
     */
    public String getRemoteHost ();
    
    /**
     * Returns an id that identifies the client.
     * <br/>This id is not always set. It can be retrieved from a pre-determined header.
     *
     * @return a String that identifies the client.
     */
    public String getRemoteId();

    /**
     * Sets an id that identifies the client.
     * <br/>This id is not always set. It can be retrieved from a pre-determined header.
     *
     */
    public void setRemoteId(String clid);

    /**
     * Sets an attribute.
     * @param name the attribute name.
     * @param value the attribute value.
     */
    public void setAttribute(Object name, Object value);
    
    /**
     * Removes an attribute.
     * @param name the attribute name.
     * @return the removes attribute value or <code>null</code> if the attribute was not set.
     */
    public Object removeAttribute(Object name);

    /**
     * Destroys this session.
     * A new request from the client will be assigned a fresh session.
     */
    public void destroy();

    /*
     * Returns the maximum time interval, in seconds, that the proxylet container will keep this session
     * open between client accesses. After this interval, the proxylet container will destroy the session.
     * The maximum time interval can be set with the <code>setMaxInactiveInterval</code> method. 
     * A negative time indicates the session should never timeout.
     * By default the max inactivity interface is configured in the http agent &quot;Session Timeout&quot;
     * parameter.
     * 
     * @return
     *	an integer specifying the number of seconds this session remains open between client requests
     *
     * @see
     *	#setMaxInactiveInterval
     */
    public int getMaxInactiveInterval();

    /**
     *
     * Specifies the maximum length of time, in seconds, that the
     * proxylet engine keeps this session if no user requests
     * have been made of the session.
     *
     * @param interval		An integer specifying the number of seconds 
     * @see #getMaxInactiveInterval
     */
    public void setMaxInactiveInterval(int interval);
}

  


