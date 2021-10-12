// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * struct sctp_initmsg {
 *	__u16 sinit_num_ostreams;
 *	__u16 sinit_max_instreams;
 *	__u16 sinit_max_attempts;
 *	__u16 sinit_max_init_timeo;
 * };
 */

public class sctp_initmsg implements SctpSocketParam {
	
	public int sinit_num_ostreams;
	public int sinit_max_instreams;
	public int sinit_max_attempts;
	public int sinit_max_init_timeo;

	public sctp_initmsg() { }
	
	public sctp_initmsg(int sinit_num_ostreams, int sinit_max_instreams,
						int sinit_max_attempts, int sinit_max_init_timeo) {
		
		this.sinit_num_ostreams = sinit_num_ostreams;
		this.sinit_max_instreams = sinit_max_instreams;
		this.sinit_max_attempts = sinit_max_attempts;
		this.sinit_max_init_timeo = sinit_max_init_timeo;
	}

	@Override
	public String toString() {
		return "sctp_initmsg [sinit_num_ostreams=" + sinit_num_ostreams + ", sinit_max_instreams=" + sinit_max_instreams
				+ ", sinit_max_attempts=" + sinit_max_attempts + ", sinit_max_init_timeo=" + sinit_max_init_timeo + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(sinit_num_ostreams);
		out.writeInt(sinit_max_instreams);
		out.writeInt(sinit_max_attempts);
		out.writeInt(sinit_max_init_timeo);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sinit_num_ostreams = in.readInt();
		sinit_max_instreams = in.readInt();
		sinit_max_attempts = in.readInt();
		sinit_max_init_timeo = in.readInt();
	}

	public SctpSocketParam merge(SctpSocketParam other2) {
                if(!(other2 instanceof sctp_initmsg)) throw new IllegalArgumentException("Not an sctp_initmsg");
                sctp_initmsg other = (sctp_initmsg) other2;
		
		int num_ostreams, max_instreams, max_attempts, max_init_timeo;
		
		if(this.sinit_num_ostreams == 0) num_ostreams = other.sinit_num_ostreams;
		else if(other.sinit_num_ostreams == 0) num_ostreams = this.sinit_num_ostreams;
		else num_ostreams = other.sinit_num_ostreams;

		if(this.sinit_max_instreams == 0) max_instreams = other.sinit_max_instreams;
                else if(other.sinit_max_instreams == 0) max_instreams = this.sinit_max_instreams;
                else max_instreams = other.sinit_max_instreams;

		if(this.sinit_max_attempts == 0) max_attempts = other.sinit_max_attempts;
                else if(other.sinit_max_attempts == 0) max_attempts = this.sinit_max_attempts;
                else max_attempts = other.sinit_max_attempts;

		if(this.sinit_max_init_timeo == 0) max_init_timeo = other.sinit_max_init_timeo;
                else if(other.sinit_max_init_timeo == 0) max_init_timeo = this.sinit_max_init_timeo;
                else max_init_timeo = other.sinit_max_init_timeo;

		return new sctp_initmsg(num_ostreams, max_instreams, max_attempts, max_init_timeo);
	}
	
	public Pointer toJNA(Pointer p) {
		p.setShort(0, (short) sinit_num_ostreams);
		p.setShort(2, (short) sinit_max_instreams);
		p.setShort(4, (short) sinit_max_attempts);
		p.setShort(6, (short) sinit_max_init_timeo);
		return p;
	}
	
	public sctp_initmsg fromJNA(Pointer p) {
		sinit_num_ostreams = p.getShort(0);
		sinit_max_instreams = p.getShort(2);
		sinit_max_attempts = p.getShort(4);
		sinit_max_init_timeo = p.getShort(6);
		return this;
	}
	
	public int jnaSize() {
		return 8; //4 shorts
	}

}
