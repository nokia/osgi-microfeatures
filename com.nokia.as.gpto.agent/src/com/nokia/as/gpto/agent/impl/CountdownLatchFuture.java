// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.agent.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

public class CountdownLatchFuture extends CompletableFuture<Void> {
	
	private CountDownLatch countDownLatch;
	private static Logger LOG = Logger.getLogger(CountdownLatchFuture.class);

	
	public CountdownLatchFuture(CountDownLatch cdl) {
		countDownLatch = cdl;
	}
	
	public CompletableFuture<Void> stop() {
		CompletableFuture.runAsync(() -> {
			try {
				LOG.debug("CountDownLatch " + countDownLatch.getCount());
				countDownLatch.await();
			} catch (InterruptedException e) {
				completeExceptionally(e);
			}
			complete(null);
		});
		return this;
	}
}
