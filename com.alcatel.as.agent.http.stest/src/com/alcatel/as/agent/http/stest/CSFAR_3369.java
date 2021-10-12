// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.agent.http.stest;

import java.io.UnsupportedEncodingException;

import org.apache.felix.dm.annotation.api.Component;

import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.BufferedHttpRequestProxylet;
import com.nextenso.proxylet.http.BufferedHttpResponseProxylet;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpResponse;

/**
 * See stest/test-csfar-3369 system test.
 */
public class CSFAR_3369 {
	static volatile int _requestNumber;

	@Component
	public static class RequestProxylet1 extends AbstractRequetProxylet implements BufferedHttpRequestProxylet {
		
		@Override
		public int doRequest(HttpRequest req) throws ProxyletException {
			_requestNumber++;

			HttpResponse rsp = req.getResponse();

			new Thread(() -> {
				try {
					Thread.sleep(1000);
					rsp.getProlog().setStatus(200);
					rsp.getHeaders().setHeader("Content-Type", "text/plain");
					if (_requestNumber == 1) {
						_log.warn("setting body1");
						rsp.getBody().setContent("body1");
						rsp.setContentLength();
					} else if (_requestNumber == 2) {
						_log.warn("setting body2");
						rsp.getBody().setContent("body2");
						rsp.setContentLength();
					} else {
						_log.warn("wrong state");
					}
					req.resume(NEXT_PROXYLET);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}).start();
			return SUSPEND;
		}
	}
	
	@Component
	public static class RequestProxylet2 extends AbstractRequetProxylet implements BufferedHttpRequestProxylet  {
		@Override
		public int doRequest(HttpRequest req) throws ProxyletException {
	        return RESPOND_FIRST_PROXYLET;
		}
	}

	@Component
	public static class ResponseProxylet extends AbstractResponseProxylet implements BufferedHttpResponseProxylet {
		@Override
		public int doResponse(HttpResponse response) throws ProxyletException {
			// check if the async request proxylet has set the response body

			try {
				String responseBody = response.getBody().getContentAsString();
				_log.warn("body1 -> redirect");
				if ("body1".equals(responseBody)) {
					return REDIRECT_FIRST_PROXYLET;
				} else if ("body2".equals(responseBody)) {
					_log.warn("body2 -> returning response");
					_requestNumber = 0;
				} else {
					throw new IllegalArgumentException("wrong response: " + responseBody);
				}
			} catch (UnsupportedEncodingException e) {
			}

			return NEXT_PROXYLET;
		}
	}

}
