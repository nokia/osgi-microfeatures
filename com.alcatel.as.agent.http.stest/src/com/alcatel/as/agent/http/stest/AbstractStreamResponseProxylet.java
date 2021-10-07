package com.alcatel.as.agent.http.stest;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.HttpBody;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpResponseProlog;
import com.nextenso.proxylet.http.StreamedHttpResponseProxylet;

public abstract class AbstractStreamResponseProxylet implements StreamedHttpResponseProxylet {
	Logger _log = Logger.getLogger(getClass());

	@Override
	public int accept(HttpResponseProlog prolog, HttpHeaders headers) {
		return ACCEPT_MAY_BLOCK;
	}

	@Override
	public void destroy() {
	}

	@Override
	public String getProxyletInfo() {
		return null;
	}

	@Override
	public void init(ProxyletConfig arg0) throws ProxyletException {
	}

	@Override
	public void doResponseBody(HttpBody body, boolean isLastChunk) throws ProxyletException {
	}    
}
