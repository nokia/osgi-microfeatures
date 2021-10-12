// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.ha.HaManager;
import com.nextenso.diameter.agent.ha.SessionListener;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.diameter.agent.impl.DiameterSessionFacade;
import com.nextenso.diameter.agent.peer.LocalPeer;
import com.nextenso.diameter.agent.peer.Peer;
import com.nextenso.mux.MuxConnection;
import com.nextenso.proxylet.diameter.util.TimeFormat;

/**
 * The session manager used to store and retrieve Diameter sessions.
 */
public class SessionManager {

	private static final String CACHED_ATTRIBUTE = "com.alcatel-lucent.diameter.cached";
	private static final String DEFAULT_ORIGIN_HOST = "Default_Origin_Host";
	private static final Logger LOGGER = Logger.getLogger("agent.diameter.sessionManager");

	private static Map<Long, DiameterSessionFacade> SESSIONS_BY_STACK_ID = Utils.newConcurrentHashMap();
	private Map<String, DiameterSessionFacade> _sessions = Utils.newConcurrentHashMap();

	public SessionManager() {}

	public void removeSession(DiameterSessionFacade session) {
		Object cached = session.getAttribute(CACHED_ATTRIBUTE);
		if (cached == null)  {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("The session is not cached -> do nothing for session=" + session);
			}
			return;
		}
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removeSession: session id=" + session.getSessionId());
		}
		_sessions.remove(session.getSessionId());
		session.removeAttribute(CACHED_ATTRIBUTE);
	}

	public void getSession(DiameterRequestFacade request, SessionListener listener) {
		String sessionId = request.getSessionId();
		DiameterSessionFacade res = _sessions.get(sessionId);
		if (res == null && DiameterProperties.isHa()) {
			HaManager.getSession(request, this, listener);
		} else {
			listener.handleSession(res);
		}
	}

	public void addSession(DiameterSessionFacade session) {
		session.setAttribute(CACHED_ATTRIBUTE, Boolean.TRUE);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addSession: id=" + session.getSessionId());
		}
		_sessions.put(session.getSessionId(), session);
	}

	public String newSessionId(String handlerName) {
		LocalPeer localPeer = Utils.getClientLocalPeer(handlerName);
		String originHost = DEFAULT_ORIGIN_HOST;
		if (localPeer != null) {
			originHost = localPeer.getOriginHost();
		}
		return newSessionId("client", originHost);
	}

	/************* SessionId **************/

	private static String HIGH_BITS = String.valueOf(TimeFormat.toNtp(System.currentTimeMillis())); // follow rfc suggestion
	private static AtomicLong LOW_BITS = new AtomicLong(0L);

	public static String newSessionId(String id, String originHost) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(originHost);
		buffer.append(';');
		buffer.append(HIGH_BITS);
		buffer.append(';');
		buffer.append(String.valueOf(LOW_BITS.getAndIncrement()));
		buffer.append(';');
		buffer.append(id);
		return buffer.toString();
	}

}
