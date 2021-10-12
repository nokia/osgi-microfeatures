// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

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
