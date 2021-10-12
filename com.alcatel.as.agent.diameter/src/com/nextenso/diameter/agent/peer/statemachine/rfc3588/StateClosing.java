// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer.statemachine.rfc3588;

import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.I_PEER_DISC;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.I_RCV_DPA;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.R_PEER_DISC;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.R_RCV_DPA;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.STOP;

import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event;

public class StateClosing
		implements State {

	public static final StateClosing INSTANCE = new StateClosing();

	private StateClosing() {}

	public State event(PeerStateMachine machine, Event eventType, PeerSocket socket, DiameterMessageFacade message) {
		if (eventType == I_RCV_DPA) {
			return StateClosed.INSTANCE;
		} else if (eventType == R_RCV_DPA) {
			return StateClosed.INSTANCE;
		} else if (eventType == I_PEER_DISC) {
			return StateClosed.INSTANCE;
		} else if (eventType == R_PEER_DISC) {
			return StateClosed.INSTANCE;
		}else if (eventType == STOP) {
			return StateClosed.INSTANCE;
		}

		return null;
	}

	@Override
	public String toString() {
		return "CLOSING";
	}

}
