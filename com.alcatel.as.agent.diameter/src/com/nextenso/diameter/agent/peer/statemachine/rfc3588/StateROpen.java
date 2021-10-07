package com.nextenso.diameter.agent.peer.statemachine.rfc3588;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event;

public class StateROpen
		implements State {
	private static final Logger LOGGER = Logger.getLogger("agent.diameter.rfc3588.state.ropen");

	public static final StateROpen INSTANCE = new StateROpen();

	private StateROpen() {}

	public State event(PeerStateMachine machine, Event event, PeerSocket socket, DiameterMessageFacade message) {
		switch (event) {
			case SEND_MESSAGE:
//				machine.doRSendMessage(message);
				return StateROpen.INSTANCE;
			case R_RCV_MESSAGE:
//				machine.processMessage(message, true);
				return StateROpen.INSTANCE;
			case R_RCV_DWR:
				machine.doProcessDWR(message);
				machine.doRSendDWA(message);
				return StateROpen.INSTANCE;
			case R_RCV_DWA:
				machine.processDWA(message);
				return StateROpen.INSTANCE;
			case R_CONN_CER:
				if (LOGGER.isEnabledFor(Level.WARN)) {
					LOGGER.warn("Cannot receive a connection with CER on an existing peer, the origin-host must not be duplicated on several peers -> reject this connection - CER=" + message);
				}
				machine.doRReject(socket);
				return StateROpen.INSTANCE;
			case STOP:
				machine.doRSendDPR();
				return StateClosing.INSTANCE;
			case R_RCV_DPR:
				machine.doRSendDPA(message);
				return StateClosed.INSTANCE;
			case R_PEER_DISC:
				return StateClosed.INSTANCE;
			case R_RCV_CER:
				boolean isCerCompliant = Utils.getCapabilities().isCompliantMessage(message);
				machine.doRSendCEA(message, isCerCompliant);
				if (!isCerCompliant) {
					return StateClosed.INSTANCE;
				}
				return StateROpen.INSTANCE;
			case R_RCV_CEA:
				if (machine.processCEA(message) == false) {
					return StateClosed.INSTANCE;
				}
				return StateROpen.INSTANCE;

			default:
		}
		return null;
	}

	@Override
	public String toString() {
		return "R-OPEN";
	}

}
