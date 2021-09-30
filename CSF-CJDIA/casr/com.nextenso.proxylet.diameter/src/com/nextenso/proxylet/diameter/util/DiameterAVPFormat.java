package com.nextenso.proxylet.diameter.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * This is the super class of all Diameter AVP Formats.
 * <p>
 * Diameter AVP Formats are used to parse AVP values and provide human-readable
 * representations of AVP values.
 * <p>
 * See RFC 3588 paragraphs 4.2, 4.3 and 4.4 for more information on AVP Formats.
 */
public abstract class DiameterAVPFormat
		implements Serializable {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor used by subclasses.
	 */
	protected DiameterAVPFormat() {}

	/**
	 * Returns a human-readable String representation of the value.
	 * <p/>
	 * This value should not be used for applicative processing, only for display.
	 * 
	 * @param data The value to decode.
	 * @return a String representation.
	 */
	public String toString(byte[] data) {
		return toString(data, 0, data.length, 0);
	}

	/**
	 * Returns a human-readable String representation of the value.
	 * <p/>
	 * This value should not be used for applicative processing, only for display.
	 * 
	 * @param data The value to decode
	 * @param level The level (depth) if the AVP in the Diameter Message. 0 means
	 *          top level AVP.
	 * @return a String representation
	 */
	public String toString(byte[] data, int level) {
		return toString(data, 0, data.length, level);
	}

	/**
	 * Returns a human-readable String representation of the value.
	 * <p/>
	 * This value should not be used for applicative processing, only for display.
	 * 
	 * @param data The value to decode.
	 * @param off The offset in the provided data.
	 * @param len The length of the provided data.
	 * @return a String representation.
	 */
	public String toString(byte[] data, int off, int len) {
		return toString(data, off, len, 0);
	}

	/**
	 * Returns a human-readable String representation of the value.
	 * <p/>
	 * This value should not be used for applicative processing, only for display.
	 * 
	 * @param data The value to decode.
	 * @param off The offset in the provided data.
	 * @param len The length of the provided data.
	 * @param level The level (depth) if the AVP in the Diameter Message. 0 means
	 *          top level AVP.
	 * @return a String representation.
	 */
	public abstract String toString(byte[] data, int off, int len, int level);
	
	public String getName() {
		String className = getClass().getSimpleName();
		if(className.endsWith("Format")) {
			return className.substring(0, className.length() - "Format".length());
		} else {
			return className;
		}
	}
	
	/**
	 * Encode the input object as a byte array appropriate for the Diameter
	 * protocol. 
	 * 
	 * @param value the object encode
	 * @exception IllegalArgumentException thrown if the input object type
	 * 	cannot be encoded
	 * @return byte representation for Diameter
	 */
	public abstract byte[] encode(Object value) throws IllegalArgumentException;

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
