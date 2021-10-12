// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.impl;

import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.ha.HaManager;
import com.nextenso.diameter.agent.peer.Peer;
import com.nextenso.diameter.agent.peer.RemotePeer;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterSession;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import alcatel.tess.hometop.gateways.utils.ByteOutputStream;

public class DiameterResponseFacade
		extends DiameterMessageFacade
		implements DiameterClientResponse {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.response");

	private static final Integer INT_1 = Integer.valueOf (1);
	private static final String REDIRECT_ATTR = "redirect";

	public static final int E_FLAG = 0x20;

	private DiameterRequestFacade _request;
	private boolean _isLocalResponse = false;
	private boolean _ignore = false; // used to drop a response upon timeout (if configured to do so)

	public DiameterResponseFacade(DiameterRequestFacade request) {
		super(request.getDiameterApplication(), request.getDiameterCommand(),
					request.hasProxyFlag() ? DiameterRequestFacade.PROXIABLE_FLAG : DiameterRequestFacade.NO_FLAG);
		_request = request;
	}

	// called when a response pxlet returns REDIRECT : we clean the response object
	public void resetForRedirect (){
		removeDiameterAVPs ();
		setDefaultAVPs ();
		setFlags (_request.hasProxyFlag() ? DiameterRequestFacade.PROXIABLE_FLAG : DiameterRequestFacade.NO_FLAG);
		Integer i = (Integer) _request.getAttribute (REDIRECT_ATTR);
		if (i == null) _request.setAttribute (REDIRECT_ATTR, INT_1);
		else _request.setAttribute (REDIRECT_ATTR, Integer.valueOf (i+1));
		_isLocalResponse = false;
		_request.resetForRedirect ();
	}

	public void setDefaultAVPs() {
		// add session-id AVP
		DiameterAVP avp = _request.getDiameterAVP(DiameterBaseConstants.AVP_SESSION_ID);
		if (avp != null) {
			addDiameterAVP((DiameterAVP) avp.clone());
		}

		setDefaultOriginAVPs();

		// add application-id AVP
		if (getDiameterApplication() != DiameterBaseConstants.APPLICATION_BASE_ACCOUNTING
				&& getDiameterApplication() != DiameterBaseConstants.APPLICATION_COMMON_MESSAGES) {
			avp = _request.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
			if (avp != null) {
				addDiameterAVP((DiameterAVP) avp.clone());
				return;
			}
		}
		avp = _request.getDiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
		if (avp != null) {
			addDiameterAVP((DiameterAVP) avp.clone());
			return;
		}
		avp = _request.getDiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
		if (avp != null) {
			addDiameterAVP((DiameterAVP) avp.clone());
			return;
		}
	}

	public void setLocalOrigin(boolean local) {
		_isLocalResponse = local;
	}

	public void setUnableToDeliverCause (UNABLE_TO_DELIVER_CAUSE cause){
		if (cause == null) return; // possible in theory if triggered by CannotProxyException
		setAttribute (ATTR_UNABLE_TO_DELIVER_CAUSE, cause);
		DiameterAVP avp = new DiameterAVP (DiameterBaseConstants.AVP_ERROR_MESSAGE);
		avp.setValue (UTF8StringFormat.toUtf8String (cause.errorMessage ()), false);
		addDiameterAVP (avp);
	}

	public void ignore (){ _ignore = true;}
	public boolean ignorable (){ return _ignore;}

	/******************* abstract methods ****************/

	@Override
	public void writeStackTimestamp (ByteOutputStream baos){
		// for a response, there are 1 or 2 timestamps
		long ts1 = _request.getStackTimestamp ();
		if (ts1 == 0L) return;
		long ts2 = getStackTimestamp ();
		if (ts2 == 0L){
			// server case
			baos.write ((byte)10);
			writeStackTimestamp (baos, ts1);
		} else {
			baos.write ((byte)20); 
			writeStackTimestamp (baos, ts1);
			writeStackTimestamp (baos, ts2);
		}
	}

	@Override
	public boolean isLocalOrigin() {
		return _isLocalResponse;
	}

	@Override
	public boolean isRequest() {
		return false;
	}

	@Override
	public DiameterRequestFacade getRequestFacade() {
		return _request;
	}

	@Override
	public DiameterResponseFacade getResponseFacade() {
		return this;
	}

	@Override
	public int getClientHopIdentifier() {
		return _request.getClientHopIdentifier();
	}

	@Override
	public int getServerHopIdentifier() {
		return _request.getServerHopIdentifier();
	}

	@Override
	public int getOutgoingClientHopIdentifier() {
		return _request.getClientHopIdentifier();
	}

	@Override
	public int getEndIdentifier() {
		return _request.getEndIdentifier();
	}

	/**********************************
	 * Implementation of DiameterResponse
	 *********************************/

	public long getResultCode() {
		DiameterAVP avp = getDiameterAVP(DiameterBaseConstants.AVP_RESULT_CODE);
		if (avp == null) {
			return -1L;
		}
		return Unsigned32Format.getUnsigned32(avp.getValue(), 0);
	}

	public void setResultCode(long code) {
		DiameterAVP avp = getDiameterAVP(DiameterBaseConstants.AVP_RESULT_CODE);
		if (avp == null) {
			avp = new DiameterAVP(DiameterBaseConstants.AVP_RESULT_CODE);
			addDiameterAVP(avp);
		}
		avp.setValue(Unsigned32Format.toUnsigned32(code), false);
		setErrorFlag(code > 3000L && code < 4000L);
	}

	public boolean hasErrorFlag() {
		return hasFlag(E_FLAG);
	}

	public void setErrorFlag(boolean flag) {
		setFlag(E_FLAG, flag);
	}

	public DiameterRequest getRequest() {
		return _request;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterMessage#getDiameterSession()
	 */
	public DiameterSession getDiameterSession() {
		return _request.getDiameterSession();
	}

	public DiameterPeer getClientPeer() {
		return _request.getClientPeer();
	}

	public DiameterPeer getServerPeer() {
		return _request.getServerPeer();
	}

	/**********************************
	 * Implementation of DiameterClientResponse
	 *********************************/

	public DiameterClient getDiameterClient() {
		return _request.getDiameterClient();
	}

	public DiameterClientRequest getDiameterClientRequest() {
		return _request;
	}

	public void send()
		throws java.io.IOException {
		if (_request.isClientRequest()) {
			throw new IllegalStateException("Cannot call send() on a response received from a peer");
		}
		try {
			((Peer) getClientPeer()).sendMessage(this);
		}
		catch (IllegalStateException e) {
			throw new java.io.IOException("No connection to remote host");
		}
	}

	@Override
	public String getHandlerName() {
		return _request.getHandlerName();
	}

	/**
	 * @see com.nextenso.diameter.agent.impl.DiameterMessageFacade#getLocalOriginHost()
	 */
	@Override
	protected String getLocalOriginHost() {
	    Peer peer = (Peer) getClientPeer ();
	    if (peer == null || peer.isLocalDiameterPeer ()) // peer may be null when rejecting a CER
		return Utils.getServerOriginHost(getHandlerName()); // old behavior : play it safe
	    String res = ((RemotePeer) peer).getLocalOriginHost ();
	    return res;
	}
	protected String getLocalOriginRealm() {
	    Peer peer = (Peer) getClientPeer ();
	    if (peer == null || peer.isLocalDiameterPeer ()) // peer may be null when rejecting a CER
		return DiameterProperties.getOriginRealm();
	    String res = ((RemotePeer) peer).getLocalOriginRealm ();
	    return res;
	}

	/**
	 * @see com.nextenso.proxylet.engine.AsyncProxyletManager.ProxyletResumer#resumeProxylet(com.nextenso.proxylet.ProxyletData,
	 *      int)
	 */
	@Override
	public void resumeProxylet(ProxyletData msg, int status) {
		Utils.getEngine().resume(this, status);
	}

	@Override
	public void send(PeerSocket socket) {
		if (ignorable ()){
			LOGGER.debug("ignorable -> do not send");
			return;
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("write: socket=" + socket);
		}

		Utils.handleRoutingAVPs(this);
		if (socket != null) socket.handleRoutingAVPs (this);
		if (DiameterProperties.isHa()) {
			if (getSessionId() != null) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("write: HA is supported and message has a session id-> save session");
				}
				HaManager.setSession((DiameterSessionFacade) getDiameterSession(), null);
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("write: Sending message :\n" + this);
		}

		if (socket != null){
		    boolean ok = socket.write(this); // socket may be null in multithreaded mode
		    if (ok){
			try{
			    fireProxyletEvent (new com.nextenso.proxylet.event.FlushEvent (this, this), false);
			}catch(Throwable t){
			    LOGGER.warn ("Exception while firing MessageFlushedEvent", t);
			}
		    }
		}
	}

}
