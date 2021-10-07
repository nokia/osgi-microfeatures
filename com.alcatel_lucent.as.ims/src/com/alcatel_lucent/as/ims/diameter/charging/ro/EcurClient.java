package com.alcatel_lucent.as.ims.diameter.charging.ro;

import java.io.Serializable;

import com.alcatel_lucent.as.ims.diameter.ImsClient;

/**
 * The ECUR (Event Charging with Unit Reservation) Client.
 */
public interface EcurClient
		extends ImsClient, Serializable {

	/**
	 * Creates a new INITIAL request.
	 * 
	 * @return The request.
	 * @exception IllegalStateException if an initial request has already been
	 *              created.
	 */
	public CreditControlRequest createInitialRequest()
		throws IllegalStateException;

	/**
	 * Creates a new TERMINATION request.
	 * 
	 * @return The request.
	 * @exception IllegalStateException if a termination request has already been
	 *              created.
	 */
	public CreditControlRequest createTerminationRequest()
		throws IllegalStateException;

	/**
	 * Gets the service context identifier.
	 * 
	 * @return The Service Context identifier.
	 */
	public String getServiceContextId();
}
