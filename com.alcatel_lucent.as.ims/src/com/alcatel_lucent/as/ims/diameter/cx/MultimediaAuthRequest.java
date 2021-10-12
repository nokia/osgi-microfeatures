// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.cx;

import java.io.IOException;

import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;

/**
 * The Cx Multimedia-Auth-Request (MAR).
 */
public interface MultimediaAuthRequest
		extends CxRequest {

	/**
	 * Sets the SIP-Auth-Data-Item AVP value.
	 * 
	 * @param item The SIP-Auth-Data-Item AVP value.
	 */
	public void setSipAuthDataItem(SIPAuthDataItem item);

	/**
	 * Sets the SIP-Number-Auth-Items AVP value.
	 * 
	 * @param nb The SIP-Number-Auth-Items AVP value.
	 */
	public void setSipNumberAuthItems(long nb);

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
	 * 
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 */
	public void execute(ImsAnswerListener<MultimediaAuthRequest, MultimediaAuthAnswer> listener)
		throws DiameterMissingAVPException;
}
