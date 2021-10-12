// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.gaa;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;

/**
 * The Zh Client Factory.
 */
public interface ZhClientFactory {
	/**
	 * Gets the supported version.
	 * 
	 * @return The supported version of the 3GPP 29.109 document.
	 */
	public abstract Version getVersion();

	/**
	 * Creates a Zh Client.
	 * 
	 * @param destinationHost The host the Cx Client will route messages to.
	 * @param destinationRealm The realm the Cx Client will route messages to.
	 * @return The Zh client.
	 * @throws NoRouteToHostException if the destination cannot be reached.
	 */
	public abstract ZhClient createZhClient(String destinationHost, String destinationRealm)
		throws NoRouteToHostException;

}
