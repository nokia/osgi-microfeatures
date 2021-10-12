// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

import com.nextenso.proxylet.ProxyletException;

/**
 * A BufferedHttpRequestProxylet is an HttpRequestProxylet that needs a full request to perform its job.
 * <p/>It implies buffering the whole body prior to calling the proxylet.
 */
public interface BufferedHttpRequestProxylet extends HttpRequestProxylet {

    /**
     * The proxy will call the same proxylet again.
     */
    public static final int SAME_PROXYLET = 9999;
    /**
     * The Engine will return to the first proxylet.
     */
    public static final int FIRST_PROXYLET = 10000;
    /**
     * The Engine will call the next proxylet.
     */
    public static final int NEXT_PROXYLET = 10001;
    /**
     * The Engine will not call any other proxylet.
     */
    public static final int LAST_PROXYLET = 1002;
    /**
     * The Engine will respond to the client and the response will go through the response proxylets.
     */
    public static final int RESPOND_FIRST_PROXYLET = 1003;
    /**
     * The Engine will respond to the client and the response will NOT go through the response proxylets.
     */
    public static final int RESPOND_LAST_PROXYLET = 1004;
  
    /**
     * The Engine will suspend to processing of the current proxylet chain.
     */
    public static final int SUSPEND = 1005;
  
    /**
     * Processes the request.
     * <br/>Returns one of the predefined codes to specify what the Engine should do next with the request.
     * @param request the request to process
     * @return SAME_PROXYLET, FIRST_PROXYLET, NEXT_PROXYLET, LAST_PROXYLET, RESPOND_FIRST_PROXYLET or RESPOND_LAST_PROXYLET
     * @throws ProxyletException if any problem occurs while processing the request
     */
    public int doRequest(HttpRequest request)
	throws ProxyletException;
    
}
