package com.nextenso.proxylet.diameter.client;

import java.io.IOException;

import com.nextenso.proxylet.diameter.DiameterRequest;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The request Object used by the DiameterClient.
 */
@ProviderType
public interface DiameterClientRequest
		extends DiameterRequest {

	/**
	 * Gets the associated client.
	 * 
	 * @return The client.
	 */
	public DiameterClient getDiameterClient();
	
	/**
	 * Gets the associated response.
	 * 
	 * @return The response.
	 */
	public DiameterClientResponse getDiameterClientResponse();

	/**
	 * Sets the Origin-Host AVP.
	 */
	public void setOriginHostAVP();

	/**
	 * Sets the Origin-Realm AVP.
	 */
	public void setOriginRealmAVP();

	/**
	 * Sends the request synchronously.
	 * 
	 * @return The response.
	 * @throws IOException if the request cannot be sent, if the response times
	 *           out or if the response has the ERROR flag set.
	 */
	public DiameterClientResponse execute()
		throws IOException;

	/**
	 * Sends the request asynchronously.
	 * 
	 * @param listener The listener to notify.
	 */
	public void execute(DiameterClientListener listener);

	/**
	 * Attaches an object to the request.
	 * 
	 * @param attachment The attachment.
	 */
	public void attach(Object attachment);

	/**
	 * Gets the attachment.
	 * 
	 * @return The attachment
	 */
	public Object attachment();

}
