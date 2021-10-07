package com.alcatel_lucent.as.ims.diameter.gaa;

import java.io.IOException;
import java.util.Date;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.ImsRequest;

/**
 * The Zh Multimedia-Auth-Request (MAR).
 */
public interface MultimediaAuthRequest
		extends ImsRequest {

	/**
	 * Sets the IMPI.
	 * 
	 * @param impi The IMPI.
	 */
	public void setUserName(String impi);

	/**
	 * Sets the IMPU.
	 * 
	 * @param impu The IMPU.
	 */
	public void setPublicUserIdentity(String impu);

	/**
	 * Sets the timestamp of GUSS in BSF.
	 * 
	 * @param timestamp The timestamp.
	 */
	public void setGussTimestamp(Date timestamp);

	/**
	 * Sends the request.
	 * 
	 * @return the User Registration Status Response received from the HSS
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 * @exception IOException if an error occurs when sending the request.
	 */
	public MultimediaAuthAnswer execute()
		throws DiameterMissingAVPException, IOException;

	/**
	 * Sends asynchronously the request.
	 * 
	 * @param listener The listener.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 */
	public void execute(ImsAnswerListener<MultimediaAuthRequest, MultimediaAuthAnswer> listener)
		throws DiameterMissingAVPException;

}
