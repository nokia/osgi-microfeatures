// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.cx;

/**
 * The Location Info Answer (LIA).
 */
public interface LocationInfoAnswer
		extends CxAnswer {

	/**
	 * Gets the server name (mapped to the Server-Name diameter AVP).
	 * 
	 * @return The name of the server.
	 */
	public String getServerName();

	/**
	 * Gets the server capabilities parameter of this response (mapped to the
	 * Server-Capabilities diameter AVP).
	 * 
	 * @return The information to help the I-CSCF in the selection of the S-CSCF
	 */
	public ServerCapabilities getServerCapabilities();

	/**
	 * Gets the Wilcarded PSI.
	 * 
	 * @return The PSI.
	 * @since 3GPP 29.299 v7.4
	 */
	public String getWildcardedPSI();

	/**
	 * Gets the Wilcarded public user identity.
	 * 
	 * @return The identity.
	 * @since 3GPP 29.299 v8.1
	 */
	public String getWildcardedIMPU();

}
