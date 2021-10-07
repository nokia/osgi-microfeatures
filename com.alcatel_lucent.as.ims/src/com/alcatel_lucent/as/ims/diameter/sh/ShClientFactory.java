package com.alcatel_lucent.as.ims.diameter.sh;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterRequest;

/**
 * The ShClient Factory.
 */
public interface ShClientFactory {

	/**
	 * Gets the supported version.
	 * 
	 * @return The supported version of the 3GPP 29.329 document.
	 */
	public Version getVersion();

	/**
	 * Creates a Sh Client.
	 * 
	 * @param destinationHost The host the Sh Client will route messages to.
	 * @param destinationRealm The realm the Sh Client will route messages to.
	 * @return The Sh client.
	 * @throws NoRouteToHostException if the destination cannot be reached.
	 * @since ASR 4.0.
	 */
	public ShClient createShClient(String destinationHost, String destinationRealm)
		throws NoRouteToHostException;

	/**
	 * Creates a new Push Notification Request with a received Diameter request.
	 * @param request The received request.
	 * @return The Push Notification Request.
	 */
	public PushNotificationRequest createPNR(DiameterRequest request);
}
