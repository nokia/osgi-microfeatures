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
