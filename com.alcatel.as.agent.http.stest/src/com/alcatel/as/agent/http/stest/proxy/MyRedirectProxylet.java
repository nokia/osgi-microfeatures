// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.agent.http.stest.proxy;

import java.net.MalformedURLException;
import java.util.concurrent.CompletableFuture;

import org.apache.felix.dm.annotation.api.Component;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.http.HttpBody;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequestProlog;
import com.nextenso.proxylet.http.HttpURL;
import com.nextenso.proxylet.http.StreamedHttpRequestProxylet;

@Component
public class MyRedirectProxylet implements StreamedHttpRequestProxylet {

	static final String REDIRECT_MARKER_ATTR = "redirected";

	@Override
	public int accept(HttpRequestProlog arg0, HttpHeaders arg1) {
		return ACCEPT;
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

	@Override
	public int doRequestHeaders(HttpRequestProlog prolog, HttpHeaders headers) throws ProxyletException {
		final Boolean redirected = (Boolean) headers.getRequest().getAttribute(REDIRECT_MARKER_ATTR);
		if(headers.getHeader("X-test-CSFS27968") != null) {
	    System.out.println("REDIRECT SUSPEND" + redirected);

	    CompletableFuture.runAsync(() -> {
	      try {
	        Thread.sleep(1_500L);
	      } catch (InterruptedException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
	      }
	      if(redirected != null && redirected) {
	        headers.addHeader("X-has-redirect", "test");

	        HttpURL target = prolog.getURL();
	        try {
	          target.setPort(8080);
	        } catch (MalformedURLException e) {
	          System.out.println("error " + e);
	        }
	        prolog.setURL(target);
	      } else {
	        headers.addHeader("X-alexa", "play-despacito");
	        HttpURL target = prolog.getURL();
	        try {
	          target.setPort(8081);
	        } catch (MalformedURLException e) {
	          System.out.println("error " + e);
	        }
	      }
	      
	      prolog.getRequest().resume(NEXT_PROXYLET);      
	    });

	    return SUSPEND;
		} else {
		   if(redirected != null && redirected) {
		      headers.addHeader("X-has-redirect", "test");

		      HttpURL target = prolog.getURL();
		      try {
		        target.setPort(8080);
		      } catch (MalformedURLException e) {
		        throw new ProxyletException(e);
		      }
		      prolog.setURL(target);
		    } else {
		      headers.addHeader("X-alexa", "play-despacito");
		      HttpURL target = prolog.getURL();
		      try {
		        target.setPort(8081);
		      } catch (MalformedURLException e) {
		        throw new ProxyletException(e);
		      }
		    }
		    
		    return NEXT_PROXYLET; 
		}

	}

	@Override
	public void doRequestBody(HttpBody body, boolean isLastChunk) throws ProxyletException {
		// TODO Auto-generated method stub

	}

}
