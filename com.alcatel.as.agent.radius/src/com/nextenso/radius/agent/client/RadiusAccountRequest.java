package com.nextenso.radius.agent.client;

import java.security.SignatureException;
import java.util.Enumeration;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.radius.agent.Utils;
import com.nextenso.radius.agent.impl.AccountingRequestFacade;
import com.nextenso.radius.agent.impl.AccountingResponseFacade;
import com.nextenso.radius.agent.impl.RadiusMessageFacade;
import com.nextenso.radius.agent.impl.RadiusServer;

public class RadiusAccountRequest
		extends RadiusRequest {

	private AccountingRequestFacade _request;
	private static final Logger LOGGER = Logger.getLogger("agent.radius.request.accounting");

	protected RadiusAccountRequest(RadiusAttribute[] attributes, RadiusServer server, int code) {
		super();
		_request = new AccountingRequestFacade(getRadiusIdentifier(), true);
		_request.setCode(code);
		_request.setServer(server);
		if (attributes != null) {
			for (RadiusAttribute attribute :  attributes) {
				_request.addRadiusAttribute(attribute);
			}
		}
	
	}
	/**
	 * @see com.nextenso.radius.agent.client.RadiusRequest#getRequest()
	 */
	@Override
	protected RadiusMessageFacade getRequest() {
		return _request;
	}

	/**
	 * @see com.nextenso.radius.agent.client.RadiusRequest#getResponseAttributes()
	 */
	@Override
	protected Enumeration getResponseAttributes() {
		return _request.getResponse().getRadiusAttributes();
	}

	/**
	 * @see com.nextenso.radius.agent.client.RadiusRequest#getResponseCode()
	 */
	@Override
	protected int getResponseCode() {
		return _request.getResponse().getCode();
	}

	/**
	 * @see com.nextenso.radius.agent.client.RadiusRequest#handleRadiusResponse(byte[],
	 *      int, int)
	 */
	@Override
	protected void handleRadiusResponse(byte[] buff, int off, int len) {
		try {

			if (!_request.authenticate(buff, off, len)) {
				throw new SignatureException("Accounting Response Authentication Failed");
			}

			AccountingResponseFacade acctResponse = (AccountingResponseFacade) _request.getResponse();
			acctResponse.setCode(buff[off] & 0xFF);
			acctResponse.readAttributes(buff, off + 20, len - 20);
			
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("handleRadiusResponse: response=" + acctResponse);
			}

			Utils.handleResponseProxyState(acctResponse);

			setValid(true);
		}
		catch (Throwable e) {
			setValid(false);
			LOGGER.warn("RadiusRequest : Received invalid radius response (identifier=" + getIdentifier() + "): " + e.getMessage());
		}
	}

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("type=ACCOUNTING, ");
		res.append(super.toString());
		return res.toString();
	}

}
