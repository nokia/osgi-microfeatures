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
 * struct sctp_assocparams {
 *	sctp_assoc_t	sasoc_assoc_id;
 *	__u16			sasoc_asocmaxrxt;
 *	__u16			sasoc_number_peer_destinations;
 *	__u32			sasoc_peer_rwnd;
 *	__u32			sasoc_local_rwnd;
 *	__u32			sasoc_cookie_life;
 * };
 */

public class sctp_assocparams implements SctpSocketParam {
	
	public int  sasoc_assoc_id;
	public int  sasoc_asocmaxrxt;
	public int  sasoc_number_peer_destinations;
	public long sasoc_peer_rwnd;
	public long sasoc_local_rwnd;
	public long sasoc_cookie_life;

	public sctp_assocparams() { }
	
	public sctp_assocparams(int sasoc_assoc_id) {
		this(sasoc_assoc_id, 0, 0, 0, 0, 0);
	}

	public sctp_assocparams(int sasoc_asocmaxrxt, long sasoc_cookie_life) {
		this(0, sasoc_asocmaxrxt, 0, 0, 0, sasoc_cookie_life);
	}
	
	public sctp_assocparams(int sasoc_assoc_id, int sasoc_asocmaxrxt, int sasoc_number_peer_destinations,
						  long sasoc_peer_rwnd, long sasoc_local_rwnd, long sasoc_cookie_life) {
		this.sasoc_assoc_id = sasoc_assoc_id;
		this.sasoc_asocmaxrxt = sasoc_asocmaxrxt;
		this.sasoc_number_peer_destinations = sasoc_number_peer_destinations;
		this.sasoc_peer_rwnd = sasoc_peer_rwnd;
		this.sasoc_local_rwnd = sasoc_local_rwnd;
		this.sasoc_cookie_life = sasoc_cookie_life;
	}

	@Override
	public String toString() {
		return "sctp_assocparams [sasoc_assoc_id=" + sasoc_assoc_id + ", sasoc_asocmaxrxt=" + sasoc_asocmaxrxt
				+ ", sasoc_number_peer_destinations=" + sasoc_number_peer_destinations + ", sasoc_peer_rwnd="
				+ sasoc_peer_rwnd + ", sasoc_local_rwnd=" + sasoc_local_rwnd + ", sasoc_cookie_life="
				+ sasoc_cookie_life + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(sasoc_assoc_id);
		out.writeInt(sasoc_asocmaxrxt);
		out.writeInt(sasoc_number_peer_destinations);
		out.writeLong(sasoc_peer_rwnd);
		out.writeLong(sasoc_local_rwnd);
		out.writeLong(sasoc_cookie_life);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sasoc_assoc_id = in.readInt();
		sasoc_asocmaxrxt = in.readInt();
		sasoc_number_peer_destinations = in.readInt();
		sasoc_peer_rwnd = in.readLong();
		sasoc_local_rwnd = in.readLong();
		sasoc_cookie_life = in.readLong();
	}

	public SctpSocketParam merge(SctpSocketParam other2) {
		if(!(other2 instanceof sctp_assocparams)) throw new IllegalArgumentException("Not an sctp_assocparams");
                sctp_assocparams other = (sctp_assocparams) other2;

		int asocmaxrxt;
		long cookie_life;

		if(this.sasoc_asocmaxrxt == 0) asocmaxrxt = other.sasoc_asocmaxrxt;
		else if(other.sasoc_asocmaxrxt == 0) asocmaxrxt = this.sasoc_asocmaxrxt;
		else asocmaxrxt = other.sasoc_asocmaxrxt;

		if(this.sasoc_cookie_life == 0) cookie_life = other.sasoc_cookie_life;             
                else if(other.sasoc_cookie_life == 0) cookie_life = this.sasoc_cookie_life;
                else cookie_life = other.sasoc_cookie_life;
		
		return new sctp_assocparams(asocmaxrxt, cookie_life);
	}
	
	public Pointer toJNA(Pointer p) {
		p.setInt(0, sasoc_assoc_id);
		p.setShort(4, (short) sasoc_asocmaxrxt);
		p.setShort(6, (short) sasoc_number_peer_destinations);
		p.setInt(8, (int) sasoc_peer_rwnd);
		p.setInt(12, (int) sasoc_local_rwnd);
		p.setInt(16, (int) sasoc_cookie_life);
		return p;
	}
	
	public sctp_assocparams fromJNA(Pointer p) {
		sasoc_assoc_id = p.getInt(0);
		sasoc_asocmaxrxt = p.getShort(4);
		sasoc_number_peer_destinations = p.getShort(6);
		sasoc_peer_rwnd = p.getInt(8);
		sasoc_local_rwnd = p.getInt(12);
		sasoc_cookie_life = p.getInt(16);
		return this;
	}
	
	public int jnaSize() {
		return 20; //4 ints + 2 shorts
	}
}
