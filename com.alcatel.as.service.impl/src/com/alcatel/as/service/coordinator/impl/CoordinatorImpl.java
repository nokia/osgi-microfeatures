// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.coordinator.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.coordinator.Callback;
import com.alcatel.as.service.coordinator.Coordination;
import com.alcatel.as.service.coordinator.Coordinator;
import com.alcatel.as.service.coordinator.Participant;

import alcatel.tess.hometop.gateways.utils.Log;

public class CoordinatorImpl implements Coordinator {
	/**
	 * Our logger.
	 */
	final static Log _logger = Log.getLogger(CoordinatorImpl.class);

	/**
	 * Our bundle context; Injected by DM
	 */
	BundleContext _bctx;

	/**
	 * List of all participants. Thread safe.
	 */
	final Map<Participant, Dictionary<String, Object>> _participants = new ConcurrentHashMap<>();

	/**
	 * List of expected participants.
	 */
	int _participantCount;

	/**
	 * When we have been injected with all expected participants, then we are
	 * registered in the OSGi service registry.
	 */
	private volatile boolean _isRegistered;

	// Detect how many participants will register.
	void start() {
		_logger.info("Coordinator service starting.");

		for (Bundle b : _bctx.getBundles()) {
			if (b.getState() == Bundle.ACTIVE) {
				String participants = b.getHeaders().get(Participant.PARTICIPANTS);
				if (participants != null) {
					_logger.info("%s has %s participant(s).", b.getSymbolicName(), participants);
					_participantCount += Integer.valueOf(participants);
				}
			}
		}
		if (_participantCount == 0) {
			register();
		}
		_logger.info("Will wait for " + _participantCount + " participants.");
	}

	/**
	 * Binds a participant. This is an optional dependency, which is always
	 * injected after the start lifecycle callback. Thread safe (because all DM
	 * dependencies and lifecycle callbacks are serialized).
	 * 
	 * @param participant
	 * @param properties
	 */
	void bind(Participant participant, Dictionary<String, Object> properties) {
		if (_isRegistered) {
			_logger.error("Ignoring participant %s for coordination %s", participant, properties.get(Participant.COORDINATION));
			return;
		}
		if (properties.get(Participant.COORDINATION) == null) {
			_logger.error("Ignoring participant %s without a \"coordination\" service property for coordination %s", participant, properties.get(Participant.COORDINATION));
			return;
		}
		_logger.info("Bound participant " + participant);
		_participants.put(participant, properties);
		if (! _isRegistered) {
			if ((--_participantCount) == 0) {
				// All participants injected: register the Coordinator service.
				register();
			} else {
				_logger.info("Still awaiting for some participants (%d)", _participantCount);
			}
		}
	}

	/**
	 * Creates a new coordination.
	 * 
	 * @param name
	 *            the coordination name. All Participants having a COORDINATION
	 *            service properties that matches this name will be bound to
	 *            this coordination.
	 */
	@Override
	public Coordination newCoordination(String name, Map<String, Object> coordinationProperties) {
		List<Participant> participants = new ArrayList<>();
		for (Map.Entry<Participant, Dictionary<String, Object>> e : _participants.entrySet()) {
			Dictionary<String, Object> properties = e.getValue();
			if (name.equals(properties.get(Participant.COORDINATION))) {
				participants.add(e.getKey());
			}
		}

		return new CoordinationImpl(name, participants, coordinationProperties, _bctx);
	}

	@Override
	public int begin(Coordination coordination, Callback onComplete, Executor exec) {
		return ((CoordinationImpl) coordination).begin(onComplete, exec);
	}
	
	private void register() {
		_logger.info("Registering service (all participant injected).");
		_bctx.registerService(Coordinator.class.getName(), this, null);
		_isRegistered = true;
	}
}
