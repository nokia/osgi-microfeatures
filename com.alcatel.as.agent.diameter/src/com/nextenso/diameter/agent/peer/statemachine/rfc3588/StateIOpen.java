// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer.statemachine.rfc3588;

import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event;

public class StateIOpen
		implements State {
	private static final Logger LOGGER = Logger.getLogger("agent.diameter.rfc3588.state.iopen");
	public static final StateIOpen INSTANCE = new StateIOpen();

	private StateIOpen() {}

	public State event(PeerStateMachine machine, Event event, PeerSocket socket, DiameterMessageFacade message) {
		switch (event) {
			case SEND_MESSAGE:
//				machine.doISendMessage(message);
				return StateIOpen.INSTANCE;
			case I_RCV_MESSAGE:
//				machine.processMessage(message, true);
				return StateIOpen.INSTANCE;
			case I_RCV_DWR:
				machine.processDWR(message);
				machine.doISendDWA(message);
				return StateIOpen.INSTANCE;
			case I_RCV_DWA:
				machine.doProcessDWA(message);
				return StateIOpen.INSTANCE;
			case R_CONN_CER:
				LOGGER.warn("Cannot receive a connection with CER on an existing peer, the origin-host must not be duplicated on several peers -> reject this connection");
				machine.doRReject(socket);
				return StateIOpen.INSTANCE;
			case STOP:
				machine.doISendDPR();
				return StateClosing.INSTANCE;
			case I_RCV_DPR:
				machine.doISendDPA(message);
				machine.doIDisc();
				return StateClosed.INSTANCE;
			case I_PEER_DISC:
				machine.doIDisc();
				return StateClosed.INSTANCE;
			case I_RCV_CER:
				boolean isCerCompliant  = machine.doISendCEA(message);
				if (!isCerCompliant) {
					return StateClosed.INSTANCE;
				}
				return StateIOpen.INSTANCE;
			case I_RCV_CEA:
				// we dont send CER
				if (machine.processCEA(message) == false) {
					return StateClosed.INSTANCE;
				}
				return StateIOpen.INSTANCE;
			default:
		}
		return null;
	}

	@Override
	public String toString() {
		return "I-OPEN";
	}

}
