package com.alcatel_lucent.as.ims.diameter.cx;

import java.io.IOException;

import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;

/**
 * The Location Info Request (LIR).
 */
public interface LocationInfoRequest
		extends CxRequest {

	/**
	 * Sends the request to the HSS.
	 * 
	 * @return The answer received from the HSS.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 * @exception IOException if an error occurs when sending the request.
	 */
	public LocationInfoAnswer execute()
		throws DiameterMissingAVPException, IOException;

	/**
	 * Sends asynchronously the request to the HSS.
	 * 
	 * @param listener The listener.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 */
	public void execute(ImsAnswerListener<LocationInfoRequest, LocationInfoAnswer> listener)
		throws DiameterMissingAVPException;
}
