package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.sun.jna.Pointer;

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
	
	public Pointer toJNA(Pointer p) {
		p.setInt(0, scact_assoc_id);
		p.setShort(4, (short) scact_keynumber);
		return p;
	}
	
	public sctp_authkeyid fromJNA(Pointer p) {
		scact_assoc_id = p.getInt(0);
		scact_keynumber = p.getShort(4);
		return this;
	}
	
	public int jnaSize() {
		return 8; //int + short + 2 padding
	}
}
