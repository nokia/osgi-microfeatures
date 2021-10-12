// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.util;

/**
 * The Address AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.3 for information.
 */
public class AddressFormat
		extends DiameterAVPFormat {
	
	public static class DiameterAddress {
		private int addressType;
		private byte[] addressData;
		
		public DiameterAddress(int addressType, byte[] addressData) {
			super();
			this.addressType = addressType;
			this.addressData = addressData;
		}

		public int getAddressType() {
			return addressType;
		}

		public void setAddressType(int addressType) {
			this.addressType = addressType;
		}

		public byte[] getAddressData() {
			return addressData;
		}

		public void setAddressData(byte[] addressData) {
			this.addressData = addressData;
		}
	}

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The single static instance.
	 */
	public static final AddressFormat INSTANCE = new AddressFormat();

	protected AddressFormat() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[],
	 *      int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		StringBuilder buff = new StringBuilder();
		buff.append("Address=[type=");
		buff.append(getAddressTypeAsString(getAddressType(data, off)));
		buff.append(", value={");
		for (int i = 2; i < len; i++) {
			int val = data[off + i] & 0xFF;
			buff.append("0x").append(Integer.toHexString(val)).append("('").append(val).append("')");
			if (i != len - 1)
				buff.append(", ");
		}
		buff.append("}]");
		return buff.toString();
	}

	/**
	 * Returns the type of an Address AVP value.
	 * 
	 * @param data The value to decode. Must not be null.
	 * @param offset The offset in the provided data.
	 * @return The address type.
	 */
	public static final int getAddressType(byte[] data, int offset) {
		int type = data[offset] & 0xFF;
		type <<= 8;
		type |= data[offset + 1] & 0xFF;
		return type;
	}

	/**
	 * Returns the value of an Address AVP value.
	 * 
	 * @param data The value to decode. May be null.
	 * @return The address value. Or null if data is null.
	 */
	public static final byte[] getAddressValue(byte[] data) {
		if (data == null) return null;
		return getAddressValue(data, 0, data.length);
	}

	/**
	 * Returns the value of an Address AVP value.
	 * 
	 * @param data The value to decode. Must not be null.
	 * @param offset The offset in the provided data.
	 * @param length The length of the provided data.
	 * @return The address value.
	 */
	public static final byte[] getAddressValue(byte[] data, int offset, int length) {
		byte[] res = new byte[length - 2];
		System.arraycopy(data, offset + 2, res, 0, res.length);
		return res;
	}

	/**
	 * Encodes into an Address AVP value.
	 * 
	 * @param addressType The address type.
	 * @param value The address value.
	 * @return The encoded value.
	 */
	public static final byte[] toAddress(int addressType, byte[] value) {
		return toAddress(addressType, value, 0, value.length);
	}
	
	/**
	 * Encode into an Address AVP value
	 * @param the address
	 * @return the encoded value
	 */
	public static final byte[] toAddress(DiameterAddress addr) {
		return toAddress(addr.getAddressType(), addr.getAddressData());
	}

	/**
	 * Encodes into an Address AVP value.
	 * 
	 * @param addressType The address type.
	 * @param value The address value.
	 * @param offset The offset in the value.
	 * @param length The length of the address value.
	 * @return The encoded value.
	 */
	public static final byte[] toAddress(int addressType, byte[] value, int offset, int length) {
		byte[] res = new byte[2 + length];
		toAddress(res, 0, addressType, value, offset, length);
		return res;
	}

	/**
	 * Encodes into an Address AVP value.
	 * 
	 * @param destination The destination array where the encoded value should be
	 *          placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param addressType The address type.
	 * @param value The address value.
	 * @param offset The offset in the value.
	 * @param length The length of the value.
	 * @return The encoded value.
	 */
	public static final int toAddress(byte[] destination, int destinationOffset, int addressType, byte[] value, int offset, int length) {
		destination[destinationOffset] = (byte) (addressType >> 8);
		destination[destinationOffset + 1] = (byte) addressType;
		System.arraycopy(value, offset, destination, destinationOffset + 2, length);
		return 2 + length;
	}

	/**
	 * The Address type for IPV4.
	 */
	public static final int IPV4 = 1;
	/**
	 * The Address type for IPV6.
	 */
	public static final int IPV6 = 2;
	/**
	 * The Address type for NSAP.
	 */
	public static final int NSAP = 3;
	/**
	 * The Address type for HDLC.
	 */
	public static final int HDLC = 4;
	/**
	 * The Address type for BBN_1822.
	 */
	public static final int BBN_1822 = 5;
	/**
	 * The Address type for _802.
	 */
	public static final int _802 = 6;
	/**
	 * The Address type for E_163.
	 */
	public static final int E_163 = 7;
	/**
	 * The Address type for E_164.
	 */
	public static final int E_164 = 8;
	/**
	 * The Address type for F_69.
	 */
	public static final int F_69 = 9;
	/**
	 * The Address type for X_121.
	 */
	public static final int X_121 = 10;
	/**
	 * The Address type for IPX.
	 */
	public static final int IPX = 11;
	/**
	 * The Address type for APPLETALK.
	 */
	public static final int APPLETALK = 12;
	/**
	 * The Address type for DECNET_IV.
	 */
	public static final int DECNET_IV = 13;
	/**
	 * The Address type for BANYAN_VINES.
	 */
	public static final int BANYAN_VINES = 14;
	/**
	 * The Address type for E_164_NSAP.
	 */
	public static final int E_164_NSAP = 15;
	/**
	 * The Address type for DNS.
	 */
	public static final int DNS = 16;
	/**
	 * The Address type for DN.
	 */
	public static final int DN = 17;
	/**
	 * The Address type for AS.
	 */
	public static final int AS = 18;
	/**
	 * The Address type for XTP_IPV4.
	 */
	public static final int XTP_IPV4 = 19;
	/**
	 * The Address type for XTP_IPV6.
	 */
	public static final int XTP_IPV6 = 20;
	/**
	 * The Address type for XTP_NATIVE.
	 */
	public static final int XTP_NATIVE = 21;
	/**
	 * The Address type for FCWWPN.
	 */
	public static final int FCWWPN = 22;
	/**
	 * The Address type for FCWWNN.
	 */
	public static final int FCWWNN = 23;
	/**
	 * The Address type for GWID.
	 */
	public static final int GWID = 24;

	/**
	 * Returns a String representation of an Address type.
	 * 
	 * @param type The address type.
	 * @return A String representation.
	 */
	public static final String getAddressTypeAsString(int type) {
		switch (type) {
			case IPV4:
				return "IPV4";
			case IPV6:
				return "IPV6";
			case NSAP:
				return "NSAP";
			case HDLC:
				return "HDLC";
			case BBN_1822:
				return "BBN 1822";
			case 802:
				return "802";
			case E_163:
				return "E.163";
			case E_164:
				return "E.164";
			case F_69:
				return "F.69";
			case X_121:
				return "X.121";
			case IPX:
				return "IPX";
			case APPLETALK:
				return "Appletalk";
			case DECNET_IV:
				return "Decnet IV";
			case BANYAN_VINES:
				return "Banyan Vines";
			case E_164_NSAP:
				return "E.164 NSAP";
			case DNS:
				return "DNS";
			case DN:
				return "Distinguished Name";
			case AS:
				return "AS Number";
			case XTP_IPV4:
				return "XTP over IPV4";
			case XTP_IPV6:
				return "XTP over IPV6";
			case XTP_NATIVE:
				return "XTP Native Mode";
			case FCWWPN:
				return "Fibre Channel World-Wide Port Name";
			case FCWWNN:
				return "Fibre Channel World-Wide Node Name";
			case GWID:
				return "GWID";
			default:
				return "Unknown";
		}
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof DiameterAddress) {
			return toAddress((DiameterAddress) value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + "."
					+ "An instance of " + DiameterAddress.class.getName() + " is expected");
		}
	}

}
