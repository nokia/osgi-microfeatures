// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent.impl;

import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.acct.CoAUtils;

/**
 * The CoA Response (RFC 5176)..
 */
public class CoAResponse
		extends AccountingResponseFacade {

	/**
	 * Constructor for this class.
	 * 
	 * @param request The request
	 */
	public CoAResponse(AccountingRequestFacade request) {
		super(request);
	}

	/**
	 * @see com.nextenso.radius.agent.impl.AccountingResponseFacade#getDefaultPort()
	 */
	@Override
	public int getDefaultPort() {
		return CoAUtils.COA_PORT;
	}

	/**
	 * @see com.nextenso.radius.agent.impl.AccountingResponseFacade#isValidAttribute(com.nextenso.proxylet.radius.RadiusAttribute)
	 */
	@Override
	public boolean isValidAttribute(RadiusAttribute attribute) {
		boolean res = CoAUtils.isValidAttribute(getCode(), attribute);
		return res;
	}

	/**
	 * @see com.nextenso.radius.agent.impl.AccountingResponseFacade#getMessageType()
	 */
	@Override
	protected String getMessageType() {
		if (getCode() == CoAUtils.CODE_COA_ACK) {
			return "CoA  ACK Response";
		}
		if (getCode() == CoAUtils.CODE_COA_NAK) {
			return "CoA  NAK Response";
		}
		return "CoA Response (unknown code)";
	}

}
