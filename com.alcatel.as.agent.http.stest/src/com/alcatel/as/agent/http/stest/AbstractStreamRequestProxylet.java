// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.agent.http.stest;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.HttpBody;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequestProlog;
import com.nextenso.proxylet.http.StreamedHttpRequestProxylet;

public abstract class AbstractStreamRequestProxylet implements StreamedHttpRequestProxylet {
	Logger _log = Logger.getLogger(getClass());

	@Override
	public int accept(HttpRequestProlog prolog, HttpHeaders headers) {
		return ACCEPT_MAY_BLOCK;
	}

	@Override
	public void destroy() {

	}

	@Override
	public String getProxyletInfo() {
		return "Test HTTP Proxylet - Suspend";
	}

	@Override
	public void init(ProxyletConfig arg0) throws ProxyletException {
	}

	public void doRequestBody(HttpBody body, boolean isLastChunk) throws ProxyletException {
	}
}
