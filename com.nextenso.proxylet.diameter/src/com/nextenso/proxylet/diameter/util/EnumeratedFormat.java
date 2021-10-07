package com.nextenso.proxylet.diameter.util;

/**
 * The Enumerated AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.3 for information.
 */
public class EnumeratedFormat
		extends DiameterAVPFormat {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The single static instance.
	 */
	public static final EnumeratedFormat INSTANCE = new EnumeratedFormat();

	protected EnumeratedFormat() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[],
	 *      int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return "Integer32(Enumerated)=" + String.valueOf(getEnumerated(data, off));
	}

	/**
	 * Decodes an Enumerated AVP value.
	 * 
	 * @param data The value to decode.
	 * @param offset The offset in the provided data.
	 * @return The decoded value.
	 */
	public static final int getEnumerated(byte[] data, int offset) {
		return Integer32Format.getInteger32(data, offset);
	}

	/**
	 * Encodes into an Enumerated AVP value.
	 * 
	 * @param value The value to encode.
	 * @return The encoded value.
	 */
	public static final byte[] toEnumerated(int value) {
		byte[] res = new byte[4];
		toEnumerated(res, 0, value);
		return res;
	}

	/**
	 * Encodes into an Enumerated AVP value.
	 * 
	 * @param destination the destination array where the encoded value should be
	 *          placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param value The value to encode.
	 * @return The length of the encoded value.
	 */
	public static final int toEnumerated(byte[] destination, int destinationOffset, int value) {
		return Integer32Format.toInteger32(destination, destinationOffset, value);
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof Integer) {
			return toEnumerated((Integer) value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + "."
					+ "An Integer is expected");
		}
	}
}
