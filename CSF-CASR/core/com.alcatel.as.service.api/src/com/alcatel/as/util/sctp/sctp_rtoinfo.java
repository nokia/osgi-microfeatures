package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * struct sctp_rtoinfo {
 *	sctp_assoc_t	srto_assoc_id;
 *	__u32			srto_initial;
 *	__u32			srto_max;
 *	__u32			srto_min;
 * };
 */

public class sctp_rtoinfo implements SctpSocketParam {

	public int  sctp_assoc_id;
	public long srto_initial;
	public long srto_max;
	public long srto_min;

	public sctp_rtoinfo() { }

	public sctp_rtoinfo(long srto_initial, long srto_max, long srto_min) {
		this(0, srto_initial, srto_max, srto_min);
	}

	public sctp_rtoinfo(int sctp_assoc_id, long srto_initial, long srto_max, long srto_min) {
		this.sctp_assoc_id = sctp_assoc_id;
		this.srto_initial = srto_initial;
		this.srto_max = srto_max;
		this.srto_min = srto_min;
	}

	@Override
	public String toString() {
		return "sctp_rtoinfo [sctp_assoc_id=" + sctp_assoc_id + ", srto_initial=" + srto_initial + ", srto_max="
				+ srto_max + ", srto_min=" + srto_min + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(sctp_assoc_id);
		out.writeLong(srto_initial);
		out.writeLong(srto_max);
		out.writeLong(srto_min);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sctp_assoc_id = in.readInt();
		srto_initial = in.readLong();
		srto_max = in.readLong();
		srto_min = in.readLong();
	}

	public SctpSocketParam merge(SctpSocketParam other2) {
		if(!(other2 instanceof sctp_rtoinfo)) throw new IllegalArgumentException("Not an sctp_rtoinfo");
		sctp_rtoinfo other = (sctp_rtoinfo) other2;
		long initial, max, min;
		
		if(this.srto_initial == 0) initial = other.srto_initial;
		else if(other.srto_initial == 0) initial = this.srto_initial;
		else initial = other.srto_initial;
		
		if(this.srto_max == 0) max = other.srto_max;
                else if(other.srto_max == 0) max = this.srto_max;
                else max = other.srto_max;

		if(this.srto_min == 0) min = other.srto_min;
                else if(other.srto_min == 0) min = this.srto_min;
                else min = other.srto_min;

		return new sctp_rtoinfo(initial, max, min);
	}
}
