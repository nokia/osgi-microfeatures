// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.impl;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.metering.Gauge;
import com.nextenso.diameter.agent.SessionManager;
import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.peer.Peer;
import com.nextenso.proxylet.ProxyletContext;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterSession;
import com.nextenso.proxylet.diameter.event.DiameterSessionListener;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

public class DiameterSessionFacade
		implements DiameterSession {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.session");

	private static final String CREATION_TIME_ATTRIBUTE = "diameter.session.creationTime";
	private static final String STATE_ATTRIBUTE = "diameter.session.state";

	private final Hashtable<Object, Object> _attributes = new Hashtable<Object, Object>();
	private String _sessionId, _senderId;
	private final long _lifeTime;
	private boolean _isClientSession;
	private final transient Set<Object> _modifiedAttributes = new HashSet<Object>();
	private final transient List<DiameterSessionListener> _listeners = new CopyOnWriteArrayList<DiameterSessionListener>();
	private transient long _vendorId, _applicationId;
	private transient DiameterPeer _peer;
	private transient ProxyletContext _context;
	private final transient AtomicLong _lastAccessTime = new AtomicLong();
	private final transient AtomicBoolean _isDestroyed = new AtomicBoolean(false);
	private transient SessionManager _manager;
	private transient LifetimeTimer _lifetimeTimer = null;

	public static class LifetimeTimer
			implements Runnable {

		DiameterSessionFacade _session = null;
		private transient Future _future = null;

		public LifetimeTimer(DiameterSessionFacade session) {
			_session = session;
			PlatformExecutor executor = Utils.getCurrentExecutor();
			_future = Utils.schedule(executor, this, _session.getSessionLifetime() + 100, TimeUnit.MILLISECONDS);// 100ms for triggering
		}

		/**
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			if (_session == null) {
				return;
			}

			try {
				long now = System.currentTimeMillis();
				long elapsed = now - _session.getLastAccessedTime();
				if (elapsed > _session.getSessionLifetime()) {
					_session.destroy();
					return;
				}

				PlatformExecutor executor = Utils.getCurrentExecutor();
				_future = Utils.schedule(executor, this, _session.getSessionLifetime() + 100 - elapsed, TimeUnit.MILLISECONDS);
			}
			catch (Throwable t) {
				LOGGER.error("Exception while running Session Timeout  for " + this, t);
			}
		}

		public void cancel(boolean b) {
			_session = null;
			_future.cancel(b);
		}

	}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param isClient
	 * @param vendorId
	 * @param lifetime
	 * @param applicationId
	 * @param peer
	 * @param sessionId
	 * @param manager
	 */
	private DiameterSessionFacade(boolean isClient, long vendorId, long creationTime, long lifetime, long applicationId, Peer peer, String sessionId,
			SessionManager manager) {

		_isClientSession = isClient;
		_vendorId = vendorId;
		_lifeTime = lifetime;
		_applicationId = applicationId;
		_peer = peer;
		_sessionId = sessionId;
		_manager = manager;

		long now = System.currentTimeMillis();
		long time = creationTime;
		if (time <= 0) {
			time = now;
		}
		setCreationTime(time);
		_lastAccessTime.set(now);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Instanciated " + this);
		}
		if (getSessionLifetime() > 0) {
			_lifetimeTimer = new LifetimeTimer(this);
		}

		Gauge nbGauge = Utils.getNbSessionsGauge();
		if (nbGauge != null) {
			nbGauge.add(1L);
		}

	}

	/**
	 * Used for Diameter Client. The agent acts as client.
	 * 
	 * @param vendorId
	 * @param applicationId
	 * @param lifeTime
	 * @param peer
	 */
	public DiameterSessionFacade(long vendorId, long applicationId, long lifeTime, Peer peer) {
		this(true, vendorId, 0, lifeTime, applicationId, peer, peer.getSessionManager().newSessionId(peer.getHandlerName()), peer.getSessionManager());
	}

	public DiameterSessionFacade(long vendorId, long applicationId, long lifetime, Peer peer, String sessionId) {
		this(true, vendorId, 0, lifetime, applicationId, peer, sessionId, peer.getSessionManager());
	}

	/**
	 * 
	 * Constructor for this class. Used when creating a session for HA.
	 * 
	 * @param request
	 * @param manager
	 * @param lifeTime
	 */
	public DiameterSessionFacade(DiameterMessageFacade request, SessionManager manager, long lifeTime) {
		this(request, request.getSessionId(), manager, lifeTime, 0);
	}

	/**
	 * Used for Diameter received requests. The agent acts as server.
	 * 
	 * @param request The received request.
	 * @param sessionId The session id.
	 * @param manager The session manager
	 * @param lifeTime The life time in milliseconds.
	 */
	public DiameterSessionFacade(DiameterMessageFacade request, String sessionId, SessionManager manager, long lifeTime, long creationTime) {
		this(false, 0, creationTime, lifeTime, request.getDiameterApplication(), (Peer) request.getClientPeer(), sessionId, manager);

		_context = request.getProxyletContext();

		DiameterAVP avp = request.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
		if (avp != null && avp.getValueSize() > 0) {
			List list = GroupedFormat.getGroupedAVPs(avp.getValue(0), false);
			if (list.size() > 0) {
				avp = (DiameterAVP) list.get(0);
				if (avp.getValueSize() > 0 && avp.isInstanceOf(DiameterBaseConstants.AVP_VENDOR_ID))
					_vendorId = Unsigned32Format.getUnsigned32(avp.getValue(0), 0);
			}
		}

		// extract senderId (??)

	}

	private final void setCreationTime(long time) {
		setAttribute(CREATION_TIME_ATTRIBUTE, Long.valueOf(time));
	}

	/**
	 * Updates the last accessed time with the current date.
	 * 
	 * @return true if changed
	 */
	public boolean updateLastAccessedTime() {
		if (!isAlive()) {
			return false;
		}
		_lastAccessTime.set(System.currentTimeMillis());
		return true;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#destroy()
	 */
	public void destroy() {
		boolean isDestroyed = _isDestroyed.getAndSet(true);
		if (isDestroyed) {
			return;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Destroyed " + this);
		}
		_manager.removeSession(this);

		if (_lifetimeTimer != null) {
			_lifetimeTimer.cancel(false);
		}
		_lifetimeTimer = null;

		Gauge nbGauge = Utils.getNbSessionsGauge();
		if (nbGauge != null) {
			nbGauge.add(-1L);
		}

		// the notified may use the session to generate a client ? >> TODO
		for (final DiameterSessionListener listener : _listeners) {
			Runnable r = new Runnable() {

				public void run() {
					try {
						listener.sessionDestroyed(DiameterSessionFacade.this);
					}
					catch (Throwable t) {
						if (LOGGER.isEnabledFor(Level.WARN)) {
							LOGGER.warn("Exception while calling sessionDestroyed on " + DiameterSessionFacade.this, t);
						}
					}
				}
			};
			Utils.start(r);
		}

		_listeners.clear();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#isAlive()
	 */
	public boolean isAlive() {
		return !_isDestroyed.get();
	}

	/**
	 * Sets the associated peer.
	 * 
	 * @param peer The peer.
	 */
	public void setDiameterPeer(DiameterPeer peer) {
		_peer = peer;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#getDiameterPeer()
	 */
	public DiameterPeer getDiameterPeer() {
		return _peer;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#getState()
	 */
	public int getState() {
		Object o = getAttribute(STATE_ATTRIBUTE);
		int res = 0;
		if (o != null) {
			res = (Integer) o;
		}
		return res;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#setState(int)
	 */
	public void setState(int state) {
		setAttribute(STATE_ATTRIBUTE, Integer.valueOf(state));
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#getDiameterApplication()
	 */
	public long getDiameterApplication() {
		return _applicationId;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#getDiameterApplicationVendorId()
	 */
	public long getDiameterApplicationVendorId() {
		return _vendorId;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#getSessionId()
	 */
	public String getSessionId() {
		return _sessionId;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#getSenderId()
	 */
	public String getSenderId() {
		return _senderId;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#getProxyletContext()
	 */
	public ProxyletContext getProxyletContext() {
		return _context;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#getSessionLifetime()
	 */
	public long getSessionLifetime() {
		return _lifeTime;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#getCreationTime()
	 */
	public long getCreationTime() {
		Object o = getAttribute(CREATION_TIME_ATTRIBUTE);
		long res = 0;
		if (o != null) {
			res = (Long) o;
		}
		return res;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#getLastAccessedTime()
	 */
	public long getLastAccessedTime() {
		return _lastAccessTime.get();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#getAttribute(java.lang.Object)
	 */
	public Object getAttribute(Object key) {
		if (_attributes == null) {
			return null;
		}
		return _attributes.get(key);
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#getAttributeNames()
	 */
	public Enumeration getAttributeNames() {
		return _attributes.keys();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#setAttribute(java.lang.Object,
	 *      java.lang.Object)
	 */
	public void setAttribute(Object name, Object value) {
		if (value == null) {
			removeAttribute(name);
		} else {
			_attributes.put(name, value);
			_modifiedAttributes.add(name);
		}
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#removeAttribute(java.lang.Object)
	 */
	public Object removeAttribute(Object name) {
		Object value = _attributes.remove(name);
		if (value != null) {
			_modifiedAttributes.add(name);
		}
		return value;
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#removeAttributes()
	 */
	public void removeAttributes() {
		_modifiedAttributes.addAll(_attributes.keySet());
		_attributes.clear();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.DiameterSession#addSessionListener(com.nextenso.proxylet.diameter.event.DiameterSessionListener)
	 */
	public void addSessionListener(DiameterSessionListener listener) {
		_listeners.add(listener);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DiameterSession [sessionId=" + _sessionId + ", lifetime=" + getSessionLifetime() + "]";
	}

	/**
	 * Indicates whether this session has been created with a client.
	 * 
	 * @return true for a client, false in proxy or server mode.
	 */
	public boolean isClientSession() {
		return _isClientSession;
	}

	public void clearModifiedAttributes() {
		_modifiedAttributes.clear();
	}

	public Iterable<Object> getModifiedAttributes() {
		return _modifiedAttributes;
	}
}
