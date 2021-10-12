// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

import java.util.Enumeration;
import java.util.ArrayList;

/**
 * This Class encapsulates the first line of the request.
 * <p/>It includes the method, the url and the http version.
 */
public interface HttpRequestProlog extends HttpObject {
        
    /**
     * Returns the http method of this http request.
     * <br/>Http methods are typically &quot;GET&quot;, or &quot;POST&quot;.
     * @return	the http method of this http request.
     */
    public String getMethod ();
    
    /**
     * Sets the http method of this http request.
     * <br/>Http methods are typically &quot;GET&quot;, or &quot;POST&quot;.
     * @param method	the http method 
     */
    public void setMethod (String method);
  
    /**
     * Returns the HttpURL the request is bound to.
     * @return the HttpURL
     */
    public HttpURL getURL ();
  
    /**
     * Sets the HttpURL.
     * @param url the HttpURL
     */
    public void setURL (HttpURL url);
  
    /**
     * Specifies if the connection is secure.
     * @return true if secure; false otherwise.
     */
    public boolean isSecure ();
  
    /**
     * Returns the value of a request parameter.
     * <br/>Only the parameters contained in the query part of the url are parsed.
     * <br/>If the parameter is multivalued, the returned value is equal to the first value
     * in the array returned by <code>getParameterValues</code>.
     * @param name the parameter name.
     * @return the parameter value, or <code>null</code> if the parameter is not set.
     * @see com.nextenso.proxylet.http.HttpRequestProlog#getParameterValues(String)
     */
    public String getParameter(String name);

    /**
     * Returns the names of the parameters contained in the url.
     * @return an Enumeration of the parameter names.
     */
    public Enumeration getParameterNames();

    /**
     * Returns the values of a request parameter.
     * <br/>Only the parameters contained in the query part of the url are parsed.
     * <br/>This method should be used to handle the multivalued parameters.
     * @return the parameter list of values, or <code>null</code> if the parameter is not set.
     */
    public ArrayList getParameterValues(String name);

    /**
     * Returns the HTTP version used by the request.
     * <br/>The usual values are HttpUtils.HTTP_10 and HttpUtils.HTTP_11.
     * @return the http version
     */
    public String getProtocol ();
  
    /**
     * Sets the HTTP version to use for this request.
     * <br/>This method should be used by the Engine only.
     * <br/>The usual values are HttpUtils.HTTP_10 and HttpUtils.HTTP_11.
     * @param protocol the http version
     */
    public void setProtocol (String protocol);  
}
