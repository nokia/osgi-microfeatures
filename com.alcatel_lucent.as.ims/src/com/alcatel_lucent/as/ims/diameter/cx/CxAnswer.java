// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.cx;

import java.util.List;

import com.alcatel_lucent.as.ims.diameter.ImsAnswer;
import com.alcatel_lucent.as.ims.diameter.SupportedFeatures;

/**
 * The Cx generic response.
 */
public interface CxAnswer
		extends ImsAnswer {

	/**
	 * Gets the list of features supported.
	 * 
	 * @return The list of features supported.
	 */
	public List<SupportedFeatures> getSupportedFeatures();

	/**
	 * Gets the server name.
	 * 
	 * @return The server name.
	 */
	public String getServerName();

}
