// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

import java.io.IOException;
import java.util.Hashtable;

/**
 * This is a utility Class used to perform a GET or a POST on a URL.
 * <p/>An instance can be retrieved via <code>HttpClientFactory.newHttpClient(String url)</code>.
 * <br/>Note that it follows redirects and that it is not thread-safe.
 */

public interface HttpClient {

    /**
     * Returns the URL involved.
     * @return the url.
     */
    public HttpURL getURL();

    /**
     * Sets a header in the request.
     * The headers must be set prior to doing doGET or doPOST
     * @param name the header name.
     * @param value the header value.
     */
    public void setHeader(String name, String value);

    /**
     * Adds authentication information 
     * Authentication must be set prior to doing doGET or doPOST
     * Challenges are then handled automatically
     * @param realm the realm corresponding to this user/password combination.
     * @param user the user name.
     * @param pswd the password.
     */
    public void addCredentials(String realm, String user, String pswd);

    /**
     * Performs a GET and returns the response code.
     * The response can be obtained by calling getResponse().
     * @return the response code.
     * @throws IOException if an I/O error occurs.
     */
    public int doGET()
	throws IOException;

    /**
     * Performs a GET asynchronously.
     * @param listener the object called when the http response is available. 
     */
    public void doGET(Listener listener);

    /**
     * Performs a POST and returns the response code.
     * The response can be obtained by calling getResponse().
     * @param content_type the request content-type.
     * @param body the body of the POST.
     * @return the response code.
     * @throws IOException if an I/O error occurs.
     */
    public int doPOST(String content_type, byte[] body)
	throws IOException ;

    /**
     * Performs a POST asynchronously.
     * @param content_type the request content-type.
     * @param body the body of the POST.
     * @param listener the object called when the http response is available. 
     */
    public void doPOST(String content_type, byte[] body, Listener listener);

    /**
     * Performs an OPTIONS and returns the response code.
     * The response can be obtained by calling getHeaders() and
     * getResponse().
     * @return the response code.
     */
    public int doOPTIONS() 
      throws IOException;

    /**
     * Performs an OPTIONS asynchronously.
     * @param listener the object called when the http response is available. 
     */
    public void doOPTIONS(Listener listener);

    /**
     * Performs a PUT and returns the response code.
     * The response can be obtained by calling getResponse().
     * @param content_type the request content-type.
     * @param body the body of the PUT.
     * @return the response code.
     * @throws IOException if an I/O error occurs.
     */
    public int doPUT(String content_type, byte[] body) throws IOException;

    /**
     * Performs a PUT asynchronously.
     * @param content_type the request content-type.
     * @param body the body of the PUT.
     * @param listener the object called when the http response is available. 
     */
    public void doPUT(String content_type, byte[] body, HttpClient.Listener listener);

    /**
     * Performs a DELETE and returns the response code.
     * The response can be obtained by calling getResponse().
     * @return the response code.
     * @throws IOException if an I/O error occurs.
     */
    public int doDELETE() throws IOException;
    
    /**
     * Performs a DELETE asynchronously.
     * @param listener the object called when the http response is available. 
     */
    public void doDELETE(HttpClient.Listener listener);

    /**
     * Returns the reponse from the server.
     * @return the response content or <code>null</code> if an IOException occurred while performing the request.
     */
    public byte[] getResponse();

    /**
     * Returns the headers from the response.
     * <br/>The keys are the header names and the values are the header values.
     * <br/>These values are String except for the header <code>set-cookie</code> for which the value is a String[].
     * @return the response headers or <code>null</code> if an IOException occurred while performing the request.
     */
    public Hashtable getHeaders();

    /**
     * Returns the reason from the response.
     * @return the response reason or <code>null</code> if an IOException occurred while performing the request.
     */
    public String getReason();

    /**
     * Returns the status of the response.
     * @return the response status or -1 if an IOException occurred while performing the request.
     */
    public int getStatus();

    /**
     * Attach a context to this http client.
     */
    void attach(Object attachment);

    /** 
     * Retrieves the http client context.
     */
    Object attachment();
    
    /**
     * Cancels this pending async http request.
     */
    boolean cancel();

    /**
     * This interface is a callback used to notify one http client about an asynchronous http response.
     */
    interface Listener {
        /** 
	 * The http request completed.
	 * @param client The http client used to make the request.
	 */
        void httpRequestCompleted(HttpClient client, int status);

        /** 
	 * The http request failed.
	 * @param client The http client used to make the request.
	 * @param cause The reason why the http request failed.
	 */
        void httpRequestFailed(HttpClient client, Throwable cause);
    }
}
