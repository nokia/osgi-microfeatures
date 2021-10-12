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
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpRequestProlog;
import com.nextenso.proxylet.http.HttpResponse;

/**
 * See stest/test-server-proxylet system test.
 */
@Component
public class ServerProxylet implements BufferedHttpRequestProxylet  {
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
        rsp.getBody().setContent("ServerProxylet");
        rsp.setContentLength();
		return RESPOND_FIRST_PROXYLET;
	}
}
