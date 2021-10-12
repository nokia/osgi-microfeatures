// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.util;

/**
 * The Integer32 AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.2 for information.
 */
public class Integer32Format
		extends DiameterAVPFormat {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The single static instance.
	 */
	public static final Integer32Format INSTANCE = new Integer32Format();

	protected Integer32Format() {
		super();
	}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[], int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return "Integer32=" + String.valueOf(getInteger32(data, off));
	}

	/**
	 * Decodes an Integer32 AVP value.
	 * 
	 * @param data The data to decode.
	 * @param offset The offset in the provided data.
	 * @return The decoded value.
	 */
	public static final int getInteger32(byte[] data, int offset) {
		int res = data[offset];
		res <<= 8;
		res |= data[offset + 1] & 0xFF;
		res <<= 8;
		res |= data[offset + 2] & 0xFF;
		res <<= 8;
		res |= data[offset + 3] & 0xFF;
		return res;
	}

	/**
	 * Encodes into an Integer32 AVP value.
	 * 
	 * @param value The value to encode.
	 * @return The encoded value.
	 */
	public static final byte[] toInteger32(int value) {
		byte[] res = new byte[4];
		toInteger32(res, 0, value);
		return res;
	}

	/**
	 * Encodes into an Integer32 AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param val The value to encode.
	 * @return The length of the encoded value.
	 */
	public static final int toInteger32(byte[] destination, int destinationOffset, int val) {
		destination[destinationOffset] = (byte) (val >> 24);
		destination[destinationOffset + 1] = (byte) (val >> 16);
		destination[destinationOffset + 2] = (byte) (val >> 8);
		destination[destinationOffset + 3] = (byte) val;
		return 4;
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof Integer) {
			return toInteger32((Integer) value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". "
					+ "An Integer is expected");
		}
	}
}
