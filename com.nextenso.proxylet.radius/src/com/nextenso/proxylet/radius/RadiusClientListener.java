// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.radius;

public interface RadiusClientListener {

	/**
	 * Called when the response to a request is available.
	 * 
	 * @param client The client.
	 * @param response The response. The radius response or -1 if any problem.
	 */
	public void handleResponse(RadiusClient client, int response);

}
