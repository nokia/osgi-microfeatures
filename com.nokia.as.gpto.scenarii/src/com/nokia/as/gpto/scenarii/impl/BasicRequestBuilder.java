package com.nokia.as.gpto.scenarii.impl;

import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class BasicRequestBuilder implements RequestBuilder<BasicRequestBuilder> {
	protected String method;
	protected URI uri;
	protected InetAddress localAddress;
	protected InetAddress remoteAddress;
	protected Map<String, String> headers;
	protected String queryString;
	protected String body;
	protected String contentType;
	protected boolean proxyRequest;
  
	public BasicRequestBuilder() {
		headers = new HashMap<>();
	}
	
	/* (non-Javadoc)
	 * @see com.nokia.as.gpto.scenarii.RequestBuilder#setMethod(java.lang.String)
	 */
	@Override
	public BasicRequestBuilder setMethod(String string) {
		this.method = string;
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.nokia.as.gpto.scenarii.RequestBuilder#setUrl(java.lang.String)
	 */
	@Override
	public BasicRequestBuilder setUrl(String url) {
		this.uri = URI.create(url);
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.nokia.as.gpto.scenarii.RequestBuilder#setUri(java.net.URI)
	 */
	@Override
	public BasicRequestBuilder setUri(URI uri) {
		this.uri = uri;
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.nokia.as.gpto.scenarii.RequestBuilder#setHeader(java.lang.String, java.lang.String)
	 */
	@Override
	public BasicRequestBuilder putHeader(String key, String value) {
		headers.put(key, value);
		return this;
	}
	
	public BasicRequestBuilder setHeaders(Map<String, String> headers) {
		this.headers.putAll(headers);
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.nokia.as.gpto.scenarii.RequestBuilder#setQueryParam(java.lang.String, java.lang.String)
	 */
	@Override
	public BasicRequestBuilder setQueryString(String queryString) {
		this.queryString = queryString;
		return this;
	}

	@Override
	public BasicRequestBuilder setContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	@Override
	public BasicRequestBuilder setBody(String body) {
		this.body = body;
		return this;
	}

	@Override
	public BasicRequestBuilder setProxyRequest(boolean proxy) {
		this.proxyRequest = proxy;
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.nokia.as.gpto.scenarii.RequestBuilder#build()
	 */
	@Override
	public HttpRequest build() {
		return new BasicHttpRequest(method, 
				uri, 
				remoteAddress, 
				localAddress, 
				headers,
				queryString, 
				contentType, 
				body,
				proxyRequest);
	}
}
