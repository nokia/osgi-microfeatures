package com.nextenso.diameter.agent.peer.statemachine.rfc3588;

import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.START;
import static com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event.TIMEOUT_CONN;

import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.diameter.agent.impl.DiameterResponseFacade;
import com.nextenso.diameter.agent.peer.ISocket;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.RSocket;
import com.nextenso.diameter.agent.peer.RemotePeer;
import com.nextenso.diameter.agent.peer.statemachine.rfc3588.Rfc3588Constants.Event;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterPeerListener;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;

public class PeerStateMachine
		implements Runnable {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.rfc3588.state.machine");
	public static final long TIMEOUT_SERVER_CONN = 10; // in seconds

	private State _state;
	private final RemotePeer _peer;
	//	private String _originHost;
	private ISocket _iSocket;
	private RSocket _rSocket;
	private DiameterResponseFacade _cea;
	private int _disconnectCause = -1;

	public PeerStateMachine(RemotePeer peer) {
		_peer = peer;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("new machine, peer=" + getOriginHost());
		}
		// according to RFC3588, INIT state does not exists -> CLOSED must be used
		setState(StateClosed.INSTANCE);
	}

	public RemotePeer getPeer() {
		return _peer;
	}

	public boolean isConnected() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("isConnected: peer=" + getOriginHost() + ", state=" + getState());
		}
		return (getState() == StateROpen.INSTANCE || getState() == StateIOpen.INSTANCE);
	}

	public boolean isConnecting() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("isConnecting: peer=" + getOriginHost() + ", state=" + getState());
		}
		return (getState() == StateWaitICEA.INSTANCE || getState() == StateWaitConnAck.INSTANCE || getState() == StateWaitConnAckElect.INSTANCE);
	}

	public boolean isDisconnected() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("isDisconnected: peer=" + getOriginHost() + ", state=" + getState());
		}
		return (getState() == StateClosed.INSTANCE);
	}

	public boolean isDisconnecting() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("isDisconnecting: peer=" + getOriginHost() + ", state=" + getState());
		}
		return (getState() == StateClosing.INSTANCE);
	}

	public ISocket getISocket() {
		return _iSocket;
	}

	public PeerSocket getRSocket() {
		return _rSocket;
	}

	public PeerSocket getSocket (){
		return _state == StateIOpen.INSTANCE ? getISocket () : (_state == StateROpen.INSTANCE ? getRSocket () : null);
	}

	public void storeCEA(DiameterResponseFacade cea) {
		_cea = cea;
	}

	public DiameterResponseFacade getStoredCEA() {
		DiameterResponseFacade res = _cea;
		_cea = null;
		return res;
	}

	public void processMessage(DiameterMessageFacade message, PeerSocket socket) {
		boolean isInitiator = socket.isInitiator();
		Rfc3588Constants.Event event;

		if (message.getDiameterApplication() == DiameterBaseConstants.APPLICATION_COMMON_MESSAGES) {
			int command = message.getDiameterCommand();
			if (message.isRequest()) {
				switch (command) {
					case DiameterBaseConstants.COMMAND_CER:
						event = (isInitiator) ? Rfc3588Constants.Event.I_RCV_CER : Rfc3588Constants.Event.R_RCV_CER;
						break;
					case DiameterBaseConstants.COMMAND_DPR:
						DiameterAVP avp = message.getDiameterAVP(DiameterBaseConstants.AVP_DISCONNECT_CAUSE);
						if (avp != null) {
							try{
								_disconnectCause = EnumeratedFormat.getEnumerated(avp.getValue(), 0);
							}catch(Exception e){
								// AVP value is not correct (Codenomicon test)
								_disconnectCause = -1;
							}
						}
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("processMessage: disconnect avp=" + avp + ", disconnectCause=" + _disconnectCause);
						}
						event = (isInitiator) ? Rfc3588Constants.Event.I_RCV_DPR : Rfc3588Constants.Event.R_RCV_DPR;
						break;
					case DiameterBaseConstants.COMMAND_DWR:
						event = (isInitiator) ? Rfc3588Constants.Event.I_RCV_DWR : Rfc3588Constants.Event.R_RCV_DWR;
						break;
					default:
						event = (isInitiator) ? Rfc3588Constants.Event.I_RCV_MESSAGE : Rfc3588Constants.Event.R_RCV_MESSAGE;
				}
			} else {
				switch (command) {
					case DiameterBaseConstants.COMMAND_CEA:
						event = (isInitiator) ? Rfc3588Constants.Event.I_RCV_CEA : Rfc3588Constants.Event.R_RCV_CEA;
						break;
					case DiameterBaseConstants.COMMAND_DPA:
						event = (isInitiator) ? Rfc3588Constants.Event.I_RCV_DPA : Rfc3588Constants.Event.R_RCV_DPA;
						break;
					case DiameterBaseConstants.COMMAND_DWA:
						event = (isInitiator) ? Rfc3588Constants.Event.I_RCV_DWA : Rfc3588Constants.Event.R_RCV_DWA;
						break;
					default:
						event = (isInitiator) ? Rfc3588Constants.Event.I_RCV_MESSAGE : Rfc3588Constants.Event.R_RCV_MESSAGE;
				}
			}
		} else {
			event = (isInitiator) ? Rfc3588Constants.Event.I_RCV_MESSAGE : Rfc3588Constants.Event.R_RCV_MESSAGE;
		}

		handleEvent(event, message, socket);
	}

	/**
	 * This method must only be called when the thread is the agent thread.
	 * 
	 * @param event
	 * @param socket
	 */
	private void handleEvent(Event event, DiameterMessageFacade message, PeerSocket socket) {
		boolean debug = LOGGER.isDebugEnabled();
		State currentState = getState();
		if (event == Event.SEND_MESSAGE) {
			if (debug) {
				LOGGER.debug("handleEvent: peer=" + getOriginHost() + ", Received event=" + event + " with current-state=" + currentState
						+ " for machine=" + this);
			}
			if (currentState == StateClosed.INSTANCE) {
				return;
			}
			if (currentState == StateROpen.INSTANCE) {
				doRSendMessage(message);
				return;
			}
			if (currentState == StateIOpen.INSTANCE) {
				doISendMessage(message);
				return;
			}
			LOGGER.warn("handleEvent: " + this + " - Illegal event=" + event + " with current-state=" + currentState);
			throw new IllegalStateException();
		}

		if ((currentState == StateROpen.INSTANCE && event == Event.R_RCV_MESSAGE)
				|| (currentState == StateIOpen.INSTANCE && event == Event.I_RCV_MESSAGE)) {
			if (debug) {
				LOGGER.debug("handleEvent: peer=" + getOriginHost() + ", Received event=" + event + " with current-state=" + currentState
						+ " for machine=" + this);
			}

			processMessage(message, true);
			return;
		}

		synchronized (this) {
			currentState = getState(); // get state  in the synchronized zone
			if (debug) {
				LOGGER.debug("handleEvent: peer=" + getOriginHost() + ", Received event=" + event + " with current-state=" + currentState
						+ " for machine=" + this);
			}

			if (event == START) { // sent automatically for static peers
				if (currentState != StateClosed.INSTANCE) {
					if (debug) {
						LOGGER.debug("handleEvent: START -> do nothing");
					}
					return;
				}
			} else if (event == TIMEOUT_CONN) {
				if (currentState == StateROpen.INSTANCE || currentState == StateIOpen.INSTANCE || currentState == StateClosed.INSTANCE) {
					if (debug) {
						LOGGER.debug("handleEvent (TIMEOUT_CONN): current state=" + currentState + " -> do nothing");
					}
					return;
				}
				clean(true, currentState);
				if (debug) {
					LOGGER.debug("handleEvent (TIMEOUT_CONN): new state=" + currentState + " for machine=" + this);
				}
				return;
			}

			State newState = currentState.event(this, event, socket, message);
			if (debug) {
				LOGGER.debug("handleEvent: new processed state=" + newState + " for machine=" + this);
			}

			if (newState == null && currentState != StateClosed.INSTANCE) {
				LOGGER.warn("handleEvent: " + this + " - Illegal event=" + event + " with current-state=" + currentState);
				throw new IllegalStateException();
			}

			if (newState == StateClosed.INSTANCE){
				if (event == START){ // CSFS-6114
					clean(false, StateWaitConnAck.INSTANCE); // this is a trick to call connectionFailed listeners
				} else {
					clean(false, currentState);
				}
				return;
			}

			if (newState != null) {
				setState(newState);
			}

			if ((currentState != newState) && (newState == StateROpen.INSTANCE || newState == StateIOpen.INSTANCE)) {
				if (debug) {
					LOGGER.debug("handleEvent: entering in a OPEN state-> call connected on the peer");
				}
				_peer.connected();
			}

		}

	}

	private String getLocalOriginHost() {
		if (_peer != null) {
			return _peer.getLocalOriginHost();
		}
		return null;
	}
	
	private String getOriginHost() {
		if (_peer != null) {
			return _peer.getOriginHost();
		}
		return null;
	}
	
	

	public void processMessage(DiameterMessageFacade message, boolean mainThread) {
		_peer.processMessage(message, mainThread);
	}

	/**
	 * The transport layer connection is disconnected, either politely or
	 * abortively, in response to an error condition. Local resources are freed.
	 */
	private void clean(boolean abort) {
	    clean (abort, getState ());
	}
	
	private void clean(boolean abort, State currentState) {
		setState(StateClosed.INSTANCE);
		doError(abort, currentState == StateWaitICEA.INSTANCE || currentState == StateWaitConnAck.INSTANCE || currentState == StateWaitConnAckElect.INSTANCE);
	}

	public void doRSendMessage(DiameterMessageFacade message) {
		message.send(getRSocket());
	}

	public void doISendMessage(DiameterMessageFacade message) {
		message.send(getISocket());
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PeerStateMachine [state=" + getState() + ", peer=" + getPeer() + "]";
	}

	public void setISocket(ISocket socket) {
		_iSocket = socket;
	}

	public void setRSocket(RSocket socket) {
		_rSocket = socket;
	}

	public State getState() {
		return _state;
	}

	private void setState(State state) {
		_state = state;
	}

	public void doRSendCEA() {
		doRSendMessage(getStoredCEA());
	}

	public void doProcessDWR(@SuppressWarnings("unused") DiameterMessageFacade message) {
		// do nothing
	}

	public void doRSendDWA(DiameterMessageFacade message) {
		doRSendMessage(Utils.createDWA(message));
	}

	public void doISendDWA(DiameterMessageFacade message) {
		doISendMessage(Utils.createDWA(message));
	}

	public void doProcessDWA(DiameterMessageFacade message) {
		processDWA(message);
	}

	public void doISendDPA(DiameterMessageFacade message) {
		message.getRequestFacade ().setClientPeer (_peer);
		doISendMessage(Utils.createDPA(message));
	}

	public void doISendCER() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("doISendCER: peer=" + getOriginHost());
		}
		doISendMessage(Utils.createCER(getPeer(), _iSocket.getLocalInetSocketAddresses ()));
	}

	public void doRSendCEA(DiameterMessageFacade message, boolean isCompliant) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("doRSendCEA: peer=" + getOriginHost());
		}

		long resultCode = DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS;
		if (!isCompliant) {
			resultCode = DiameterBaseConstants.RESULT_CODE_DIAMETER_NO_COMMON_APPLICATION;
		}
		Utils.fillCEA((DiameterResponseFacade) message.getRequestFacade().getResponse(), resultCode);
		doRSendMessage(message.getResponseFacade());
	}

	public void connected(PeerSocket peerSocket) {
		handleEvent(Rfc3588Constants.Event.I_RCV_CONN_ACK, null, peerSocket);
	}

	public void notConnected(PeerSocket peerSocket) {
		handleEvent(Rfc3588Constants.Event.I_RCV_CONN_NACK, null, peerSocket);
	}

	public void sctpAddressChanged (String addr, int port, DiameterPeerListener.SctpAddressEvent event){
		// here, we raise the event regardless of the state. for now, no better policy defined.
		if (_peer != null)
			((RemotePeer)_peer).sctpAddressChanged (addr, port, event);
	}

	public void start() {
		handleEvent(Rfc3588Constants.Event.START, null, null);
	}

	public void disconnect(int disconnectCause) {
		_disconnectCause = disconnectCause;
		handleEvent(Rfc3588Constants.Event.STOP, null, null);
	}

	public void sendMessage(DiameterMessageFacade message) {
		handleEvent(Rfc3588Constants.Event.SEND_MESSAGE, message, null);
	}

	public void peerDisconnected(PeerSocket socket) {
		if (socket.isInitiator()) {
			handleEvent(Rfc3588Constants.Event.I_PEER_DISC, null, socket);
		} else {
			handleEvent(Rfc3588Constants.Event.R_PEER_DISC, null, socket);
		}
	}

	public void connectedWithCER(PeerSocket socket, DiameterMessageFacade cer) {
		handleEvent(Rfc3588Constants.Event.R_CONN_CER, cer, socket);
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		timeout();
	}

	public void timeout() {
		handleEvent(Rfc3588Constants.Event.TIMEOUT_CONN, null, null);
	}

	/**
	 * Process the DWA. Do nothing.
	 * 
	 * @param dwa The DWA.
	 */
	public void processDWA(DiameterMessageFacade dwa) {
		// nothing to do
	}

	public void processDWR(@SuppressWarnings("unused") DiameterMessageFacade message) {
		// do nothing
	}

	public void doIDisc() {
		if (getISocket() != null) {
			getISocket().disconnect(false);
			setISocket(null);
		}
	}

	/**
	 * Sends a CEA to the I-Peer.
	 * 
	 * @param message The CEA.
	 * @return true if the capabilities are compliant.
	 */
	public boolean doISendCEA(DiameterMessageFacade message) {
		long result = processCER(message.getRequestFacade());
		doISendMessage(message.getResponseFacade());
		return (result == DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS);
	}

	/**
	 * Sends a DPR message to the I-Peer.
	 */
	public void doISendDPR() {
		DiameterMessageFacade message = getPeer().createDPR(_disconnectCause);
		doISendMessage(message);
	}

	/**
	 * Initiates a transport connection with the I-Peer.
	 * 
	 * @return true if the I-Peer has been opened.
	 */
	public boolean doISendConnReq() {
		PeerSocket iSocket = getISocket();
		if (iSocket == null) {
			return false;
		}

		if (!iSocket.open()) {
			return false;
		}

		return true;
	}

	/**
	 * Processes the CER associated with the R-Conn-CER.
	 * 
	 * @param cer The CER
	 * @return true if compliant.
	 */
	public long processCER(DiameterMessageFacade cer) {
		return getPeer().processCER((DiameterRequestFacade)cer);
	}

	/**
	 * Processes the received CEA.
	 * 
	 * @param cea The CEA
	 * @return true if compliant.
	 */
	public boolean processCEA(DiameterMessageFacade cea) {
		return getPeer().processCEA(cea);
	}

	private void doError(boolean abort, boolean wasConnecting) {
		if (_peer != null) {
			_peer.disconnected(_disconnectCause, wasConnecting);
		}

		if (_iSocket != null) {
			_iSocket.disconnect(abort);
			_iSocket = null;
		}
		if (_rSocket != null) {
			_rSocket.disconnect(abort);
			_rSocket = null;
		}

		_cea = null;
	}

	/**
	 * Sends a DPR message to the R-Peer.
	 */
	public void doRSendDPR() {
		DiameterMessageFacade message = getPeer().createDPR(_disconnectCause);
		doRSendMessage(message);
	}

	/**
	 * Accepts as the responder connection the incoming connection associated with
	 * the R-Conn-CER.
	 * 
	 * @param socket The R-Socket to be associated.
	 */
	public void doRAccept(PeerSocket socket) {
		if (socket instanceof RSocket) {
			RSocket rSocket = (RSocket) socket;
			setRSocket(rSocket);
		}
	}

	/**
	 * Sends a CEA to the R-Peer.
	 * 
	 * @param message
	 */
	public void doRSendCEA(DiameterMessageFacade message) {
		doRSendMessage(message.getResponseFacade());
	}

	/**
	 * An election occurs. <BR>
	 * See RFC 3588 section 5.6.4 for more information.
	 * 
	 * @return The election result.
	 */
	public boolean elect() {
		String clientOriginHost = getLocalOriginHost();
		// if local origin host > remote origin host then win
		boolean win = (clientOriginHost.compareTo(getOriginHost()) > 0);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("elect: result=" + (win ? "WIN" : "LOSE") + " for  " + this);
		}
		return win;
	}

	/**
	 * Disconnects the transport layer connection.
	 */
	public void doRDisc() {
		if (getRSocket() != null) {
			getRSocket().disconnect(false);
			setRSocket(null);
		}
	}

	public void doRReject(PeerSocket socket) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("doRReject: socket=" + socket);
		}
		if (socket instanceof RSocket) {
			RSocket rSocket = (RSocket) socket;
			rSocket.unbindStateMachine();
			rSocket.disconnect(false);
		} else {
			LOGGER.warn("doRReject: the socket is not a RSocket -> do nothing");
		}
	}

	public void doRSendDPA(DiameterMessageFacade message) {
		message.getRequestFacade ().setClientPeer (_peer);
		doRSendMessage(Utils.createDPA(message));
	}

}
