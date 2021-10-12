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
import com.nextenso.proxylet.radius.RadiusUtils;
import com.nextenso.proxylet.radius.acct.AccountingRequest;
import com.nextenso.proxylet.radius.acct.AccountingResponse;
import com.nextenso.proxylet.radius.acct.AcctUtils;
import com.nextenso.radius.agent.Utils;

/**
 * The AccountingRequest Implementation.
 */
public class AccountingRequestFacade
		extends RadiusMessageFacade
		implements AccountingRequest {

	private AccountingResponse _response;
	private byte[] _proxyAuthenticator;
	private byte[] _clientAuthenticator;

	public AccountingRequestFacade(int identifier, boolean instanciateResp) {
		super();
		setId(identifier);

		if (instanciateResp) {
			setResponse(new AccountingResponseFacade(this));
		}
	}

	protected void setResponse(AccountingResponse response) {
		_response = response;
	}

	/**
	 * @see com.nextenso.proxylet.radius.acct.AccountingRequest#getResponse()
	 */
	public AccountingResponse getResponse() {
		return _response;
	}

	public byte[] getClientAuthenticator() {
		return Utils.copyArray(_clientAuthenticator);
	}

	public void setClientAuthenticator(byte[] authenticator, int off) {
		_clientAuthenticator = new byte[16];
		System.arraycopy(authenticator, off, _clientAuthenticator, 0, 16);
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#isValid()
	 */
	@Override
	public String isValid() {
		if (getRadiusAttribute(RadiusUtils.NAS_IP_ADDRESS) == null && getRadiusAttribute(RadiusUtils.NAS_IDENTIFIER) == null) {
			return "NAS-IP-Address or NAS-Identifier must be set";
		}
		String res = validateAttributes();
		return res;
	}

	protected String validateAttributes() {
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
		ByteBuffer buffer = new ByteBuffer(len + getServerSecret().length);

		// append the first line
		writePrologTo(buffer.getOutputStream(), len);

		// append a void Authenticator
		buffer.append(VOID_16, 0, 16);

		// append the Attributes
		writeAttributesTo(buffer.getOutputStream());
		
		// set Message-Authenticator if present
		if (getMessageAuthenticatorOffset () != -1){
		    byte[] msgAuthenticator = AuthenticationUtils.hmacDigest(buffer.toByteArray (false), 0, buffer.size (), getServerSecretAsString ());
		    System.arraycopy (msgAuthenticator, 0, buffer.toByteArray (false), 20 + getMessageAuthenticatorOffset (), 16);
		}
		
		// append the secret
		buffer.append(getServerSecret());

		// generate the Proxy Authenticator
		_proxyAuthenticator = AuthenticationUtils.digest(getDigest(), buffer);

		// write the result
		writeResultTo(buffer, len, _proxyAuthenticator, out);
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#abort()
	 */
	@Override
	public void abort() {
		super.abort();
		if (getResponse() != null) {
			((RadiusMessageFacade) getResponse()).abort();
		}
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getDefaultPort()
	 */
	@Override
	public int getDefaultPort() {
		return AcctUtils.ACCT_PORT;
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getRadiusServer(java.lang.String,
	 *      int, byte[], int)
	 */
	@Override
	public RadiusServer getRadiusServer(String host, int port, byte[] secret, int clientIP) {
		return new RadiusServer(host, port, secret, clientIP, true);
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getMessageType()
	 */
	@Override
	protected String getMessageType() {
		return "Accounting Request";
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
		boolean res = AcctUtils.isValidAccountingRequestAttribute(attribute);
		return res;
	}

	public boolean authenticate(byte[] response, int off, int len) {
		return AuthenticationUtils.authenticate(getDigest(), response, off, len, getServerSecret(), _proxyAuthenticator);
	}

	public boolean checkMessageAuthenticator(byte[] response, int off, int len, int msgAuthOffset) {
	    return AuthenticationUtils.checkMessageAuthenticator(response, off, len, msgAuthOffset, _proxyAuthenticator, getServerSecretAsString ());
	}

}
