// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * This is a a generic DNS resource record. The specific record types extend
 * this class. A record contains a name, type, class, and TTL.
 */

public abstract class Record  implements Externalizable{

	protected static final String EMPTY_STRING = "";

	private String _name;
	private RecordDClass _dclass =RecordDClass.UNKNOWN;
	private long _ttl = -1L;
	private RecordType _type;

	/**
	 * Creates a Record from the given data.
	 * 
	 * @param type The type of the request
	 * @param name The record's name.
	 * @param dclass The record's class.
	 * @param ttl The record's TTL.
	 */
	protected Record(RecordType type, String name, RecordDClass dclass, long ttl) {
		this(type, name);
		if ( dclass == null) {
			throw new IllegalArgumentException("The Record DNS class cannot be null");
		}
		_dclass = dclass;
		_ttl = ttl;
	}

	public Record(RecordType type, String name) {
		this(type);
		if ( name == null) {
			throw new IllegalArgumentException("The Record name cannot be null");
		}
		_name = name;
	}


	protected Record(RecordType type) {
		if ( type == null) {
			throw new IllegalArgumentException("The Record type cannot be null");
		}
		_type = type;
	}

	/**
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in)
		throws IOException, ClassNotFoundException {
		_name=in.readUTF();
		int id=in.readInt();
		_dclass=RecordDClass.getDClass(id);
		_ttl=in.readLong();
	}

	/**
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out)
		throws IOException {
		out.writeUTF(_name);
		out.writeInt(_dclass.getId());
		out.writeLong(_ttl);
	}

	/**
	 * Returns a String object representing this Record's value.
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Record type=").append(getType());
		buffer.append(", name=").append(getName());
		buffer.append(", dclass=").append(getDClass());
		buffer.append(", ttl=").append(getTTL());
		return buffer.toString();
	}

	/**
	 * Returns a hash code for this record.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Indicates whether both strings s1 an s2 are equal.
	 * @param s1 The first string to compare.
	 * @param s2 The second string to compare.
	 * @return true if both strings are equal.
	 */
	protected boolean isStringEquals(String s1, String s2) {
		if (s1 == null) {
			if (s2 != null) {
				return false;
			}
		} else {
			if (!s1.equals(s2)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Compares this Record to the specified object.
	 * 
	 * @see java.lang.Object#equals(Object)
	 */
	@Override
	public boolean equals(Object record) {
		if (!(record instanceof Record)) {
			return false;
		}
		Record r = (Record) record;

		if (getType() != r.getType()) {
			return false;
		}

		if (!isStringEquals(getName(), r.getName())) {
			return false;
		}

		if (getDClass() != r.getDClass()) {
			return false;
		}

		if (getTTL() != r.getTTL()) {
			return false;
		}

		return true;
	}

	/**
	 * Gets the record's name.
	 * 
	 * @return The record's name.
	 */
	public final String getName() {
		return _name;
	}

	/**
	 * Gets the record's type.
	 * 
	 * @return The record's type.
	 */
	public final RecordType getType() {
		return _type;
	}

	/**
	 * Gets the record's class.
	 * 
	 * @return The record's class or a negative value if not found.
	 */
	public final RecordDClass getDClass() {
		return _dclass;
	}

	/**
	 * Gets the record's TTL.
	 * 
	 * @return The record's TTL  or a negative value if not found.
	 */
	public final long getTTL() {
		return _ttl;
	}
}
