package com.nextenso.proxylet.diameter.util;

import java.math.BigInteger;

/**
 * The Unsigned64 AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.2 for information.
 * <p>
 * Since no Java primitive can wrap a 64 bit unsigned integer, the methods are
 * available for 3 types of representation:
 * <ul>
 * <li>2 ints : 1 for the higher 32 bits, 1 for the lower 32 bits
 * <li>a BigInteger
 * <li>a long : only valid when the value is positive
 * </ul>
 */
public class Unsigned64Format
		extends DiameterAVPFormat {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The single static instance.
	 */
	public static final Unsigned64Format INSTANCE = new Unsigned64Format();

	protected Unsigned64Format() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[], int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return "Unsigned64=" + getUnsigned64BigInteger(data, off).toString();
	}

	/**
	 * Decodes an Unsigned64 AVP value.
	 * 
	 * @param data The data to decode.
	 * @param offset The offset in the provided data.
	 * @return The decoded value represented as 2 integers : the first one for the
	 *         higher 32 bits and the second for the lower 32 bits.
	 */
	public static final int[] getUnsigned64Bytes(byte[] data, int offset) {
		return new int[] { Integer32Format.getInteger32(data, offset), Integer32Format.getInteger32(data, offset + 4) };
	}

	/**
	 * Encodes into an Unsigned64 AVP value.
	 * 
	 * @param big The 32 higher bits of the value to encode.
	 * @param small The 32 lower bits of the value to encode.
	 * @return The encoded value.
	 */
	public static final byte[] toUnsigned64(int big, int small) {
		byte[] res = new byte[8];
		toUnsigned64(res, 0, big, small);
		return res;
	}

	/**
	 * Encodes into an Unsigned64 AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param big The 32 higher bits of the value to encode.
	 * @param small The 32 lower bits of the value to encode.
	 * @return The length of the encoded value.
	 */
	public static final int toUnsigned64(byte[] destination, int destinationOffset, int big, int small) {
		Integer32Format.toInteger32(destination, destinationOffset, big);
		Integer32Format.toInteger32(destination, destinationOffset + 4, small);
		return 8;
	}

	/**
	 * Decodes an Unsigned64 AVP value.
	 * 
	 * @param data The data to decode.
	 * @param offset The offset in the provided data.
	 * @return The decoded value represented as a long : <b>be careful with the
	 *         sign of the returned value</b>.
	 */
	public static final long getUnsigned64Long(byte[] data, int offset) {
		return Integer64Format.getInteger64(data, offset);
	}

	/**
	 * Encodes into an Unsigned64 AVP value.
	 * 
	 * @param value The value to encode.
	 * @return The encoded value.
	 */
	public static final byte[] toUnsigned64(long value) {
		return Integer64Format.toInteger64(value);
	}

	/**
	 * Encodes into an Unsigned64 AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param value The value to encode.
	 * @return The length of the encoded value.
	 */
	public static final int toUnsigned64(byte[] destination, int destinationOffset, long value) {
		return Integer64Format.toInteger64(destination, destinationOffset, value);
	}

	/**
	 * Decodes an Unsigned64 AVP value.
	 * 
	 * @param data The data to decode.
	 * @param offset The offset in the provided data.
	 * @return The decoded value represented as a BigInteger.
	 */
	public static final BigInteger getUnsigned64BigInteger(byte[] data, int offset) {
		long l = getUnsigned64Long(data, offset);
		return new BigInteger(new byte[] { (byte) (l >>> 56), (byte) (l >>> 48), (byte) (l >>> 40), (byte) (l >>> 32), (byte) (l >>> 24),
				(byte) (l >>> 16), (byte) (l >>> 8), (byte) (l), });
	}

	/**
	 * Encodes into an Unsigned64 AVP value.
	 * 
	 * @param value The value to encode.
	 * @return The encoded value.
	 */
	public static final byte[] toUnsigned64(BigInteger value) {
		byte[] res = new byte[8];
		toUnsigned64(res, 0, value);
		return res;
	}

	/**
	 * Encodes into an Unsigned64 AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param value the value to encode.
	 * @return the encoded value.
	 */
	public static final int toUnsigned64(byte[] destination, int destinationOffset, BigInteger value) {
		byte[] b = value.toByteArray();
		int length = (b.length < 8 ? b.length : 8);
		System.arraycopy(b, 0, destination, destinationOffset + 8 - length, length);
		return length;
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof BigInteger) {
			return toUnsigned64((BigInteger) value);
		} else if(value instanceof Long) {
			return toUnsigned64((Long) value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". "
					+ "A BigInteger or Long is expected");
		}
	}

}
