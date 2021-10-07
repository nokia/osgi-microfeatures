package com.nextenso.radius.agent.impl;

import java.util.StringTokenizer;

import com.nextenso.mux.util.MuxUtils;
import com.nextenso.proxylet.radius.AuthenticationRule;
import com.nextenso.radius.agent.Utils;

public class RadiusSecret
		implements AuthenticationRule {

	private enum Type {
		MASK,
		HOST
	}

	private byte[] _secret;
	private int _ip;
	private Type _type;
	private boolean _isAuthenticationRequired = true;

	public RadiusSecret(String def) {
		StringTokenizer st = new StringTokenizer(def);
		int count = st.countTokens();
		if (count != 2 && count != 3) {
			throw new IllegalArgumentException("Invalid Radius Secret: " + def);
		}
		String ip_s = st.nextToken();
		if (ip_s.regionMatches(true, 0, "H:", 0, 2)) {
			_type = Type.HOST;
			ip_s = ip_s.substring(2).trim();
		} else {
			_type = Type.MASK;
		}

		_ip = MuxUtils.getIPAsInt(ip_s);
		_secret = st.nextToken().getBytes();

		if (st.hasMoreTokens()) {
			String nocheck = st.nextToken();
			if (nocheck != null) {
				if (!"nocheck".equalsIgnoreCase(nocheck)) {
					throw new IllegalArgumentException("Invalid Radius Secret: (bad check content) " + def);
				}
				_isAuthenticationRequired = false;
			}
		}
	}

	/**
	 * Used internally when parsing next servers to instanciate the secret for
	 * each server.
	 * 
	 * @param ip
	 * @param secret
	 */
	public RadiusSecret(String ip, String secret) {
		this(ip + " " + secret);
	}

	/**
	 * Used by proxylets when setting next server.
	 * 
	 * @param secret
	 */
	public RadiusSecret(byte[] secret) {
		_secret = Utils.copyArray(secret);
	}

	/**
	 * @see com.nextenso.proxylet.radius.AuthenticationRule#getPassword()
	 */
	public byte[] getPassword() {
		return Utils.copyArray(_secret);
	}

	/**
	 * @see com.nextenso.proxylet.radius.AuthenticationRule#requiresAuthentication()
	 */
	public boolean requiresAuthentication() {
		return _isAuthenticationRequired;
	}

	/**
	 * @see com.nextenso.proxylet.radius.AuthenticationRule#match(int)
	 */
	public boolean match(int ip) {
		if (_type == null) {
			return true;
		}

		switch (_type) {
			case HOST:
				return (_ip == ip);
			case MASK:
				return ((_ip & ip) == ip);
			default:
				return true; // meaningless
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder("RadiusSecret [");
		if (_type == Type.HOST) {
			buff.append("clientIP=");
			buff.append(MuxUtils.getIPAsString(_ip));
		} else if (_type == Type.MASK) {
			buff.append("clientMask=");
			buff.append(MuxUtils.getIPAsString(_ip));
		} else { // unknown or null
			buff.append("IP=All");
		}
		// DO NOT display secret !
		if (!_isAuthenticationRequired) {
			buff.append(", no required authentication");
		}
		buff.append(']');
		return buff.toString();
	}
}
