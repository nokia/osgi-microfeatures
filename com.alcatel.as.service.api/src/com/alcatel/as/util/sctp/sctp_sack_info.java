// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.sun.jna.Pointer;

/**
 * struct sctp_sack_info {
 *	sctp_assoc_t	sack_assoc_id;
 *	uint32_t		sack_delay;
 *	uint32_t		sack_freq;
 * };
 */

public class sctp_sack_info implements SctpSocketParam {

	public int  sack_assoc_id;
	public long sack_delay;
	public long sack_freq;
	
	public sctp_sack_info() { }
	
	public sctp_sack_info(int sack_assoc_id) {
		this(sack_assoc_id, 0, 0);
	}

	public sctp_sack_info(long sack_delay, long sack_freq) {
		this(0, sack_delay, sack_freq);
	}

	public sctp_sack_info(int sack_assoc_id, long sack_delay, long sack_freq) {
		this.sack_assoc_id = sack_assoc_id;
		this.sack_delay = sack_delay;
		this.sack_freq = sack_freq;
	}

	@Override
	public String toString() {
		return "sctp_sack_info [sack_assoc_id=" + sack_assoc_id + ", sack_delay=" + sack_delay + ", sack_freq="
				+ sack_freq + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(sack_assoc_id);
		out.writeLong(sack_delay);
		out.writeLong(sack_freq);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sack_assoc_id = in.readInt();
		sack_delay = in.readLong();
		sack_freq = in.readLong();
	}

	public SctpSocketParam merge(SctpSocketParam other2) {
                if(!(other2 instanceof sctp_sack_info)) throw new IllegalArgumentException("Not an sctp_sack_info");
                sctp_sack_info other = (sctp_sack_info) other2;		

		long delay, freq;
		
		if(this.sack_delay == 0) delay = other.sack_delay;
		else if(other.sack_delay == 0) delay = this.sack_delay;
		else delay = other.sack_delay;

		if(this.sack_freq == 0) freq = other.sack_freq;
                else if(other.sack_freq == 0) freq = this.sack_freq;
                else freq = other.sack_freq;

		return new sctp_sack_info(delay, freq);
	}
	
	public Pointer toJNA(Pointer p) {
		p.setInt(0, sack_assoc_id);
		p.setInt(4, (int) sack_delay);
		p.setInt(8, (int) sack_freq);
		return p;
	}
	
	public sctp_sack_info fromJNA(Pointer p) {
		sack_assoc_id = p.getInt(0);
		sack_delay = p.getInt(4);
		sack_freq = p.getInt(8);
		return this;
	}
	
	public int jnaSize() {
		return 12; //3 int
	}
}
