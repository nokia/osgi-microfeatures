// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.impl;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.metering.Counter;
import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.peer.LocalPeer;
import com.nextenso.diameter.agent.peer.Peer;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.RemotePeer;
import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterResponse;
import com.nextenso.proxylet.diameter.DiameterSession;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientListener;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.IdentityFormat;
import alcatel.tess.hometop.gateways.utils.ByteOutputStream;

public class DiameterRequestFacade
		extends DiameterMessageFacade
		implements DiameterClientRequest, Runnable {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.request");

	public static final int NO_FLAG = 0x00;
	public static final int REQUEST_FLAG = 0x80;
	public static final int PROXIABLE_FLAG = 0x40;
	public static final int RETRANSMITTED_FLAG = 0x10;
	public static final int RP_FLAGS = REQUEST_FLAG | PROXIABLE_FLAG;
	public static final int RT_FLAGS = REQUEST_FLAG | RETRANSMITTED_FLAG;
	public static final int RPT_FLAGS = RP_FLAGS | RETRANSMITTED_FLAG;
	
	private static final String LAST_USED_PEER_ATTRIBUTE = "agent.diameter.request.lastUsedPeer";
	private static final String SERVER_PEER_ATTRIBUTE = "agent.diameter.request.ServerPeer"; // used by IPD to set the routing peer: so the prop value must not be changed

	private DiameterResponseFacade _response;
	private Peer _clientPeer, _serverPeer;
	private DiameterPeer _appClientPeer;  // this is used by the httpgw to replace temporarily the client peer seen by the application
	private boolean _useAppClientPeer;
	private DiameterSessionFacade _session;
	private int _clientHopIdentifier, _serverHopIdentifier, _endIdentifier;
	private DiameterClientFacade _client;
	private Object _attachment;
	private boolean _isLocalRequest;
	private Executor _callerExecutor;
	private ClassLoader _callerCL;
	private Integer _retryTimeoutInMs;

	private boolean _isClient = false;
	private boolean _executeDirect = false;

	private volatile Future<?> _resentFuture = null;
	private AtomicBoolean _responseReceived = new AtomicBoolean(false);

	private DiameterClientListener _listener;
	private volatile String _failure = null; // volatile because not modified in a synchronized block
	private boolean _requestCompleted; // boolean used by synchronous execute method
	private int _retransmissions = 0;
	private String _handlerName = null;

	private PeerSocket _lastUsedSocket;
	private String _destinationHost = null;
	private String _destinationRealm = null;

	private final AtomicBoolean _mustResponseBeProcessed = new AtomicBoolean(true);

	/**
	 * Constructor for this class.
	 * 
	 * Used for client requests: the hopIdentifier is unique (clientHopIdentifier
	 * == serverHopIdentifier)
	 * 
	 * @param client The Diameter client
	 * @param application The application identifier.
	 * @param command The diameter command.
	 * @param proxiable true if the request is proxiable.
	 */
	public DiameterRequestFacade(DiameterClientFacade client, long application, int command, boolean proxiable) {
		super(application, command, (proxiable) ? RP_FLAGS : REQUEST_FLAG);
		_isClient = true;
		_clientHopIdentifier = getNextHopByHopIdentifier();
		setServerHopIdentifier(_clientHopIdentifier);
		_endIdentifier = getNextEndToEndIdentifier();
		_isLocalRequest = true;
		_client = client;
		_session = (DiameterSessionFacade) client.getDiameterSession();
		_handlerName = client.getHandlerName();
		// acts as a client
		LocalPeer localPeer = Utils.getClientLocalPeer(_handlerName);
		setClientPeer(localPeer);
		setServerPeer(client.getPeer());
	}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * Used for CER, DWR and DPR (local requests that do not come from a client)
	 * 
	 * @param command The diameter command.
	 * @param peer The peer.
	 */
	public DiameterRequestFacade(int command, Peer peer) {
		super(DiameterBaseConstants.APPLICATION_COMMON_MESSAGES, command, DiameterRequestFacade.REQUEST_FLAG);
		_clientHopIdentifier = getNextHopByHopIdentifier();
		setServerHopIdentifier(_clientHopIdentifier);
		_endIdentifier = getNextEndToEndIdentifier();
		_isLocalRequest = true;
		_handlerName = peer.getHandlerName();
		setServerPeer(peer);
	}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * Used for proxy requests: clientHopIdentifier != serverHopIdentifier
	 * 
	 * @param handlerName
	 * @param stackSessionId
	 * @param application The application identifier.
	 * @param command The diameter command.
	 * @param flags The request flags.
	 * @param clientHopIdentifier
	 * @param endIdentifier
	 */
	public DiameterRequestFacade(String handlerName, long stackSessionId, long application, int command, int flags, int clientHopIdentifier,
			int endIdentifier) {
		super(application, command, flags);
		_clientHopIdentifier = clientHopIdentifier;
		setServerHopIdentifier(getNextHopByHopIdentifier());
		_endIdentifier = endIdentifier;
		_isLocalRequest = false;
		setStackSessionId(stackSessionId);
		_handlerName = handlerName;
	}

	@Override
	public String getHandlerName() {
		return _handlerName;
	}

	/**
	 * Sets the session.
	 * 
	 * @param session The session.
	 */
	public void setDiameterSession(DiameterSessionFacade session) {
		_session = session;
	}

	/**
	 * Sets the client peer.
	 * 
	 * @param peer The peer.
	 */
	public void setClientPeer(Peer peer) {
		_clientPeer = peer;
	}
	public void setApplicationClientPeer (DiameterPeer peer){
	    _appClientPeer = peer;
	}
	public void useApplicationClientPeer (boolean use){
	    _useAppClientPeer = use && (_appClientPeer != null); // if null, dont use it.
	}

	/**
	 * Sets the server peer.
	 * 
	 * @param peer The peer.
	 */
	public void setServerPeer(Peer peer) {
		_serverPeer = peer;
	}

	public void setRetryTimeout (Integer seconds){
		_retryTimeoutInMs = seconds * 1000;
	}
	public void setRetryTimeoutInMs (Integer milliseconds){
		_retryTimeoutInMs = milliseconds;
	}
	public Integer getRetryTimeout() {
	    return _retryTimeoutInMs != null ? _retryTimeoutInMs / 1000 : null;
	}
	public Integer getRetryTimeoutInMs() {
	    return _retryTimeoutInMs != null ? _retryTimeoutInMs : null;
	}

	/**
	 * indicates whether this request has been created by a local client.
	 * 
	 * @return true if this request has been created by a local client.
	 */
	public boolean isClientRequest() {
		return (isLocalOrigin() && _isClient);
	}
	public boolean isDirectClientRequest() {
		return isClientRequest () && _executeDirect;
	}

	@Override
	public void writeStackTimestamp (ByteOutputStream baos){
		long ts = getStackTimestamp ();
		if (ts != 0L){
			baos.write ((byte)10); // for a request, there is a single timestamp : we write 10 to help the stack anticipate
			writeStackTimestamp (baos, ts);
		}
	}

	/**
	 * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#isLocalOrigin()
	 */
	@Override
	public boolean isLocalOrigin() {
		return _isLocalRequest;
	}

	/**
	 * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#isRequest()
	 */
	@Override
	public boolean isRequest() {
		return true;
	}

	/**
	 * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#getRequestFacade()
	 */
	@Override
	public DiameterRequestFacade getRequestFacade() {
		return this;
	}

	/**
	 * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#getResponseFacade()
	 */
	@Override
	public DiameterResponseFacade getResponseFacade() {
		if (_response == null)
			_response = new DiameterResponseFacade(this);
		return _response;
	}

	/** identifiers mgmt from the API / DiameterRequest / methods added later, but old methods kept to avoid breakage ***/
	public int getEndToEndIdentifier () { return _endIdentifier;}
	public int getIncomingHopByHopIdentifier (){ return _clientHopIdentifier;}
	public int getOutgoingHopByHopIdentifier (){ return _serverHopIdentifier;}
	/** identifiers mgmt from the API ***/

	/**
	 * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#getClientHopIdentifier()
	 */
	@Override
	public int getClientHopIdentifier() {
		return _clientHopIdentifier;
	}

	private void setServerHopIdentifier(int serverHopIdentifier) {
		_serverHopIdentifier = serverHopIdentifier;
	}

	/**
	 * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#getServerHopIdentifier()
	 */
	@Override
	public int getServerHopIdentifier() {
		return _serverHopIdentifier;
	}

	/**
	 * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#getOutgoingClientHopIdentifier()
	 */
	@Override
	public int getOutgoingClientHopIdentifier() {
		return _serverHopIdentifier;
	}

	/**
	 * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#getEndIdentifier()
	 */
	@Override
	public int getEndIdentifier() {
		return _endIdentifier;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterRequest#hasProxyFlag()
	 */
	public boolean hasProxyFlag() {
		return hasFlag(PROXIABLE_FLAG);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterRequest#hasRetransmissionFlag()
	 */
	public boolean hasRetransmissionFlag() {
		return hasFlag(RETRANSMITTED_FLAG);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterRequest#setProxyFlag(boolean)
	 */
	public void setProxyFlag(boolean flag) {
		setFlag(PROXIABLE_FLAG, flag);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterRequest#setRetransmissionFlag(boolean)
	 */
	public void setRetransmissionFlag(boolean flag) {
		setFlag(RETRANSMITTED_FLAG, flag);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterRequest#getResponse()
	 */
	public DiameterResponse getResponse() {
		if (_response == null) {
			_response = new DiameterResponseFacade(this);
		}
		return _response;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterSession()
	 */
	public DiameterSession getDiameterSession() {
		return _session;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getClientPeer()
	 */
	public DiameterPeer getClientPeer() {
		return _useAppClientPeer ? _appClientPeer : _clientPeer;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getServerPeer()
	 */
	public DiameterPeer getServerPeer() {
		return _serverPeer;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientRequest#getDiameterClient()
	 */
	public DiameterClient getDiameterClient() {
		return _client;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientRequest#attach(java.lang.Object)
	 */
	public void attach(Object attachment) {
		_attachment = attachment;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientRequest#attachment()
	 */
	public Object attachment() {
		return _attachment;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientRequest#execute()
	 */
	public DiameterClientResponse execute()
		throws IOException {
		LOGGER.debug("Synchronous execute");
		// Avoid deadlock if synchronous request is performed within diameter main thread.
		Thread thread = Thread.currentThread();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("execute: currentExecutor id=" + thread.getName());
			LOGGER.debug("execute: Diameter current thread id=" + Utils.getAgentPlatformThread().getName());
		}
		if (thread == Utils.getAgentPlatformThread()) {
			throw new IllegalStateException("Cannot use synchronous client in DIAMETER main thread. "
					+ "(Your proxylet's accept method did not return ACCEPT_MAY_BLOCK)");
		}
		if (!isClientRequest()) {
			// this may happen if we receive a request with a DiameterRequestListener set
			throw new IllegalStateException("Cannot call execute() on a request coming from a peer");
		}

		if (_session != null) {
			if (_session.updateLastAccessedTime() == false) {
				localResponse("Session expired");
				throw new IOException(_failure);
			}
		}
		if (_serverPeer.isLocalDiameterPeer()) {
			try {
				_serverPeer.processMessage(this, false);
			}
			catch (Throwable t) {
				LOGGER.error("Exception while processing synchronous DiameterClient request locally", t);
				localResponse(t.toString());
			}
		} else {
			try {
				executeRemote(true);
			}
			catch (Throwable t) {
				LOGGER.error("Exception while executing synchronous DiameterClient request", t);
				localResponse(t.toString());
			}
		}
		if (_failure == null) {
			return _response;
		}

		throw new IOException(_failure);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClientRequest#execute(com.nextenso.proxylet.diameter.client.DiameterClientListener)
	 */
	public void execute(DiameterClientListener listener) {
		LOGGER.debug("Asynchronous execute");
		if (!isClientRequest()) {
			// this may happen if we receive a request with a DiameterRequestListener set
			throw new IllegalStateException("Cannot call execute() on a request coming from a peer");
		}
		// Store the current thread executor, which will be used to callback the listener
		_callerExecutor = Utils.getCallbackExecutor();
		_callerCL = Thread.currentThread().getContextClassLoader();

		_listener = listener;
		if (_session != null) {
			if (_session.updateLastAccessedTime() == false) {
				localResponse("Session expired");
				return;
			}
		}
		if (_serverPeer.isLocalDiameterPeer()) {
			try {
				_serverPeer.processMessage(DiameterRequestFacade.this, true);
			}
			catch (Throwable t) {
				LOGGER.error("Exception while processing asynchronous DiameterClient request locally", t);
				localResponse("Runtime exception");
			}
		} else {
			executeRemote(true);
		}
	}

	/**
	 * 
	 * @param clientRequest
	 */
	// direct = false means that the request was proxied via LocalPeer prior to being exectuted below (so the response must go to LocalPeer as well)
	public void executeRemote(boolean direct) {
		_executeDirect = direct;
		if (!hasConnectedPeer()) {
			localResponse("No connection to remote host");
			return;
		}

		if (_listener == null) {
			synchronized (this) {
				try {
					_requestCompleted = false;
					send();// throws IllegalStateException if remote peer is not open
					while (!_requestCompleted) {
						wait();
					}
				}
				catch (IllegalStateException ise) {
					localResponse("No connection to remote host");
				}
				catch (InterruptedException ie) {
					localResponse("Request interrupted");
				}
			}
		} else {
			try {
				send();
			}
			catch (IllegalStateException e) {
				localResponse("No connection to remote host");
			}
		}
	}

	private boolean hasConnectedPeer() {
		if (!_serverPeer.isConnected() || _serverPeer.isQuarantined()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("hasConnectedPeer: we change the client  peer, the current one is no more available");
			}

			try {
				Peer peer = null;

				if (_client != null) {
					_client.changePeer();
					peer = _client.getPeer();
				} else {
					peer = Utils.getPeer(getHandlerName(), getDestinationHost(), getDestinationRealm(), getDiameterApplication(), getClientType());
					if (peer == null) {
						throw new java.net.NoRouteToHostException(getDestinationHost() + " at " + getDestinationRealm() + " is unreachable");
					}
				}
				setServerPeer(peer);
			}
			catch (NoRouteToHostException noRouteExc) {
				return false;
			}
		}

		return true;
	}

	private String getDestinationRealm() {
		if (_destinationRealm == null) {
			DiameterAVP destinationRealmAVP = getDiameterAVP(DiameterBaseConstants.AVP_DESTINATION_REALM);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("getDestinationRealm: destinationRealm AVP=" + destinationRealmAVP);
			}

			if (destinationRealmAVP != null) {
				_destinationRealm = IdentityFormat.getIdentity(destinationRealmAVP.getValue());
			}
		}
		return _destinationRealm;
	}

	private String getDestinationHost() {
		if (_destinationHost == null) {
			DiameterAVP destinationHostAVP = getDiameterAVP(DiameterBaseConstants.AVP_DESTINATION_HOST);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("getDestinationHost: destinationHost AVP=" + destinationHostAVP);
			}
			if (destinationHostAVP != null) {
				_destinationHost = IdentityFormat.getIdentity(destinationHostAVP.getValue());
			}
		}
		return _destinationHost;
	}

	/**
	 * Called when the request is sent.
	 */
	public void requestSent(PeerSocket socket) {
		int id = Utils.getRequestManagerKey(this);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("requestSent -  id=" + id + ",  socket=" + socket);
		}
		if (_responseReceived.get()) {
			LOGGER.debug("requestSent: A response had been received before this method was called.");
			return;
		}

		_lastUsedSocket = socket;

		if (_lastUsedSocket.getRequestManager().getRequest(id) == null) {
			_lastUsedSocket.getRequestManager().addRequest(this);
		}

		// We want to schedule the retransmit timer in the same executor.
		PlatformExecutor executor = Utils.getCurrentExecutor();
		Integer timeout = getRetryTimeoutInMs ();
		if (timeout == null) timeout = _serverPeer.getRetryTimeoutInMs();
		long delay = DiameterProperties.getDefaultRetryTimeout();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("requestSent: server timeout in seconds=" + timeout + ", default=" + delay);
		}
		if (timeout != null) {
			delay = timeout;
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("requestSent: used for future in milliseconds=" + delay);
		}

		_resentFuture = Utils.schedule(executor, this, delay, TimeUnit.MILLISECONDS);
	}

	/**
	 * Called when a response is received
	 * 
	 * @param socket
	 */
	public void responseReceived(PeerSocket socket) {
		LOGGER.debug("responseReceived");
		_responseReceived.set(true);
		cancelFuture();

		socket.getRequestManager().removeRequest(this);
		_lastUsedSocket = null;
	}
	public void resetForRedirect (){
		LOGGER.debug ("resetForRedirect");
		setServerHopIdentifier(getNextHopByHopIdentifier()); // to avoid response retransmissions
		_responseReceived.set(false);
		_mustResponseBeProcessed.set(true);
		_serverPeer = null;
		_destinationHost = null;
		_destinationRealm = null;
		removeAttribute (SERVER_PEER_ATTRIBUTE);
	}

	private void cancelFuture() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("cancelFuture: future=" + _resentFuture);
		}

		Future<?> resentFuture = _resentFuture;
		if (resentFuture != null && !resentFuture.isDone()) {
			resentFuture.cancel(true);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("cancelFuture: needed because still living");
			}

			_resentFuture = null;
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("cancelFuture: not needed (null or isDone())");
			}

		}
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			timeout();
		}
		finally {
			Thread.interrupted();
		}
	}

	public void timeout() {
		if (isDirectClientRequest()) {
			int nbMaxRetries = DiameterProperties.getMaxNumberOfRetransmission();
			Integer nb = getServerPeer().getNbRetries();
			if (nb != null) {
				nbMaxRetries = nb;
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("timeout: max nb retries=" + nbMaxRetries + ", nb retransmission=" + _retransmissions);
			}

			if (_retransmissions >= nbMaxRetries) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("timeout: max number of retransmissions has been reached -> do not send again - id=" + Utils.getRequestManagerKey(this));
				}

				remoteResponse("Request Timeout");
			} else {
				_retransmissions++;
				setFlag(RETRANSMITTED_FLAG, true);
				if (!hasConnectedPeer()) {
					remoteResponse("No connection to remote hosts");
					return;
				}

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("timeout: max number of retransmissions has not been reached ->  send again - id=" + Utils.getRequestManagerKey(this));
				}

				try {
					Counter c = Utils.getRetransmissionNbCounter();
					if (c != null) {
						c.add(1L);
					}
					send();// throws IllegalStateException if remote peer is not open
				}
				catch (IllegalStateException ise) {
					remoteResponse("No connection to remote hosts");
				}

			}
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("timeout: cancel the request");
			}
			// for CER and DPR : must timeout the peer state machine
			if (getDiameterCommand() == DiameterBaseConstants.COMMAND_CER || getDiameterCommand() == DiameterBaseConstants.COMMAND_DPR) {
				if (getServerPeer() instanceof RemotePeer) {
					RemotePeer rPeer = (RemotePeer) getServerPeer();
					rPeer.getStateMachine().timeout();
				}
				return;
			}
			cancel(DiameterResponse.UNABLE_TO_DELIVER_CAUSE.TIMEOUT);
		}
	}

	/**
	 * The connection is closed
	 */
	public void cancel(DiameterResponse.UNABLE_TO_DELIVER_CAUSE cause) {
		cancelFuture();
		if (!mustResponseBeProcessed()) { // false means already processed
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("cancel: response must not be processed -> do nothing");
			}
			return;
		}

		if (isDirectClientRequest()) {
			// client request
			remoteResponse("Connection to remote host was closed");
		} else {
			// Diameter based command
		    if (isLocalOrigin() && !isClientRequest()) {
				// do nothing for CER, DWR or DPR
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("cancel: isLocalOrigin, server peer=" + getServerPeer() + ", client peer=" + getClientPeer()+" / remove from RequestManager ");
				}
				_lastUsedSocket.getRequestManager().removeRequest(this);
			} else {
				// proxy request.
			  
			  // Remove the request from the request manager
			  PeerSocket lastUsedSocket = _lastUsedSocket;
			  if (lastUsedSocket != null) {
			    if (LOGGER.isDebugEnabled()) {
			      LOGGER.debug("cancel: remove the request from the request manager");
			    }
			    lastUsedSocket.getRequestManager().removeRequest(this);
			  }
			  
				getResponse().removeDiameterAVPs();
				Utils.cloneAvp(this, _response, DiameterBaseConstants.AVP_SESSION_ID);
				int result = (cause == DiameterResponse.UNABLE_TO_DELIVER_CAUSE.TIMEOUT) ?
				    DiameterProperties.getReqTimeoutResult () : // may be 0
				    (int) DiameterBaseConstants.RESULT_CODE_DIAMETER_UNABLE_TO_DELIVER;
				_response.setResultCode(result);
				if (result == 0){
				    _response.ignore ();
				}
				_response.setUnableToDeliverCause (cause);
				_response.setLocalOrigin(true);
				Utils.handleRoutingAVPs(_response);
				_serverPeer.processMessage(_response, true);
			}
		}
	}

	/**
	 * 
	 * @param failure
	 */
	public void remoteResponse(String failure) {
		_failure = failure;
		if (_lastUsedSocket != null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("remoteResponse: remove the request from the request manager");
			}
			_lastUsedSocket.getRequestManager().removeRequest(this);
			_lastUsedSocket = null;
		}

		if (isAlternativePeerFound()) {
			LOGGER.debug("remoteResponse: send the message to an alternative peer because quarantine is used");
			executeRemote(_isClient);
			return;
		}

		LOGGER.debug("remoteResponse: no alternative found -> process the response");

		if (_listener == null) {
			synchronized (this) {
				_requestCompleted = true;
				notifyAll();
			}
		} else {
			Runnable runnable = new Runnable() {

				public void run() {
					Thread.currentThread().setContextClassLoader(_callerCL);
					if (_failure == null) {
						_listener.handleResponse(DiameterRequestFacade.this, _response);
					} else {
						_listener.handleException(DiameterRequestFacade.this, new IOException(_failure));
					}
				}
			};
			_callerExecutor.execute(runnable);
		}
	}

	public boolean isAlternativePeerFound() {
		if (DiameterProperties.isQuarantineEnabled() && getResponse().getResultCode() == DiameterBaseConstants.RESULT_CODE_DIAMETER_UNABLE_TO_DELIVER) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("isQuarantineUsed: quarantine enabled and result-code=3002 (Unable to deliver) -> quarantine the peer " + getServerPeer());
			}
			getServerPeer().quarantine();

			// find another peer
			boolean newPeerAvailable = hasConnectedPeer();
			if (newPeerAvailable) {
				// forget that a response has been received
				_responseReceived.set(false);
				_mustResponseBeProcessed.set(true);

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("isQuarantineUsed: try another peer after quarantined a peer with retransmisted flag but without incrementing the number of retransmission");
				}
				setFlag(RETRANSMITTED_FLAG, true);

				//  retry to this peer
				return true;
			}
			LOGGER.debug("After a quarantine, no more available peer -> continue with this response");
		}

		return false;
	}

	/**
	 * 
	 * @param failure
	 */
	public void localResponse(String failure) {
		_failure = failure;
		if (_listener != null) {
			Runnable runnable = new Runnable() {

				public void run() {
					Thread.currentThread().setContextClassLoader(_callerCL);
					if (_failure == null) {
						_listener.handleResponse(DiameterRequestFacade.this, _response);
					} else {
						_listener.handleException(DiameterRequestFacade.this, new IOException(_failure));
					}
				}
			};
			_callerExecutor.execute(runnable);
		}
	}

	public void selectHostToProxy()
		throws CannotProxyException {
		// we forward the request
		if (DiameterProperties.checkProxiable ()){
			if (!hasFlag(DiameterRequestFacade.PROXIABLE_FLAG)) {
				LOGGER.debug("selectHostToProxy: not proxiable -> DIAMETER_UNABLE_TO_DELIVER");
				throw new CannotProxyException(DiameterBaseConstants.RESULT_CODE_DIAMETER_UNABLE_TO_DELIVER).unableToDeliverCause (DiameterResponse.UNABLE_TO_DELIVER_CAUSE.NOT_PROXIABLE);
			}
		}
		Peer destinationPeer = (Peer) getAttribute(SERVER_PEER_ATTRIBUTE); // may have been set by application (IPD does it)
		if (destinationPeer != null && (destinationPeer.isQuarantined() || !destinationPeer.isConnected())) {
			LOGGER.debug("selectHostToProxy: the peer requested by application is no more connected (or quarantined), set it to null to try another one if it exists");
			destinationPeer = null;
		}
		String host = getDestinationHost();
		DiameterPeer localPeer = Utils.getTableManager().getLocalDiameterPeer(getHandlerName());
		DiameterSession session = getDiameterSession();
		if (destinationPeer == null){
			for (DiameterProperties.RoutingPolicy policy : DiameterProperties.getRoutingPolicies()) {
				switch (policy) {
				case DESTINATION_HOST:
					if (host != null) {
						// We try to Forward the message (RFC 3588 6.1.5)
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("selectHostToProxy: trying to find a Static peer with this defined Destination-Host=" + host);
						}
						destinationPeer = (Peer) Utils.getTableManager().getDiameterPeer(localPeer, host);
						if (destinationPeer != null && destinationPeer.isConnected() && !destinationPeer.isQuarantined()) {
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("selectHostToProxy: a connected peer with defined Destination-Host has been found: " + destinationPeer);
							}
						} else {
							destinationPeer = null;
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("selectHostToProxy: no connected peer with defined Destination-Host");
							}
						}
					}
					break;

				case ROUTES:
					String realm = getDestinationRealm();
					if (realm != null && localPeer != null) {
						destinationPeer = (Peer) Utils.getRouteTableManager().getDestinationPeer(localPeer, host, realm, getDiameterApplication(), getClientType(), DiameterProperties.doAbsoluteProxyRouting ());
					}
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("selectHostToProxy:  the selected peer for routes is " + destinationPeer);
					}
					break;

				case SESSION:
					if (session != null) {
						destinationPeer = (Peer) session.getAttribute(LAST_USED_PEER_ATTRIBUTE);
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("selectHostToProxy:  the last used peer is " + destinationPeer);
						}
						if (destinationPeer != null && (destinationPeer.isQuarantined() || !destinationPeer.isConnected())) {
							LOGGER.debug("selectHostToProxy: the last used peer is no more connected (or quarantined), set it to null to try another one if it exists");
							destinationPeer = null;
						}
					}
					break;
				}

				if (destinationPeer != null) {
					// quit the for loop
					break;
				}
			}
		}
		
		if (session != null && destinationPeer != null) {
			session.setAttribute(LAST_USED_PEER_ATTRIBUTE, destinationPeer);
		}

		if (destinationPeer == null) {
			LOGGER.debug("selectHostToProxy: no peer, no route -> DIAMETER_UNABLE_TO_DELIVER");
			throw new CannotProxyException(DiameterBaseConstants.RESULT_CODE_DIAMETER_UNABLE_TO_DELIVER).unableToDeliverCause (DiameterResponse.UNABLE_TO_DELIVER_CAUSE.NO_ROUTE);
		}

		if (destinationPeer.isLocalDiameterPeer()) {
			throw new CannotProxyException(DiameterBaseConstants.RESULT_CODE_DIAMETER_LOOP_DETECTED);
		}
		setServerPeer(destinationPeer);

	}

	/**
	 * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#getLocalOriginHost()
	 */
	@Override
	protected String getLocalOriginHost() {
	    Peer peer = (Peer) getServerPeer ();
	    if (peer.isLocalDiameterPeer ())
		return Utils.getServerOriginHost(getHandlerName()); // old behavior : play it safe
	    String res = ((RemotePeer) peer).getLocalOriginHost ();
	    return res;
	}
	protected String getLocalOriginRealm() {
	    Peer peer = (Peer) getServerPeer ();
	    if (peer.isLocalDiameterPeer ())
		return DiameterProperties.getOriginRealm();
	    String res = ((RemotePeer) peer).getLocalOriginRealm ();
	    return res;
	}

	/**
	 * @see com.nextenso.proxylet.engine.AsyncProxyletManager.ProxyletResumer#resumeProxylet(com.nextenso.proxylet.ProxyletData,
	 *      int)
	 */
	@Override
	public void resumeProxylet(ProxyletData message, int status) {
		Utils.getEngine().resume(this, status);
	}

	private final static AtomicInteger END_SEED = new AtomicInteger((int) (System.currentTimeMillis() & 0xFFF) << 20); // follow rfc suggestion

	public int getNextEndToEndIdentifier() {
		return END_SEED.incrementAndGet();
	}

	private static final AtomicInteger HOP_SEED = new AtomicInteger(END_SEED.get());
	public static int HOP_ID_MASK = 0xFFFFFFFF; // may be sent by java ioh to limit the hop id range

	public int getNextHopByHopIdentifier() {
		return HOP_SEED.getAndIncrement() & HOP_ID_MASK;
	}

	@Override
	public void send(PeerSocket socket) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("write: socket=" + socket);
		}
		Utils.handleRoutingAVPs(this);
		if (socket != null){
		    socket.handleRoutingAVPs (this);
		    requestSent(socket);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("write: Sending message :\n" + this);
		}

		boolean sent = socket != null && socket.write(this); // socket may be null in multithreaded mode
		if (!sent) {
			remoteResponse("cannot send request");
		}

	}

	public boolean mustResponseBeProcessed() {
		return _mustResponseBeProcessed.compareAndSet(true, false);
	}

	public void send() {
		_response = null;
		_serverPeer.sendMessage(this);
	}

	@Override
	public DiameterClientResponse getDiameterClientResponse() {
		return getResponseFacade();
	}

}
