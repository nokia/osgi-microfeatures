// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.scenarii.impl;

import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;

public class BasicHttpRequest implements HttpRequest {

	private final String method;
	private final URI uri;
	private final InetAddress address;
	private final InetAddress localAddress;
	private final Map<String, String> headers;
	private final String query;
	private final String contentType;
	private final String body;
	private final boolean proxyRequest;
	
	String preamble;
	
	public BasicHttpRequest(String method, URI uri, InetAddress address, InetAddress localAddress,
			Map<String, String> headers, String queryString, String contentType, String body, boolean proxyRequest) {
		super();
		this.method = method;
		this.uri = uri;
		this.address = address;
		this.localAddress = localAddress;
		this.headers = headers;
		this.query = queryString;
		this.contentType = contentType;
		this.body = body;
		this.proxyRequest = proxyRequest;
		
		if(method == null) {
			throw new IllegalArgumentException("no method provided");
		}
		
		if(uri == null) {
			throw new IllegalArgumentException("no uri provided");
		}
		
		this.preamble = preparePreamble();
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public URI getURI() {
		// TODO Auto-generated method stub
		return uri;
	}

	@Override
	public String getURL() {
		return uri.toString();
	}

	@Override
	public InetAddress getAddress() {
		return address;
	}

	@Override
	public InetAddress getLocalAddress() {
		return localAddress;
	}

	@Override
	public Map<String, String> getHeaders() {
		return null;
	}

	@Override
	public String getBody() {
		return body;
	}

	@Override
	public String getQueryString() {
		return query;
	}
	

	@Override
	public String getContentType() {
		return contentType;
	}
	
	@Override	
	public ByteBuffer toBytes() {
		return ByteBuffer.wrap(toString().getBytes());
	}

	@Override
	public boolean isProxyRequest() {
		return proxyRequest;
	}
	
	private String preparePreamble() {
		StringBuilder builder = new StringBuilder();
		
		builder.append(getMethod()).append(" ");
		
		if(isProxyRequest()) {
			builder.append(getURI().getScheme())
				.append("://")
				.append(getURI().getAuthority())
				.append(getURI().getPath());
		} else {
			builder.append(getURI().getPath().toString());
		}
		
		if(getURI().getQuery() != null) {
			builder.append("?").append(getURI().getQuery());
		}
		
		builder.append(" HTTP/1.1\r\n");
		
//		if(!isProxyRequest()) {
//			builder.append("Host: ").append(uri.getHost()).append("\r\n");
//		}
		if(!isProxyRequest()) {
			builder.append("Host: ").append(uri.getHost());
			if (uri.getPort() != -1) builder.append(":").append(uri.getPort());
            builder.append("\r\n");
        }
		
		for(Map.Entry<String, String> e : headers.entrySet()) {
			builder.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
		}
		if(getContentType() != null && getBody() != null) {
			builder.append("Content-Type: ").append(getContentType()).append("\r\n");
		}
		return builder.toString();
	}
	
	@Override
	public String toString(StringBuilder sb) {
		sb.append(preamble);
		
		if(getContentType() != null && getBody() != null) {
			sb.append("Content-Length: " + getBody().getBytes().length).append("\r\n");
			sb.append("\r\n");
			sb.append(getBody());
		} else {
			sb.append("\r\n");
		}
		
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return toString(new StringBuilder());
	}

	@Override
	public ByteBuffer toBytes(StringBuilder sb) {
		return ByteBuffer.wrap(toString(sb).getBytes());
	}

	@Override
	public ByteBuffer toBytes(StringBuilder sb, Map<String, String> template) {
		return ByteBuffer.wrap(toString(sb, template).getBytes());
	}

	@Override
	public String toString(Map<String, String> template) {
		return toString(new StringBuilder(), template);
	}

	@Override
	public String toString(StringBuilder sb, Map<String, String> template) {
		String preambleTemplated = preamble;
		String bodyTemplated = body;
		for(Map.Entry<String, String> entry : template.entrySet()) {
			preambleTemplated = preambleTemplated.replace(entry.getKey(), entry.getValue());
			
			if(getBody() != null) {
				bodyTemplated = bodyTemplated.replace(entry.getKey(), entry.getValue());
			}
		}
		
		sb.append(preambleTemplated);

		if(getContentType() != null && getBody() != null) {
			sb.append("Content-Length: " + bodyTemplated.getBytes().length).append("\r\n");
			sb.append("\r\n");

			sb.append(bodyTemplated);
		} else {
			sb.append("\r\n");
		}
		
		return sb.toString();
	}

}
