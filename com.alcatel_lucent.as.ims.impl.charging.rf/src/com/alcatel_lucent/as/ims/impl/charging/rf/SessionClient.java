// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.charging.rf;

import java.net.NoRouteToHostException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.AccountingRecordType;
import com.alcatel_lucent.as.ims.diameter.charging.rf.AccountingRequest;
import com.alcatel_lucent.as.ims.diameter.charging.rf.InterimListener;
import com.alcatel_lucent.as.ims.diameter.charging.rf.RfSessionClient;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;

public class SessionClient
		extends AbstractRfClient
		implements RfSessionClient, Runnable {

	private static enum State {
		INIT,
		START,
		STOP
	}

	private InterimListener _interimListener = null;
	private State _state = State.INIT;
	private Future _future = null;

	public SessionClient(Iterable<String> servers, String realm, InterimListener listener, Version version)
			throws NoRouteToHostException {
		super(servers, realm, version);
		_interimListener = listener;
	}

	private AccountingRequest createAcr(AccountingRecordType type) {
		DiameterClientRequest request = newRequest(type);
		Acr res = new Acr(request, getVersion());
		if (_interimListener != null) {
			res.setSessionClient(this);
		}
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.RfSessionClient#createInterimRequest()
	 */
	public AccountingRequest createInterimRequest()
		throws IllegalStateException {
		if (_state != State.START) {
			throw new IllegalStateException("cannot create an INTERIM record with current state=" + _state);
		}
		AccountingRequest res = createAcr(AccountingRecordType.INTERIM_RECORD);
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.RfSessionClient#createStartRequest()
	 */
	public AccountingRequest createStartRequest()
		throws IllegalStateException {
		if (_state != State.INIT) {
			throw new IllegalStateException("cannot create an START record with current state=" + _state);
		}
		AccountingRequest res = createAcr(AccountingRecordType.START_RECORD);
		_state = State.START;
		return res;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.rf.RfSessionClient#createStopRequest()
	 */
	public AccountingRequest createStopRequest()
		throws IllegalStateException {
		if (_state != State.START) {
			throw new IllegalStateException("cannot create an STOP record with current state=" + _state);
		}
		AccountingRequest res = createAcr(AccountingRecordType.STOP_RECORD);
		_state = State.STOP;
		notifyInterimIterval(0);

		return res;
	}

	/**
	 * Arms the timer if needed.
	 * 
	 * @param interval The interval.
	 */
	public void notifyInterimIterval(long interval) {
		if (_interimListener == null) {
			return;
		}

		if (_future != null) {
			_future.cancel(false);
			_future = null;
		}

		if (interval > 0) {
			PlatformExecutor executor = PlatformExecutors.getInstance().getCurrentThreadContext().getCurrentExecutor();
			_future = executor.schedule(this, interval, TimeUnit.SECONDS);
		}
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if (_interimListener != null) {
			_interimListener.doInterim(this);
		}
	}
}
