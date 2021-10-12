// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.sun.jna.Pointer;

/**
 * struct sctp_hmacalgo {
 *	__u32		shmac_num_idents;
 *	__u16		shmac_idents[];
 * };
 */
public class sctp_hmacalgo implements SctpSocketParam {
	
	public enum idents {
		SCTP_AUTH_HMAC_ID_SHA1,
		SCTP_AUTH_HMAC_ID_SHA256
	}
	
	public long shmac_num_idents;
	public idents[] shmac_idents;

	public sctp_hmacalgo() { }
	
	public sctp_hmacalgo(long shmac_num_idents, idents[] shmac_idents) {
		this.shmac_num_idents = shmac_num_idents;
		this.shmac_idents = shmac_idents;
	}

	@Override
	public String toString() {
		return "sctp_hmacalgo [shmac_num_idents=" + shmac_num_idents + ", shmac_idents=" + Arrays.toString(shmac_idents)
				+ "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(shmac_num_idents);
		
		int len = shmac_idents.length;
		out.writeInt(len);
		for(int i = 0; i < len; i++) out.writeUTF(shmac_idents[i].name());
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		shmac_num_idents = in.readInt();

		int len = in.readInt();
		shmac_idents = new idents[len];
		for(int i = 0; i < len; i++) shmac_idents[i] = idents.valueOf(in.readUTF());
	}

	public SctpSocketParam merge(SctpSocketParam other) {
		return other;
	}
	
	public Pointer toJNA(Pointer p) {
		p.setInt(0, (int) shmac_num_idents);
		for(int i = 0; i < shmac_num_idents; i++) {
			if(shmac_idents[i] == idents.SCTP_AUTH_HMAC_ID_SHA1) {
				p.setShort(4 + (i*2), (short) 1);
			} else {
				p.setShort(4 + (i*2), (short) 3);
			}
		}
		return p;
	}
	
	public sctp_hmacalgo fromJNA(Pointer p) {
		shmac_num_idents = p.getInt(0);
		shmac_idents = new idents[(int) shmac_num_idents];
		for(int i = 0; i < shmac_num_idents; i++) {
			short ident = p.getShort(4 + i*2);
			if(ident == 1) {
				shmac_idents[i] = idents.SCTP_AUTH_HMAC_ID_SHA1;
			} else {
				shmac_idents[i] = idents.SCTP_AUTH_HMAC_ID_SHA256;
			}
		}
		return this;
	}
	
	public int jnaSize() {
		if(shmac_num_idents == 0) {
			return 4 + 64; // enough space to get
		} else {
			return 4 + (2 * (int) shmac_num_idents);
		}
	}
}
