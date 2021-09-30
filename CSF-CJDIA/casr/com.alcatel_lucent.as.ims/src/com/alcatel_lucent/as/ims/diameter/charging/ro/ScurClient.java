package com.alcatel_lucent.as.ims.diameter.charging.ro;

import java.io.Serializable;

import com.alcatel_lucent.as.ims.diameter.ImsClient;

/**
 * The SCUR (Session Charging with Unit Reservation) Client.
 */
public interface ScurClient
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
	 * Creates a new UPDATE request.
	 * 
	 * @return The request.
	 * @exception IllegalStateException if an initial request has not already been
	 *              created or if a termination request has been created.
	 */
	public CreditControlRequest createUpdateRequest()
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
