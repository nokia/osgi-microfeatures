// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent.impl;

import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.acct.CoAUtils;

/**
 * The CoA Request (RFC 5176)..
 */
public class CoARequest
		extends AccountingRequestFacade {

	/**
	 * Constructor for this class. 
	 *
	 * @param identifier The identifier.
	 * @param instanciateResp true if a response must be created for this request.
	 */
	public CoARequest(int identifier, boolean instanciateResp) {
		super(identifier, false);
		if (instanciateResp) {
			setResponse(new CoAResponse(this));
		}
	}

	/**
	 * @see com.nextenso.radius.agent.impl.AccountingRequestFacade#getDefaultPort()
	 */
	@Override
	public int getDefaultPort() {
		return CoAUtils.COA_PORT;
	}

	/**
	 * @see com.nextenso.radius.agent.impl.AccountingRequestFacade#isValid()
	 */
	@Override
	public String isValid() {
		String res = validateAttributes();
		return res;
	}

	/**
	 * @see com.nextenso.radius.agent.impl.AccountingRequestFacade#isValidAttribute(com.nextenso.proxylet.radius.RadiusAttribute)
	 */
	@Override
	public boolean isValidAttribute(RadiusAttribute attribute) {
		boolean res = CoAUtils.isValidAttribute(getCode(), attribute);
		return res;
	}
	
	/**
	 * @see com.nextenso.radius.agent.impl.AccountingRequestFacade#getMessageType()
	 */
	@Override
	protected String getMessageType() {
		return "CoA Request";
	}

}
