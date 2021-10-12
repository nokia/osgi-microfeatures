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
 * struct sctp_authchunk {
 *	__u8		sauth_chunk;
 * };
 */
public class sctp_authchunk implements SctpSocketParam {
	
	public enum sctp_cid_t {
		SCTP_CID_DATA,
		SCTP_CID_INIT,
		SCTP_CID_INIT_ACK,
		SCTP_CID_SACK,
		SCTP_CID_HEARTBEAT,
		SCTP_CID_HEARTBEAT_ACK,
		SCTP_CID_ABORT,
		SCTP_CID_SHUTDOWN,
		SCTP_CID_SHUTDOWN_ACK,
		SCTP_CID_ERROR,
		SCTP_CID_COOKIE_ECHO,
		SCTP_CID_COOKIE_ACK,
		SCTP_CID_ECN_ECNE,
		SCTP_CID_ECN_CWR,
		SCTP_CID_SHUTDOWN_COMPLETE,

		SCTP_CID_AUTH,

		SCTP_CID_FWD_TSN,

		SCTP_CID_ASCONF,
		SCTP_CID_ASCONF_ACK,
		SCTP_CID_RECONF
	}
	
	public sctp_cid_t sauth_chunk;

	public sctp_authchunk() { }

	public sctp_authchunk(sctp_cid_t sauth_chunk) {
		this.sauth_chunk = sauth_chunk;
	}

	@Override
	public String toString() {
		return "sctp_authchunk [sauth_chunk=" + sauth_chunk + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(sauth_chunk.name());
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sauth_chunk = sctp_cid_t.valueOf(in.readUTF());
	}
	
	public SctpSocketParam merge(SctpSocketParam other) {
		return other;
	}
	
	public Pointer toJNA(Pointer p) {
		switch(sauth_chunk) {
			case SCTP_CID_DATA: p.setByte(0, (byte) 0); break;
			case SCTP_CID_INIT: p.setByte(0, (byte) 1); break;
			case SCTP_CID_INIT_ACK: p.setByte(0, (byte) 2); break;
			case SCTP_CID_SACK: p.setByte(0, (byte) 3); break;
			case SCTP_CID_HEARTBEAT: p.setByte(0, (byte) 4); break;
			case SCTP_CID_HEARTBEAT_ACK: p.setByte(0, (byte) 5); break;
			case SCTP_CID_ABORT: p.setByte(0, (byte) 6); break;
			case SCTP_CID_SHUTDOWN: p.setByte(0, (byte) 7); break;
			case SCTP_CID_SHUTDOWN_ACK: p.setByte(0, (byte) 8); break;
			case SCTP_CID_ERROR: p.setByte(0, (byte) 9); break;
			case SCTP_CID_COOKIE_ECHO: p.setByte(0, (byte) 10); break;
			case SCTP_CID_COOKIE_ACK: p.setByte(0, (byte) 11); break;
			case SCTP_CID_ECN_ECNE: p.setByte(0, (byte) 12); break;
			case SCTP_CID_ECN_CWR: p.setByte(0, (byte) 13); break;
			case SCTP_CID_SHUTDOWN_COMPLETE: p.setByte(0, (byte) 14); break;
			case SCTP_CID_AUTH: p.setByte(0, (byte) 0x0F); break;
			case SCTP_CID_FWD_TSN: p.setByte(0, (byte) 0xC0); break;
			case SCTP_CID_ASCONF: p.setByte(0, (byte) 0xC1); break;
			case SCTP_CID_ASCONF_ACK: p.setByte(0, (byte) 0x80); break;
			default: p.setByte(0, (byte) 0x82); //SCTP_CID_RECONF
		}
		return p;
	}
	
	public int jnaSize() {
		return 1; //8 bits
	}
}
