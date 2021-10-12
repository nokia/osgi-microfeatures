// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter;

import com.nextenso.proxylet.diameter.DiameterSession;

/**
 * The IMS Client.
 */
public interface ImsClient {

	/**
	 * Gets the Diameter Session.
	 * 
	 * @return The Diameter Session, or <code>null</code> if the client is
	 *         stateless.
	 */
	public DiameterSession getDiameterSession();

	/**
	 * Closes this client.
	 */
	public void close();

}
