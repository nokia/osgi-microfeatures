package com.alcatel_lucent.as.ims.diameter.sh;

import java.io.IOException;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference;

/**
 * The Profile Update Request (PUR )
 */
public interface ProfileUpdateRequest
		extends ShRequest {

	/**
	 * Sets the user data (mapped to the User-Data diameter AVP).
	 * 
	 * @param data The data.
	 */
	public void setUserData(byte[] data);

	/**
	 * Sets the reference to the data on which notifications of change are
	 * required (mapped to the Data-Reference diameter AVP).
	 * 
	 * @param dataReference The data reference.
	 */
	public void setDataReference(DataReference dataReference);

	/**
	 * Sends the request.
	 * 
	 * @return The Profile Update Response received from the HSS.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 */
	public ProfileUpdateAnswer execute()
		throws DiameterMissingAVPException, IOException;

	/**
	 * Sends asynchronously the request.
	 * 
	 * @param listener The listener to call when the response is received.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 */
	public void execute(ImsAnswerListener<ProfileUpdateRequest, ProfileUpdateAnswer> listener)
		throws DiameterMissingAVPException;
}
