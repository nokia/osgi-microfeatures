package com.alcatel_lucent.as.ims.diameter.charging.rf;

import com.alcatel_lucent.as.ims.diameter.ImsClient;

/**
 * The Rf Event Client .
 */
public interface RfEventClient
		extends ImsClient {

	/**
	 * Creates a new EVENT request.
	 * 
	 * @return The new request.
	 */
	public AccountingRequest createEventRequest();
}
