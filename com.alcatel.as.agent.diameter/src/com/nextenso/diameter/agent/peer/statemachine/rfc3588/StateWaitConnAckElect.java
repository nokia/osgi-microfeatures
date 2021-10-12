// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer.statemachine.rfc3588;

import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.I_RCV_CONN_ACK;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.I_RCV_CONN_NACK;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.R_CONN_CER;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.R_PEER_DISC;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.TIMEOUT;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.WIN_ELECTION;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.STOP;

import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event;

public class StateWaitConnAckElect
		implements State {

	public static final StateWaitConnAckElect INSTANCE = new StateWaitConnAckElect();

	private StateWaitConnAckElect() {}

	public State event(PeerStateMachine machine, Event eventType, PeerSocket socket, DiameterMessageFacade message) {
		if (eventType == I_RCV_CONN_ACK) {
			machine.doISendCER();
			if (machine.elect()) {
				return StateWaitReturns.INSTANCE.event(machine, WIN_ELECTION, null, null);
			}
			return StateWaitReturns.INSTANCE;
		} else if (eventType == I_RCV_CONN_NACK) {
			machine.doRSendCEA();
			return StateROpen.INSTANCE;
		} else if (eventType == R_PEER_DISC) {
			return StateWaitConnAck.INSTANCE;
		} else if (eventType == R_CONN_CER) {
			machine.doRReject(socket);
			return StateWaitConnAckElect.INSTANCE;
		} else if (eventType == TIMEOUT || eventType == STOP) {
			return StateClosed.INSTANCE;
		}
		return null;
	}

	@Override
	public String toString() {
		return "WAIT-CONN-ACK/ELECT";
	}

}
