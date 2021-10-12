// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class StopListeningService {
	public final int DELAY = 5000;

	public interface Listener {

		public int stoppingService(int disconnectCause);
	}

	public StopListeningService() {
	}

	private Set<Listener> _listeners  = new HashSet<Listener>();

	public StopListeningService register(Listener service) {
		_listeners.add(service);
		return this;
	}
	
	private void stop(final Runnable callback, long retrytimeout,final int timeout,int disconnectCause) {
		Iterator<Listener> it = _listeners.iterator();
		int retry = 0;
		while (it.hasNext()) {
			int serviceretry = it.next().stoppingService(disconnectCause);
			if (serviceretry == 0)
				it.remove();
			else if (serviceretry < retry)
				retry = serviceretry;
			else {
				if (retry == 0)
					retry=Math.min(timeout,serviceretry);
			}
		}
		if (retry > 0) {
			try {
				Thread.sleep(retry);
			} catch (InterruptedException e) {
			} finally {
				if (System.currentTimeMillis() < retrytimeout)
					stop(callback, retrytimeout,timeout, disconnectCause);
				else
					callback.run();
			}
		} else {
			callback.run();
		}

	}


	public void stop(final Runnable callback, int disconnectCause) {
		stop(callback,DELAY, disconnectCause);
	}
	
	public void stop(final Runnable callback,final int timeout,int disconnectCause) {
		if (_listeners != null) {
			stop(callback, System.currentTimeMillis() + timeout,timeout, disconnectCause);
		} else {
			callback.run();
		}
	}
}

