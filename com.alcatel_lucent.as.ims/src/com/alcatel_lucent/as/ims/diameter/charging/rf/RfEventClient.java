// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging.rf;

import com.alcatel_lucent.as.ims.diameter.ImsClient;

/**
 * The Rf Event Client .
 */
public interface RfEventClient
		extends ImsClient {

	/**
	 * Creates a new EVENT request.
	 * 
	 * @return The new request.
	 */
	public AccountingRequest createEventRequest();
}
