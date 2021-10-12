// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.okhttp.impl;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;

import com.alcatel.as.service.metering2.MeteringService;
import com.nokia.as.gpto.agent.api.ExecutionContext;
import com.nokia.as.gpto.agent.api.Scenario;
import com.nokia.as.gpto.agent.api.SessionContext;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OkHttpScenario implements Scenario {
	private static final Logger LOG = Logger.getLogger(OkHttpScenario.class);
	MeteringService ms;
	HttpClientMetering httpMeters;
	ExecutionContext ectx;
	Request request;
	private HttpClientConfig httpConfig;
	
	@Component(provides = Scenario.Factory.class, properties = {
			@Property(name = Scenario.Factory.PROP_NAME, value = "okhttp_scenario") })
	public static class Factory implements Scenario.Factory {
		@ServiceDependency(required = true)
		MeteringService meteringService;
		
		@Override
		public Scenario instanciate(ExecutionContext ectx) {
			return new OkHttpScenario(meteringService, ectx);
		}

		@Override
		public String help() {
			return null;
		}
	
	}
	
	public OkHttpScenario(MeteringService ms, ExecutionContext ectx) {
		this.ms = ms;
		this.ectx = ectx;
		httpConfig = new HttpClientConfig(ectx.getOpts());
		
		Request.Builder b = new Request.Builder();
		try {
			b.url(httpConfig.getURL().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException("malformed url");
		}
		
		RequestBody body;
		switch(httpConfig.getMethodType()) {
		case "GET":
			b.get();
			break;
		case "POST": 
			body = RequestBody.create(MediaType.parse(httpConfig.getContentType()), httpConfig.getBody());
			b.post(body);
			break;
		case "PUT": 
			body = RequestBody.create(MediaType.parse(httpConfig.getContentType()), httpConfig.getBody());
			b.put(body);
			break;
		case "DELETE": 
			body = RequestBody.create(MediaType.parse(httpConfig.getContentType()), httpConfig.getBody());
			b.delete(body);
			break;
		}
		
		request = b.build();
	}
	
	@Override
	public CompletableFuture<Boolean> init() {
		httpMeters = new HttpClientMetering(ms, ectx.getMonitorable(), ectx.getExecutionId());
		httpMeters.createIncrementalMeter("gpto.execution." + ectx.getExecutionId(), null);
		httpMeters.init();

		return CompletableFuture.completedFuture(true);
	}

	@Override
	public CompletableFuture<Boolean> beginSession(SessionContext sctx) {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.eventListenerFactory(new OkHttpListener.Factory(httpMeters));
		
		if(httpConfig.getProxyHost() != null && httpConfig.getProxyPort() != null) {
			InetSocketAddress addr = new InetSocketAddress(
					httpConfig.getProxyHost(), 
					httpConfig.getProxyPort());
			builder.proxy(new Proxy(Type.HTTP, addr));
			LOG.info("Proxy configured");
		}
		
		if(httpConfig.isHttp2PriorKnowledge()) {
			builder.protocols(Arrays.asList(Protocol.H2_PRIOR_KNOWLEDGE));
		}
		
		if(httpConfig.getHttps()) {
			try {
				initSecureClient(builder);
			} catch (Exception e) {
				throw new RuntimeException("failed to init secure client", e);
			}
		}
		
		sctx.attach(builder.build());
		return CompletableFuture.completedFuture(true);
	}

	private void initSecureClient(Builder builder) throws Exception {
		KeyStore keyStore = httpConfig.readKeyStore(); //your method to obtain KeyStore
		SSLContext sslContext = SSLContext.getInstance("SSL");
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(keyStore);
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, httpConfig.getKsPassword().toCharArray());
		sslContext.init(keyManagerFactory.getKeyManagers(),trustManagerFactory.getTrustManagers(), new SecureRandom());
		
	    TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
	    if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
	      throw new IllegalStateException("Unexpected default trust managers:"
	          + Arrays.toString(trustManagers));
	    }		

	    //not very good, risk of MITM attack?
	    builder.hostnameVerifier(new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});
	    
		builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0]);
	}

	@Override
	public CompletableFuture<Boolean> run(SessionContext sctx) {
		OkHttpClient client = sctx.attachment();
		Call c = client.newCall(request);
		OkCallbackFuture cb = new OkCallbackFuture(httpMeters);
		c.enqueue(cb);
		return cb;
	}

	@Override
	public CompletableFuture<Void> endSession(SessionContext sctx) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> dispose() {
		return CompletableFuture.completedFuture(null);
	}

}
