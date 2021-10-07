package com.nextenso.diameter.agent.peer.statemachine.rfc3588;

import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.I_PEER_DISC;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.I_RCV_CEA;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.I_RCV_NON_CEA;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.R_CONN_CER;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.TIMEOUT;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.WIN_ELECTION;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.STOP;

import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

public class StateWaitICEA
		implements State {

	public static final StateWaitICEA INSTANCE = new StateWaitICEA();

	private StateWaitICEA() {}

	public State event(PeerStateMachine machine, Event eventType, PeerSocket socket, DiameterMessageFacade message) {
		if (eventType == I_RCV_CEA) {
			if (machine.processCEA(message) == false) {
				return StateClosed.INSTANCE;
			}
			return StateIOpen.INSTANCE;
		} else if (eventType == R_CONN_CER) {
			machine.doRAccept(socket);
			if (machine.processCER(message) == DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS) {
				machine.storeCEA(message.getResponseFacade());
			} else {
				machine.doRSendMessage(message.getResponseFacade());
				socket.disconnect(false);
				return StateClosed.INSTANCE;
			}
			if (machine.elect()) {
				return StateWaitReturns.INSTANCE.event(machine, WIN_ELECTION, null, null);
			}
			return StateWaitReturns.INSTANCE;
		} else if (eventType == I_PEER_DISC) {
			machine.doIDisc();
			return StateClosed.INSTANCE;
		} else if (eventType == I_RCV_NON_CEA) {
			return StateClosed.INSTANCE;
		} else if (eventType == TIMEOUT || eventType == STOP) {
			return StateClosed.INSTANCE;
		}
		return null;
	}

	@Override
	public String toString() {
		return "WAIT-I-CEA";
	}
}
