// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent;

import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;

public class HttpCnxMeters extends HttpMeters {

	private HttpMeters parent;
	public HttpCnxMeters(MeteringService metering, SimpleMonitorable mon, HttpMeters parent) {
		super(metering, mon);
		this.parent = parent;
	}
	
	@Override
	public void incAbortedRequests() {
		super.incAbortedRequests();
		parent.incAbortedRequests();
	}
	
	@Override
	public void incPendingRequests() {
		super.incPendingRequests();
		parent.incPendingRequests();
	}
	
	@Override
	public void incProcessedRequests() {
		super.incProcessedRequests();
		parent.incProcessedRequests();
	}
	
	@Override
	public void channelClosed() {
		super.channelClosed();
		parent.channelClosed();
	}
	
	@Override
	public void incWebSockets() {
		super.incWebSockets();
		parent.incWebSockets();
	}
	
	@Override
	public void decPendingRequests() {
		super.decPendingRequests();
		parent.decPendingRequests();
	}
	
	@Override
	public void channelConnected(boolean client) {
		super.channelConnected(client);
		parent.channelConnected(client);
	}
	
	public void stop() {
		mon.stop();
	}
}
