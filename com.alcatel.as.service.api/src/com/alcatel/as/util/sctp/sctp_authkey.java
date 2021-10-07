package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import com.sun.jna.Pointer;

/**
 * struct sctp_authkey {
 *	sctp_assoc_t	sca_assoc_id;
 *	__u16		sca_keynumber;
 *	__u16		sca_keylength;
 *	__u8		sca_key[];
 * };
 */
public class sctp_authkey implements SctpSocketParam {
	public int sca_assoc_id;
	public int sca_keynumber;
	public int sca_keylength;
	public byte[] sca_key;

	public sctp_authkey() { }

	public sctp_authkey(int sca_keynumber, int sca_keylength, byte[] sca_key) {
		this(0, sca_keynumber, sca_keylength, sca_key);
	}
	
	public sctp_authkey(int sca_assoc_id, int sca_keynumber, int sca_keylength, byte[] sca_key) {
		this.sca_assoc_id = sca_assoc_id;
		this.sca_keynumber = sca_keynumber;
		this.sca_keylength = sca_keylength;
		this.sca_key = sca_key;
	}

	@Override
	public String toString() {
		return "sctp_authkey [sca_assoc_id=" + sca_assoc_id + ", sca_keynumber=" + sca_keynumber + ", sca_keylength="
				+ sca_keylength + ", sca_key=" + Arrays.toString(sca_key) + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(sca_assoc_id);
		out.writeInt(sca_keynumber);
		out.writeInt(sca_keylength);
		
		int len = sca_key.length;
		out.writeInt(len);
		for(int i = 0; i < len; i++) out.writeByte(sca_key[i]);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sca_assoc_id = in.readInt();
		sca_keynumber = in.readInt();
		sca_keylength = in.readInt();

		int len = in.readInt();
		sca_key = new byte[len];
		for(int i = 0; i < len; i++) sca_key[i] = in.readByte();
	}

	public SctpSocketParam merge(SctpSocketParam other) {
		return other;
	}
	
	public Pointer toJNA(Pointer p) {
		p.setInt(0, sca_assoc_id);
		p.setShort(4, (short) sca_keynumber);
		p.setShort(6, (short) sca_keylength);
		for(int i = 0; i < sca_keylength; i++) {
			p.setByte(8 + i, sca_key[i]);
		}
		return p;
	}
	
	public int jnaSize() {
		return 8 + sca_keylength; //1 int, 2 short and a byte array
	}
}
