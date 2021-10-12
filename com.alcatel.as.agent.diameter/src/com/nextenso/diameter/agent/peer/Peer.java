// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.SessionManager;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.engine.DiameterProxyletEngine.Action;
import com.nextenso.diameter.agent.engine.MessageProcessingListener;
import com.nextenso.diameter.agent.ha.SessionListener;
import com.nextenso.diameter.agent.impl.CannotProxyException;
import com.nextenso.diameter.agent.impl.DiameterClientFacade;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.diameter.agent.impl.DiameterResponseFacade;
import com.nextenso.diameter.agent.impl.DiameterSessionFacade;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterApplication;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeerListener;
import com.nextenso.proxylet.diameter.DiameterSession;
import com.nextenso.proxylet.diameter.client.DiameterRequestListener;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.IdentityFormat;
import com.nextenso.proxylet.engine.ProxyletEngineException;

public abstract class Peer
		implements DiameterPeer {

	public interface ProcessMessageResultListener {

		public void handleResult(DiameterMessageFacade result);
	}

	private static class PeerSessionListener
			implements SessionListener {

		private final Peer _peer;
		private final DiameterRequestFacade _request;
		private final boolean _mainThread;
		private final ProcessMessageResultListener _resultListener;

		public PeerSessionListener(Peer peer, DiameterRequestFacade request, boolean mainThread, ProcessMessageResultListener resultListener) {
			_peer = peer;
			_request = request;
			_mainThread = mainThread;
			_resultListener = resultListener;
		}

		public void handleSession(DiameterSessionFacade session) {
			_peer.processRequestAfterGettingSession(session, _request, _mainThread, _resultListener);
		}

	}

	private static class RequestProcessingListener
			implements MessageProcessingListener {

		private final Peer _peer;
		private final DiameterRequestFacade _request;
		private final boolean _mainThread;
		private final ProcessMessageResultListener _resultListener;

		public RequestProcessingListener(Peer peer, DiameterRequestFacade message, boolean mainThread, ProcessMessageResultListener resultListener) {
			_peer = peer;
			_request = message;
			_mainThread = mainThread;
			_resultListener = resultListener;
			_request.useApplicationClientPeer (true);
		}

		@Override
		public void messageProcessed(DiameterMessageFacade message, Action action) {
			_request.useApplicationClientPeer (false);
			_peer.processRequestAfterEngine(action, _request, _mainThread, _resultListener);
		}

		@Override
		public void messageProcessingError(DiameterMessageFacade message, ProxyletEngineException error) {
			_request.useApplicationClientPeer (false);
			Utils.logProxyletException(error);
			if (_resultListener != null) {
				_resultListener.handleResult(null);
			}
			return;
		}
	}

	private static class ResponseProcessingListener
			implements MessageProcessingListener {

		private final Peer _peer;
		private final DiameterResponseFacade _response;
		private final ProcessMessageResultListener _resultListener;

		public ResponseProcessingListener(Peer peer, DiameterResponseFacade message, ProcessMessageResultListener resultListener) {
			_peer = peer;
			_response = message;
			_resultListener = resultListener;
			_response.getRequestFacade ().useApplicationClientPeer (true);
		}

		@Override
		public void messageProcessed(DiameterMessageFacade message, Action action) {
			_response.getRequestFacade ().useApplicationClientPeer (false);
			_peer.processResponseAfterEngine(action, _response, _resultListener);
		}

		@Override
		public void messageProcessingError(DiameterMessageFacade message, ProxyletEngineException error) {
			_response.getRequestFacade ().useApplicationClientPeer (false);
			Utils.logProxyletException(error);
			if (_resultListener != null) {
				_resultListener.handleResult(null);
			}
			return;
		}

	}
	
	protected final static AtomicLong SEED_LOCAL = new AtomicLong(0x4000000000000000L);
	protected final static AtomicLong SEED_REMOTE_R = new AtomicLong(0x2000000000000000L);
	protected final static AtomicLong SEED_REMOTE_I_SHARED = new AtomicLong(0x1000000000000000L);
	protected final static AtomicLong SEED_REMOTE_I_NOT_SHARED = new AtomicLong(0x0800000000000000L);
	
	private final String _handlerName;

	private String _originHost;
	private String _host;
	private int _port;
	private List<String> _hosts;
	private Integer _nbRetry;
	private Integer _retryTimeout;
	private Integer _retryTimeoutInMs;
	private long _id;
	private java.util.Map<Object, Object> _attributes = new java.util.concurrent.ConcurrentHashMap<> ();

	private final Protocol _protocol;

	private final SessionManager _sessionManager;
	private final List<ListenerWithExecutor> _listeners = new CopyOnWriteArrayList<ListenerWithExecutor>();

	private DiameterAVP _originHostAvp;

	public long getId(){ return _id;}
	public void setId (long id){ _id = id;}

	public abstract void sendMessage(DiameterMessageFacade message)
		throws IllegalStateException;

	public abstract void processMessage(DiameterMessageFacade message, boolean mainThread);

	public DiameterAVP getLocalOriginHostAvp(){
	    // NOT USED / kept for baselining
	    throw new RuntimeException ("Not implemented");
	}

	protected abstract Logger getLogger();

	protected Peer(String handlerName, String originHost, Protocol protocol) {
		_handlerName = handlerName;
		setOriginHost(originHost);
		_protocol = protocol;
		_sessionManager = new SessionManager();
	}

	protected Peer(String handlerName, String originHost, String host, int port, Protocol protocol) {
		this(handlerName, originHost, protocol);
		if (host == null) throw new IllegalArgumentException ("Remote host is null");
		_hosts = new ArrayList<String>(1);
		_hosts.add (host);
		_host = host; // we still keep it set in this case for backwards compliancy
		_port = port;
	}
	protected Peer(String handlerName, String originHost, List<String> hosts, int port, Protocol protocol) {
		this(handlerName, originHost, protocol);
		if (hosts == null || hosts.size () == 0) throw new IllegalArgumentException ("Remote hosts list is empty");
		_hosts = hosts;
		_port = port;
		_host = _hosts.get (0);
	}
	public void setHost (String host){
		_host = host; // updated by PeerSocket when trying the next configured host
	}

	public String getHandlerName() {
		return _handlerName;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#setRelay()
	 */
	public void setRelay()
		throws UnsupportedOperationException {
		throw new UnsupportedOperationException("setRelay is not supported");
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getHost()
	 */
	public String getHost() {
		return _host;
	}
	public List<String> getConfiguredHosts() {
		return _hosts;
	}
	
	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getPort()
	 */
	public int getPort() {
		return _port;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getOriginHost()
	 */
	public final String getOriginHost() {
		return _originHost;
	}

	protected final void setOriginHost(String originHost) {
		_originHost = originHost;
		if (originHost != null) {
			if (_originHostAvp == null) {
				_originHostAvp = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
			}
			_originHostAvp.setValue(IdentityFormat.toIdentity(originHost), false);
		} else {
			_originHostAvp = null;
		}
	}

	public DiameterAVP getOriginHostAvp() {
		return _originHostAvp;
	}

	public SessionManager getSessionManager() {
		return _sessionManager;
	}

	/**
	 * 
	 * @param request
	 * @param mainThread
	 */
	protected void processRequest(final DiameterRequestFacade request, boolean mainThread, LocalPeer localPeer,
			ProcessMessageResultListener resultListener) {

		if (!request.isLocalOrigin()) {

			if (mainThread) {
				request.setClientPeer(this);

				// check if the application was advertised
				if (!Utils.getCapabilities().isCompliantMessage(request)) {
					if (getLogger().isEnabledFor(Level.DEBUG)) {
						getLogger().debug("Received request with unsupported Application-Id / Vendor-Id - returning DIAMETER_APPLICATION_UNSUPPORTED for "
								+ request);
					}
					DiameterResponseFacade response = request.getResponseFacade();
					Utils.cloneAvp(request, response, DiameterBaseConstants.AVP_SESSION_ID);
					response.setResultCode(DiameterBaseConstants.RESULT_CODE_DIAMETER_APPLICATION_UNSUPPORTED);
					response.setLocalOrigin(true);
					// the response is not processed
					if (resultListener != null) {
						resultListener.handleResult(response);
					}
					return;
				}

				// check localLoop - configurable
				if (Utils.isCheckLoop()) {
					DiameterAVP routeAVP = request.getDiameterAVP(DiameterBaseConstants.AVP_ROUTE_RECORD);
					if (routeAVP != null) {
						int size = routeAVP.getValueSize();

						byte[] localOriginHost = null;
						if (localPeer.getOriginHostAvp() != null) {
							localOriginHost = localPeer.getOriginHostAvp().getValue();
						}

						for (int i = 0; i < size; i++)
							if (java.util.Arrays.equals(localOriginHost, routeAVP.getValue(i))) {
								if (getLogger().isEnabledFor(Level.DEBUG)) {
									getLogger().debug("Received request with local peer in route-record - returning DIAMETER_LOOP_DETECTED for " + request);
								}
								DiameterResponseFacade response = request.getResponseFacade();
								Utils.cloneAvp(request, response, DiameterBaseConstants.AVP_SESSION_ID);
								response.setResultCode(DiameterBaseConstants.RESULT_CODE_DIAMETER_LOOP_DETECTED);
								response.setLocalOrigin(true);
								// the response is not processed
								if (resultListener != null) {
									resultListener.handleResult(response);
								}
								return;
							}
					}
				}

				// Load the response default AVPs
				request.getResponseFacade().setDefaultAVPs();

				// Associate  the session
				String sessionId = request.getSessionId();
				if (sessionId != null) {
					SessionListener sessionListener = new PeerSessionListener(this, request, mainThread, resultListener);
					if (getLogger().isDebugEnabled()) {
						getLogger().debug("processRequest: asynchronously (or not) get the session");
					}
					_sessionManager.getSession(request, sessionListener);
					return;
				}

			}
		}
		processRequestEnd(request, mainThread, resultListener);
	}

	/**
	 * 
	 * @param foundSession
	 * @param request
	 * @param mainThread
	 * @param resultListener
	 */
	private void processRequestAfterGettingSession(DiameterSessionFacade foundSession, DiameterRequestFacade request, boolean mainThread,
			ProcessMessageResultListener resultListener) {
		if (getLogger().isDebugEnabled()) {
			getLogger().debug("processRequestAfterGettingSession: session=" + foundSession);
		}
		DiameterRequestListener reqListener = null;
		DiameterSessionFacade session = foundSession;
		if (session == null || session.updateLastAccessedTime() == false) {
			long lifetime = DiameterProperties.getSessionLifetime();
			if (lifetime >= 0) {
				session = new DiameterSessionFacade(request, _sessionManager, lifetime);
				_sessionManager.addSession(session);
			} else {
				if (getLogger().isDebugEnabled()) {
					getLogger().debug("processRequestAfterGettingSession: negative lifetime -> no session");
				}
			}
		} else {
			Object o = session.getAttribute(DiameterClientFacade.DIAMETER_REQUEST_LISTENER_ATTRIBUTE);
			if (o != null) {
				reqListener = (DiameterRequestListener) o;
			}
		}
		request.setDiameterSession(session);

		if (reqListener != null) {
			if (getLogger().isDebugEnabled()) {
				getLogger().debug("processRequestAfterGettingSession: a request listener is found -> only call the listener");
			}
			reqListener.handleRequest(request, request.getResponseFacade());
			if (resultListener != null) {
				resultListener.handleResult(null);
			}
			return;
		}

		processRequestEnd(request, mainThread, resultListener);
	}

	/**
	 * 
	 * @param request
	 * @param mainThread
	 * @param resultListener
	 */
	private void processRequestEnd(DiameterRequestFacade request, boolean mainThread, ProcessMessageResultListener resultListener) {
		if (getLogger().isDebugEnabled()) {
			getLogger().debug("processRequestEnd");
		}

		RequestProcessingListener listener = new RequestProcessingListener(this, request, mainThread, resultListener);
		Utils.getEngine().handleRequest(request, !mainThread, listener);
	}

	/**
	 * 
	 * @param action
	 * @param request
	 * @param mainThread
	 * @param resultListener
	 */
	public void processRequestAfterEngine(Action action, final DiameterRequestFacade request, boolean mainThread,
			final ProcessMessageResultListener resultListener) {
		getLogger().debug("processRequestAfterEngine...");

		if (action == null){
			// NO_RESPONSE
			if (resultListener != null) {
				resultListener.handleResult(null);
			}
			return;
		}

		if (action == Action.REQUEST) {
			try {
				request.selectHostToProxy();
				if (resultListener != null) {
					resultListener.handleResult(request);
				}
			}
			catch (CannotProxyException e) {
				// Cannot proxy the request -> send error response
				DiameterSession session = request.getDiameterSession();
				if (session != null) {
					session.destroy();
				}

				DiameterResponseFacade response = request.getResponseFacade();
				response.removeDiameterAVPs();
				Utils.cloneAvp(request, response, DiameterBaseConstants.AVP_SESSION_ID);
				response.setResultCode(e.getReason());
				response.setUnableToDeliverCause (e.unableToDeliverCause ());
				response.setLocalOrigin(true);
				// acts as a server
				Utils.handleRoutingAVPs(response);
				processResponse(response, mainThread, resultListener);
			}
			return;
		}

		if (action == Action.RESPONSE) {
			DiameterResponseFacade response = request.getResponseFacade();
			if (resultListener != null) {
				resultListener.handleResult(response);
			}
			return;
		}

		if (action == Action.MAY_BLOCK_REQUEST) {
			Utils.start(new Runnable() {

				public void run() {
					processRequestEnd(request, false, resultListener);
				}
			});
		} else if (action == Action.MAY_BLOCK_RESPONSE) {
			final DiameterResponseFacade response = request.getResponseFacade();
			Utils.start(new Runnable() {

				public void run() {
					processResponse(response, false, resultListener);
				}
			});
		}
	}

	/**
	 * 
	 * @param response
	 * @param mainThread
	 */
	protected void processResponse(final DiameterResponseFacade response, boolean mainThread, ProcessMessageResultListener resultListener) {
		ResponseProcessingListener listener = new ResponseProcessingListener(this, response, resultListener);
		if (response.ignorable ()){
		    getLogger().debug("skipping response processing : ignorable");
		    listener.messageProcessed(response, Action.RESPONSE);
		    return;
		}
		Utils.getEngine().handleResponse(response, !mainThread, listener);
	}

	public void processResponseAfterEngine(Action action, final DiameterResponseFacade response, final ProcessMessageResultListener resultListener) {

		if (action == Action.RESPONSE) {
			if (resultListener != null) {
				resultListener.handleResult(response);
			}
			return;
		}

		if (action == Action.MAY_BLOCK_RESPONSE) {
			Utils.start(new Runnable() {

				public void run() {
				    processResponse(response, false, resultListener);
				}
			});
			return;
		}

		if (action == Action.MAY_BLOCK_REQUEST) {
			Utils.start(new Runnable() {

				public void run() {
				    processRequestEnd(response.getRequestFacade (), false, resultListener);
				}
			});
			return;
		}

		if (action == Action.REQUEST) {
			DiameterRequestFacade request = response.getRequestFacade ();
			try {
				request.selectHostToProxy();
				if (resultListener != null) {
					resultListener.handleResult(request);
				}
			}
			catch (CannotProxyException e) {
				// Cannot proxy the request -> send error response
				DiameterSession session = request.getDiameterSession();
				if (session != null) {
					session.destroy();
				}
				response.removeDiameterAVPs();
				Utils.cloneAvp(request, response, DiameterBaseConstants.AVP_SESSION_ID);
				response.setResultCode(e.getReason());
				response.setUnableToDeliverCause (e.unableToDeliverCause ());
				response.setLocalOrigin(true);
				// acts as a server
				Utils.handleRoutingAVPs(response);
				processResponse(response, true, resultListener); // we use 'true' as the best choice
			}
			return;
		}

		if (resultListener != null) {
			resultListener.handleResult(null);
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#addListener(com.nextenso.proxylet.diameter.DiameterPeerListener)
	 */
	public void addListener(DiameterPeerListener listener) {
		if (listener != null) {
			_listeners.add(new ListenerWithExecutor(listener));
		}

	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#removeListener(com.nextenso.proxylet.diameter.DiameterPeerListener)
	 */
	public void removeListener(DiameterPeerListener listener) {
		for (ListenerWithExecutor l : _listeners) {
			if (listener == l.getListener()) {
				_listeners.remove(l);
				break;
			}
		}
	}

	public List<ListenerWithExecutor> getDiameterListeners() {
		return _listeners;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#isLocalDiameterPeer()
	 */
	public boolean isLocalDiameterPeer() {
		return false;
	}
    public String getLocalDiameterPeerName (){
	return _handlerName;
    }

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#isLocalInitiator()
	 */
	public boolean isLocalInitiator() {
		return false;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getVendorSpecificApplications()
	 * @deprecated
	 */
	@SuppressWarnings("deprecation")
	@Deprecated
	public long[] getVendorSpecificApplications() {
		List<DiameterApplication> l = getSpecificApplications();
		if (l == null) {
			getLogger().debug("getVendorSpecificApplications: no SpecificApplications using getSpecificApplications()");
			return new long[0];
		}
		long[] res = new long[l.size()];
		int i = 0;
		for (DiameterApplication app : l) {
			res[i++] = app.getApplicationId();
		}
		return res;
	}

	protected long[] getLongArray(Collection<Long> set) {
		long[] res = new long[set.size()];
		int i = 0;
		for (long id : set) {
			res[i++] = id;
		}

		return res;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#setRetryTimeout(java.lang.Integer)
	 */
	public void setRetryTimeout(Integer seconds) {
		_retryTimeout = seconds;
		if (getLogger().isDebugEnabled()) {
			getLogger().debug("setRetryTimeout: _retryTimeout=" + _retryTimeout);
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#setRetryTimeoutInMs(java.lang.Integer)
	 */
	@Override
	public void setRetryTimeoutInMs(Integer milliseconds) {
		_retryTimeoutInMs = milliseconds;
		if (getLogger().isDebugEnabled()) {
			getLogger().debug("setRetryTimeoutInMS: _retryTimeoutInMs=" + _retryTimeoutInMs);
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getRetryTimeout()
	 */
	public Integer getRetryTimeout() {
		return _retryTimeout;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getRetryTimeoutInMs()
	 */
	@Override
	public Integer getRetryTimeoutInMs() {
		Integer res = null;
		if (_retryTimeoutInMs != null) {
			res = _retryTimeoutInMs;
		}
		if (res == null && _retryTimeout != null) {
			res = _retryTimeout * 1000;
		}

		if (getLogger().isDebugEnabled()) {
			getLogger().debug("getRetryTimeoutInMs: res=" + res);
		}

		return res;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#setNbRetries(java.lang.Integer)
	 */
	public void setNbRetries(Integer nb) {
		_nbRetry = nb;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getNbRetries()
	 */
	public Integer getNbRetries() {
		return _nbRetry;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterPeer#getProtocol()
	 */
	@Override
	public Protocol getProtocol() {
		return _protocol;
	}

	public java.util.Map<Object, Object> getAttributes (){
		return _attributes;
	}

}
