// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.cx;

import java.io.IOException;
import java.util.List;

import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.MultipleRegistrationIndication;

/**
 * The Server-Assignment-Request (SAR).
 */
public interface ServerAssignmentRequest
		extends CxRequest {

	/**
	 * Sets the user name (mapped to the User-Name diameter AVP).
	 * 
	 * @param name the user name.
	 */
	public void setUserName(String name);

	/**
	 * Adds the specified public User ID to the list of public user identities
	 * that are deregistered, in the form of SIP URL or TEL URL (mapped to the
	 * Public-Identity).
	 * 
	 * @param publicUserId The public user identity to add
	 */
	public void addPublicIdentity(String publicUserId);

	/**
	 * Sets the list of public user identities that are deregistered, in the form
	 * of SIP URL or TEL URL.
	 * 
	 * @param publicIdentities a list of String representing the public user
	 *          identities
	 */
	public void setPublicIdentities(List<String> publicIdentities);

	/**
	 * Sets the Wilcarded PSI.
	 * 
	 * @param psi The PSI.
	 * @since 3GPP 29.299 v7.4
	 */
	public void setWildcardedPSI(String psi);

	/**
	 * Sets the Wilcarded public user identity.
	 * 
	 * @param impu The identity.
	 * @since 3GPP 29.299 v8.1
	 */
	public void setWildcardedIMPU(String impu);

	/**
	 * Sets the information related to a specific registration required for a
	 * S-CSCF to handle the request to a user.
	 * 
	 * @param information The restoration information.
	 * @since 3GPP 29.299 v8.5
	 */
	public void setSCSCFRestorationInfo(RestorationInfo information);

	/**
	 * Sets the indication to indicates to the HSS whether or not the request is
	 * related to a multiple registration.
	 * 
	 * @param indication The indication.
	 * @since 3GPP 29.299 v8.5
	 */
	public void setMultipleRegistrationIndication(MultipleRegistrationIndication indication);

	/**
	 * Sends the request to the HSS.
	 * 
	 * @return The answer received from the HSS.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 * @exception IOException if an error occurs when sending the request.
	 */
	public ServerAssignmentAnswer execute()
		throws DiameterMissingAVPException, IOException;

	/**
	 * Sends asynchronously the request to the HSS.
	 * 
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing
	 */
	public void execute(ImsAnswerListener<ServerAssignmentRequest, ServerAssignmentAnswer> listener)
		throws DiameterMissingAVPException;

}
