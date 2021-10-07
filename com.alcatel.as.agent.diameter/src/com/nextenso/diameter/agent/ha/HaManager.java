package com.nextenso.diameter.agent.ha;

import java.io.Serializable;

import org.apache.log4j.Logger;

import com.alcatel.as.session.distributed.SessionException;
import com.alcatel.as.session.distributed.SessionType;
import com.alcatel.as.session.distributed.Transaction;
import com.alcatel.as.session.distributed.TransactionListener;
import com.nextenso.diameter.agent.SessionManager;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.diameter.agent.impl.DiameterSessionFacade;

/**
 * The HA Manager.
 */
public class HaManager {

	private static com.alcatel.as.session.distributed.SessionManager MANAGER = null;
	private static SessionType TYPE = null;
	private static final Logger LOGGER = Logger.getLogger("agent.diameter.ha.manager");
	private static final GetListener GET_LISTENER = new GetListener();
	private static final SetListener SET_LISTENER = new SetListener();

	private static class GetListener
			implements TransactionListener {

		public void transactionCompleted(Transaction cmd, Serializable result) {
			if (!(cmd instanceof GetSessionTransaction)) {
				return;
			}
			GetSessionTransaction transaction = (GetSessionTransaction) cmd;
			DiameterSessionFacade session = transaction.getSession();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("transactionCompleted: session=" + session);
			}
			SessionListener listener = transaction.getListener();

			if (listener != null) {
				listener.handleSession(session);
			}
		}

		public void transactionFailed(Transaction cmd, SessionException exc) {
			if (!(cmd instanceof GetSessionTransaction)) {
				return;
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("transactionFailed: cannot get session", exc);
			}
			GetSessionTransaction transaction = (GetSessionTransaction) cmd;
			SessionListener listener = transaction.getListener();
			if (listener != null) {
				listener.handleSession(null);
			}
		}

	}

	private static class SetListener
			implements TransactionListener {

		public void transactionCompleted(Transaction cmd, Serializable result) {
			if (!(cmd instanceof SetSessionTransaction)) {
				return;
			}

			SetSessionTransaction transaction = (SetSessionTransaction) cmd;
			DiameterSessionFacade session = transaction.getSession();

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("transactionCompleted: session=" + session);
			}
			SessionListener listener = transaction.getListener();
			if (listener != null) {
				listener.handleSession(session);
			}
		}

		public void transactionFailed(Transaction cmd, SessionException exc) {
			if (!(cmd instanceof SetSessionTransaction)) {
				return;
			}
			SetSessionTransaction transaction = (SetSessionTransaction) cmd;
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("transactionFailed: cannot get session", exc);
			}
			SessionListener listener = transaction.getListener();
			if (listener != null) {
				listener.handleSession(null);
			}
		}

	}

	/**
	 * Initializes the manager.
	 */
	public static void init() {
		if (MANAGER != null) {
			// already initialized
			return;
		}
		MANAGER = com.alcatel.as.session.distributed.SessionManager.getSessionManager();
		TYPE = MANAGER.getSessionType("diameteragent");
	}

	/**
	 * Stores the session.
	 * 
	 * @param session The session to be stored.
	 * @param listener The listener if any.
	 * @throws IllegalArgumentException if session is null.
	 */
	public static void setSession(DiameterSessionFacade session, SessionListener listener)
		throws IllegalArgumentException {
		if (session == null) {
			throw new IllegalArgumentException("No session identifier");
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setSession: session=" + session);
		}
		Transaction t = new SetSessionTransaction(TYPE, session, listener);
		MANAGER.execute(t, SET_LISTENER);
	}

	/**
	 * Retrieves a session.
	 * 
	 * @param request The request.
	 * @param listener The listener.
	 * @throws IllegalArgumentException if a parameter is null or ifthe request
	 *           does not have a session id.
	 */
	public static void getSession(DiameterRequestFacade request, SessionManager sessionManager, SessionListener listener)
		throws IllegalArgumentException {
		if (listener == null) {
			throw new IllegalArgumentException("No listener");
		}
		if (request == null) {
			throw new IllegalArgumentException("No request");
		}
		String sessionId = request.getSessionId();
		if (sessionId == null) {
			throw new IllegalArgumentException("No session identifier");
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getSession: sessionId=" + sessionId);
		}
		Transaction t = new GetSessionTransaction(TYPE, request, sessionManager, listener);
		MANAGER.execute(t, GET_LISTENER);

	}

}
