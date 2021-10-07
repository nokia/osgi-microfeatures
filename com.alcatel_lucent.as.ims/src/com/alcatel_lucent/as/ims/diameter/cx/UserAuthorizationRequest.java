package com.alcatel_lucent.as.ims.diameter.cx;

import java.io.IOException;

import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.UserAuthorizationType;

/**
 * The User-Authorization-Request (UAR).
 */
public interface UserAuthorizationRequest
		extends CxRequest {

	/**
	 * Sets the <i>Type of Authorization</i> parameter of this request (Mapped to
	 * the User-Authorization-Type diameter AVP).
	 * 
	 * @param authType The user authorization type.
	 */
	public void setAuthorizationType(UserAuthorizationType authType);

	/**
	 * Sets the UAR flags.
	 * 
	 * @param flags The flags
	 * @since 3GPP 29.229 v7.8
	 */
	public void setUARFlags(Long flags);

	/**
	 * Sends the request.
	 * 
	 * @return the User Registration Status Response received from the HSS
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 * @exception IOException if an error occurs when sending the request.
	 */
	public UserAuthorizationAnswer execute()
		throws DiameterMissingAVPException, IOException;

	/**
	 * Sends asynchronously the request to the HSS.
	 * 
	 * @param listener The listener.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 */
	public void execute(ImsAnswerListener<UserAuthorizationRequest, UserAuthorizationAnswer> listener)
		throws DiameterMissingAVPException;
}
