// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.okhttp.impl;

import java.io.IOException;

import org.apache.log4j.Logger;

import okhttp3.Call;
import okhttp3.EventListener;

public class OkHttpListener extends EventListener {
	private static final Logger LOG = Logger.getLogger(OkHttpListener.class);

	private HttpClientMetering meters; 
	private long queryStart;
	
	public static class Factory implements EventListener.Factory {

		private HttpClientMetering meters;
		public Factory(HttpClientMetering meters) {
			this.meters = meters;
		}
		
		@Override
		public EventListener create(Call arg0) {
			return new OkHttpListener(meters);
		}
		
	}
	
	public OkHttpListener(HttpClientMetering meters) {
		this.meters = meters;
	}
	
	public void callStart(Call call) {
		switch(call.request().method()) {
		case "GET":
			meters.getRequestHttpSendMeterGET().inc(1);
			break;
		case "POST":
			meters.getRequestHttpSendMeterPOST().inc(1);
			break;
		case "PUT":
			meters.getRequestHttpSendMeterPUT().inc(1);
			break;
		case "DELETE":
			meters.getRequestHttpSendMeterDELETE().inc(1);
			break;
		}

		queryStart = System.currentTimeMillis();
	}
	
	public void callEnd(Call call) {
		long duration = System.currentTimeMillis() - queryStart;

		meters.getTotalResponseLatency().inc(duration);
		meters.getLastResponseLatency().set(duration);	
	}
	
	public void callFailed(Call call, IOException ioe){
		LOG.error("call failed!", ioe);
	}
}
