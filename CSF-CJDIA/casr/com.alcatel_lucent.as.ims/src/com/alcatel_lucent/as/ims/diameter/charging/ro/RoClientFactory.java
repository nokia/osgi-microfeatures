package com.alcatel_lucent.as.ims.diameter.charging.ro;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;

/**
 * The Ro Client Factory.
 * 
 * @since ASR 4.0 SP3
 */
public interface RoClientFactory {

	/**
	 * Gets the supported version.
	 * 
	 * @return The supported version of the 3GPP 32.299 document..
	 */
	public Version getVersion();

	/**
	 * Creates a Session Charging with Unit Reservation (SCUR) client.
	 * 
	 * @param servers The list of server names.
	 * @param realm The realm.
	 * @param serviceContextId The service context id (see the AVP
	 *          Service-Context-Id in the RFC 4006).
	 * @return A new client.
	 * @throws NoRouteToHostException if no server can be reached.
	 */
	public ScurClient createScurClient(Iterable<String> servers, String realm, String serviceContextId)
		throws NoRouteToHostException;

	/**
	 * Creates an Event Charging with Unit Reservation (SCUR)client.
	 * 
	 * @param servers The list of server names.
	 * @param realm The realm.
	 * @param serviceContextId The service context id (see the AVP
	 *          Service-Context-Id in the RFC 4006).
	 * @return A new client.
	 * @throws NoRouteToHostException if no server can be reached.
	 */
	public EcurClient createEcurClient(Iterable<String> servers, String realm, String serviceContextId)
		throws NoRouteToHostException;

	/**
	 * Creates an Immediate Event Charging (IEC) client.
	 * 
	 * @param servers The list of server names.
	 * @param realm The realm.
	 * @param serviceContextId The service context id (see the AVP
	 *          Service-Context-Id in the RFC 4006).
	 * @return A new client.
	 * @throws NoRouteToHostException if no server can be reached.
	 */
	public IecClient createIecClient(Iterable<String> servers, String realm, String serviceContextId)
		throws NoRouteToHostException;

}
