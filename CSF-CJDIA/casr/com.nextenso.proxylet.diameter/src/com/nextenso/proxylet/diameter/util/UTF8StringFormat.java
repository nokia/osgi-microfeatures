package com.nextenso.proxylet.diameter.util;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * The UTF8String AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.3 for information.
 */
public class UTF8StringFormat
		extends DiameterAVPFormat {

	/**
	 * The UTF-8 charset.
	 */
	private static Charset UTF8_CHARSET = null;
	static {
		try {
			UTF8_CHARSET = Charset.forName("UTF-8");
		}
		catch (UnsupportedCharsetException e) {}
	}

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The single static instance.
	 */
	public static final UTF8StringFormat INSTANCE = new UTF8StringFormat();

	protected UTF8StringFormat() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[],
	 *      int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return "UTF8String=\"" + getUtf8String(data, off, len) + "\"";
	}

	/**
	 * Decodes a UTF8String AVP value.
	 * 
	 * @param data The data to decode. May be null.
	 * @return The decoded value. Or null if data is null.
	 */
	public static final String getUtf8String(byte[] data) {
		if (data == null) return null;
		return getUtf8String(data, 0, data.length);
	}

	/**
	 * Decodes a UTF8String AVP value.
	 * 
	 * @param data The data to decode. Must not be null.
	 * @param offset The offset in the provided data.
	 * @param length The length of the provided data.
	 * @return The decoded value.
	 */
	public static final String getUtf8String(byte[] data, int offset, int length) {
		if (UTF8_CHARSET == null) {
			throw new RuntimeException("UTF8 not available");
		}
		return new String(data, offset, length, UTF8_CHARSET);
	}

	/**
	 * Encodes into a UTF8String AVP value.
	 * 
	 * @param value The value to encode.
	 * @return The encoded value.
	 */
	public static final byte[] toUtf8String(String value) {
		if (UTF8_CHARSET == null) {
			throw new RuntimeException("UTF8 not available");
		}
		return value.getBytes(UTF8_CHARSET);
	}

	/**
	 * Encodes into a UTF8String AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be
	 *          placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param value The value to encode.
	 * @return The length of the encoded value.
	 */
	public static final int toUtf8String(byte[] destination, int destinationOffset, String value) {
		if (UTF8_CHARSET == null) {
			throw new RuntimeException("UTF8 not available");
		}
		byte[] b = value.getBytes(UTF8_CHARSET);
		System.arraycopy(b, 0, destination, destinationOffset, b.length);
		return b.length;
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof String) {
			return toUtf8String((String) value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". "
					+ "A String is expected");
		}
	}
}
