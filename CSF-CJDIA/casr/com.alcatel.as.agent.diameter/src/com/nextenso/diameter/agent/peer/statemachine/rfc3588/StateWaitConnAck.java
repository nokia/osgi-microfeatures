package com.nextenso.diameter.agent.peer.statemachine.rfc3588;

import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.I_RCV_CONN_ACK;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.I_RCV_CONN_NACK;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.R_CONN_CER;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.TIMEOUT;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.STOP;

import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

public class StateWaitConnAck
		implements State {

	public static final StateWaitConnAck INSTANCE = new StateWaitConnAck();

	private StateWaitConnAck() {}

	public State event(PeerStateMachine machine, Event event, PeerSocket socket, DiameterMessageFacade message) {

		if (event == I_RCV_CONN_ACK) {
			machine.doISendCER();
			return StateWaitICEA.INSTANCE;
		} else if (event == I_RCV_CONN_NACK) {
			return StateClosed.INSTANCE;
		} else if (event == R_CONN_CER) {
			machine.doRAccept(socket);
			if (machine.processCER(message) == DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS) {
				machine.storeCEA(message.getResponseFacade());
				return StateWaitConnAckElect.INSTANCE;
			}
			machine.doRSendMessage(message.getResponseFacade());
			socket.disconnect(false);
			return StateClosed.INSTANCE;
		} else if (event == TIMEOUT || event == STOP) {
			return StateClosed.INSTANCE;
		}

		return null;
	}

	@Override
	public String toString() {
		return "WAIT-CONN-ACK";
	}
}
