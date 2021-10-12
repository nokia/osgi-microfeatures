// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.agent.http.stest;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.apache.felix.dm.annotation.api.Component;

import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.BufferedHttpRequestProxylet;
import com.nextenso.proxylet.http.BufferedHttpResponseProxylet;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpResponse;
import com.nextenso.proxylet.http.HttpURL;

/**
 * unreachable request is sent. the response proxylet changes the URL to the
 * correct one, and redirect with a respond_last status.
 * This test is not run by default because redirect async proxylets are not yet supported.
 */
public class AsyncBufferedRedirectRespondLast {
	
	@Component
	public static class Proxylet1 extends AbstractRequetProxylet implements BufferedHttpRequestProxylet {
		volatile int _requestNumber;

		@Override
		public int doRequest(HttpRequest req) throws ProxyletException {
			_requestNumber++;
			new Thread(() -> {
				_log.warn("received request: count=" + _requestNumber);
				req.getHeaders().setHeader("Test", String.valueOf(_requestNumber));
				req.resume(NEXT_PROXYLET);
			}).start();
			return SUSPEND;
		}
	}
	
	@Component
	public static class Proxylet2 extends AbstractRequetProxylet implements BufferedHttpRequestProxylet {
		@Override
		public int doRequest(HttpRequest req) throws ProxyletException {
			return NEXT_PROXYLET;
		}
	}
	

	@Component
	public static class Proxylet3 extends AbstractResponseProxylet implements BufferedHttpResponseProxylet {
		@Override
		public int doResponse(HttpResponse response) throws ProxyletException {
			new Thread(() -> _doResponse(response)).start();
			return SUSPEND;
		}
		
		void _doResponse(HttpResponse response) {			
			try {
				int status = response.getProlog().getStatus();
				String responseBody = response.getBody().getContentAsString();
				_log.warn("response status=" + status + ", body:" + responseBody);
				if (status != 200) {
					try {
						response.getHeaders().setHeader("Response-Header", "Foo");
						response.getBody().setContent("Foo");
						response.getRequest().getProlog().setURL(new HttpURL("http://localhost:8080/services/helloworld/get"));
					} catch (MalformedURLException e) {
						_log.warn("exception", e);
					}
					response.resume(REDIRECT_LAST_PROXYLET);
				} else if (status == 200) {
					int requestCount = Integer.parseInt(response.getRequest().getHeaders().getHeader("Test"));
					if (requestCount != 1) {
						throw new IllegalStateException("request count not equals to 1: " +  requestCount);
					}
					if (response.getHeaders().getHeader("Response-Header") != null) {
						throw new IllegalStateException("response contains wrong header: Response-Header");
					}
					if (response.getBody().getContentAsString().indexOf("Foo") != -1) {
						throw new IllegalStateException("response contains wrong body");
					}
				}
			} catch (UnsupportedEncodingException e) {
			}

			response.resume(NEXT_PROXYLET);
		}
	}

}
