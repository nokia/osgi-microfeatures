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
import com.nextenso.proxylet.radius.auth.AccessRequest;
import com.nextenso.proxylet.radius.auth.AccessResponse;
import com.nextenso.proxylet.radius.auth.AuthUtils;
import com.nextenso.radius.agent.Utils;

/**
 * The AccessRequest implementation.
 */
public class AccessRequestFacade
		extends RadiusMessageFacade
		implements AccessRequest {

	private AccessResponse _response;
	private byte[] _authenticator;
	private byte[] _password;
	private byte[] _passwordEncoded;

	public AccessRequestFacade(int identifier, boolean instanciateResp) {
		super();
		setId(identifier);
			if (instanciateResp) {
			setResponse(new AccessResponseFacade(this));
		}
	}

	public void setResponse(AccessResponse response) {
		_response = response;
	}

	/**
	 * @see com.nextenso.proxylet.radius.auth.AccessRequest#getResponse()
	 */
	public AccessResponse getResponse() {
		return _response;
	}

	/**
	 * Gets the authenticator.
	 * 
	 * @return The authenticator.
	 */
	public byte[] getAuthenticator() {
		return Utils.copyArray(_authenticator);
	}

	/**
	 * Sets the authenticator.
	 * 
	 * @param authenticator The authenticator.
	 * @param offset The offset.
	 */
	public void setAuthenticator(byte[] authenticator, int offset) {
		// generate the Authenticator or keep the original
		if (authenticator == null) {
			_authenticator = AuthenticationUtils.getRandomAuthenticator();
		} else {
			_authenticator = new byte[16];
			System.arraycopy(authenticator, offset, _authenticator, 0, 16);
		}
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#setServer(com.nextenso.radius.agent.impl.RadiusServer)
	 */
	@Override
	public void setServer(RadiusServer server) {
		// we assume the secret is modified
		_passwordEncoded = null;
		super.setServer(server);
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getLength()
	 */
	@Override
	public int getLength() {
		if (_password != null) {
			return super.getLength() + 2 + _password.length;
		}
		return super.getLength();
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#isValid()
	 */
	@Override
	public String isValid() {
		boolean hasPassword = (_password != null);
		boolean hasChapPassword = (getRadiusAttribute(RadiusUtils.CHAP_PASSWORD) != null);
		if (hasPassword && hasChapPassword) {
			return "User-Password and CHAP-Password cannot be both set";
		}
		if (!hasPassword && !hasChapPassword && getRadiusAttribute(RadiusUtils.STATE) == null) {
			return "User-Password or CHAP-Password or State must be set";
		}
		if (getRadiusAttribute(RadiusUtils.NAS_IP_ADDRESS) == null && getRadiusAttribute(RadiusUtils.NAS_IDENTIFIER) == null) {
			return "NAS-IP-Address or NAS-Identifier must be set";
		}
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
		// if no Message-Authenticator if present --> the normal case (rfc3579 support was added later)
		if (getMessageAuthenticatorOffset () == -1){
			// write the first line
			writePrologTo(out);

			// write the authenticator
			out.write(_authenticator, 0, 16);

			// write the password
			// password and passwordEncoded have the same length
			if (_password != null) {
				encodePassword();
				out.write(RadiusUtils.USER_PASSWORD);
				out.write(2 + _passwordEncoded.length);
				out.write(_passwordEncoded, 0, _passwordEncoded.length);
			}

			// write the Attributes
			writeAttributesTo(out);
		} else {
			// if there is a message-authenticator : similar to acct request code (which was done a while back for rfc 5176)
			
			int len = getLength();
			ByteBuffer buffer = new ByteBuffer(len);

			// append the first line
			writePrologTo(buffer.getOutputStream(), len);

			// write the authenticator
			buffer.append(_authenticator, 0, 16);
			
			// write the password - yet not expected if there is a message-authenticator
			// password and passwordEncoded have the same length
			if (_password != null) {
				encodePassword();
				buffer.append((byte) RadiusUtils.USER_PASSWORD);
				buffer.append((byte) (2 + _passwordEncoded.length));
				buffer.append(_passwordEncoded, 0, _passwordEncoded.length);
			}
			// append the Attributes
			writeAttributesTo(buffer.getOutputStream());
		
			// set Message-Authenticator
			byte[] msgAuthenticator = AuthenticationUtils.hmacDigest(buffer.toByteArray (false), 0, buffer.size (), getServerSecretAsString ());
			System.arraycopy (msgAuthenticator, 0, buffer.toByteArray (false), 20 + getMessageAuthenticatorOffset (), 16);
			
			// write the result
			out.write (buffer.toByteArray (false), 0, len);
		}
	}

	/**
	 * Encodes the password.
	 */
	private void encodePassword() {
		if (_passwordEncoded != null) {
			return;
		}
		if (_password == null) {
			return;
		}

		ByteBuffer result = new ByteBuffer(_password.length);

		ByteBuffer tmp = new ByteBuffer(getServerSecret().length + 16);
		tmp.append(getServerSecret());
		tmp.append(_authenticator, 0, 16);
		byte[] digest = AuthenticationUtils.digest(getDigest(),tmp);
		xor16(digest, 0, _password, 0, result);

		int off = 0;
		int left = _password.length - 16;
		while (left > 0) {
			tmp.init();
			tmp.append(getServerSecret());
			tmp.append(result.toByteArray(false), off, 16);
			digest = AuthenticationUtils.digest(getDigest(),tmp);
			off += 16;
			xor16(digest, 0, _password, off, result);
			left -= 16;
		}

		_passwordEncoded = result.toByteArray(true);
	}

	private static void xor16(byte[] b1, int off1, byte[] b2, int off2, ByteBuffer dest) {
		for (int i = 0; i < 16; i++)
			dest.append((byte) (b1[off1 + i] ^ b2[off2 + i]));
	}

	public void setEncodedPassword(byte[] password, int off, int len) {
		decodePassword(password, off, len);
		_passwordEncoded = null;
	}

	public void setPassword(byte[] password) {
		if (password == null) {
			return;
		}

		int size = (password.length / 16) * 16;
		if (password.length == 0 || password.length % 16 != 0) {
			size += 16;
		}
		// we pad to have a multiple of 16
		_password = new byte[size];
		_passwordEncoded = null; // we force encoding
		System.arraycopy(password, 0, _password, 0, password.length);
	}

	public byte[] getPassword() {
		return Utils.copyArray(_password);
	}

	public void decodePassword(byte[] pass, int offset, int len) {
		ByteBuffer result = new ByteBuffer(len);

		ByteBuffer tmp = new ByteBuffer(getProxySecret().length + 16);
		tmp.append(getProxySecret());
		tmp.append(_authenticator, 0, 16);
		byte[] digest = AuthenticationUtils.digest(getDigest(), tmp);
		xor16(digest, 0, pass, offset, result);

		int off = 0;
		int left = len - 16;
		while (left > 0) {
			tmp.init();
			tmp.append(getProxySecret());
			tmp.append(result.toByteArray(false), off, 16);
			digest = AuthenticationUtils.digest(getDigest(),tmp);
			off += 16;
			xor16(digest, 0, pass, offset + off, result);
			left -= 16;
		}

		_password = result.toByteArray(true);
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
		return AuthUtils.AUTH_PORT;
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getRadiusServer(java.lang.String,
	 *      int, byte[], int)
	 */
	@Override
	public RadiusServer getRadiusServer(String host, int port, byte[] secret, int clientIP) {
		return new RadiusServer(host, port, secret, clientIP, false);
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getMessageType()
	 */
	@Override
	protected String getMessageType() {
		return "Access Request";
	}

	/**
	 * @see com.nextenso.radius.agent.impl.RadiusMessageFacade#getSpecificAttributesToPrint()
	 */
	@Override
	public String getSpecificAttributesToPrint() {
		StringBuilder buff = new StringBuilder();
		byte[] password = getPassword();
		if (password != null) {
			buff.append("User-Password: ");
			buff.append(new String(password));
			buff.append('\n');
		}
		return buff.toString();
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusMessage#isValidAttribute(com.nextenso.proxylet.radius.RadiusAttribute)
	 */
	public boolean isValidAttribute(RadiusAttribute attribute) {
		boolean res = AuthUtils.isValidAccessRequestAttribute(attribute);
		return res;
	}

	public boolean authenticate(byte[] response, int off, int len) {
		return AuthenticationUtils.authenticate(getDigest(), response, off, len, getServerSecret(), getAuthenticator());
	}

	public boolean checkMessageAuthenticator(byte[] response, int off, int len, int msgAuthOffset) {
		return AuthenticationUtils.checkMessageAuthenticator(response, off, len, msgAuthOffset, getAuthenticator (), getServerSecretAsString ());
	}
}
