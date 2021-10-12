// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.sh;

import java.util.List;

import com.alcatel_lucent.as.ims.diameter.ImsAnswer;
import com.alcatel_lucent.as.ims.diameter.SupportedFeatures;

/**
 * The Sh Response.
 */
public interface ShAnswer
		extends ImsAnswer {

	/**
	 * Gets the list of supported features
	 * 
	 * @return The list of the supported features.
	 */
	public List<SupportedFeatures> getSupportedFeatures();

	/**
	 * Gets the Wilcarded PSI.
	 * 
	 * @return The Wildcarded PSI of null if not found.
	 */
	public String getWildcardedPSI();
	
	/**
	 * Gets the Wildcarded public user identity.
	 * 
	 * @return The Wildcarded IMPU of null if not found.
	 */
	public String getWildcardedIMPU();
}
