package com.nokia.as.gpto.scenarii.impl;

import java.net.URI;
import java.util.Map;

public interface RequestBuilder<T extends RequestBuilder<T>> {

	T setMethod(String string);

	T setUrl(String url);

	T setUri(URI uri);

	T putHeader(String key, String value);
	
	T setHeaders(Map<String, String> headers);

	T setQueryString(String as);
	
	T setContentType(String contentType);
	
	T setBody(String body);
	
	T setProxyRequest(boolean proxy);

	HttpRequest build();

}