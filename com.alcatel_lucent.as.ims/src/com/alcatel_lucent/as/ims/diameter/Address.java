// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter;

import com.nextenso.proxylet.diameter.util.AddressFormat;

/**
 * The Diameter Address..
 */
public class Address {

	private int _type = AddressFormat.IPV4;
	private byte[] _address = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param type
	 * @param address
	 */
	public Address(int type, byte[] address) {
		_type = type;
		_address = copyArray(address);
	}

	/**
	 * Constructor for this class from an AVP value.
	 *
	 * @param value The AVP value to be parsed.
	 */
	public Address(byte[] value) {
		this(AddressFormat.getAddressType(value, 0), AddressFormat.getAddressValue(value));
	}

	/**
	 *Gets the value to put into an AVP.
	 * 
	 * @return The value.
	 */
	public byte[] getAvpValue() {
		if (_address == null) {
			return null;
		}
		byte[] res = AddressFormat.toAddress(_type, _address);
		return res;
	}

	/**
	 * Gets the type.
	 * 
	 * @return The type.
	 * @see com.nextenso.proxylet.diameter.util.AddressFormat for the return value
	 *      meaning.
	 */
	public int getType() {
		return _type;
	}

	/**
	 * Gets the address value.
	 * 
	 * @return The address.
	 */
	public byte[] getAddress() {
		return copyArray(_address);
	}

	private byte[] copyArray(byte[] src) {
		if (src == null) {
			return null;
		}

		int len = src.length;
		byte[] res = new byte[len];
		System.arraycopy(src, 0, res, 0, len);
		return res;
	}

}
