// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.cx;

/**
 * The User-Authorization-Answer (UAA).
 */
public interface UserAuthorizationAnswer
		extends CxAnswer {

	/**
	 * Gets the name of the assigned S-CSCF. Mapped to the Server-Name diameter
	 * AVP.
	 * 
	 * @return the name of the assigned S-CSCF.
	 */
	public String getServerName();

	/**
	 * Gets the required capabilities of the S-CSCF to be assigned to the user
	 * Mapped to the Server-Capabilities diameter AVP.
	 * 
	 * @return the information to help the I-CSCF in the selection of the S-CSCF
	 */
	public ServerCapabilities getServerCapabilities();
	
	/**
	 * Gets the Wilcarded public user identity.
	 * 
	 * @return The identity.
	 * @since 3GPP 29.299 v8.1
	 */
	public String getWildcardedIMPU();
}
