// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.radius.auth;

import com.nextenso.proxylet.radius.RadiusProxylet;

/**
 * The interface to implement to handle Access Requests.
 */
public interface AccessRequestProxylet
		extends RadiusProxylet {

	/**
	 * Called by the Engine to know how the proxylet will handle the Request. <br/>
	 * The possible return codes are: ACCEPT, ACCEPT_MAY_BLOCK, IGNORE. <b>NOTE:
	 * This method can be called by the Engine several times in a row for the same
	 * request.</b> Therefore it should behave accordingly.
	 * 
	 * @param request The request to handle.
	 * @return ACCEPT, ACCEPT_MAY_BLOCK or IGNORE.
	 */
	public int accept(AccessRequest request);

	/**
	 * Processes the request. <br/>
	 * Returns one of the predefined codes to specify what the Engine should do
	 * next with the request.
	 * 
	 * @param request The request to process.
	 * @return SAME_PROXYLET, FIRST_PROXYLET, NEXT_PROXYLET, LAST_PROXYLET,
	 *         RESPOND_FIRST_PROXYLET or RESPOND_LAST_PROXYLET
	 */
	public int doRequest(AccessRequest request);

}
