package com.alcatel_lucent.as.service.dns;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Address Record - maps a domain name to an Internet address.
 */

public abstract class RecordAddress
		extends Record {

	private String _address = EMPTY_STRING;
	
	protected RecordAddress(RecordType type) {
		super(type);
	}

	/**
	 * Creates an A Record from the given data
	 * 
	 * @param name The record's name.
	 * @param dclass The record's class.
	 * @param ttl The record's TTL.
	 * @param address The record's address.
	 */
	protected RecordAddress(RecordType type, String name, RecordDClass dclass, long ttl, String address) {
		super(type, name, dclass, ttl);
		setAddress(address);
	}

	public RecordAddress(RecordType a, String name, String address) {
		super(a, name);
		setAddress(address);
	}

	/**
	 * Gets the record's address.
	 * 
	 * @return The record's address.
	 */
	public String getAddress() {
		if (_address == EMPTY_STRING) {
			 return null;
		}
		return _address;
	}

	
	
	/**
	 * Sets the address.
	 *
	 * @param address The Address.
	 */
	private final void setAddress(String address) {
		_address = address;
		if (_address == null || EMPTY_STRING.equals(_address)) {
			_address = EMPTY_STRING;
		}
	}

	/**
	 * Compares this Record to the specified object.
	 * 
	 * @see com.alcatel_lucent.as.service.dns.Record#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object record) {
		if (!(record instanceof RecordAddress)) {
			return false;
		}

		boolean res = super.equals(record);
		if (!res) {
			return false;
		}

		RecordAddress r = (RecordAddress) record;
		if (!isStringEquals(getAddress(), r.getAddress())) {
			return false;
		}

		return true;
	}

	/**
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	@Override
	public void readExternal(ObjectInput in)
		throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setAddress( in.readUTF());
	}

	/**
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	@Override
	public void writeExternal(ObjectOutput out)
		throws IOException {
		super.writeExternal(out);
		out.writeUTF(_address);
	}

	/**
	 * Returns a String object representing this Record's value.
	 * 
	 * @see com.alcatel_lucent.as.service.dns.Record#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder(super.toString());
		buffer.append(", address=").append(getAddress());
		return buffer.toString();
	}
}
