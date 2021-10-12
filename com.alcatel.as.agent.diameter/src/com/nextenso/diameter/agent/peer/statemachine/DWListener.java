// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer.statemachine;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.peer.PeerSocket;

/**
 * A listener for the forced DWR sending.
 */
public class DWListener
		implements Runnable {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.dwListener");

	private PeerSocket _incomingSocket;
	private PeerSocket _existingSocket;
	private DiameterMessageFacade _message;
	private DiameterStateMachine _stateMachine;
	private Future _future = null;
	private final PlatformExecutor _executor;
	private final AtomicBoolean _dwaReceived = new AtomicBoolean(false);

	public DWListener(DiameterStateMachine stateMachine, PeerSocket newSocket, PeerSocket existingSocket, DiameterMessageFacade message) {
		_incomingSocket = newSocket;
		_existingSocket = existingSocket;
		_message = message;
		_stateMachine = stateMachine;
		_executor = Utils.getCurrentExecutor();
	}

	/**
	 * Called when the DWA is not received in the delay.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if (_dwaReceived.get()) {
			LOGGER.debug("timeout: DWA already received -> do nothing");
			return;
		}
		LOGGER.debug("timeout (no DWA) -> disconnect the existing socket and process the message");
		_future = null;
		_stateMachine.peerDisconnected(_existingSocket);
		_incomingSocket.processMessage(_message);
	}

	/**
	 * Called if the DWA is received before the end of the delay.
	 */
	public void dwaReceived() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("dwaReceived: DWA has been received ->  process the message");
		}
		final Runnable runnable = new Runnable() {

			@Override
			public void run() {
				if (_future == null) {
					_dwaReceived.set(true);
				} else {
					_future.cancel(true);
					_future = null;
				}
				_incomingSocket.processMessage(_message);
			}
		};
		_executor.execute(runnable);
	}

	/**
	 * Called to indicated that a DWR has been sent and the DWA must be waited.
	 */
	public void waitDwa() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("waitDwa -> a DWR has been sent, waiting for the DWA for (ms) " + DiameterProperties.getForcedWatchdogTimer());
		}
		if (!_dwaReceived.get()) {
			_future = Utils.schedule(_executor, this, DiameterProperties.getForcedWatchdogTimer(), TimeUnit.MILLISECONDS);
		}
	}
}
