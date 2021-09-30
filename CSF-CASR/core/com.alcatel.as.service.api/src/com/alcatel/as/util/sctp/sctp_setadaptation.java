package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * struct sctp_setadaptation {
 *	__u32	ssb_adaptation_ind;
 * };
 */
public class sctp_setadaptation implements SctpSocketParam {
	
	public long ssb_adaptation_ind;

	public sctp_setadaptation() { }
	
	public sctp_setadaptation(long ssb_adaptation_ind) {
		this.ssb_adaptation_ind = ssb_adaptation_ind;
	}

	@Override
	public String toString() {
		return "sctp_adaptation_layer [ssb_adaptation_ind=" + ssb_adaptation_ind + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(ssb_adaptation_ind);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		ssb_adaptation_ind = in.readLong();
	}

	public SctpSocketParam merge(SctpSocketParam other) {
		return other;
	}	
}
