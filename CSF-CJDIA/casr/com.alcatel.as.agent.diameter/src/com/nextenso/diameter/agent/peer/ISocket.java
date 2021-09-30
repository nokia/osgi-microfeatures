package com.nextenso.diameter.agent.peer;

import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.peer.statemachine.DiameterStateMachine;
import com.nextenso.mux.MuxConnection;
import com.nextenso.proxylet.diameter.DiameterPeer.Protocol;
import com.alcatel.as.util.sctp.*;

public class ISocket
		extends PeerSocket {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.ISocket");

	/**
	 * Creates a new Initiator socket.
	 * 
	 * @param stateMachine The peer state machine
	 * @param connection The MUX connection.
	 * @param peer The peer.
	 */
    public ISocket(DiameterStateMachine stateMachine, MuxConnection connection, RemotePeer peer, String[] localIP, int localPort, Protocol protocol, java.util.Map<SctpSocketOption, SctpSocketParam> sctpOptions, java.util.Map<String, String> params) {
		super(stateMachine, connection, peer, localIP, localPort, protocol, sctpOptions, params);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("new ISocket: localAddress=" + localIP+"/"+localPort);
		}
	}

	/**
	 * @see com.nextenso.diameter.agent.peer.PeerSocket#isInitiator()
	 */
	@Override
	public boolean isInitiator() {
		return true;
	}

	/**
	 * @see com.nextenso.diameter.agent.peer.PeerSocket#toString()
	 */
	@Override
	public String toString() {
		return "I-Socket - " + super.toString();
	}

	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

}
