// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.cx;

import java.util.List;

/**
 * The Cx Multimedia-Auth-Answer (MAA).
 */
public interface MultimediaAuthAnswer
		extends CxAnswer {

	/**
	 * Gets the user name (mapped to the User-Name diameter AVP).
	 * 
	 * @return The user name.
	 */
	public String getUserName();

	/**
	 * Gets the public Identity (mapped to the Public-Identity diameter AVP).
	 * 
	 * @return The public Identity.
	 */
	public String getPublicIdentity();

	/**
	 * Gets the Number Authentication Items of this response (mapped to the
	 * SIP-Number-Auth-Items).
	 * 
	 * @return The Number Authentication Items of this response.
	 */
	public Long getNumberAuthItems();

	/**
	 * Gets the list of Authentication Data (mapped to the SIP-Auth-Data-Item).
	 * 
	 * @return The list of Authentication Data.
	 */
	public List<SIPAuthDataItem> getAuthenticationData();
}
