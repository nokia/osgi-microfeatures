// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent.client;

import java.security.SignatureException;
import java.util.Enumeration;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.auth.AuthUtils;
import com.nextenso.radius.agent.Utils;
import com.nextenso.radius.agent.impl.AccessRequestFacade;
import com.nextenso.radius.agent.impl.AccessResponseFacade;
import com.nextenso.radius.agent.impl.RadiusMessageFacade;
import com.nextenso.radius.agent.impl.RadiusServer;

public class RadiusAccessRequest
		extends RadiusRequest {

	private static final Logger LOGGER = Logger.getLogger("agent.radius.request.access");
	private AccessRequestFacade _request;

	protected RadiusAccessRequest(byte[] password, RadiusAttribute[] attributes, RadiusServer server) {
		this(password, attributes, server, AuthUtils.CODE_ACCESS_REQUEST);
	}

	protected RadiusAccessRequest(byte[] password, RadiusAttribute[] attributes, RadiusServer server, int code) {
		super();
		_request = new AccessRequestFacade(getRadiusIdentifier(), true);
		_request.setCode(code);
		_request.setServer(server);
		_request.setPassword(password);
		_request.setAuthenticator(null, 0);
		if (attributes != null) {
			for (int i = 0; i < attributes.length; i++) {
				_request.addRadiusAttribute(attributes[i]);
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

	@Override
	protected void handleRadiusResponse(byte[] buff, int off, int len) {
		try {
			if (!_request.authenticate(buff, off, len)) {
				throw new SignatureException("Access Response Authentication Failed");
			}
			AccessResponseFacade authResponse = (AccessResponseFacade) _request.getResponse();
			authResponse.setCode(buff[off] & 0xFF);
			authResponse.readAttributes(buff, off + 20, len - 20);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("handleRadiusResponse: response=" + authResponse);
			}
			Utils.handleResponseProxyState(authResponse);

			setValid(true);
		}
		catch (Throwable e) {
			setValid(false);
			if (LOGGER.isEnabledFor(Level.WARN)) {
				LOGGER.warn("RadiusRequest : Received invalid radius response (identifier=" + getIdentifier() + "):" + e.getMessage());
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("type=ACCESS, ");
		res.append(super.toString());
		return res.toString();
	}

}
