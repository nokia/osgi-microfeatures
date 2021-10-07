package com.nextenso.proxylet.diameter.util;

/**
 * The Float64 AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.2 for information.
 */
public class Float64Format
		extends DiameterAVPFormat {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The single static instance.
	 */
	public static final Float64Format INSTANCE = new Float64Format();

	protected Float64Format() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[],
	 *      int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return "Float64=" + String.valueOf(getFloat64(data, off));
	}

	/**
	 * Decodes a Float64 AVP value.
	 * 
	 * @param data The data to decode.
	 * @param offset The offset in the provided data.
	 * @return The decoded value.
	 */
	public static final double getFloat64(byte[] data, int offset) {
		long bits = Integer64Format.getInteger64(data, offset);
		return Double.longBitsToDouble(bits);
	}

	/**
	 * Encodes into a Float64 AVP value.
	 * 
	 * @param value The value to encode.
	 * @return The encoded value.
	 */
	public static final byte[] toFloat64(double value) {
		byte[] res = new byte[8];
		toFloat64(res, 0, value);
		return res;
	}

	/**
	 * Encodes into a Float64 AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be
	 *          placed.
	 * @param offset The offset in the destination array.
	 * @param value The value to encode.
	 * @return The length of the encoded value.
	 */
	public static final int toFloat64(byte[] destination, int offset, double value) {
		return Integer64Format.toInteger64(destination, offset, Double.doubleToLongBits(value));
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof Double) {
			return toFloat64((Double) value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". "
					+ "A Double is expected");
		}
	}
}
