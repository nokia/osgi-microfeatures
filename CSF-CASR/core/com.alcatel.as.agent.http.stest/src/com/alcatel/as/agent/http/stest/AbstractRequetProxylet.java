package com.alcatel.as.agent.http.stest;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.BufferedHttpRequestProxylet;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequestProlog;

public abstract class AbstractRequetProxylet implements BufferedHttpRequestProxylet {
	Logger _log = Logger.getLogger(getClass());

	@Override
	public int accept(HttpRequestProlog arg0, HttpHeaders arg1) {
		return ACCEPT_MAY_BLOCK;
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
}

