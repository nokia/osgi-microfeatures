package com.alcatel_lucent.as.ims.diameter.charging.ro;

import java.io.Serializable;

import com.alcatel_lucent.as.ims.diameter.ImsClient;

/**
 * The IEC (Immediate Event Charging) client.
 */
public interface IecClient
		extends ImsClient, Serializable {

	/**
	 * Creates a new EVENT request.
	 * 
	 * @return The request.
	 */
	public CreditControlRequest createEventRequest()
		throws IllegalStateException;

	/**
	 * Gets the service context identifier.
	 * 
	 * @return The Service Context identifier.
	 */
	public String getServiceContextId();

}
