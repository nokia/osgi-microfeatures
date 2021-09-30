package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * struct sctp_assoc_value {
 *   sctp_assoc_t            assoc_id;
 *   uint32_t                assoc_value;
 * };
 */

public class sctp_assoc_value implements SctpSocketParam {
	
	public int  assoc_id;
	public long assoc_value;
	
	public sctp_assoc_value() { }

	public sctp_assoc_value(long assoc_value) {
		this(0, assoc_value);
	}

	public sctp_assoc_value(int assoc_id, long assoc_value) {
		this.assoc_id = assoc_id;
		this.assoc_value = assoc_value;
	}

	@Override
	public String toString() {
		return "sctp_assoc_value [assoc_id=" + assoc_id + ", assoc_value=" + assoc_value + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(assoc_id);
		out.writeLong(assoc_value);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		assoc_id = in.readInt();
		assoc_value = in.readLong();
	}

	public SctpSocketParam merge(SctpSocketParam other2) {
		if(!(other2 instanceof sctp_assoc_value)) throw new IllegalArgumentException("Not an sctp_assoc_value");
                sctp_assoc_value other = (sctp_assoc_value) other2;
	
		long assoc_value;
		if(this.assoc_value == 0) assoc_value = other.assoc_value;
		else if(other.assoc_value == 0) assoc_value = this.assoc_value;
		else assoc_value = other.assoc_value;

		return new sctp_assoc_value(assoc_value);
	}
}
