// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.cx;

import com.alcatel_lucent.as.ims.diameter.ImsRequest;
import com.alcatel_lucent.as.ims.diameter.SupportedFeatures;


/**
 * The Cx generic request.
 */
public interface CxRequest
		extends ImsRequest {

	/**
	 * Adds to the list of supported features the specified features.
	 * 
	 * @param feature The feature to be added.
	 */
	public void addSupportedFeature(SupportedFeatures feature);
}
