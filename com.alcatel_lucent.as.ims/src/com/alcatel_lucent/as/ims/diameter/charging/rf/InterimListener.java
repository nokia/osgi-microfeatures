// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging.rf;

/**
 * The Interim Listener.
 */
public interface InterimListener {

	/**
	 * Called when an INTERIM must be sent.
	 * 
	 * @param client The client.
	 */
	void doInterim(RfSessionClient client);

}
