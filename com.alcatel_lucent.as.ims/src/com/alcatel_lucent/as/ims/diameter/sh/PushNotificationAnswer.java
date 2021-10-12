// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.sh;

import com.nextenso.proxylet.diameter.DiameterAVP;

/**
 * The push notification answer (PNA).
 */

public interface PushNotificationAnswer {

	/**
	 * Sets the result code.
	 * 
	 * @param code The result code.
	 */
	public void setResultCode(long code);

	/**
	 * Sets the experimental result.
	 * 
	 * @param vendorId The vendor id.
	 * @param experimentalResultCode The experimental result code
	 */
	public void setExperimentalResult(int vendorId, int experimentalResultCode);

	/**
	 * Adds an AVP in the answer.
	 * 
	 * @param avp The AVP to be added.
	 */
	public void addAvp(DiameterAVP avp);

}
