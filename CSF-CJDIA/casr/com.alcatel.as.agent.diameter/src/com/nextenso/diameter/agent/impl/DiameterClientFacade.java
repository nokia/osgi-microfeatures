package com.nextenso.diameter.agent.impl;

import java.net.NoRouteToHostException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.peer.Peer;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterMessage;
import com.nextenso.proxylet.diameter.DiameterRequest;
import com.nextenso.proxylet.diameter.DiameterSession;
import com.nextenso.proxylet.diameter.client.DiameterClient;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.client.DiameterRequestListener;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.IdentityFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Diameter client implementation
 */
public class DiameterClientFacade
		implements DiameterClient {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.client");

	public static final Object DIAMETER_REQUEST_LISTENER_ATTRIBUTE = "DiameterRequestListener";

	private Peer _peer;
	private DiameterSessionFacade _session;
	private DiameterAVP _destHostAVP, _destRealmAVP, _sessionIdAVP, _authIdAVP, _acctIdAVP;
	private boolean _vendorSpecific;
	private String _destHost, _destRealm;
	private long _applicationId, _vendorId;
	private int _type;

	private String _handlerName;

	private DiameterClientFacade(String handlerName, String destinationHost, String destinationRealm, int type)
			throws NoRouteToHostException {
		String destHost = destinationHost;
		if (destHost != null && destHost.length() == 0) {
			destHost = null;
		}

		String destRealm = destinationRealm;
		if (destRealm != null && destRealm.length() == 0) {
			destRealm = null;
		}

		if (destHost == null && destRealm == null) {
			throw new NoRouteToHostException("No destination specified");
		}

		_destHost = destHost;
		_destRealm = destRealm;
		_handlerName = handlerName;
		_type = type;

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("new Client: destHost=" + getDestinationHost() + ", destRealm=" + getDestinationRealm() + ", handler=" + getHandlerName()
					+ ", type=" + getType());
		}

	}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param handlerName
	 * @param destinationHost
	 * @param destinationRealm
	 * @param vendorId
	 * @param applicationId
	 * @param type
	 * @param stateful
	 * @param lifetime
	 * @throws NoRouteToHostException
	 */
	public DiameterClientFacade(String handlerName, String destinationHost, String destinationRealm, long vendorId, long applicationId, int type,
			boolean stateful, long lifetime)
			throws NoRouteToHostException {
		this(handlerName, destinationHost, destinationRealm, type);
		_applicationId = applicationId;
		_vendorId = vendorId;

		changePeer();
		if (stateful) {
			_session = new DiameterSessionFacade(vendorId, applicationId, lifetime, getPeer());
		}

		init(vendorId, applicationId);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("new Client: Session=" + getDiameterSession());
			LOGGER.debug("new Client: Session Id AVP=" + _sessionIdAVP);
		}

	}

	public DiameterClientFacade(String handlerName, String destinationHost, String destinationRealm, long vendorId, long applicationId, int type,
			String sessionId, long lifetime)
			throws NoRouteToHostException {
		this(handlerName, destinationHost, destinationRealm, type);
		_applicationId = applicationId;
		_vendorId = vendorId;
		changePeer();
		if (sessionId != null) {
			_session = new DiameterSessionFacade(vendorId, applicationId, lifetime, getPeer(), sessionId);
		}

		init(vendorId, applicationId);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("new Client: Session=" + getDiameterSession());
			LOGGER.debug("new Client: Session id AVP=" + _sessionIdAVP);
		}
	}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param handlerName
	 * @param destinationHost
	 * @param destinationRealm
	 * @param session
	 * @throws NoRouteToHostException
	 */
	public DiameterClientFacade(String handlerName, String destinationHost, String destinationRealm, DiameterSession session)
			throws NoRouteToHostException {
		this(handlerName, destinationHost, destinationRealm, TYPE_ACCT | TYPE_AUTH);

		_session = (DiameterSessionFacade) session;

		init(session.getDiameterApplicationVendorId(), session.getDiameterApplication());
		changePeer();
	}

	public DiameterClientFacade(DiameterRequest request)
			throws NoRouteToHostException {

		DiameterRequestFacade req = (DiameterRequestFacade) request;
		_handlerName = req.getHandlerName();
		_type = TYPE_ACCT | TYPE_AUTH;

		DiameterAVP avp = req.getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
		if (avp != null) {
			_destHost = IdentityFormat.getIdentity(avp.getValue());
		}

		avp = req.getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_REALM);
		if (avp != null) {
			_destRealm = IdentityFormat.getIdentity(avp.getValue());
		}

		long applicationId = req.getDiameterApplication();
		long vendorId = 0;
		_session = (DiameterSessionFacade) req.getDiameterSession();

		avp = req.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
		if (avp != null && avp.getValue() != null) {
			avp = GroupedFormat.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID, avp.getValue(), false);
			if (avp != null && avp.getValue() != null) {
				_vendorId = Unsigned32Format.getUnsigned32(avp.getValue(), 0);
			}
		}

		_peer = (Peer) req.getClientPeer();

		init(vendorId, applicationId);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("new Client (request): destHost=" + getDestinationHost() + ", destRealm=" + getDestinationRealm() + ", handler="
					+ getHandlerName() + ", type=" + getType());
			LOGGER.debug("new Client (request): Session=" + getDiameterSession());
			LOGGER.debug("new Client (request): Session id AVP=" + _sessionIdAVP);
		}

	}

	/**
	 * Changes the peer.
	 * 
	 * @throws NoRouteToHostException if no more peer can be found.
	 */
	public void changePeer()
		throws NoRouteToHostException {
		_peer = Utils.getPeer(_handlerName, _destHost, _destRealm, _applicationId, _type);

		if (_peer == null) {
			throw new java.net.NoRouteToHostException(_destHost + " at " + _destRealm + " is unreachable");
		}
		_handlerName = _peer.getHandlerName();

		if (_session != null) {
			_session.setDiameterPeer(_peer);
		}
	}

	/**
	 * 
	 * @param destHost
	 * @param destRealm
	 * @param vendorId
	 * @param applicationId
	 * @throws java.net.NoRouteToHostException
	 */
	private void init(long vendorId, long applicationId)
		throws java.net.NoRouteToHostException {
		// check the applicationId ??

		if (_session != null) {
			_sessionIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_SESSION_ID);
			_sessionIdAVP.addValue(UTF8StringFormat.toUtf8String(_session.getSessionId()), false);
		}
		if (_destHost != null) {
			_destHostAVP = new DiameterAVP(DiameterBaseConstants.AVP_DESTINATION_HOST);
			_destHostAVP.addValue(IdentityFormat.toIdentity(_destHost), false);
		}
		if (_destRealm != null) {
			_destRealmAVP = new DiameterAVP(DiameterBaseConstants.AVP_DESTINATION_REALM);
			_destRealmAVP.addValue(IdentityFormat.toIdentity(_destRealm), false);
		}

		_applicationId = applicationId;
		_vendorId = vendorId;
		_vendorSpecific = (vendorId != 0);
		if (_vendorSpecific == false) {
			_authIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
			_authIdAVP.addValue(Unsigned32Format.toUnsigned32(applicationId), false);

			_acctIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
			_acctIdAVP.addValue(Unsigned32Format.toUnsigned32(applicationId), false);
		} else {
			_authIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
			_authIdAVP.addValue(Unsigned32Format.toUnsigned32(applicationId), false);
			_acctIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
			_acctIdAVP.addValue(Unsigned32Format.toUnsigned32(applicationId), false);
			DiameterAVP vendorIdAVP = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID);
			vendorIdAVP.addValue(Unsigned32Format.toUnsigned32(vendorId), false);

			DiameterAVP avp = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
			ArrayList list = new ArrayList();
			list.add(vendorIdAVP);
			list.add(_authIdAVP);
			avp.addValue(GroupedFormat.toGroupedAVP(list), false);
			_authIdAVP = avp;

			list.clear();
			avp = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
			list.add(vendorIdAVP);
			list.add(_acctIdAVP);
			avp.addValue(GroupedFormat.toGroupedAVP(list), false);
			_acctIdAVP = avp;
		}
	}

	/**
	 * Gets the type.
	 * 
	 * @return The type.
	 */
	public int getType() {
		return _type;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClient#getDestinationHost()
	 */
	public String getDestinationHost() {
		return _destHost;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClient#getDestinationRealm()
	 */
	public String getDestinationRealm() {
		return _destRealm;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClient#getDiameterApplication()
	 */
	public long getDiameterApplication() {
		return _applicationId;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClient#getDiameterApplicationVendorId()
	 */
	public long getDiameterApplicationVendorId() {
		return _vendorId;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClient#getDiameterSession()
	 */
	public DiameterSession getDiameterSession() {
		return _session;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClient#newAuthRequest(int,
	 *      boolean)
	 */
	public DiameterClientRequest newAuthRequest(int commandCode, boolean proxiable) {
		return newRequestFacade(commandCode, proxiable, _authIdAVP);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClient#newAcctRequest(int,
	 *      boolean)
	 */
	public DiameterClientRequest newAcctRequest(int commandCode, boolean proxiable) {
		return newRequestFacade(commandCode, proxiable, _acctIdAVP);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClient#newRequest(int,
	 *      boolean)
	 */
	public DiameterClientRequest newRequest(int commandCode, boolean proxiable) {
		return newRequestFacade(commandCode, proxiable, null);
	}

	/**
	 * 
	 * @param commandCode
	 * @param proxiable
	 * @param avp
	 * @return The request.
	 */
	public DiameterRequestFacade newRequestFacade(int commandCode, boolean proxiable, DiameterAVP avp) {
		DiameterRequestFacade request = new DiameterRequestFacade(this, _applicationId, commandCode, proxiable);
		if (avp != null) {
			if (_sessionIdAVP != null) {
				request.addDiameterAVP((DiameterAVP) _sessionIdAVP.clone());
			}
			request.addDiameterAVP(avp);
			request.setDefaultOriginAVPs();

			if (_destHostAVP != null) {
				request.addDiameterAVP((DiameterAVP) _destHostAVP.clone());
			}
			if (_destRealmAVP != null) {
				request.addDiameterAVP((DiameterAVP) _destRealmAVP.clone());
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("newRequestFacade: request=" + request);
		}
		return request;
	}
	

	@Override
	public void fillMessage(DiameterMessage msg) {
		
		DiameterAVP sessionId = msg.getDiameterAVP(DiameterBaseConstants.AVP_SESSION_ID);
		if(sessionId != null && _sessionIdAVP != null) {
			sessionId.setValue(_sessionIdAVP.getValue(), true);
		}
		
		DiameterAVP destHost = msg.getDiameterAVP(DiameterBaseConstants.AVP_DESTINATION_HOST);
		if(destHost != null && _destHostAVP != null) {
			destHost.setValue(_destHostAVP.getValue(), true);
		}
		
		DiameterAVP destRealm = msg.getDiameterAVP(DiameterBaseConstants.AVP_DESTINATION_REALM);
		if(destRealm != null && _destRealmAVP != null) {
			destRealm.setValue(_destRealmAVP.getValue(), true);
		}
		
		DiameterAVP authAppId = msg.getDiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
		if(authAppId != null && _authIdAVP != null) {
			authAppId.setValue(Unsigned32Format.toUnsigned32(_applicationId), true);
		}
		
		DiameterAVP accAppId = msg.getDiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
		if(accAppId != null && _acctIdAVP != null) {
			accAppId.setValue(Unsigned32Format.toUnsigned32(_applicationId), true);
		}
		
		DiameterAVP appSpecificId = msg.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
		if(appSpecificId != null && _acctIdAVP != null && _vendorSpecific) {
			appSpecificId.setValue(_authIdAVP.getValue(), true);
		}
	}

	/**
	 * Gets the peer.
	 * 
	 * @return The peer.
	 */
	public Peer getPeer() {
		return _peer;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClient#close()
	 */
	public void close() {
		if (_session != null)
			_session.destroy();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.client.DiameterClient#setDiameterRequestListener(com.nextenso.proxylet.diameter.client.DiameterRequestListener)
	 */
	public void setDiameterRequestListener(DiameterRequestListener listener) {
		if (listener == null) {
			throw new NullPointerException("DiameterRequestListener cannot be null");
		}
		if (_session == null) {
			throw new IllegalStateException("Cannot set a DiameterRequestListener on a stateless client");
		}
		if (!_session.isClientSession()) {
			throw new IllegalStateException("Cannot set a DiameterRequestListener on a Session not initiated by the client");
		}

		boolean first = (_session.getAttribute(DIAMETER_REQUEST_LISTENER_ATTRIBUTE) == null);
		_session.setAttribute(DIAMETER_REQUEST_LISTENER_ATTRIBUTE, listener);
		if (first) {
			_peer.getSessionManager().addSession(_session);
		}
	}

	public String getHandlerName() {
		return _handlerName;
	}
}
