// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.nextenso.proxylet.diameter.DiameterAVPDefinition.Dictionary;
import com.nextenso.proxylet.diameter.util.OctetStringFormat;

/**
 * This Class encapsulates a Diameter AVP (See RFC#3588 4.1).
 * <p/>
 * It is made of:
 * <ul>
 * <li>a code
 * <li>flags ('V', 'M' and 'P')
 * <li>a vendorId (optional)
 * <li>one or more values.<b>Note that an AVP must have at least 1 value (which can be blank) when it is fully built.</b>
 * </ul>
 * The values are ordered. <br>
 */
public class DiameterAVP
		implements Cloneable, Externalizable {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The 'V' (Vendor-Id Specific) flag.
	 */
	public static final int V_FLAG = 0x80;
	/**
	 * The 'M' (Mandatory) flag.
	 */
	public static final int M_FLAG = 0x40;
	/**
	 * The 'P' (Protected) flag.
	 */
	public static final int P_FLAG = 0x20;

	/**
	 * If no flag is set.
	 */
	public static final int NO_FLAG = 0;

	/**
	 * The 'V' (Vendor-Id Specific) and 'M' (Mandatory) flag.
	 */
	public static final int VM_FLAGS = V_FLAG | M_FLAG;
	/**
	 * The 'V' (Vendor-Id Specific) and 'P' (Protected) flag.
	 */
	public static final int VP_FLAGS = V_FLAG | P_FLAG;
	/**
	 * The 'M' (Mandatory) and 'P' (Protected) flag.
	 */
	public static final int MP_FLAGS = M_FLAG | P_FLAG;
	/**
	 * The 'V' (Vendor-Id Specific), 'M' (Mandatory) and 'P' (Protected) flag.
	 */
	public static final int VMP_FLAGS = V_FLAG | M_FLAG | P_FLAG;

	private static final int DEFAULT_FLAGS_VALUE = -1;

	private long _code, _vendorId;
	private int _defaultFlags;
	private final List<Value> _values = new ArrayList<Value>(1);
	private DiameterAVPDefinition _definition;

	private static class Value
			implements Serializable {

		private static final long serialVersionUID = 1L;
		private byte[] _value;
		private int _flags;

		public Value(byte[] value, int flags) {
			_value = value;
			setFlags(flags);
		}

		public byte[] getValue() {
			return _value;
		}

		public int getFlags() {
			return _flags;
		}

		public void setFlags(int flags) {
			_flags = flags;
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + _flags;
			result = prime * result + Arrays.hashCode(_value);
			return result;
		}

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
			Value other = (Value) obj;
			if (_flags != other._flags) {
				return false;
			}
			if (!Arrays.equals(_value, other._value)) {
				return false;
			}
			return true;
		}

	}

	public DiameterAVP() {}

	/**
	 * Constructs a new DiameterAVP given its code, its vendorId and its flags. <br/>
	 * By default, unless the definition is found in the DiameterAVPDefinition
	 * Dictionary, the format is OctetStringFormat.
	 * 
	 * @param code The code.
	 * @param vendorId The Vendor-Id.
	 * @param flags The flags.
	 */
	public DiameterAVP(long code, long vendorId, int flags) {
		_code = code;
		_vendorId = vendorId;
		_definition = Dictionary.getDiameterAVPDefinition(code, vendorId);
		_defaultFlags = flags;
	}

	/**
	 * Constructs a new DiameterAVP given its definition.<br>
	 * The code, flags and vendorId are taken from the definition.<br>
	 * Only the required flags are taken from the definition, it is possible to
	 * modify the flags later on.
	 * 
	 * @param definition The definition.
	 */
	public DiameterAVP(DiameterAVPDefinition definition) {
		if (definition == null) {
			throw new NullPointerException("DiameterAVPDefinition is null");
		}
		_definition = definition;
		_code = definition.getAVPCode();
		_vendorId = definition.getVendorId();
		_defaultFlags = definition.getRequiredFlags();
	}

	/**
	 * Constructs a new DiameterAVP given its definition and its flags. <br>
	 * The code and vendorId are taken from the definition.
	 * 
	 * @param definition The definition.
	 * @param flags The flags.
	 */
	public DiameterAVP(DiameterAVPDefinition definition, int flags) {
		this(definition);
		_defaultFlags = flags;
	}

	/**
	 * Gets the AVP code of this AVP.
	 * 
	 * @return The AVP code.
	 */
	public long getAVPCode() {
		return _code;
	}

	/**
	 * Gets the Vendor-Id field of this AVP.
	 * 
	 * @return The Vendor-Id.
	 */
	public long getVendorId() {
		return _vendorId;
	}

	/**
	 * Gets the default flags of this AVP.
	 * 
	 * @return The flags.
	 */
	public int getAVPFlags() {
		return _defaultFlags;
	}

	/**
	 * Gets the flags for the value at the specified index
	 * 
	 * @param index The index of the value.
	 * @return The flags associated to the value.
	 */
	public int getAVPFlags(int index) {
		int res = 0;
		try {
			Value value = _values.get(index);
			res = value.getFlags();
			if (res == DEFAULT_FLAGS_VALUE) {
				res = getAVPFlags();
			}
		}
		catch (IndexOutOfBoundsException e) {}
		return res;

	}

	/**
	 * Sets the flags for a specified value.
	 * 
	 * @param index The index of the value.
	 * @param flags The value.
	 * @exception IndexOutOfBoundsException if the index is out of the range.
	 * @exception IllegalArgumentException if this value does not exist.
	 */
	public void setFlags(int index, int flags) {
		if (index < 0 || index >= _values.size()) {
			throw new IndexOutOfBoundsException("invalid index");
		}
		Value value = _values.get(index);
		if (value == null) {
			throw new IllegalArgumentException("no data for the specified index");
		}
		value.setFlags(flags);
	}

	/**
	 * Sets the 'V' flag value for a specified value.
	 * 
	 * @param index The index of the value.
	 * @param set The value of the 'V' flag.
	 * @exception IndexOutOfBoundsException if the index is out of the range.
	 * @exception IllegalArgumentException if this value does not exist.
	 */
	public void setvFlag(int index, boolean set) {
		setFlag(index, V_FLAG, set);
	}

	/**
	 * Sets the 'M' flag value for a specified value.
	 * 
	 * @param index The index of the value.
	 * @param set The value of the 'M' flag.
	 * @exception IndexOutOfBoundsException if the index is out of the range.
	 * @exception IllegalArgumentException if this value does not exist.
	 */
	public void setmFlag(int index, boolean set) {
		setFlag(index, M_FLAG, set);
	}

	/**
	 * Sets the 'P' flag value for a specified value.
	 * 
	 * @param index The index of the value.
	 * @param set The value of the 'P' flag.
	 * @exception IndexOutOfBoundsException if the index is out of the range.
	 * @exception IllegalArgumentException if this value does not exist.
	 */
	public void setpFlag(int index, boolean set) {
		setFlag(index, P_FLAG, set);
	}

	private void setFlag(int index, int flag, boolean set) {
		Value value = _values.get(index);
		if (value == null) {
			throw new IllegalArgumentException("no data for the specified index");
		}
		int flags = getAVPFlags(index);
		if (set) {
			flags |= flag;
		} else {
			flags &= (0xFF ^ flag);
		}
		value.setFlags(flags);
	}

	/**
	 * Sets the default flags of this AVP.
	 * 
	 * @param flags The flags.
	 */
	public void setFlags(int flags) {
		_defaultFlags = flags;
	}

	/**
	 * Indicates whether the 'V' (Vendor Specific) flag is set.
	 * 
	 * @return true if the flag is set.
	 */
	public boolean vFlagSet() {
		return vFlagSet(_defaultFlags);
	}

	/**
	 * Indicates whether the 'V' (Vendor Specific) flag is set.
	 * 
	 * @param flags The flags to test.
	 * 
	 * @return true if the flag is set.
	 */
	public static boolean vFlagSet(int flags) {
		return ((flags & V_FLAG) == V_FLAG);
	}

	/**
	 * Indicates whether the 'M' (Mandatory) flag is set.
	 * 
	 * @return true if the flag is set.
	 */
	public boolean mFlagSet() {
		return mFlagSet(_defaultFlags);
	}

	/**
	 * Indicates whether the 'M' (Mandatory) flag is set.
	 * 
	 * @param flags The flags to test.
	 * 
	 * @return true if the flag is set.
	 */
	public static boolean mFlagSet(int flags) {
		return ((flags & M_FLAG) == M_FLAG);
	}

	/**
	 * Indicates whether the 'P' (Protected) flag is set.
	 * 
	 * @return true if the flag is set.
	 */
	public boolean pFlagSet() {
		return pFlagSet(_defaultFlags);
	}

	/**
	 * Indicates whether the 'P' (Protected) flag is set.
	 * 
	 * @param flags The flags to test.
	 * 
	 * @return true if the flag is set.
	 */
	public static boolean pFlagSet(int flags) {
		return ((flags & P_FLAG) == P_FLAG);
	}

	/**
	 * Gets the definition of the AVP.
	 * 
	 * @return The definition.
	 */
	public DiameterAVPDefinition getDiameterAVPDefinition() {
		return _definition;
	}

	/**
	 * Specifies if the AVP is of the specified type.
	 * 
	 * @param definition The type.
	 * @return true if the AVP is an instance of this definition.
	 */
	public boolean isInstanceOf(DiameterAVPDefinition definition) {
		return (_code == definition.getAVPCode() && _vendorId == definition.getVendorId());
	}
    
	/**
	 * Gets the number of values for that AVP.
	 * 
	 * @return The number of values.
	 */
	public int getValueSize() {
		return _values.size();
	}

	/**
	 * Indicates if the AVP is empty.
	 * Emptiness consists of either:
	 * <ul>
	 * <li>no value (value size = 0) : this case must not happen on a fully built AVP, but may happen while building it.
	 * <li>all values have empty content : usually an empty AVP has a single value, which is empty.
	 * </ul>
	 * 
	 * @return true if empty.
	 */
	public boolean isEmpty() {
	    for (int i=0; i<getValueSize (); i++)
		if (getValue (i).length > 0) return false;
	    return true;
	}

	/**
	 * Gets the first value of the AVP.
	 * 
	 * @return The first value, or <code>null</code> if the AVP has no value.
	 */
	public byte[] getValue() {
		return getValue(0);
	}

	/**
	 * Returns the AVP value at the specified index, starting at 0.
	 * 
	 * @param index The index (cannot be lower than 0 or greater than
	 *          <code>getValueSize()</code>).
	 * @return The value at the specified index, or <code>null</code> if there is
	 *         no value at the specified index.
	 */
	public byte[] getValue(int index) {
		try {
			Value value = _values.get(index);
			if (value == null) {
				return null;
			}
			return value.getValue();
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Sets the value of the AVP.
	 * 
	 * <br/>
	 * The AVP will be single-valued. Its value WILL BE a clone of the specified
	 * value if the copy flag is set to true and MAY BE a clone otherwise.
	 * 
	 * @param value The attribute value.
	 * @param copy true if the value must be cloned.
	 */
	public void setValue(byte[] value, boolean copy) {
		setValue(value, 0, value.length, copy);
	}

	/**
	 * Sets the value of the AVP.
	 * 
	 * <br/>
	 * The AVP will be single-valued. Its value WILL BE a clone of the specified
	 * value if the copy flag is set to true and MAY BE a clone otherwise.
	 * 
	 * @param value The attribute value.
	 * @param flags The flags associated to the value.
	 * @param copy true if the value must be cloned.
	 */
	public void setValue(byte[] value, int flags, boolean copy) {
		setValue(value, 0, value.length, flags, copy);
	}

	/**
	 * Sets the value of the AVP.
	 * 
	 * <br/>
	 * The AVP will be single-valued. Its value WILL BE a clone of the specified
	 * value if the copy flag is set to true and MAY BE a clone otherwise.
	 * 
	 * @param value The attribute value.
	 * @param off The attribute offset in the value.
	 * @param len The attribute value length.
	 * @param copy true if the value must be cloned.
	 */
	public void setValue(byte[] value, int off, int len, boolean copy) {
		setValue(value, off, len, DEFAULT_FLAGS_VALUE, copy);
	}

	/**
	 * Sets the value of the AVP.
	 * 
	 * <br/>
	 * The AVP will be single-valued. Its value WILL BE a clone of the specified
	 * value if the copy flag is set to true and MAY BE a clone otherwise.
	 * 
	 * @param value The attribute value.
	 * @param off The attribute offset in the value.
	 * @param len The attribute value length.
	 * @param flags The flags associated to the value.
	 * @param copy true if the value must be cloned.
	 */
	public void setValue(byte[] value, int off, int len, int flags, boolean copy) {
		removeValues();
		addValue(value, off, len, flags, copy);
	}

	/**
	 * Appends a value at the end of the AVP values list.
	 * 
	 * <br/>
	 * The added value WILL BE a clone of the specified value if the copy flag is
	 * set to true and MAY BE a clone otherwise.
	 * 
	 * @param value the attribute value.
	 * @param copy true if the value must be cloned.
	 */
	public void addValue(byte[] value, boolean copy) {
		addValue(value, DEFAULT_FLAGS_VALUE, copy);
	}

	/**
	 * Appends a value at the end of the AVP values list.
	 * 
	 * <br/>
	 * The added value WILL BE a clone of the specified value if the copy flag is
	 * set to true and MAY BE a clone otherwise.
	 * 
	 * @param value the attribute value.
	 * @param flags The flags associated to the value.
	 * @param copy true if the value must be cloned.
	 */
	public void addValue(byte[] value, int flags, boolean copy) {
		addValue(value, 0, value.length, flags, copy);
	}

	/**
	 * Appends a value at the end of the AVP values list.
	 * 
	 * <br/>
	 * The added value WILL BE a clone of the specified value if the copy flag is
	 * set to true and MAY BE a clone otherwise.
	 * 
	 * @param value The attribute value.
	 * @param off The attribute value offset in the value.
	 * @param len The attribute value length.
	 * @param copy true if the value must be cloned.
	 */
	public void addValue(byte[] value, int off, int len, boolean copy) {
		addValue(value, off, len, DEFAULT_FLAGS_VALUE, copy);
	}

	/**
	 * Appends a value at the end of the AVP values list.
	 * 
	 * <br/>
	 * The added value WILL BE a clone of the specified value if the copy flag is
	 * set to true and MAY BE a clone otherwise.
	 * 
	 * @param value The attribute value.
	 * @param off The attribute value offset in the value.
	 * @param len The attribute value length.
	 * @param flags The flags associated to the value.
	 * @param copy true if the value must be cloned.
	 */
	public void addValue(byte[] value, int off, int len, int flags, boolean copy) {
		addValue(_values.size(), value, off, len, flags, copy);
	}

	/**
	 * Inserts a value at the specified index in the AVP values list.
	 * 
	 * <br/>
	 * The added value WILL BE a clone of the specified value if the copy flag is
	 * set to true and MAY BE a clone otherwise.
	 * 
	 * @param index The index in the list.
	 * @param value The attribute value.
	 * @param offset The attribute value offset.
	 * @param length The attribute value length.
	 * @param copy true if the value must be cloned.
	 */
	public void addValue(int index, byte[] value, int offset, int length, boolean copy) {
		addValue(index, value, offset, length, DEFAULT_FLAGS_VALUE, copy);
	}

	/**
	 * Inserts a value at the specified index in the AVP values list.
	 * 
	 * <br/>
	 * The added value WILL BE a clone of the specified value if the copy flag is
	 * set to true and MAY BE a clone otherwise.
	 * 
	 * @param index The index in the list.
	 * @param value The attribute value.
	 * @param offset The attribute value offset.
	 * @param length The attribute value length.
	 * @param flags The flags associated to the value.
	 * @param copy true if the value must be cloned.
	 */
	public void addValue(int index, byte[] value, int offset, int length, int flags, boolean copy) {
		if (value == null) {
			throw new NullPointerException();
		}
		boolean iscopy = (copy || (offset != 0) || (length != value.length));
		byte[] clone;
		if (iscopy) {
			clone = new byte[length];
			System.arraycopy(value, offset, clone, 0, length);
		} else {
			clone = value;
		}
		Value newValue = new Value(clone, flags);
		_values.add(index, newValue);
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
	 * @param index the Index of the value to be removed.
	 * @return The removed value or <code>null</code> if the value is not defined.
	 */
	public byte[] removeValue(int index) {
		try {
			Value value = _values.remove(index);
			byte[] res = null;
			if (value != null) {
				res = value.getValue();
			}
			return res;
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		DiameterAVP clone = new DiameterAVP(getDiameterAVPDefinition());
		clone.setFlags(getAVPFlags());
		copy(clone);
		return clone;
	}

	/**
	 * Copies (clones) the values into another AVP. <br/>
	 * The destinationAVP values are cleared before the copy takes place.
	 * 
	 * @param destinationAVP The avp where this avp must be copied.
	 */
	public void copy(DiameterAVP destinationAVP) {
		destinationAVP.removeValues();
		for (Value value : _values) {
			destinationAVP.addValue(value.getValue(), value.getFlags(), true);
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return toString(0);
	}

	/**
	 * Returns a String representation of the AVP and its values.
	 * 
	 * @param level The depth of the AVP in the message.
	 * @return the String representation.
	 */
	public String toString(int level) {
		StringBuilder buffer = new StringBuilder();
		String tab = "  ";
		String tabs = "\t";
		for (int i = 0; i < level; i++) {
			tabs += tab;
		}
		buffer.append("DiameterAVP");
		buffer.append(" [code=").append(String.valueOf(_code));
		if (_definition != null) {
			buffer.append(", name=").append(_definition.getAVPName());
		} else {
			buffer.append(", name=<???>");
		}
		buffer.append(", vendorId=").append(_vendorId);
		String sep = ",";
		if (_values.size() > 1) {
			sep = "\n" + tabs + tab;
		}
		for (int i = 0; i < _values.size(); i++) {
			int flags = getAVPFlags(i);
			buffer.append(sep).append("flags#").append(i).append("=").append((vFlagSet(flags)) ? 'V' : '-').append((mFlagSet(flags)) ? 'M' : '-').append((pFlagSet(flags)) ? 'P' : '-');
			buffer.append(",value#").append(i).append(':');
			try {
				buffer.append(_definition.getDiameterAVPFormat().toString(getValue(i), level + 1));
			}
			catch (Exception e) {
				buffer.append(OctetStringFormat.INSTANCE.toString(getValue(i), level + 1));
			}
		}
		if (getValueSize() == 0) {
			buffer.append(", no value");
		}

		buffer.append(']');
		return buffer.toString();
	}

	/**
	 * Gets the byte array containing the AVP.
	 * 
	 * @return The byte array.
	 */
	public byte[] getBytes() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			getBytes(out);
			return out.toByteArray();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void getBytes(OutputStream out)
		throws IOException {
		int code = (int) getAVPCode();
		int vendorId = (int) getVendorId();
		if (getValueSize () > 0){
		    for (int i = 0; i < getValueSize(); i++) {
			int flags = getAVPFlags(i);
			boolean hasVendorId = vFlagSet (flags);
			byte[] value = getValue(i);
		    
			int pad = value.length % 4;
			if (pad != 0) {
			    pad = 4 - pad;
			}
			int len = 8 + ((hasVendorId) ? 4 : 0) + value.length;

			out.write((byte) ((code >> 24) & 0xFF));
			out.write((byte) ((code >> 16) & 0xFF));
			out.write((byte) ((code >> 8) & 0xFF));
			out.write((byte) (code & 0xFF));

			out.write((byte) (flags & 0xFF));

			out.write((byte) ((len >> 16) & 0xFF));
			out.write((byte) ((len >> 8) & 0xFF));
			out.write((byte) (len & 0xFF));
			if (hasVendorId) {
			    out.write((byte) ((vendorId >> 24) & 0xFF));
			    out.write((byte) ((vendorId >> 16) & 0xFF));
			    out.write((byte) ((vendorId >> 8) & 0xFF));
			    out.write((byte) (vendorId & 0xFF));
			}
			out.write(value, 0, value.length);
			for (int p = 0; p < pad; p++) {
			    out.write(0);
			}
		    }
		} else {
		    // no value
		    int flags = getAVPFlags();
		    boolean hasVendorId = vFlagSet (flags);
		    out.write((byte) ((code >> 24) & 0xFF));
		    out.write((byte) ((code >> 16) & 0xFF));
		    out.write((byte) ((code >> 8) & 0xFF));
		    out.write((byte) (code & 0xFF));
		    out.write((byte) (flags & 0xFF));
		    int len = 8 + ((hasVendorId) ? 4 : 0);
		    out.write((byte) ((len >> 16) & 0xFF));
		    out.write((byte) ((len >> 8) & 0xFF));
		    out.write((byte) (len & 0xFF));
		    if (hasVendorId) {
			out.write((byte) ((vendorId >> 24) & 0xFF));
			out.write((byte) ((vendorId >> 16) & 0xFF));
			out.write((byte) ((vendorId >> 8) & 0xFF));
			out.write((byte) (vendorId & 0xFF));
		    }
		}
	}

	@Override
	public void writeExternal(ObjectOutput out)
		throws IOException {
		out.writeLong(_code);
		out.writeLong(_vendorId);
		out.writeInt(_defaultFlags);
		out.writeObject(_definition);
		out.writeObject(_values);
	}

	@Override
	public void readExternal(ObjectInput in)
		throws IOException, ClassNotFoundException {
		_code = in.readLong();
		_vendorId = in.readLong();
		_defaultFlags = in.readInt();
		_definition = (DiameterAVPDefinition) in.readObject();
		List<Value> values = (List<Value>) in.readObject();
		_values.addAll(values);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_code ^ (_code >>> 32));
		result = prime * result + _defaultFlags;
		result = prime * result + ((_definition == null) ? 0 : _definition.hashCode());
		result = prime * result + ((_values == null) ? 0 : _values.hashCode());
		result = prime * result + (int) (_vendorId ^ (_vendorId >>> 32));
		return result;
	}

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
		DiameterAVP other = (DiameterAVP) obj;
		if (_code != other._code) {
			return false;
		}
		if (_defaultFlags != other._defaultFlags) {
			return false;
		}
		if (_definition == null) {
			if (other._definition != null) {
				return false;
			}
		} else if (!_definition.equals(other._definition)) {
			return false;
		}
		if (_values == null) {
			if (other._values != null) {
				return false;
			}
		} else if (!_values.equals(other._values)) {
			return false;
		}
		if (_vendorId != other._vendorId) {
			return false;
		}
		return true;
	}
}
