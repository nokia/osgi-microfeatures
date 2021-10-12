// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter;

import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;

/**
 * The IMS generic request.
 */
public interface ImsRequest {

	/**
	 * Adds an AVP in the request.
	 * 
	 * @param avp The AVP to be added.
	 */
	public void addAvp(DiameterAVP avp);

	/**
	 * Gets an AVP according to its definition.
	 * 
	 * @param definition The definition of the AVP to be retrieved.
	 * @return The AVP or null if not found.
	 */
	public DiameterAVP getAvp(DiameterAVPDefinition definition);
}
