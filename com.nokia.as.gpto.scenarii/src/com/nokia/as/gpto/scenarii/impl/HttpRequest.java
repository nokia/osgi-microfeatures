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

public interface HttpRequest {
	String getMethod();
	URI getURI();
	String getURL();
	boolean isProxyRequest();
	InetAddress getAddress();
	InetAddress getLocalAddress();
	Map<String, String> getHeaders();
	String getBody();
	String getContentType();
	String getQueryString();
	ByteBuffer toBytes();
	ByteBuffer toBytes(StringBuilder sb);
	ByteBuffer toBytes(StringBuilder sb, Map<String, String> template);
	String toString(Map<String, String> template);
	String toString(StringBuilder sb, Map<String, String> template);
	String toString(StringBuilder sb);

}
