// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging.rf;

import java.net.NoRouteToHostException;

import com.alcatel_lucent.as.ims.Version;

/**
 * The Rf Client Factory.
 * 
 * @since ASR 4.0 SP3
 */
public interface RfClientFactory {

	/**
	 * Gets the supported version.
	 * 
	 * @return The supported version of the 3GPP 32.299 document..
	 */
	public Version getVersion();

	/**
	 * Creates a client to send events.
	 * 
	 * @param servers The list of server names.
	 * @param realm The realm.
	 * @return A new client.
	 * @throws NoRouteToHostException if no server can be reached.
	 */
	public RfEventClient createEventClient(Iterable<String> servers, String realm)
		throws NoRouteToHostException;

	/**
	 * Creates a client to send requests in a session.
	 * 
	 * @param servers The list of server names.
	 * @param realm The realm.
	 * @param listener A listener to notify that an interim request must be sent.
	 * @return A new client.
	 * @throws NoRouteToHostException if no server can be reached.
	 */
	public RfSessionClient createSessionClient(Iterable<String> servers, String realm, InterimListener listener)
		throws NoRouteToHostException;
}
