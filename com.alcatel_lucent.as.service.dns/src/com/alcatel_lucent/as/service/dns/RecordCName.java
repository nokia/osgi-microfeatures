// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Alias Record - maps a domain name to an alias.
 */
public class RecordCName
		extends Record {

	private String _alias = EMPTY_STRING;

	public RecordCName() {
		super(RecordType.CNAME);
	}

	/**
	 * Creates an CName Record from the given data
	 * 
	 * @param name The record's name.
	 * @param dclass The record's class.
	 * @param ttl The record's TTL.
	 * @param alias The record's alias.
	 */
	public RecordCName(String name, RecordDClass dclass, long ttl, String alias) {
		super(RecordType.CNAME, name, dclass, ttl);
		setAlias(alias);
	}

	/**
	 * Creates an CName Record from the given data
	 * 
	 * @param name The record's name.
	 * @param alias The record's alias.
	 */
	public RecordCName(String name, String alias) {
		super(RecordType.CNAME, name);
		setAlias(alias);
	}

	/**
	 * Sets the alias.
	 * @param alias The alias.
	 */
	private void setAlias(String alias) {
		_alias = alias;
		if (_alias == null || EMPTY_STRING.equals(_alias)) {
			_alias = EMPTY_STRING;
		}
	}

	/**
	 * Gets the record's alias.
	 * 
	 * @return The record's alias.
	 */
	public String getAlias() {
		if (_alias == EMPTY_STRING) {
			return null;
		}
		return _alias;
	}

	/**
	 * Compares this Record to the specified object.
	 * 
	 * @see com.alcatel_lucent.as.service.dns.Record#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object record) {
		if (!(record instanceof RecordCName)) {
			return false;
		}

		boolean res = super.equals(record);
		if (!res) {
			return false;
		}

		RecordCName r = (RecordCName) record;
		if (!isStringEquals(getAlias(), r.getAlias())) {
			return false;
		}

		return true;
	}
	
	/**
	 * Returns a hash code for this record.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}


	/**
	 * @see com.alcatel_lucent.as.service.dns.Record#readExternal(java.io.ObjectInput)
	 */
	@Override
	public void readExternal(ObjectInput in)
		throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setAlias(in.readUTF());
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.Record#writeExternal(java.io.ObjectOutput)
	 */
	@Override
	public void writeExternal(ObjectOutput out)
		throws IOException {
		super.writeExternal(out);
		out.writeUTF(_alias);
	}

	/**
	 * Returns a String object representing this Record's value.
	 * 
	 * @see com.alcatel_lucent.as.service.dns.Record#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder(super.toString());
		buffer.append(", alias=").append(getAlias());
		return buffer.toString();
	}

}
