package com.alcatel_lucent.as.ims.diameter.gaa;

import com.alcatel_lucent.as.ims.diameter.ImsClient;

/**
 * The Zn Client.
 */
public interface ZnClient
		extends ImsClient {

	/**
	 * Creates a new Bootstapping-Info request.
	 * 
	 * @param transactionIdentifier The transaction identifier.
	 * @param nafId The NAF identifier.
	 * @return The new Bootstapping-Info request.
	 */
	public BootstrappingInfoRequest createBIR(byte[] transactionIdentifier, byte[] nafId);
}
