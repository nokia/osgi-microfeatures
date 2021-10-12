// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.radius.acct;

import com.nextenso.proxylet.radius.RadiusProxylet;

/**
 * The interface to implement to handle Accounting Requests.
 */
public interface AccountingRequestProxylet
		extends RadiusProxylet {

	/**
	 * The Engine does not respond to this request.
	 */
	public static final int NO_RESPONSE = 10;

	/**
	 * Called by the Engine to know how the proxylet will handle the Request. <br/>
	 * The possible return codes are: ACCEPT, ACCEPT_MAY_BLOCK, IGNORE. <b>NOTE:
	 * This method can be called by the Engine several times in a row for the same
	 * request.</b> Therefore it should behave accordingly.
	 * 
	 * @param request The request to handle.
	 * @return ACCEPT, ACCEPT_MAY_BLOCK or IGNORE.
	 */
	public int accept(AccountingRequest request);

	/**
	 * Processes the request. <br/>
	 * Returns one of the predefined codes to specify what the Engine should do
	 * next with the request.
	 * 
	 * @param request The request to process.
	 * @return NO_RESPONSE, SAME_PROXYLET, FIRST_PROXYLET, NEXT_PROXYLET,
	 *         LAST_PROXYLET, RESPOND_FIRST_PROXYLET or RESPOND_LAST_PROXYLET.
	 */
	public int doRequest(AccountingRequest request);

}
