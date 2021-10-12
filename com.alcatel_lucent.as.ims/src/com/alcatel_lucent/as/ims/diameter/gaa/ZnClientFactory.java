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
public interface ZnClientFactory {
	/**
	 * Gets the supported version.
	 * 
	 * @return The supported version.
	 */
	public abstract Version getVersion();

	/**
	 * Creates a Zn Client.
	 * 
	 * @param destinationHost The host the Cx Client will route messages to.
	 * @param destinationRealm The realm the Cx Client will route messages to.
	 * @return The Zn client.
	 * @throws NoRouteToHostException if the destination cannot be reached.
	 */
	public abstract ZnClient createZnClient(String destinationHost, String destinationRealm)
		throws NoRouteToHostException;

}
