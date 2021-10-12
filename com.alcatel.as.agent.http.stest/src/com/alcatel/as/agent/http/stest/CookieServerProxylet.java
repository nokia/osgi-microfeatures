// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.agent.http.stest;

import org.apache.felix.dm.annotation.api.Component;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.BufferedHttpRequestProxylet;
import com.nextenso.proxylet.http.HttpCookie;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpRequestProlog;
import com.nextenso.proxylet.http.HttpResponse;

/**
 * See stest/test-server-session/stest and stest/test-server-session-v2 tests.
 */
@Component
public class CookieServerProxylet implements BufferedHttpRequestProxylet  {
	@Override
	public int accept(HttpRequestProlog arg0, HttpHeaders arg1) {
		return ACCEPT;
	}

	@Override
	public void destroy() {		
	}

	@Override
	public String getProxyletInfo() {
		return "Test Http Proxylet";
	}

	@Override
	public void init(ProxyletConfig arg0) throws ProxyletException {		
	}

	@Override
	public int doRequest(HttpRequest req) throws ProxyletException {
        HttpResponse rsp = req.getResponse();
        rsp.getProlog().setStatus(200);
        rsp.getHeaders().setHeader("Content-Type", "text/plain");
        
        // TODO add cookie
        
        HttpCookie cookie = req.getHeaders().getCookie("JSESSIONID");
        if (cookie == null) {
        	// send new cookie with id = id1
        	cookie = new HttpCookie("JSESSIONID", "id1");
        	rsp.getHeaders().addCookie(cookie);
        } else {
        	if (cookie != null && cookie.getValue().equals("id1")) {
        		String counter = req.getHeaders().getHeader("X-Counter");
        		if ("3".equals(counter)) {
                	cookie = new HttpCookie("JSESSIONID", "id2");
                	rsp.getHeaders().addCookie(cookie);
        		}
        	}
        }
        
		return RESPOND_FIRST_PROXYLET;
	}
}
