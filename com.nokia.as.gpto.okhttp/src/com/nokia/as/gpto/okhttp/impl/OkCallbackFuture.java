// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.okhttp.impl;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class OkCallbackFuture extends CompletableFuture<Boolean> implements Callback {
	private static final Logger LOG = Logger.getLogger(OkCallbackFuture.class);

	private HttpClientMetering metering;
	
	public OkCallbackFuture(HttpClientMetering hcm) {
		this.metering = hcm;
	}
	
	@Override
	public void onFailure(Call arg0, IOException arg1) {
		LOG.warn("error!", arg1);
		metering.getRequestHttpReceivedKoMeter().inc(1);
		this.complete(false);
	}

	@Override
	public void onResponse(Call arg0, Response arg1) throws IOException {
		metering.getRequestHttpReceivedOkMeter().inc(1);
		metering.getResponseHttpCodeMeter(arg1.code()).inc(1);
		LOG.info(arg1);
		arg1.close();
		this.complete(true);
	}
}
