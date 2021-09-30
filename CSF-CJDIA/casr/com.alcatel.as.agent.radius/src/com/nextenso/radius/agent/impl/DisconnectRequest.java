package com.nextenso.radius.agent.impl;

import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.acct.DisconnectUtils;

/**
 * The Disconnect Request (RFC 5176).
 */
public class DisconnectRequest
		extends AccountingRequestFacade {

	public DisconnectRequest(int identifier, boolean instanciateResp) {
		super(identifier, false);
		if (instanciateResp) {
			setResponse(new DisconnectResponse(this));
		}
	}

	/**
	 * @see com.nextenso.radius.agent.impl.AccountingRequestFacade#getDefaultPort()
	 */
	@Override
	public int getDefaultPort() {
		return DisconnectUtils.DISCONNECT_PORT;
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
		boolean res = DisconnectUtils.isValidAttribute(getCode(), attribute);
		return res;
	}

	/**
	 * @see com.nextenso.radius.agent.impl.AccountingRequestFacade#getMessageType()
	 */
	@Override
	protected String getMessageType() {
		return "Disconnect Request";
	}

}
