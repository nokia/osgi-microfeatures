package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * struct sctp_authkeyid {
 *	sctp_assoc_t	scact_assoc_id;
 *	__u16		scact_keynumber;
 * };
 */
public class sctp_authkeyid implements SctpSocketParam {
	public int scact_assoc_id;
	public int scact_keynumber;

	public sctp_authkeyid() { }

	public sctp_authkeyid(int scact_keynumber) {
		this(0, scact_keynumber);
	}
	
	public sctp_authkeyid(int scact_assoc_id, int scact_keynumber) {
		this.scact_assoc_id = scact_assoc_id;
		this.scact_keynumber = scact_keynumber;
	}

	@Override
	public String toString() {
		return "sctp_authkeyid [scact_assoc_id=" + scact_assoc_id + ", scact_keynumber=" + scact_keynumber + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(scact_assoc_id);
		out.writeInt(scact_keynumber);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		scact_assoc_id = in.readInt();
		scact_keynumber = in.readInt();
	}

	public SctpSocketParam merge(SctpSocketParam other) {
		return other;
	}
}
