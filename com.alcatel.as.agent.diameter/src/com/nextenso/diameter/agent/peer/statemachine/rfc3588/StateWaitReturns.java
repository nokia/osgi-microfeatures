// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer.statemachine.rfc3588;

import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.I_PEER_DISC;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.I_RCV_CEA;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.R_CONN_CER;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.R_PEER_DISC;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.WIN_ELECTION;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.STOP;

import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event;

public class StateWaitReturns
		implements State {

	public static final StateWaitReturns INSTANCE = new StateWaitReturns();

	private StateWaitReturns() {}

	public State event(PeerStateMachine machine, Event eventType, PeerSocket socket, DiameterMessageFacade message) {
		if (eventType == WIN_ELECTION) {
			machine.doIDisc();
			machine.doRSendCEA();
			return StateROpen.INSTANCE;
		} else if (eventType == I_PEER_DISC) {
			machine.doIDisc();
			machine.doRSendCEA();
			return StateROpen.INSTANCE;
		} else if (eventType == I_RCV_CEA) {
			machine.doRDisc();
			return StateIOpen.INSTANCE;
		} else if (eventType == R_PEER_DISC) {
			machine.doRDisc();
			return StateWaitICEA.INSTANCE;
		} else if (eventType == R_CONN_CER) {
			machine.doRReject(socket);
			return StateWaitReturns.INSTANCE;
		} else if (eventType == STOP) {
			return StateClosed.INSTANCE;
		}
		return null;
	}

	@Override
	public String toString() {
		return "WAIT-RETURNS";
	}

}
