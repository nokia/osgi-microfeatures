package com.alcatel_lucent.as.ims.diameter.gaa;

import java.io.IOException;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.ImsRequest;
import com.alcatel_lucent.as.ims.diameter.gaa.GaaConstants.GbaUAwarenessIndicator;

/**
 * The Zn Bootstapping-Info Request (BIR).
 */
public interface BootstrappingInfoRequest
		extends ImsRequest {

	/**
	 * Adds a GAA service identifier (GAA-Service-Identifier AVP value)..
	 * 
	 * @param serviceIdentifier The service identifier to be added.
	 */
	public void addGaaServiceIdentifier(byte[] serviceIdentifier);

	/**
	 * Sets the GBA_U-Awareness-Indicator AVP value.
	 * 
	 * @param indicator The indicator.
	 */
	public void setGbaUAwarenessIndicator(GbaUAwarenessIndicator indicator);

	/**
	 * Sends the request.
	 * 
	 * @return the User Registration Status Response received from the HSS
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 * @exception IOException if an error occurs when sending the request.
	 */
	public BootstrappingInfoAnswer execute()
		throws DiameterMissingAVPException, IOException;

	/**
	 * Sends asynchronously the request.
	 * 
	 * @param listener The listener.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 */
	public void execute(ImsAnswerListener<BootstrappingInfoRequest, BootstrappingInfoAnswer> listener)
		throws DiameterMissingAVPException;

}
