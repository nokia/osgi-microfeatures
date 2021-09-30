package com.alcatel.as.util.sctp;

import java.util.Arrays;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.alcatel.as.util.sctp.sctp_authchunk.sctp_cid_t;

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
}
