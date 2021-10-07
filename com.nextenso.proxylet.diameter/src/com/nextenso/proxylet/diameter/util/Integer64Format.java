package com.nextenso.proxylet.diameter.util;

/**
 * The Integer64 AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.2 for information.
 */
public class Integer64Format
		extends DiameterAVPFormat {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The single static instance.
	 */
	public static final Integer64Format INSTANCE = new Integer64Format();

	protected Integer64Format() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[],
	 *      int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return "Integer64=" + String.valueOf(getInteger64(data, off));
	}

	/**
	 * Decodes an Integer64 AVP value.
	 * 
	 * @param data The data to decode.
	 * @param offset The offset in the provided data.
	 * @return The decoded value.
	 */
	public static final long getInteger64(byte[] data, int offset) {
		long res = data[offset];
		res <<= 8;
		res |= data[offset + 1] & 0xFF;
		res <<= 8;
		res |= data[offset + 2] & 0xFF;
		res <<= 8;
		res |= data[offset + 3] & 0xFF;
		res <<= 8;
		res |= data[offset + 4] & 0xFF;
		res <<= 8;
		res |= data[offset + 5] & 0xFF;
		res <<= 8;
		res |= data[offset + 6] & 0xFF;
		res <<= 8;
		res |= data[offset + 7] & 0xFF;
		return res;
	}

	/**
	 * Encodes into an Integer64 AVP value.
	 * 
	 * @param value The value to encode.
	 * @return The encoded value.
	 */
	public static final byte[] toInteger64(long value) {
		byte[] res = new byte[8];
		toInteger64(res, 0, value);
		return res;
	}

	/**
	 * Encodes into an Integer64 AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param value The value to encode.
	 * @return The length of the encoded value.
	 */
	public static final int toInteger64(byte[] destination, int destinationOffset, long value) {
		destination[destinationOffset] = (byte) (value >> 56);
		destination[destinationOffset + 1] = (byte) (value >> 48);
		destination[destinationOffset + 2] = (byte) (value >> 40);
		destination[destinationOffset + 3] = (byte) (value >> 32);
		destination[destinationOffset + 4] = (byte) (value >> 24);
		destination[destinationOffset + 5] = (byte) (value >> 16);
		destination[destinationOffset + 6] = (byte) (value >> 8);
		destination[destinationOffset + 7] = (byte) value;
		return 8;
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof Long) {
			return toInteger64((Long) value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". "
					+ "A Long is expected");
		}
	}
}
