package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class sctp_boolean implements SctpSocketParam {

	public static final sctp_boolean TRUE = new sctp_boolean (true);
	public static final sctp_boolean FALSE = new sctp_boolean (false);

	public static sctp_boolean getSctpBoolean (boolean value) { return value ? TRUE : FALSE;}
	
	public boolean value;
	
	public sctp_boolean() { }

	public sctp_boolean(boolean value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "sctp_boolean [" + value + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeBoolean(value);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		value = in.readBoolean();
	}

	public SctpSocketParam merge(SctpSocketParam other2) {
		if(!(other2 instanceof sctp_boolean)) throw new IllegalArgumentException("Not an sctp_boolean");
		return this.value ? this : other2; // same as this.value || other.value
	}
}
