// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.client;

/**
 * The interface to implement to be notified of incoming requests.
 */
public interface DiameterRequestListener {

	/**
	 * Called when a request comes in.
	 * 
	 * @param request The request that arrived (some methods that apply to
	 *          outgoing requests, like execute(), are disabled).
	 * @param response The response to send after filling in the AVPs.
	 */
	public void handleRequest(DiameterClientRequest request, DiameterClientResponse response);

}
