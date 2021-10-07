package com.nextenso.proxylet.diameter.util;

/**
 * The Unsigned32 AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.2 for information.
 */
public class Unsigned32Format
		extends DiameterAVPFormat {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The single static instance.
	 */
	public static final Unsigned32Format INSTANCE = new Unsigned32Format();

	protected Unsigned32Format() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[], int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return "Unsigned32=" + String.valueOf(getUnsigned32(data, off));
	}

	/**
	 * Decodes an Unsigned32 AVP value.
	 * 
	 * @param data The data to decode.
	 * @param offset The offset in the provided data.
	 * @return The decoded value.
	 */
	public static final long getUnsigned32(byte[] data, int offset) {
		long res = data[offset] & 0xFF;
		res <<= 8;
		res |= data[offset + 1] & 0xFF;
		res <<= 8;
		res |= data[offset + 2] & 0xFF;
		res <<= 8;
		res |= data[offset + 3] & 0xFF;
		return res;
	}

	/**
	 * Encodes into an Unsigned32 AVP value.
	 * 
	 * @param value The value to encode.
	 * @return The encoded value.
	 */
	public static final byte[] toUnsigned32(long value) {
		byte[] res = new byte[4];
		toUnsigned32(res, 0, value);
		return res;
	}

	/**
	 * Encodes into an Unsigned32 AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param value The value to encode.
	 * @return The length of the encoded value.
	 */
	public static final int toUnsigned32(byte[] destination, int destinationOffset, long value) {
		destination[destinationOffset] = (byte) (value >> 24);
		destination[destinationOffset + 1] = (byte) (value >> 16);
		destination[destinationOffset + 2] = (byte) (value >> 8);
		destination[destinationOffset + 3] = (byte) value;
		return 4;
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof Long) {
			return toUnsigned32((Long) value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". "
					+ "A Long is expected");
		}
	}
}
