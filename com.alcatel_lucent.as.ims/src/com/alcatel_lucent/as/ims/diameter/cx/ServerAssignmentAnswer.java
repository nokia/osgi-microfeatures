// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.cx;

import java.util.List;

import com.alcatel_lucent.as.ims.diameter.cx.CxConstants.LooseRouteIndication;

/**
 * The The Server-Assignment-Answer (SAA).
 */
public interface ServerAssignmentAnswer
		extends CxAnswer {

	/**
	 * Gets the user name.
	 * 
	 * @return the user name.
	 */
	public String getUserName();

	/**
	 * Gets the User Profile parameter of this response (mapped to the User-Data
	 * diameter AVP).
	 * 
	 * @return the User Profile of this response
	 */
	public byte[] getUserProfile();

	/**
	 * Gets the the charging functions of this response. Mapped to the
	 * Chargin-Information diameter AVP.
	 * 
	 * @return the Charging Information of this response
	 */
	public ChargingInformation getChargingInformation();

	/**
	 * Gets the list of private user identities associated to an IMS subscription.
	 * 
	 * @return The list of identities.
	 * @since 3GPP 29.299 v6.6
	 */
	public List<String> getAssociatedIdentities();

	/**
	 * Gets the indication whether or not the loose route mechnism is required to
	 * serve the registered Public User Identities.
	 * 
	 * @return The indication.
	 * @since 3GPP 29.299 v8.3
	 */
	public LooseRouteIndication getLooseRouteIndication();

	/**
	 * Gets the list of private user identities registered with the Public User
	 * Identity.
	 * 
	 * @return The list of identities.
	 * @since 3GPP 29.299 v8.3
	 */
	public List<String> getAssociatedRegisteredIdentities();

	/**
	 * Gets the list of information required for an S-CSCF to handle the requests
	 * for a user.
	 * 
	 * @return The SCSCF information.
	 * @since 3GPP 29.299 v8.5
	 */
	public List<SCSCFRestorationInfo> getSCSCFRestorationInformation();

}
