// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.scenarii;

import org.junit.Test;

import com.nokia.as.gpto.scenarii.impl.BasicRequestBuilder;
import com.nokia.as.gpto.scenarii.impl.HttpRequest;

import java.util.Collections;

import org.junit.Assert;
public class HttpClientTest {

	@Test
	public void testGETRequest() {
		HttpRequest req = new BasicRequestBuilder()
					.setMethod("GET")
					.setUrl("http://www.example.com/one/two")
					.setProxyRequest(false)
					.build();
		
		Assert.assertNotNull(req.toBytes());
		Assert.assertEquals(
				"GET /one/two HTTP/1.1\r\n" +
				"Host: www.example.com\r\n" + 
				"\r\n", req.toString());
		
		HttpRequest reqProxied = new BasicRequestBuilder()
				.setMethod("GET")
				.setUrl("http://www.example.com/one/two")
				.setProxyRequest(true)
				.build();
		
		Assert.assertNotNull(reqProxied.toBytes());
		Assert.assertEquals(
				"GET http://www.example.com/one/two HTTP/1.1\r\n" +
				"\r\n", reqProxied.toString());
	}
	
	@Test
	public void testTemplatedRequest() {
		HttpRequest req = new BasicRequestBuilder()
				.setMethod("GET")
				.setUrl("http://www.example.com/one/two")
				.setProxyRequest(false)
				.putHeader("X-test", "$test$")
				.setBody("Hello $test$")
				.setContentType("text/plain")
				.build();
		
		String reqStr = req.toString(Collections.singletonMap("$test$", "aaa"));
		
		Assert.assertEquals(				
				"GET /one/two HTTP/1.1\r\n" +
				"Host: www.example.com\r\n" +
				"X-test: aaa\r\n" +
				"Content-Type: text/plain\r\n" +
				"Content-Length: 9\r\n" + 
				"\r\n" + 
				"Hello aaa", reqStr);

		reqStr = req.toString(Collections.singletonMap("$test$", "a"));
		
		Assert.assertEquals(				
				"GET /one/two HTTP/1.1\r\n" +
				"Host: www.example.com\r\n" +
				"X-test: a\r\n" +
				"Content-Type: text/plain\r\n" +
				"Content-Length: 7\r\n" + 
				"\r\n" + 
				"Hello a", reqStr);
	}

}
