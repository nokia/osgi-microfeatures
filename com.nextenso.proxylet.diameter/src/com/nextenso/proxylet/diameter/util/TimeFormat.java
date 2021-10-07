package com.nextenso.proxylet.diameter.util;

import java.time.Instant;
import java.util.Date;

/**
 * The Time AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.3 for information. <br>
 * The Time format uses the NTP format (number of seconds since 0h on 1 January
 * 1900). <br>
 * Since Java uses the number of milliseconds since midnight, January 1, 1970 in
 * Date-related objects, some methods are provided to perform conversions.
 */
public class TimeFormat
		extends DiameterAVPFormat {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The number of seconds between 1.1.1900 and 1.1.1970
	 */
	private static final long NTP_OFFSET = 2208988800L;

	/**
	 * The single static instance.
	 */
	public static final TimeFormat INSTANCE = new TimeFormat();

	protected TimeFormat() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[],
	 *      int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		return "Time=" + new Date(getTime(data, off));
	}

	/**
	 * Converts a Java date value into an NTP value.
	 * 
	 * @param java The number of milliseconds since midnight, January 1, 1970.
	 * @return The number of seconds since 0h on 1 January 1900.
	 */
	public static final long toNtp(long java) {
		return NTP_OFFSET + java / 1000L;
	}

	/**
	 * Converts an NTP value into a Java date value.
	 * 
	 * @param ntp The number of seconds since 0h on 1 January 1900.
	 * @return The number of milliseconds since midnight, January 1, 1970.
	 */
	public static final long toJava(long ntp) {
		return (ntp - NTP_OFFSET) * 1000L;
	}

	/**
	 * Decodes a Time AVP value.
	 * 
	 * @param data The data to decode.
	 * @param offset The offset in the provided data.
	 * @return The decoded value in Java format.
	 */
	public static final long getTime(byte[] data, int offset) {
		return toJava(Unsigned32Format.getUnsigned32(data, offset));
	}

	/**
	 * Encodes into a Time AVP value.
	 * 
	 * @param java The java value to encode (the number of milliseconds since
	 *          midnight, January 1, 1970).
	 * @return The encoded value.
	 */
	public static final byte[] toTime(long java) {
		byte[] res = new byte[4];
		toTime(res, 0, java);
		return res;
	}

	/**
	 * Encodes into a Time AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be
	 *          placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param java The java value to encode (the number of milliseconds since
	 *          midnight, January 1, 1970).
	 * @return The length of the encoded value.
	 */
	public static final int toTime(byte[] destination, int destinationOffset, long java) {
		return Unsigned32Format.toUnsigned32(destination, destinationOffset, toNtp(java));
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof Long) {
			return toTime((Long) value);
		} else if(value instanceof Date) {
			return toTime(((Date)value).getTime());
		} else if(value instanceof Instant) {
			return toTime(((Instant)value).toEpochMilli());
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". "
					+ "A byte array is expected");
		}
	}
}
