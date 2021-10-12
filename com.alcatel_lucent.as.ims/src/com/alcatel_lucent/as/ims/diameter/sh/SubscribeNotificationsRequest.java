// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.sh;

import java.io.IOException;
import java.util.Date;

import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.ImsAnswerListener;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.DataReference;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.IdentitySet;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.SendDataIndication;
import com.alcatel_lucent.as.ims.diameter.sh.ShConstants.SubsReqType;

/**
 * The Subscribe Notifications Request (SNR)
 */
public interface SubscribeNotificationsRequest
		extends ShRequest {

	/**
	 * Adds the data reference (mapped on the Data-Reference diameter AVP).
	 * 
	 * @param dataReference The data reference.
	 */
	public void addDataReference(DataReference dataReference);

	/**
	 * Adds the Service Indication that identifies a service in an AS (mapped to
	 * the Service-Indication diameter AVP).
	 * 
	 * @param serviceIndication the service indication to be added.
	 */
	public void addServiceIndication(byte[] serviceIndication);

	/**
	 * Sets the Send-Data-Indication AVP value which indicates whether the sender
	 * requests the User-Data.
	 * 
	 * @param indication The indication.
	 */
	public void setSendDataIndication(SendDataIndication indication);

	/**
	 * Sets the SIP-URI used to identify an AS (mapped to the Server-Name diameter
	 * AVP).
	 * 
	 * @param serverName The AS server SIP URL.
	 */
	public void setServerName(String serverName);

	/**
	 * Sets the Subs-Req-Type AVP value which indicates the type of the request.
	 * 
	 * @param type The type.
	 */
	public void setSubsReqType(SubsReqType type);

	/**
	 * Gets the Subs-Req-Type AVP value which indicates the type of the request.
	 * 
	 * @return The type.
	 */
	public SubsReqType getSubsReqType();

	/**
	 * Adds the type of identity set that the HSS shall provide (mapped to the
	 * Identity-Set diameter AVP).
	 * 
	 * @param type The set value.
	 */
	public void addIdentitySet(IdentitySet type);

	/**
	 * Sets the Expiry-Time AVP value.
	 * 
	 * @param time The expiry time of subscriptions to notifications in the HSS.
	 */
	public void setExpiryTime(Date time);

	/**
	 * Adds the DSAI tag (mapped on the DSAI-TAg diameter AVP).
	 * 
	 * @param tag The tag.
	 */
	public void addDSAITag(byte[] tag);

	/**
	 * Sends the request.
	 * 
	 * @return the response received from the HSS to the AS.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing.
	 * @exception IOException if an error occurs when the request is sent.
	 */
	public SubscribeNotificationsAnswer execute()
		throws DiameterMissingAVPException, IOException;

	/**
	 * Sends asynchronously the request.
	 * 
	 * @param listener The listener to call when the response is received.
	 * @exception DiameterMissingAVPException if a mandatory or conditional AVP is
	 *              missing.
	 */
	public void execute(ImsAnswerListener<SubscribeNotificationsRequest, SubscribeNotificationsAnswer> listener)
		throws DiameterMissingAVPException;

	/**
	 * Gets the User-Identity AVP value which indicates the UserIndentity of the
	 * request.
	 * 
	 * @return The user identity.
	 */
	public UserIdentity getUserIndentity();

}
