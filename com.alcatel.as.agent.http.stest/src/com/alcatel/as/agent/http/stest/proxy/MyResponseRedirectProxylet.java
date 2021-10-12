// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.agent.http.stest.proxy;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.log4j.Logger;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.HttpBody;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpResponse;
import com.nextenso.proxylet.http.HttpResponseProlog;
import com.nextenso.proxylet.http.StreamedHttpResponseProxylet;

@Component
public class MyResponseRedirectProxylet implements StreamedHttpResponseProxylet {
	static final String REDIRECT_MARKER_ATTR = "redirected";
	private final static Logger LOG = Logger.getLogger("agent.http.stest");

	@Override
	public void destroy() {
	}

	@Override
	public String getProxyletInfo() {
		return "Test Http Response Proxylet";
	}

	@Override
	public void init(ProxyletConfig arg0) throws ProxyletException {
	}

	@Override
	public int accept(HttpResponseProlog prolog, HttpHeaders headers) {
		return ACCEPT;
	}

	@Override
	public int doResponseHeaders(HttpResponseProlog prolog, HttpHeaders headers) throws ProxyletException {
		Boolean redirected = (Boolean) headers.getRequest().getAttribute(REDIRECT_MARKER_ATTR);
		Object error = prolog.getResponse().getAttribute(HttpResponse.ERROR_REASON_ATTR);
		LOG.warn("HAS_ERROR: " +  (error == null ? "false" : "true"));
		if(error != null) LOG.warn(error.getClass());

		if(redirected == null || !redirected) {
			headers.getRequest().setAttribute(REDIRECT_MARKER_ATTR, true);
			return REDIRECT_FIRST_PROXYLET;

		}
		
		headers.setHeader("X-Has-Error", error == null ? "false" : "true");
		headers.addHeader("X-Hello", "World");
		headers.addHeader("X-Processed-By", Thread.currentThread().getName());

		return NEXT_PROXYLET;
	}

	@Override
	public void doResponseBody(HttpBody body, boolean isLastChunk) throws ProxyletException {
		// TODO Auto-generated method stub

	}

}
