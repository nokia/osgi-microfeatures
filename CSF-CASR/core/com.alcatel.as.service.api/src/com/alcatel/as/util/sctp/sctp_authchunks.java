package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import com.alcatel.as.util.sctp.sctp_authchunk.sctp_cid_t;
import com.sun.jna.Pointer;

/**
 * struct sctp_authchunks {
 *	sctp_assoc_t	gauth_assoc_id;
 *	__u32			gauth_number_of_chunks;
 *	uint8_t			gauth_chunks[];
 * };
  */
public class sctp_authchunks implements SctpSocketParam {
	public int gauth_assoc_id;
	public long gauth_number_of_chunks;
	public sctp_cid_t[] gauth_chunks;
	
	public sctp_authchunks() { }
	
	public sctp_authchunks(int gauth_assoc_id) {
		this(gauth_assoc_id, 0, new sctp_cid_t[0])	;
	}
	
	public sctp_authchunks(int gauth_assoc_id, long gauth_number_of_chunks, sctp_cid_t[] gauth_chunks) {
		this.gauth_assoc_id = gauth_assoc_id;
		this.gauth_number_of_chunks = gauth_number_of_chunks;
		this.gauth_chunks = gauth_chunks;
	}

	@Override
	public String toString() {
		return "sctp_authchunks [gauth_assoc_id=" + gauth_assoc_id + ", gauth_number_of_chunks="
				+ gauth_number_of_chunks + ", gauth_chunks=" + Arrays.toString(gauth_chunks) + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(gauth_assoc_id);
		out.writeLong(gauth_number_of_chunks);
		
		int len = gauth_chunks.length;
		out.writeInt(len);
		for(int i = 0; i < len; i++) out.writeUTF(gauth_chunks[i].name());
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		gauth_assoc_id = in.readInt();
		gauth_number_of_chunks = in.readLong();

		int len = in.readInt();
		gauth_chunks = new sctp_cid_t[len];
		for(int i = 0; i < len; i++) gauth_chunks[i] = sctp_cid_t.valueOf(in.readUTF());
	}
	
	public SctpSocketParam merge(SctpSocketParam other) {
		throw new UnsupportedOperationException("This options is GET only, can't be merged");
	}
	
	public Pointer toJNA(Pointer p) {
		p.setInt(0, gauth_assoc_id);
		p.setInt(4, 0); //empty array for get
		return p;
	}
	
	public sctp_authchunks fromJNA(Pointer p) {
		gauth_assoc_id = p.getInt(0);
		gauth_number_of_chunks = p.getInt(4);
		
		for(int i = 0; i < gauth_number_of_chunks; i++) {
			byte b = p.getByte(8 + i);
			switch(b) {
			    case 0: gauth_chunks[i] = sctp_cid_t.SCTP_CID_DATA; break;
			    case 1: gauth_chunks[i] = sctp_cid_t.SCTP_CID_INIT; break;
			    case 2: gauth_chunks[i] = sctp_cid_t.SCTP_CID_INIT_ACK; break;
			    case 3: gauth_chunks[i] = sctp_cid_t.SCTP_CID_SACK; break;
			    case 4: gauth_chunks[i] = sctp_cid_t.SCTP_CID_HEARTBEAT; break;
			    case 5: gauth_chunks[i] = sctp_cid_t.SCTP_CID_HEARTBEAT_ACK; break;
			    case 6: gauth_chunks[i] = sctp_cid_t.SCTP_CID_ABORT; break;
			    case 7: gauth_chunks[i] = sctp_cid_t.SCTP_CID_SHUTDOWN; break;
			    case 8: gauth_chunks[i] = sctp_cid_t.SCTP_CID_SHUTDOWN_ACK; break;
			    case 9: gauth_chunks[i] = sctp_cid_t.SCTP_CID_ERROR; break;
			    case 10: gauth_chunks[i] = sctp_cid_t.SCTP_CID_COOKIE_ECHO; break;
			    case 11: gauth_chunks[i] = sctp_cid_t.SCTP_CID_COOKIE_ACK; break;
			    case 12: gauth_chunks[i] = sctp_cid_t.SCTP_CID_ECN_ECNE; break;
			    case 13: gauth_chunks[i] = sctp_cid_t.SCTP_CID_SHUTDOWN_COMPLETE; break;
			    case 14: gauth_chunks[i] = sctp_cid_t.SCTP_CID_AUTH; break;
			    case (byte) 0xC0: gauth_chunks[i] = sctp_cid_t.SCTP_CID_FWD_TSN; break;
			    case (byte) 0xC1: gauth_chunks[i] = sctp_cid_t.SCTP_CID_ASCONF; break;
			    case (byte) 0x80: gauth_chunks[i] = sctp_cid_t.SCTP_CID_ASCONF_ACK; break;
			    default: gauth_chunks[i] = sctp_cid_t.SCTP_CID_RECONF;
			}
		}
		return this;
	}
	
	public int jnaSize() {
		return 8 + 64; //2 int + space for array
	}
}
