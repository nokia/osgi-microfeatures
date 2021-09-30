package com.nextenso.proxylet.radius;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class encapsulates a Radius attribute.
 * <p/>
 * It may be multivalued. Each value is an array of bytes that can be
 * interpreted as : a String, an integer, an IP Address or a Date, depending on the
 * attribute type. Note that the values are ordered.
 * <p/>
 * Three special Radius Attributes cannot be encapsulated by this class :
 * User-Password, CHAP-Password (use class CHAPPasswordAttribute),
 * Vendor-Specific (use class VendorSpecificAttribute).
 */
public class RadiusAttribute
		implements Cloneable {

	private int _type;
	private List<byte[]> _values = new ArrayList<byte[]>(2);

	/**
	 * Protected Constructor used by subclasses.
	 */
	protected RadiusAttribute() {}

	/**
	 * Constructs a new RadiusAttribute of the given type. <br/>
	 * The type cannot be modified afterwards.
	 * 
	 * @param type The type.
	 * @throws IllegalArgumentException if the type is User-Password (2),
	 *           CHAP-Password (3) or Vendor-Specific (26).
	 */
	public RadiusAttribute(int type) {
		switch (type) {
			case RadiusUtils.USER_PASSWORD:
				throw new IllegalArgumentException("User-Password Attributes cannot be instanciated - Use method setPassword in AccessRequest");
			case RadiusUtils.CHAP_PASSWORD:
				throw new IllegalArgumentException("CHAP-Password Attributes must use class CHAPPasswordAttribute");
			case RadiusUtils.VENDOR_SPECIFIC:
				throw new IllegalArgumentException("Vendor-Specific Attributes must use class VendorSpecificAttribute");
			case ExtendedTypeAttribute.Extended_Type_1:
			case ExtendedTypeAttribute.Extended_Type_2:
			case ExtendedTypeAttribute.Extended_Type_3:
			case ExtendedTypeAttribute.Extended_Type_4:
			case ExtendedTypeAttribute.Long_Extended_Type_1:
			case ExtendedTypeAttribute.Long_Extended_Type_2:
				throw new IllegalArgumentException("Extended Type Attributes must use class ExtendedTypeAttribute");
			default:
				setType(type);
		}
	}

	/**
	 * Protected method used by subclasses to set the type.
	 * 
	 * @param type The type.
	 */
	protected void setType(int type) {
		_type = type;
	}

	/**
	 * Gets the type.
	 * 
	 * @return The type.
	 */
	public int getType() {
		return _type;
	}

	/**
	 * Gets the number of values.
	 * 
	 * @return The number of values.
	 */
	public int getValueSize() {
		return _values.size();
	}

	/**
	 * Gets the first value of the attribute.
	 * 
	 * @return The first value, or <code>null</code> if the Attribute has no
	 *         value.
	 */
	public byte[] getValue() {
		return getValue(0);
	}

	/**
	 * Gets the value at the specified index, starting at 0.
	 * 
	 * @param index The index (cannot be lower than 0 or greater than
	 *          <code>getValueSize()</code>).
	 * @return The value at the specified index, or <code>null</code> if there is
	 *         no value at the specified index.
	 */
	public byte[] getValue(int index) {
		if (index < 0 || index >= getValueSize()) {
			return null;
		}
		return _values.get(index);
	}

	/**
	 * Gets the first value as a String.
	 * 
	 * @return The first value as a String or <code>null</code> if the value is
	 *         not defined or is not text.
	 */
	public String getValueAsText() {
		return convertValueToText(getValue());
	}

	/**
	 * Returns the first value as an IP Address.
	 * 
	 * @return The first value as an IP Address or <code>null</code> if the value
	 *         is not defined or is not an IP Address.
	 */
	public String getValueAsAddress() {
		return convertValueToAddress(getValue());
	}

	/**
	 * Returns the first value as an int.
	 * 
	 * @return The first value as an int.
	 * @throws IllegalArgumentException if the value is not an int.
	 */
	public int getValueAsInt() {
		return convertValueToInt(getValue());
	}

	/**
	 * Returns the first value as an integer64.
	 * 
	 * @return The first value as an integer64.
	 * @throws IllegalArgumentException if the value is not an integer64.
	 */
	public long getValueAsInt64() {
		return convertValueToInt64(getValue());
	}

	/**
	 * Returns the first value as a Date.
	 * 
	 * @return The first value as a Date or <code>null</code> if the value is not
	 *         defined or is not a Date.
	 */
	public Date getValueAsTime() {
		return convertValueToDate(getValue());
	}

	/**
	 * Sets the value. <br/>
	 * The attribute will be single-valued. Its value WILL BE a clone of the
	 * specified value if the copy flag is set to true and MAY BE a clone
	 * otherwise.
	 * 
	 * @param value The attribute value.
	 * @param copy A flag specifying if the value must be cloned.
	 */
	public void setValue(byte[] value, boolean copy) {
		setValue(value, 0, value.length, copy);
	}

	/**
	 * Sets the value. <br/>
	 * The attribute will be single-valued. Its value WILL BE a clone of the
	 * specified value if the copy flag is set to true and MAY BE a clone
	 * otherwise.
	 * 
	 * @param value The attribute value.
	 * @param off The attribute value offset.
	 * @param len The attribute value length.
	 * @param copy A flag specifying if the value must be cloned.
	 */
	public void setValue(byte[] value, int off, int len, boolean copy) {
		_values.clear();
		addValue(0, value, off, len, copy);
	}

	/**
	 * Adds a value at the end of the attribute values list. <br/>
	 * The added value WILL BE a clone of the specified value if the copy flag is
	 * set to true and MAY BE a clone otherwise.
	 * 
	 * @param value The value to be added.
	 * @param copy A flag specifying if the value must be cloned.
	 */
	public void addValue(byte[] value, boolean copy) {
		addValue(value, 0, value.length, copy);
	}

	/**
	 * Adds a value at the end of the attribute values list. <br/>
	 * 
	 * The added value WILL BE a clone of the specified value if the copy flag is
	 * set to true and MAY BE a clone otherwise.
	 * 
	 * @param value The value to be added.
	 * @param off The value offset.
	 * @param len The value length.
	 * @param copy A flag specifying if the value must be cloned.
	 */
	public void addValue(byte[] value, int off, int len, boolean copy) {
		addValue(_values.size(), value, off, len, copy);
	}

	/**
	 * Adds a value at the specified index in the attribute values list. <br/>
	 * 
	 * The added value WILL BE a clone of the specified value if the copy flag is
	 * set to true and MAY BE a clone otherwise.
	 * 
	 * @param index The index in the list.
	 * @param value The value to be added.
	 * @param off The value offset.
	 * @param len The value length.
	 * @param copy A flag specifying if the value must be cloned.
	 */
	public void addValue(int index, byte[] value, int off, int len, boolean copy) {
		if (value == null) {
			throw new NullPointerException("The value cannot be null");
		}
		boolean myCopy = copy;
		myCopy = (myCopy || off != 0 || len != value.length);
		byte[] clone;
		if (myCopy) {
			clone = new byte[len];
			System.arraycopy(value, off, clone, 0, len);
		} else {
			clone = value;
		}
		_values.add(index, clone);
	}

	/**
	 * Sets the value of the attribute to the value of the specified String. <br/>
	 * 
	 * The Attribute will be single-valued.
	 * 
	 * @param value The value.
	 */
	public void setTextValue(String value) {
		setValue(convertTextToValue(value), false);
	}

	/**
	 * Sets the value of the Attribute to the value of the specified IP Address. <br/>
	 * 
	 * The Attribute will be single-valued.
	 * 
	 * @param value The value.
	 */
	public void setAddressValue(String value) {
		setValue(convertAddressToValue(value), false);
	}

	/**
	 * Sets the value of the attribute to the value of the specified int. <br/>
	 * The Attribute will be single-valued.
	 * 
	 * @param i the attribute value
	 */
	public void setIntValue(int i) {
		setValue(convertIntToValue(i), false);
	}

	/**
	 * Sets the value of the attribute to the value of the specified Date. <br/>
	 * 
	 * The attribute will be single-valued.
	 * 
	 * @param value the value
	 */
	public void setDateValue(Date value) {
		setValue(convertDateToValue(value), false);
	}

	/**
	 * Removes all the values.
	 */
	public void removeValues() {
		_values.clear();
	}

	/**
	 * Removes the value located at the specified index.
	 * 
	 * @param index The index.
	 * @return The removed value or <code>null</code> if the value is not defined.
	 */
	public byte[] removeValue(int index) {
		if (index >= getValueSize()) {
			return null;
		}
		return _values.remove(index);
	}

	/**
	 * Converts an attribute value into a String.
	 * 
	 * @param value The value to be converted.
	 * @return The String representation or <code>null</code> if the value is
	 *         <code>null</code> or if UTF-8 is not supported by the platform.
	 */
	public static String convertValueToText(byte[] value) {
		if (value == null) {
			return null;
		}
		try {
			return new String(value, "UTF-8");
		}
		catch (Exception e) {
			// possibly java.io.UnsupportedEncodingException;
			return null;
		}
	}

	/**
	 * Converts an attribute value into an IP Address.
	 * 
	 * @param value The value to be converted.
	 * @return the IP Address String representation or <code>null</code> if the
	 *         value is not an IP Address.
	 */
	public static String convertValueToAddress(byte[] value) {
		if (value == null || value.length != 4) {
			return null;
		}
		StringBuilder buff = new StringBuilder(15);
		buff.append(String.valueOf(value[0] & 0xFF));
		buff.append('.');
		buff.append(String.valueOf(value[1] & 0xFF));
		buff.append('.');
		buff.append(String.valueOf(value[2] & 0xFF));
		buff.append('.');
		buff.append(String.valueOf(value[3] & 0xFF));
		return buff.toString();
	}

	/**
	 * Converts an attribute value into an int.
	 * 
	 * @param value The value to be converted.
	 * @return The int value.
	 * @throws IllegalArgumentException if the value is not an int.
	 */
	public static int convertValueToInt(byte[] value) {
		if (value == null || value.length != 4) {
			throw new IllegalArgumentException("The value size must be 4");
		}
		int i = value[0] << 24;
		i |= (value[1] & 0xFF) << 16;
		i |= (value[2] & 0xFF) << 8;
		i |= (value[3] & 0xFF);
		return i;
	}

	/**
	 * Converts an attribute value into an integer64.
	 * 
	 * @param value The value to be converted.
	 * @return The integer64 value.
	 * @throws IllegalArgumentException if the value is not an int.
	 */
	public static long convertValueToInt64(byte[] value) {
		if (value == null || value.length != 8) {
			throw new IllegalArgumentException("The value size must be 8");
		}
		long i = value[0] << 56;
		i |= (value[1] & 0xFFL) << 48;
		i |= (value[2] & 0xFFL) << 40;
		i |= (value[3] & 0xFFL) << 32;
		i |= (value[4] & 0xFFL) << 24;
		i |= (value[5] & 0xFFL) << 16;
		i |= (value[6] & 0xFFL) << 8;
		i |= (value[7] & 0xFFL);
		return i;
	}

	/**
	 * Converts an attribute value into a Date.
	 * 
	 * @param value The value to be converted.
	 * @return The Date or <code>null</code> if the value is not a Date.
	 */
	public static Date convertValueToDate(byte[] value) {
		if (value == null || value.length != 4) {
			return null;
		}
		try {
			int i = convertValueToInt(value);
			return new Date(i & 0xFFFFFFFFL);
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Converts a String into an attribute value.
	 * 
	 * @param text The text to be converted.
	 * @return The attribute value or <code>null</code> if the text is
	 *         <code>null</code> or if UTF-8 is not supported by the platform.
	 */
	public static byte[] convertTextToValue(String text) {
		if (text == null) {
			return null;
		}
		try {
			return text.getBytes("UTF-8");
		}
		catch (Exception e) {
			// possibly java.io.UnsupportedEncodingException;
			return null;
		}
	}

	/**
	 * Converts an IP address into an attribute value.
	 * 
	 * @param address The IP address to be converted.
	 * @return The attribute value or <code>null</code> if the address is not
	 *         valid
	 */
	public static byte[] convertAddressToValue(String address) {
		if (address == null) {
			return null;
		}
		byte[] res = new byte[4];
		int index1 = 0;
		int index2;
		try {
			for (int i = 0; i < 3; i++) {
				index2 = address.indexOf('.', index1);
				if (index2 == -1) {
					return null;
				}
				res[i] = (byte) Integer.parseInt(address.substring(index1, index2));
				index1 = index2 + 1;
			}
			res[3] = (byte) Integer.parseInt(address.substring(index1));
		}
		catch (NumberFormatException e) {
			return null;
		}
		return res;
	}

	/**
	 * Converts an int into an attribute value.
	 * 
	 * @param i The int to be converted.
	 * @return The attribute value.
	 */
	public static byte[] convertIntToValue(int i) {
		byte[] value = new byte[4];
		value[0] = (byte) (i >> 24);
		value[1] = (byte) (i >> 16);
		value[2] = (byte) (i >> 8);
		value[3] = (byte) i;
		return value;
	}

	/**
	 * Converts an integer64 into an attribute value.
	 * 
	 * @param l The long to be converted.
	 * @return The attribute value.
	 */
	public static byte[] convertInt64ToValue(long l) {
		byte[] value = new byte[8];
		value[0] = (byte) (l >> 56);
		value[1] = (byte) (l >> 48);
		value[2] = (byte) (l >> 40);
		value[3] = (byte) (l >> 32);
		value[4] = (byte) (l >> 24);
		value[5] = (byte) (l >> 16);
		value[6] = (byte) (l >> 8);
		value[7] = (byte) l;
		return value;
	}

	/**
	 * Converts a Date into an attribute value.
	 * 
	 * @param date The Date to be converted.
	 * @return the attribute value or <code>null</code> if the date is
	 *         <code>null</code>
	 */
	public static byte[] convertDateToValue(Date date) {
		if (date == null) {
			return null;
		}
		return convertIntToValue((int) date.getTime());
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		try {
			RadiusAttribute clone = (RadiusAttribute) super.clone();
			clone._values = new ArrayList<byte[]>(_values.size());
			for (byte[] value : _values) {
				clone._values.add(value.clone());
			}
			return clone;
		}
		catch (CloneNotSupportedException e) {
			// never happens
			return null;
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder();
		buff.append("RadiusAttribute");
		buff.append(" [type=").append(_type).append(']');
		String name = RadiusUtils.getRadiusAttributeName(_type);
		if (name == null) {
			name = "???";
		}
		buff.append(" [name=").append(name).append(']');
		int dataType = RadiusUtils.getRadiusAttributeDataType(_type);
		int size = getValueSize();
		for (int i = 0; i < size; i++) {
			buff.append(" [value#").append(i);
			byte[] b = getValue(i);
			String s;
			boolean done = false;
			switch (dataType) {
				case RadiusUtils.DATA_TYPE_TEXT:
					buff.append("(text)=");
					s = convertValueToText(b);
					if (s == null) {
						s = "???";
					}
					buff.append(s);
					done = true;
					break;
				case RadiusUtils.DATA_TYPE_ADDRESS:
					if (!done) {
						buff.append("(address)=");
						s = convertValueToAddress(b);
						if (s == null) {
							s = "???";
						}
						buff.append(s);
						done = true;
					}
					break;
				case RadiusUtils.DATA_TYPE_INTEGER:
					if (!done) {
						buff.append("(integer)=");
						try {
							s = String.valueOf(convertValueToInt(b));
						}
						catch (Exception e) {
							s = "???";
						}
						buff.append(s);
						done = true;
					}
					break;
				case RadiusUtils.DATA_TYPE_DATE:
					if (!done) {
						buff.append("(date)=");
						Date d = convertValueToDate(b);
						if (d == null) {
							buff.append("???");
						} else {
							buff.append(d.toString());
						}
						done = true;
					}
					break;
				case RadiusUtils.DATA_TYPE_BIN:
				case -1: // unknown data type
					if (!done) {
						buff.append("(binary)=");
					}
					buff.append('(');
					for (int j = 0; j < b.length; j++) {
						buff.append("0x").append(Integer.toHexString(b[j] & 0xFF));
						if (j != b.length - 1) {
							buff.append(',');
						}
					}
					buff.append(')');
					break;
				default:
			}
			buff.append(']');
		}
		return buff.toString();
	}
}
