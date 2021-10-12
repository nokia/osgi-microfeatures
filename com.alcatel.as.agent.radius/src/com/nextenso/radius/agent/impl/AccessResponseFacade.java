// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

import alcatel.tess.hometop.gateways.utils.ByteBuffer;

import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.auth.AccessRequest;
import com.nextenso.proxylet.radius.auth.AccessResponse;
import com.nextenso.proxylet.radius.auth.AuthUtils;

/**
 * The AccessResponse Implementation.
 */
public class AccessResponseFacade
		extends RadiusMessageFacade
		implements AccessResponse {

	private AccessRequest _request;

	public AccessResponseFacade(AccessRequestFacade request) {
		super();
		setRequest(request);
	}

	/**
	 * Sets the request.
	 * 
	 * @param request The request
	 */
	private void setRequest(AccessRequestFacade request) {
		_request = request;
		setId(getRequest().getId());
	}

	/**
	 * @see com.nextenso.proxylet.radius.auth.AccessResponse#getRequest()
	 */
	public AccessRequest getRequest() {
		return _request;
	}

	/**
	 * @see com.nextenso.proxylet.radius.auth.AccessResponse#isAcceptResponse()
	 */
	public boolean isAcceptResponse() {
		return (getCode() == AuthUtils.CODE_ACCESS_ACCEPT);
	}

	/**
	 * @see com.nextenso.proxylet.radius.auth.AccessResponse#isRejectResponse()
	 */
	public boolean isRejectResponse() {
		return (getCode() == AuthUtils.CODE_ACCESS_REJECT);
	}

	/**
	 * @see com.nextenso.proxylet.radius.auth.AccessResponse#isChallengeResponse()
	 */
	public boolean isChallengeResponse() {
		return (getCode() == AuthUtils.CODE_ACCESS_CHALLENGE);
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#isValid()
	 */
	@Override
	public String isValid() {
		Enumeration enumer = getRadiusAttributes();
		while (enumer.hasMoreElements()) {
			RadiusAttribute att = (RadiusAttribute) enumer.nextElement();
			if (!isValidAttribute(att)) {
				return "Invalid Attribute (type:" + att.getType() + ")";
			}
		}
		return null;
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#writeTo(java.io.OutputStream)
	 */
	@Override
	public void writeTo(OutputStream out)
		throws IOException {

		int len = getLength();

		ByteBuffer buffer = new ByteBuffer(len + getProxySecret().length);

		// write the first line
		writePrologTo(buffer.getOutputStream(), len);

		// append the request Authenticator
		buffer.append(((AccessRequestFacade) getRequest()).getAuthenticator());

		// append the Attributes
		writeAttributesTo(buffer.getOutputStream());

		// set Message-Authenticator if present
		if (getMessageAuthenticatorOffset () != -1){
		    byte[] msgAuthenticator = AuthenticationUtils.hmacDigest(buffer.toByteArray (false), 0, buffer.size (), getProxySecretAsString ());
		    System.arraycopy (msgAuthenticator, 0, buffer.toByteArray (false), 20 + getMessageAuthenticatorOffset (), 16);
		}

		// append the secret
		buffer.append(getProxySecret());

		// generate the Authenticator
		byte[] authenticator = AuthenticationUtils.digest(getDigest(),buffer);

		// write the result
		writeResultTo(buffer, len, authenticator, out);
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getDefaultPort()
	 */
	@Override
	public int getDefaultPort() {
		return ((RadiusMessageFacade) getRequest()).getDefaultPort();
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getMessageType()
	 */
	@Override
	protected String getMessageType() {
		String res = "";
		int code = getCode();
		if (code == AuthUtils.CODE_ACCESS_ACCEPT) {
			res = "Access Accept Response";
		} else if (code == AuthUtils.CODE_ACCESS_CHALLENGE) {
			res = "Access Challenge Response";
		} else if (code == AuthUtils.CODE_ACCESS_REJECT) {
			res = "Access Reject Response";
		}

		return res;
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getRadiusServer(java.lang.String,
	 *      int, byte[], int)
	 */
	@Override
	public RadiusServer getRadiusServer(String host, int port, byte[] secret, int clientIP) {
		return ((RadiusMessageFacade) getRequest()).getRadiusServer(host, port, secret, clientIP);
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getSpecificAttributesToPrint()
	 */
	@Override
	public String getSpecificAttributesToPrint() {
		return "";
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#isValidAttribute(com.nextenso.proxylet.radius.RadiusAttribute)
	 */
	public boolean isValidAttribute(RadiusAttribute attribute) {
		boolean res = AuthUtils.isValidAccessResponseAttribute(attribute, getCode());
		return res;
	}

}
