// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer.statemachine.rfc3588;

import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.R_CONN_CER;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.START;

import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.diameter.agent.Utils;

public class StateClosed
		implements State {

	public static final StateClosed INSTANCE = new StateClosed();

	private StateClosed() {}

	public State event(PeerStateMachine machine, Event eventType, PeerSocket socket, DiameterMessageFacade message) {
		if (eventType == START) {
			boolean hasISocket  =machine.doISendConnReq();
			if (! hasISocket) {
				return INSTANCE;
			}
			return StateWaitConnAck.INSTANCE;
		} else if (eventType == R_CONN_CER) {
			machine.doRAccept(socket);
			long res = 0L;
			try{
				res = machine.processCER(message);
			}catch(RuntimeException e){
				// only a parsing exception is expected
				if (e.getCause () instanceof DiameterMessageFacade.ParsingException){
					res = Utils.fillCEA (message, (DiameterMessageFacade.ParsingException) e.getCause ());
				}
			}
			machine.doRSendCEA(message);
			
			if (res == DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS) {
				return StateROpen.INSTANCE;
			}

			socket.disconnect(false);
			return INSTANCE;
		}
		return null;
	}

	@Override
	public String toString() {
		return "CLOSED";
	}

}
