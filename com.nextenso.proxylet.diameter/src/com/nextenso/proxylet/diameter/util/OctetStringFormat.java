// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.util;

/**
 * The OctetString AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.2 for information. <br>
 * This is the default Format of an AVP.
 */
public class OctetStringFormat
		extends DiameterAVPFormat {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The single static instance.
	 */
	public static final OctetStringFormat INSTANCE = new OctetStringFormat();

	protected OctetStringFormat() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[], int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		StringBuilder buff = new StringBuilder("OctetString(Binary)=[");
		for (int i = 0; i < len; i++) {
			int val = data[off + i] & 0xFF;
			buff.append("0x");
			if (val < 0x10)
				buff.append('0');
			buff.append(Integer.toHexString(val)).append("('").append((val >= 32 && val < 128) ? (char) val : '?').append("')");
			if (i != len - 1)
				buff.append(", ");
		}
		buff.append(']');
		return buff.toString();
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof byte[]) {
			return (byte[]) value;
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". "
					+ "A byte array is expected");
			}
	}
}
