package com.alcatel.as.agent.http.stest;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.apache.felix.dm.annotation.api.Component;

import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.HttpBody;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequestProlog;
import com.nextenso.proxylet.http.HttpResponseProlog;
import com.nextenso.proxylet.http.HttpURL;
import com.nextenso.proxylet.http.StreamedHttpRequestProxylet;
import com.nextenso.proxylet.http.StreamedHttpResponseProxylet;

/**
 * unreachable request is sent. the response proxylet changes the URL to the
 * correct one, and redirect with a respond_last status.
 * 
 * See stest/test-stream-redirect-respond-first
 */
public class StreamedRedirectRespondFirst {
	static volatile int _requestNumber;
	static volatile int _responseBodyNumber;

	@Component
	public static class Proxylet1 extends AbstractStreamRequestProxylet implements StreamedHttpRequestProxylet {
		@Override
		public int accept(HttpRequestProlog prolog, HttpHeaders headers) {
			return ACCEPT_MAY_BLOCK;
		}
		@Override
		public int doRequestHeaders(HttpRequestProlog prolog, HttpHeaders headers) throws ProxyletException {
			_requestNumber++;
			new Thread(() -> {
				_log.warn("received request: count=" + _requestNumber);
				headers.setHeader("Test", String.valueOf(_requestNumber));
				headers.getRequest().resume(NEXT_PROXYLET);
			}).start();
			return SUSPEND;
		}
	}

	@Component
	public static class Proxylet2 extends AbstractStreamRequestProxylet implements StreamedHttpRequestProxylet {
		@Override
		public int accept(HttpRequestProlog prolog, HttpHeaders headers) {
			return ACCEPT_MAY_BLOCK;
		}
		@Override
		public int doRequestHeaders(HttpRequestProlog prolog, HttpHeaders headers) throws ProxyletException {
			return NEXT_PROXYLET;
		}
	}

	@Component
	public static class Proxylet3 extends AbstractStreamResponseProxylet implements StreamedHttpResponseProxylet {
		@Override
		public int accept(HttpResponseProlog prolog, HttpHeaders headers) {
			return ACCEPT_MAY_BLOCK;
		}

		@Override
		public int doResponseHeaders(HttpResponseProlog prolog, HttpHeaders headers) throws ProxyletException {
			int status = prolog.getStatus();
			if (status != 200 || _requestNumber == 1) {
				try {
					headers.setHeader("Response-Header", "headers-to-be-removed");
					prolog.getRequest().getProlog()
							.setURL(new HttpURL("http://localhost:8080/services/helloworld/get"));
				} catch (MalformedURLException e) {
					_log.warn("exception", e);
				}
				return REDIRECT_FIRST_PROXYLET;
			} else if (_requestNumber == 2 && status == 200) {
				int requestCount = Integer.parseInt(prolog.getRequest().getHeaders().getHeader("Test"));
				if (requestCount != 2) {
					throw new IllegalStateException("request count not equals to 2: " + requestCount);
				}
				if (headers.getHeader("Response-Header") != null) {
					throw new IllegalStateException("response contains wrong header: Response-Header");
				}
				_requestNumber = 0;
				_responseBodyNumber = 0;
			}

			return NEXT_PROXYLET;
		}

		@Override
		public void doResponseBody(HttpBody body, boolean isLastChunk) throws ProxyletException {
			try {
				_log.warn("response body: " + body.getContentAsString());
				_responseBodyNumber++;
				if (_responseBodyNumber < 2) {
					body.setContent("Foo");
				} else {
					if (body.getContentAsString().indexOf("Foo") != -1) {
						throw new IllegalStateException("response contains wrong body");
					}
					_requestNumber = 0;
					_responseBodyNumber = 0;
				}
			} catch (UnsupportedEncodingException e) {
				throw new ProxyletException("doResponseBody failed", e);
			}
		}
	}

}
