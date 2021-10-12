// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * struct sctp_paddrparams {
 *	sctp_assoc_t			spp_assoc_id;
 *	struct sockaddr_storage	spp_address;
 *	__u32					spp_hbinterval;
 *	__u16					spp_pathmaxrxt;
 *	__u32					spp_pathmtu;
 *	__u32					spp_sackdelay;
 *	__u32					spp_flags;
 * } __attribute__((packed, aligned(4)));
 */
public class sctp_paddrparams implements SctpSocketParam {
	
	public int 		    spp_assoc_id;
	public InetSocketAddress  spp_address;
	public long 		    spp_hbinterval;
	public int 		    spp_pathmaxrxt;
	public long		    spp_pathmtu;
	public long		    spp_sackdelay;
	public sctp_spp_flags spp_flags;

	public sctp_paddrparams() { }
	
	public sctp_paddrparams(int spp_assoc_id) {
		this(spp_assoc_id, null, 0, 0, 0, 0, new sctp_spp_flags(0));
	}

	public sctp_paddrparams(InetSocketAddress spp_address, long spp_hbinterval, 
							int spp_pathmaxrxt, long spp_pathmtu, long spp_sackdelay, sctp_spp_flags spp_flags) {
		this(0, spp_address, spp_hbinterval, spp_pathmaxrxt, spp_pathmtu, spp_sackdelay, spp_flags);
	}
	
	public sctp_paddrparams(int spp_assoc_id, InetSocketAddress spp_address, long spp_hbinterval, 
							int spp_pathmaxrxt, long spp_pathmtu, long spp_sackdelay, sctp_spp_flags spp_flags) {
		this.spp_assoc_id = spp_assoc_id;
		this.spp_address = spp_address;
		this.spp_hbinterval = spp_hbinterval;
		this.spp_pathmaxrxt = spp_pathmaxrxt;
		this.spp_pathmtu = spp_pathmtu;
		this.spp_sackdelay = spp_sackdelay;
		this.spp_flags = spp_flags;
	}

	@Override
	public String toString() {
		return "sctp_paddrparams [spp_assoc_id=" + spp_assoc_id + ", spp_address=" + spp_address + ", spp_hbinterval="
				+ spp_hbinterval + ", spp_pathmaxrxt=" + spp_pathmaxrxt + ", spp_pathmtu=" + spp_pathmtu
				+ ", spp_sackdelay=" + spp_sackdelay + ", spp_flags=" + spp_flags + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(spp_assoc_id);
		out.writeUTF(spp_address.getHostName());
		out.writeInt(spp_address.getPort());
		out.writeLong(spp_hbinterval);
		out.writeInt(spp_pathmaxrxt);
		out.writeLong(spp_pathmtu);
		out.writeLong(spp_sackdelay);
		out.writeLong(spp_flags.flags);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		spp_assoc_id = in.readInt();

		String hostname = in.readUTF();
		int port = in.readInt();
		spp_address = new InetSocketAddress(hostname, port);

		spp_hbinterval = in.readLong();
		spp_pathmaxrxt = in.readInt();
		spp_pathmtu = in.readLong();
		spp_sackdelay = in.readLong();
		spp_flags = new sctp_spp_flags(in.readLong());
	}

	public SctpSocketParam merge(SctpSocketParam other2) {
                if(!(other2 instanceof sctp_paddrparams)) throw new IllegalArgumentException("Not an sctp_paddrparams");
                sctp_paddrparams other = (sctp_paddrparams) other2;

		long hbinterval, pathmtu, sackdelay;
		int pathmaxrxt;

		if(this.spp_hbinterval == 0) hbinterval = other.spp_hbinterval;
		else if(other.spp_hbinterval == 0) hbinterval = this.spp_hbinterval;
		else hbinterval = other.spp_hbinterval;

		if(this.spp_pathmtu == 0) pathmtu = other.spp_pathmtu;
                else if(other.spp_pathmtu == 0) pathmtu = this.spp_pathmtu;
                else pathmtu = other.spp_pathmtu;

		if(this.spp_sackdelay == 0) sackdelay = other.spp_sackdelay;
                else if(other.spp_sackdelay == 0) sackdelay = this.spp_sackdelay;
                else sackdelay = other.spp_sackdelay;

		if(this.spp_pathmaxrxt == 0) pathmaxrxt = other.spp_pathmaxrxt;
                else if(other.spp_pathmaxrxt == 0) pathmaxrxt = this.spp_pathmaxrxt;
                else pathmaxrxt = other.spp_pathmaxrxt;

		boolean hbzero;
		if(other.spp_flags.heartbeatTimeIsZero()) hbzero = true;
		else if(other.spp_hbinterval != 0) hbzero = false;
		else hbzero = this.spp_flags.heartbeatTimeIsZero();

		sctp_spp_flags flags = new sctp_spp_flags(this.spp_flags.isHeartbeatEnabled() || other.spp_flags.isHeartbeatEnabled(),
							  false,
							  this.spp_flags.isPMTUDiscoveryEnabled() || other.spp_flags.isPMTUDiscoveryEnabled(),
							  this.spp_flags.isSACKEnabled() || other.spp_flags.isSACKEnabled(),
							  hbzero);

		return new sctp_paddrparams(other.spp_address, hbinterval, pathmaxrxt, pathmtu, sackdelay, flags);
	}
	
	public Pointer toJNA(Pointer p) {
		int size = sockaddr_storage.jnaSize();
		p.setInt(0, spp_assoc_id);
		p = sockaddr_storage.toJNA(spp_address, p, 4);
		p.setInt(4 + size, (int) spp_hbinterval);
		p.setShort(8 + size, (short) spp_pathmaxrxt);
		p.setInt(10 + size, (int) spp_pathmtu);
		p.setInt(14 + size, (int) spp_sackdelay);
		p.setInt(18 + size, (int) spp_flags.flags);
		return p;
	}
	
	public sctp_paddrparams fromJNA(Pointer p) throws UnknownHostException {
		int size = sockaddr_storage.jnaSize();
		spp_assoc_id = p.getInt(0);
		spp_address = sockaddr_storage.fromJNA(p, 4);
		spp_hbinterval = p.getInt(4 + size);
		spp_pathmaxrxt = p.getShort(8 + size);
		spp_pathmtu = p.getInt(10 + size);
		spp_sackdelay = p.getInt(14 + size);
		spp_flags = new sctp_spp_flags(p.getInt(18 + size));
		return this;
	}
	
	public int jnaSize() {
		return 22 + sockaddr_storage.jnaSize() + 2; //5 int + 1 short + 128 sockaddr_storage + 2 alignment
	}

}
