package com.alcatel.as.agent.http.stest;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.BufferedHttpResponseProxylet;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpResponseProlog;

public abstract class AbstractResponseProxylet implements BufferedHttpResponseProxylet {
	Logger _log = Logger.getLogger(getClass());

	@Override
	public int accept(HttpResponseProlog prolog, HttpHeaders headers) {
		return ACCEPT_MAY_BLOCK;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getProxyletInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(ProxyletConfig arg0) throws ProxyletException {
		// TODO Auto-generated method stub
		
	}
}
