package com.nextenso.proxylet.radius;

/**
 * This class encapsulates a Vendor-Specific Attribute (See RFC 2865 section
 * 5.26).
 * <p/>
 * A Vendor-Specific Attribute is a regular Attribute whose type is 26 but which
 * has an extra field for the Vendor-Id. No format is imposed for the attribute
 * value, but the RFC suggests the following:<br/>
 * <i>vendor-type(1 byte)/vendor-length(1 byte)/vendor-value(n bytes).</i><br/>
 * Therefore, some utility methods are provided to handle this format.
 * <p/>
 * A VendorSpecificAttribute is usually presented as <i>26/xxx/yyy</i> : 26 is
 * the type (fixed), xxx is the vendorId, yyy is the vendor-type. This
 * representation implies that the format proposed by the RFC is followed.
 */
public class VendorSpecificAttribute
		extends RadiusAttribute {

	private int _vendorId;

	/**
	 * Constructs a new VendorSpecificAttribute with the specified vendorId.
	 * 
	 * @param vendorId The vendorId.
	 */
	public VendorSpecificAttribute(int vendorId) {
		super();
		setType(RadiusUtils.VENDOR_SPECIFIC);
		_vendorId = vendorId;
	}

	/**
	 * Gets the vendorId.
	 * 
	 * @return The vendorId.
	 */
	public int getVendorId() {
		return _vendorId;
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusAttribute#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder(super.toString());
		buff.append(" [vendor-id=");
		buff.append(String.valueOf(_vendorId));
		buff.append(']');
		return buff.toString();
	}

	/**
	 * Gets the Vendor-Type. <br/>
	 * This is a utility method that parses a VendorSpecificAttribute value
	 * assuming that it follows the format:
	 * vendor-type/vendor-length/vendor-value.<br/>
	 * This format is proposed by RFC 2865, but is not mandatory.
	 * 
	 * @param value The value to parse.
	 * @return The vendor-type, or -1 if the value does not follow the format.
	 */
	public static int getVendorType(byte[] value) {
		if (!checkVendorValue(value)) {
			return -1;
		}

		return (value[0] & 0xFF);
	}

	/**
	 * Gets the Vendor-Value. <br/>
	 * This is a utility method that parses a VendorSpecificAttribute value
	 * assuming that it follows the format:
	 * vendor-type/vendor-length/vendor-value.<br/>
	 * This format is proposed by RFC 2865, but is not mandatory.
	 * 
	 * @param value The value to parse.
	 * @return The vendor-value, or null if the value does not follow the format.
	 */
	public static byte[] getVendorValue(byte[] value) {
		if (!checkVendorValue(value)) {
			return null;
		}

		byte[] res = new byte[value.length - 2];
		System.arraycopy(value, 2, res, 0, res.length);
		return res;
	}

	/**
	 * Formats a VendorSpecificAttribute value. <br/>
	 * This is a utility method that generates the VendorSpecificAttribute value
	 * given a vendor-type and a vendor-value.<br/>
	 * It generates the value using the format:
	 * vendor-type/vendor-length/vendor-value.<br/>
	 * This format is proposed by RFC2865, but is not mandatory.
	 * 
	 * @param vendorType The vendor-type.
	 * @param vendorValue The vendor-value.
	 * @return The VendorSpecificAttribute value.
	 */
	public static byte[] formatValue(int vendorType, byte[] vendorValue) {
		int len = (vendorValue != null) ? vendorValue.length : 0;
		byte[] res = new byte[len + 2];
		res[0] = (byte) vendorType;
		res[1] = (byte) res.length;
		if (len != 0) {
			System.arraycopy(vendorValue, 0, res, 2, len);
		}
		return res;
	}

	/**
	 * Checks if the value follows the RFC proposal. However, It cannot know if it
	 * is fortuitous or voluntary.
	 */
	private static boolean checkVendorValue(byte[] value) {
		return (value != null && value.length >= 2 && (value[1] & 0xFF) == value.length);
	}

}
