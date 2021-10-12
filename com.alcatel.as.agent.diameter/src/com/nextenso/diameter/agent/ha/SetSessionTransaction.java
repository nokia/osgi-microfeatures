// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.ha;

import java.io.Serializable;

import org.apache.log4j.Logger;

import com.alcatel.as.session.distributed.Session;
import com.alcatel.as.session.distributed.SessionException;
import com.alcatel.as.session.distributed.SessionType;
import com.alcatel.as.session.distributed.Transaction;
import com.nextenso.diameter.agent.impl.DiameterSessionFacade;

public class SetSessionTransaction
		extends Transaction {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.ha.set");

	private transient SessionListener _listener = null;
	private transient DiameterSessionFacade _session = null;

	public SetSessionTransaction(SessionType sessionType, DiameterSessionFacade session, SessionListener listener) {
		super(sessionType, session.getSessionId(), Transaction.TX_CREATE_GET | Transaction.TX_SERIALIZED);
		_listener = listener;
		_session = session;
	}

	public SessionListener getListener() {
		return _listener;
	}

	public DiameterSessionFacade getSession() {
		return _session;
	}

	/**
	 * @see com.alcatel.as.session.distributed.Transaction#execute(com.alcatel.as.session.distributed.Session)
	 */
	@Override
	public void execute(Session session)
		throws SessionException {
		if (session == null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("execute: distributed session not found-> no Diameter Session");
			}
			if (_listener != null) {
				_listener.handleSession(null);
			}
			return;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("execute...");
		}
		int duration = (int) (_session.getSessionLifetime() / 1000);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("execute: duration (in seconds)=" + duration);
		}
		session.setAttribute("_duration", duration);

		//Store all modified attributes
		for (Object attKey : _session.getModifiedAttributes()) {

			if (attKey instanceof String) {
				String key = (String) attKey;
				Object value = _session.getAttribute(key);
				if (value == null) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("execute: remove attribute=" + key);
					}
					session.removeAttribute(key, false);
				} else if (value instanceof Serializable) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("execute: store attribute=" + key);
					}
					session.setAttribute(key, (Serializable) value);
				}
			}
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("execute: attributes are stored -> commit");
		}
		session.commit(session.getSessionId());

		_session.clearModifiedAttributes();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("execute: commited");
		}
	}

}
