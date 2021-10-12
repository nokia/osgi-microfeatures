// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

import com.nextenso.proxylet.ProxyletException;

/**
 * A StreamedHttpResponseProxylet is an HttpResponseProxylet that performs its job on a streamed response.
 * <p/>The Engine first calls the method that handles the headers (doResponseHeaders)
 * and then calls the method that handles the body (doResponseBody) on each chunk of body data
 * as it is available.
 */
public interface StreamedHttpResponseProxylet extends HttpResponseProxylet {
  
    /**
     * Extra option for the return code of method accept.
     * The proxylet is interested in the headers only and not in the body.
     * <br/>ex:
     * <br/>return ACCEPT & IGNORE_BODY
     * <br/>return ACCEPT_MAY_BLOCK & IGNORE_BODY
     */
    public static final int IGNORE_BODY = 0x05;

    /**
     * The proxy will call the same proxylet again.
     */
    public static final int SAME_PROXYLET = 9998;
    /**
     * The proxy will call the first proxylet.
     */
    public static final int FIRST_PROXYLET = 9999;
    /**
     * The proxy will call the next proxylet.
     */
    public static final int NEXT_PROXYLET = 10000;
    /**
     * The proxy will not call any other proxylet for the message.
     */
    public static final int LAST_PROXYLET = 10001;
    /**
     * The proxy will perform a redirect and the new request will go through the request proxylets.
     */
    public static final int REDIRECT_FIRST_PROXYLET = 10002;
    /**
     * The proxy will perform a redirect and the new request will NOT go through the request proxylets.
     */
    public static final int REDIRECT_LAST_PROXYLET = 10003;
    /**
     * The proxy will ignore the next body parts and the response will go through the response proxylets.
     */
    public static final int RESPOND_FIRST_PROXYLET = 10004;
    /**
     * The proxy will ignore the next body parts and the response will go to the following response proxylets.
     */
    public static final int RESPOND_NEXT_PROXYLET = 10005;
    /**
     * The proxy will ignore the next body parts and the response will NOT go through the response proxylets.
     */
    public static final int RESPOND_LAST_PROXYLET = 10006;
    /**
     * The Engine will suspend to processing of the current proxylet chain.
     */
    public static final int SUSPEND = 10007;
  
    /**
     * Processes the headers of a response.
     * @param prolog     the response prolog
     * @param headers     the response headers
     * @return SAME_PROXYLET, FIRST_PROXYLET, NEXT_PROXYLET, LAST_PROXYLET, REDIRECT_FIRST_PROXYLET, REDIRECT_LAST_PROXYLET, RESPOND_FIRST_PROXYLET, RESPOND_NEXT_PROXYLET or RESPOND_LAST_PROXYLET
     * @throws ProxyletException if any problem occurs while processing the headers
     */
    public int doResponseHeaders(HttpResponseProlog prolog, HttpHeaders headers)
	throws ProxyletException;
    
    /**
     * Processes the body of a response.
     * <br/>This method may be called serveral times for a single response.
     * @param body        the response body
     * @param isLastChunk specifies if more chunks of body follow
     * @throws ProxyletException if any problem occurs while processing the headers
     */
    public void doResponseBody(HttpBody body, boolean isLastChunk)
	throws ProxyletException;
    
}
