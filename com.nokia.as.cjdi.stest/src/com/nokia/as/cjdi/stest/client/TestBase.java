// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.cjdi.stest.client;

import java.io.IOException;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeerListener;
import com.nextenso.proxylet.diameter.DiameterPeerTable;

public class TestBase {
	
	final static Logger _log = Logger.getLogger(TestBase.class);
	
	void awaitPeerConnected(DiameterPeerTable table) throws InterruptedException {
		if (true) {
			Thread.sleep(2000);
			return;
		}		
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		table.addListener(new DiameterPeerListener() {
			@Override
			public void connected(DiameterPeer peer) {
				_log.warn("connected: peer.isLocalDiameterPeer=" + peer.isLocalDiameterPeer());
				if (! peer.isLocalDiameterPeer()) {
					latch.countDown();
				}
			}

			@Override
			public void connectionFailed(DiameterPeer peer, String msg) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void disconnected(DiameterPeer peer, int disconnectCause) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void sctpAddressChanged(DiameterPeer peer, String addr, int port, SctpAddressEvent event) {
				// TODO Auto-generated method stub
				
			}});
		
		if (! latch.await(5000, TimeUnit.MILLISECONDS)) {
			throw new IllegalStateException("diameter peer table not connected timely");
		}
		_log.warn("peer connected");
	}
}
