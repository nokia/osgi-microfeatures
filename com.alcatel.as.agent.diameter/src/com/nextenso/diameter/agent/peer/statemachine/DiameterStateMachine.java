// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer.statemachine;

import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.peer.ISocket;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.RemotePeer;
import com.nextenso.diameter.agent.peer.PeerTable;
import com.nextenso.diameter.agent.peer.StaticPeer;
import com.nextenso.diameter.agent.peer.statemachine.rfc3588.PeerStateMachine;
import com.nextenso.diameter.agent.peer.statemachine.rfc3588.State;
import com.nextenso.mux.MuxConnection;
import com.nextenso.proxylet.diameter.DiameterPeerListener;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.alcatel.as.util.sctp.*;

public class DiameterStateMachine {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.state.machine");

	private PeerStateMachine _peerMachine = null;

	private DWListener _forcedDWListener = null;

	public DiameterStateMachine(RemotePeer peer) {
		_peerMachine = new PeerStateMachine(peer);
	}

	public RemotePeer getPeer() {
		return _peerMachine.getPeer ();
	}

	public State getDiameterState() {
		State res = null;
		if (_peerMachine != null) {
			res = _peerMachine.getState();
		}
		return res;
	}

	public void connected(PeerSocket peerSocket) {
		_peerMachine.connected(peerSocket);
	}

	public void notConnected(PeerSocket peerSocket) {
		_peerMachine.notConnected(peerSocket);
	}

	public void sctpPeerAddressChanged (String addr, int port, DiameterPeerListener.SctpAddressEvent event){
		_peerMachine.sctpAddressChanged (addr, port, event);
	}

	public void timeout() {
		_peerMachine.timeout();
	}

    public void start(java.util.Map<String, String> params, java.util.Map<SctpSocketOption, SctpSocketParam> sctpOptions, int localPort, String... localIPs){
		if (_peerMachine.getISocket() == null) {
			String handler = _peerMachine.getPeer().getHandlerName();
			MuxConnection connection = Utils.getMuxConnectionByHandlerName(handler);
			PeerTable pt = Utils.getTableManager ().getPeerTable (handler);
			if (connection != null && pt != null) {
				ISocket iSocket = new ISocket(this, connection, _peerMachine.getPeer(), localIPs, localPort, _peerMachine.getPeer().getProtocol(), sctpOptions, params);
				_peerMachine.setISocket(iSocket);				
				// CSFS-6114
				// addStaticPeer does not add if already there
				// but make sure it is added if we muxClosed/re-opened
				// BUT : for configured static peers : do not call it (else deadlock since start() is called while adding the peers)
				StaticPeer peer = (StaticPeer) _peerMachine.getPeer ();
				if (!peer.isConfigured ())
				    pt.addStaticPeer (peer);
			}
		}
		_peerMachine.start();
	}

	public void disconnect(int disconnectCause) {
		_peerMachine.disconnect(disconnectCause);
	}

	public void sendMessage(DiameterMessageFacade message) {
		_peerMachine.sendMessage(message);
	}

	public boolean isConnected() {
		return _peerMachine.isConnected();
	}

	public PeerSocket getSocket (){
		return _peerMachine.getSocket ();
	}

	public void peerDisconnected(PeerSocket socket) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("peerDisconnected: " + this);
		}

		_peerMachine.peerDisconnected(socket);
	}

	public void connectedWithCER(PeerSocket socket, DiameterMessageFacade cer) {
		_peerMachine.connectedWithCER(socket, cer);
	}

	public void processMessage(DiameterMessageFacade message, PeerSocket socket) {
		int command = message.getDiameterCommand();

		if (_forcedDWListener != null && command == DiameterBaseConstants.COMMAND_DWR && !message.isRequest()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("processMessage: DWA and a listener is waiting " + this);
			}
			// receive the DWA of the forced
			_forcedDWListener.dwaReceived();
			_forcedDWListener = null;
		}

		try {
			_peerMachine.processMessage(message, socket);
		}
		catch (IllegalStateException e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("processMessage: cannot process data.");
			}
		}
	}

	public PeerSocket getISocket() {
		return _peerMachine.getISocket();
	}

	public PeerSocket getRSocket() {
		return _peerMachine.getRSocket();
	}

	public void unbind(PeerSocket askingSocket) {
		PeerSocket rSocket = null;
		PeerSocket iSocket = null;
		if (_peerMachine != null) {
			rSocket = _peerMachine.getRSocket();
			iSocket = _peerMachine.getISocket();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("unbind: asked by " + askingSocket);
			LOGGER.debug("unbind: rSocket=" + rSocket);
			LOGGER.debug("unbind: iSocket=" + iSocket);
		}
	}

	public boolean isConnecting() {
		return _peerMachine.isConnecting();
	}

	public boolean isDisconnected() {
		return _peerMachine.isDisconnected();
	}

	public boolean isDisconnecting() {
		return _peerMachine.isDisconnecting();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String res = null;
		if (_peerMachine != null) {
			res = _peerMachine.toString();
		}
		if (res == null) {
			res = super.toString();
		}
		return res;
	}

	public void sendForcedDWR(DWListener listener) {
		_forcedDWListener = listener;
		_forcedDWListener.waitDwa();
	}

}
