// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.util;

/**
 * The Float32 AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.2 for information.
 */
public class Float32Format
		extends DiameterAVPFormat {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The single static instance.
	 */
	public static final Float32Format INSTANCE = new Float32Format();

	protected Float32Format() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[], int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return "Float32=" + String.valueOf(getFloat32(data, off));
	}

	/**
	 * Decodes a Float32 AVP value.
	 * 
	 * @param data The value to decode.
	 * @param offset The offset in the provided data.
	 * @return The decoded value.
	 */
	public static final float getFloat32(byte[] data, int offset) {
		int bits = Integer32Format.getInteger32(data, offset);
		return Float.intBitsToFloat(bits);
	}

	/**
	 * Encodes into a Float32 AVP value.
	 * 
	 * @param value The value to encode.
	 * @return the encoded value.
	 */
	public static final byte[] toFloat32(float value) {
		byte[] res = new byte[4];
		toFloat32(res, 0, value);
		return res;
	}
	
	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof Float) {
			return toFloat32((Float)value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". " 
					+ "A Float is expected");
		}
	}

	/**
	 * Encodes into a Float32 AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param value The value to encode.
	 * @return The length of the encoded value.
	 */
	public static final int toFloat32(byte[] destination, int destinationOffset, float value) {
		return Integer32Format.toInteger32(destination, destinationOffset, Float.floatToIntBits(value));
	}
}
