package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

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
}
