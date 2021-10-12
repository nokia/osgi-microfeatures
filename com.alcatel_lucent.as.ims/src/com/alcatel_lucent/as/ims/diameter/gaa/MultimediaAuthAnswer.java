// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.gaa;

import com.alcatel_lucent.as.ims.diameter.ImsAnswer;
import com.alcatel_lucent.as.ims.diameter.cx.SIPAuthDataItem;

/**
 * The Zh Multimedia-Auth-Answer (MAA).
 */
public interface MultimediaAuthAnswer
		extends ImsAnswer {

	/**
	 * Gets the User-Name AVP value.
	 * 
	 * @return The IMPI.
	 */
	public String getUserName();

	/**
	 * Gets the Public-Identity AVP value.
	 * 
	 * @return The IMPU.
	 */
	public String getPublicIdentity();

	/**
	 * Gets the SIP-Auth-Data-Item AVP value.
	 * 
	 * @return The SIP-Auth-Data-Item AVP value.
	 */
	public SIPAuthDataItem getSIPAuthDataItem();

	/**
	 * Gets the GBA-UserSecSettings AVP value.
	 * 
	 * @return The GBA user security settings.
	 */
	public byte[] getGBAUserSecSettings();
}
