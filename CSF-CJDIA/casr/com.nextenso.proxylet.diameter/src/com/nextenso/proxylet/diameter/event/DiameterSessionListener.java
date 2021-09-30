package com.nextenso.proxylet.diameter.event;

import com.nextenso.proxylet.diameter.DiameterSession;

/**
 * The interface to implement to be notified of session creation/destruction.
 */
public interface DiameterSessionListener {

	/**
	 * Called when a session is created.
	 * 
	 * @param session The newly created session.
	 */
	public void sessionCreated(DiameterSession session);

	/**
	 * Called when a session is destroyed.
	 * 
	 * @param session The destroyed session.
	 */
	public void sessionDestroyed(DiameterSession session);

}
