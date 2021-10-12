// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter;

/**
 * The interface to implement to handle Responses.
 */
public interface DiameterResponseProxylet
		extends DiameterProxylet {

	/**
	 * The engine will return to the first proxylet.
	 */
	public static final int FIRST_PROXYLET = 1;

	/**
	 * The proxy will call the same proxylet again.
	 */
	public static final int SAME_PROXYLET = 2;

	/**
	 * The engine will call the next proxylet.
	 */
	public static final int NEXT_PROXYLET = 3;

	/**
	 * The engine will not call any other proxylet.
	 */
	public static final int LAST_PROXYLET = 4;

	/**
	 * The engine will suspend to processing of the current proxylet chain..
	 */
	public static final int SUSPEND = 5;

	/**
	 * The engine will respond to the client and the response will go through the
	 * response proxylets.
	 */
	public static final int RESPOND_FIRST_PROXYLET = 100;

	/**
	 * The engine will respond to the client and the response will NOT go through
	 * the response proxylets.
	 */
	public static final int RESPOND_LAST_PROXYLET = 101;
    
	/**
	 * The engine will re-inject the request into the request proxylet chain.
	 * The response proxylet processing is stopped and the response object is cleaned.
	 */
	public static final int REDIRECT_FIRST_PROXYLET = 200;
	
	/**
	 * The engine will take the request as is for resending.
	 * The response proxylet processing is stopped and the response object is cleaned.
	 */
	public static final int REDIRECT_LAST_PROXYLET = 201;

	/**
	 * Called by the engine to know how the proxylet will handle the Response. <br/>
	 * The possible return codes are: ACCEPT, ACCEPT_MAY_BLOCK, IGNORE. <b>NOTE:
	 * This method can be called by the engine several times in a row for the same
	 * response.</b> Therefore it should behave accordingly.
	 * 
	 * @param response the response to handle.
	 * @return ACCEPT, ACCEPT_MAY_BLOCK or IGNORE.
	 */
	public int accept(DiameterResponse response);

	/**
	 * Processes the response. <br/>
	 * Returns one of the predefined codes to specify what the engine should do
	 * next with the response.
	 * 
	 * @param response The response to process.
	 * @return SUSPEND, SAME_PROXYLET, FIRST_PROXYLET, NEXT_PROXYLET, LAST_PROXYLET,
	 *         RESPOND_FIRST_PROXYLET or RESPOND_LAST_PROXYLET.
	 */
	public int doResponse(DiameterResponse response);

}
