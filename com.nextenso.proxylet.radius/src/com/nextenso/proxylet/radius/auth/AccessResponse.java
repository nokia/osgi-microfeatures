// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.radius.auth;

import com.nextenso.proxylet.radius.RadiusMessage;

/**
 * This interface encapsulates an Access Response.
 */
public interface AccessResponse
		extends RadiusMessage {

	/**
	 * The Access-Accept message code (2).
	 * 
	 * @deprecated
	 * @use AuthUtils.CODE_ACCESS_ACCEPT
	 */
	@Deprecated
	public static final int ACCESS_ACCEPT_CODE = AuthUtils.CODE_ACCESS_ACCEPT;
	/**
	 * The Access-Reject message code (3).
	 * 
	 * @deprecated
	 * @use AuthUtils.CODE_ACCESS_REJECT
	 */
	@Deprecated
	public static final int ACCESS_REJECT_CODE = AuthUtils.CODE_ACCESS_REJECT;

	/**
	 * The Access-Challenge message code (11).
	 * 
	 * @deprecated
	 * @use AuthUtils.CODE_ACCESS_CHALLENGE
	 */
	@Deprecated
	public static final int ACCESS_CHALLENGE_CODE = AuthUtils.CODE_ACCESS_CHALLENGE;

	/**
	 * Gets the message code.
	 * 
	 * @return the code, which should be ACCESS_ACCEPT_CODE, ACCESS_REJECT_CODE or
	 *         ACCESS_CHALLENGE_CODE.
	 */
	public int getCode();

	/**
	 * Sets the message code.
	 * 
	 * @param code The code, which should be ACCESS_ACCEPT_CODE,
	 *          ACCESS_REJECT_CODE or ACCESS_CHALLENGE_CODE.
	 */
	public void setCode(int code);

	/**
	 * Indicates if it is an Access-Accept message.
	 * 
	 * @return true if the code is ACCESS_ACCEPT_CODE; false otherwise.
	 */
	public boolean isAcceptResponse();

	/**
	 * Indicates if it is an Access-Reject message.
	 * 
	 * @return true if the code is ACCESS_REJECT_CODE; false otherwise.
	 */
	public boolean isRejectResponse();

	/**
	 * Indicates if it is an Access-Challenge message.
	 * 
	 * @return true if the code is ACCESS_CHALLENGE_CODE; false otherwise.
	 */
	public boolean isChallengeResponse();

	/**
	 * Gets the associated request.
	 * 
	 * @return the request.
	 */
	public AccessRequest getRequest();
}
