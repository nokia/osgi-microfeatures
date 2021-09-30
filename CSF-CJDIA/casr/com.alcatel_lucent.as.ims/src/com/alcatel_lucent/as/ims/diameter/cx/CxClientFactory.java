package com.alcatel_lucent.as.ims.diameter.cx;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;

/**
 * 
 * The CxClient Factory.
 */
public interface CxClientFactory {
	/**
	 * Gets the supported version.
	 * 
	 * @return  The supported version of the 3GPP 29.229 document..
	 */	
	public Version getVersion();

	/**
	 * Creates a Cx Client.
	 * 
	 * @param destinationHost The host the Cx Client will route messages to.
	 * @param destinationRealm The realm the Cx Client will route messages to.
	 * @return The Cx client.
	 * @throws NoRouteToHostException if the destination cannot be reached.
	 * @since ASR 4.0.
	 */
	public CxClient createCxClient(String destinationHost, String destinationRealm)
		throws NoRouteToHostException;

}
