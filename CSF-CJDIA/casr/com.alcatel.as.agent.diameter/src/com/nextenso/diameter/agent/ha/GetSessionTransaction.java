package com.nextenso.diameter.agent.ha;

import org.apache.log4j.Logger;

import com.alcatel.as.session.distributed.Session;
import com.alcatel.as.session.distributed.SessionException;
import com.alcatel.as.session.distributed.SessionType;
import com.alcatel.as.session.distributed.Transaction;
import com.nextenso.diameter.agent.SessionManager;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.diameter.agent.impl.DiameterSessionFacade;

public class GetSessionTransaction
		extends Transaction {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger("agent.diameter.ha.get");
	private transient SessionListener _listener = null;
	private transient DiameterRequestFacade _request = null;
	private transient SessionManager _sessionManager = null;
	private transient DiameterSessionFacade _session = null;

	public GetSessionTransaction(SessionType type, DiameterRequestFacade request, SessionManager sessionManager, SessionListener listener) {
		super(type, request.getSessionId(), Transaction.TX_GET | Transaction.TX_SERIALIZED);
		_listener = listener;
		_request = request;
		_sessionManager = sessionManager;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("new instance for session id=" + request.getSessionId());
		}
	}

	/**
	 * Gets the listener.
	 * 
	 * @return The listener.
	 */
	public final SessionListener getListener() {
		return _listener;
	}

	/**
	 * Gets the session.
	 * 
	 * @return The session.
	 */
	public final DiameterSessionFacade getSession() {
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

			_listener.handleSession(null);
			return;
		}

		long lifetime = session.getDuration() * 1000L;
		_session = new DiameterSessionFacade(_request, _sessionManager, lifetime);
		_sessionManager.addSession(_session);
		// get attributes
		for (Object o : session.getAttributes()) {
			Session.Attribute att = (Session.Attribute) o;
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("execute: restoring attribute=" + att);
			}

			_session.setAttribute(att.getName(), att.getValue());
		}
		_session.clearModifiedAttributes();

		session.commit(false);
	}

}
