package com.nextenso.proxylet.http;

import com.nextenso.proxylet.ProxyletException;

/**
 * A BufferedHttpResponseProxylet is an HttpResponseProxylet that needs a full response to perform its job.
 * <p/>It implies buffering the whole body prior to calling the proxylet.
 */
public interface BufferedHttpResponseProxylet extends HttpResponseProxylet {

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
     * The Engine will perform a redirect and the new request will go through the request proxylets.
     */
    public static final int REDIRECT_FIRST_PROXYLET = 1003;
    /**
     * The proxy will perform a redirect and the new request will NOT go through the request proxylets.
     */
    public static final int REDIRECT_LAST_PROXYLET = 1004;
    /**
     * The Engine will suspend to processing of the current proxylet chain.
     */
    public static final int SUSPEND = 1005;

    /**
     * Processes the response.
     * <br/>Returns one of the predefined codes to specify what the Engine should do next with the response.
     * @param response the response to process
     * @return SAME_PROXYLET, FIRST_PROXYLET, NEXT_PROXYLET, LAST_PROXYLET, REDIRECT_FIRST_PROXYLET or REDIRECT_LAST_PROXYLET
     * @throws ProxyletException if any problem occurs while processing the response
     */
    public int doResponse(HttpResponse response)
	throws ProxyletException;
    
}
