package com.nextenso.radius.agent.impl;

import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.acct.DisconnectUtils;

/**
 * The Disconnect Response (RFC 5176).
 */
public class DisconnectResponse
		extends AccountingResponseFacade {

	/**
	 * Constructor for this class.
	 * 
	 * @param request The request
	 */
	public DisconnectResponse(AccountingRequestFacade request) {
		super(request);
	}

	/**
	 * @see com.nextenso.radius.agent.impl.AccountingResponseFacade#getDefaultPort()
	 */
	@Override
	public int getDefaultPort() {
		return DisconnectUtils.DISCONNECT_PORT;
	}

	/**
	 * @see com.nextenso.radius.agent.impl.AccountingResponseFacade#isValidAttribute(com.nextenso.proxylet.radius.RadiusAttribute)
	 */
	@Override
	public boolean isValidAttribute(RadiusAttribute attribute) {
		boolean res = DisconnectUtils.isValidAttribute(getCode(), attribute);
		return res;
	}

	/**
	 * @see com.nextenso.radius.agent.impl.AccountingResponseFacade#getMessageType()
	 */
	@Override
	protected String getMessageType() {
		if (getCode() == DisconnectUtils.CODE_DISCONNECT_ACK) {
			return "Disconnect ACK Response";
		}
		if (getCode() == DisconnectUtils.CODE_DISCONNECT_NAK) {
			return "Disconnect NAK Response";
		}
		return "Disconnect Response (unknown code)";
	}

}
