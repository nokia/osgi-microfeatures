// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.util;

import java.nio.charset.Charset;


/**
 * The DiameterIdentity AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.3 for information.
 */
public class IdentityFormat
		extends DiameterAVPFormat {

	/**
	 * The default charset.
	 */
	private  static Charset DEFAULT_CHARSET= Charset.defaultCharset();

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The single static instance.
	 */
	public static final IdentityFormat INSTANCE = new IdentityFormat();

	protected IdentityFormat() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[],
	 *      int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return "Identity=" + getIdentity(data, off, len);
	}

	/**
	 * Decodes a DiameterIdentity AVP value.
	 * 
	 * @param data The data to decode. May be null.
	 * @return The decoded value. Or null if data is null.
	 */
	public static final String getIdentity(byte[] data) {
		if (data == null) return null;
		return getIdentity(data, 0, data.length);
	}

	/**
	 * Decodes a DiameterIdentity AVP value.
	 * 
	 * @param data The data to decode. Must not be null.
	 * @param offset The offset in the provided data.
	 * @param length The length of the provided data.
	 * @return The decoded value.
	 */
	public static final String getIdentity(byte[] data, int offset, int length) {
		return new String(data, offset, length, DEFAULT_CHARSET);
	}

	/**
	 * Encodes into a DiameterIdentity AVP value.
	 * 
	 * @param value The value to encode.
	 * @return The encoded value
	 */
	public static final byte[] toIdentity(String value) {
		return value.getBytes(DEFAULT_CHARSET);
	}

	/**
	 * Encodes into a DiameterIdentity AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be
	 *          placed.
	 * @param offset The offset in the destination array.
	 * @param value The value to encode.
	 * @return The length of the encoded value.
	 */
	public static final int toIdentity(byte[] destination, int offset, String value) {
		byte[] b = value.getBytes(DEFAULT_CHARSET);
		System.arraycopy(b, 0, destination, offset, b.length);
		return b.length;
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof String) {
			return toIdentity((String) value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". "
					+ "A String is expected");
		}
	}
}
