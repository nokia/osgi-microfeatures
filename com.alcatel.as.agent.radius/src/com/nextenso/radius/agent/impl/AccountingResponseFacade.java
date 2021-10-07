package com.nextenso.radius.agent.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

import alcatel.tess.hometop.gateways.utils.ByteBuffer;

import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.acct.AccountingRequest;
import com.nextenso.proxylet.radius.acct.AccountingResponse;
import com.nextenso.proxylet.radius.acct.AcctUtils;

/**
 * The AccountingResponse implementation.
 */
public class AccountingResponseFacade
		extends RadiusMessageFacade
		implements AccountingResponse {

	private AccountingRequestFacade _request;


	public AccountingResponseFacade(AccountingRequestFacade request) {
		super();
		setRequest(request);
	}

	private void setRequest(AccountingRequestFacade request) {
		_request = request;

		setId(_request.getId());
	}

	/**
	 * @see com.nextenso.proxylet.radius.acct.AccountingResponse#getRequest()
	 */
	public AccountingRequest getRequest() {
		return _request;
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
		buffer.append(((AccountingRequestFacade) getRequest()).getClientAuthenticator());

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
		byte[] authenticator = AuthenticationUtils.digest(getDigest(), buffer);

		// write the result
		writeResultTo(buffer, len, authenticator, out);
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getMessageType()
	 */
	@Override
	protected String getMessageType() {
		String res = "Accounting Response";
		return res;
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getSpecificAttributesToPrint()
	 */
	@Override
	public String getSpecificAttributesToPrint() {
		return ((RadiusMessageFacade) getRequest()).getSpecificAttributesToPrint();
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getDefaultPort()
	 */
	@Override
	public int getDefaultPort() {
		return ((RadiusMessageFacade) getRequest()).getDefaultPort();
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
	 * @see com.nextenso.proxylet.radius.RadiusMessage#isValidAttribute(com.nextenso.proxylet.radius.RadiusAttribute)
	 */
	public boolean isValidAttribute(RadiusAttribute attribute) {
		boolean res = AcctUtils.isValidAccountingResponseAttribute(attribute);
		return res;
	}

}
