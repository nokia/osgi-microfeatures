package com.alcatel_lucent.as.ims.diameter.charging.rf;

import com.alcatel_lucent.as.ims.diameter.ImsClient;

/**
 * The Rf Session Client.
 */
public interface RfSessionClient
		extends ImsClient {

	/**
	 * Creates a new START request.
	 * 
	 * @return The new request.
	 * @exception IllegalStateException if a START request has already been
	 *              created.
	 */
	public AccountingRequest createStartRequest()
		throws IllegalStateException;

	/**
	 * Creates a new INTERIM request.
	 * 
	 * @return The new request.
	 * @throws IllegalStateException if a START request has not already been
	 *           created or if a STOP request has already been created.
	 */
	public AccountingRequest createInterimRequest()
		throws IllegalStateException;

	/**
	 * Creates a new STOP request.
	 * 
	 * @return The new request.
	 * @throws IllegalStateException if a START request has not already been
	 *           created or a STOP request has already been created.
	 */
	public AccountingRequest createStopRequest()
		throws IllegalStateException;

}
