package com.nextenso.diameter.agent.peer.statemachine.rfc3588;

import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event;

public interface State {

	public State event(PeerStateMachine machine, Event eventType, PeerSocket socket, DiameterMessageFacade message);
}
