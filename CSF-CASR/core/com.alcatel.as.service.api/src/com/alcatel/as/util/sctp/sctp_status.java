package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * struct sctp_status {
 *	sctp_assoc_t			sstat_assoc_id;
 *	__s32					sstat_state;
 *	__u32					sstat_rwnd;
 *	__u16					sstat_unackdata;
 *	__u16					sstat_penddata;
 *	__u16					sstat_instrms;
 *	__u16					sstat_outstrms;
 *	__u32					sstat_fragmentation_point;
 *	struct sctp_paddrinfo	sstat_primary;
 * };
 */
public class sctp_status implements SctpSocketParam {
	
	public enum sctp_sstat_state 	{
		SCTP_EMPTY,
		SCTP_CLOSED,
		SCTP_COOKIE_WAIT,
		SCTP_COOKIE_ECHOED,
		SCTP_ESTABLISHED,
		SCTP_SHUTDOWN_PENDING,
		SCTP_SHUTDOWN_SENT,
		SCTP_SHUTDOWN_RECEIVED,
		SCTP_SHUTDOWN_ACK_SENT;
	};
	
	public int 			  sstat_assoc_id;
	public sctp_sstat_state sstat_state;
	public long 		  sstat_rwnd;
	public int 			  sstat_unackdata;
	public int 			  sstat_penddata;
	public int 			  sstat_instrms;
	public int 			  sstat_outstrms;
	public long			  sstat_fragmentation_point;
	public sctp_paddrinfo sstat_primary;
	
	public sctp_status(int sstat_assoc_id, sctp_sstat_state sstat_state, long sstat_rwnd, int sstat_unackdata,
			int sstat_penddata, int sstat_instrms, int sstat_outstrms, long sstat_fragmentation_point,
			sctp_paddrinfo sstat_primary) {
		this.sstat_assoc_id = sstat_assoc_id;
		this.sstat_state = sstat_state;
		this.sstat_rwnd = sstat_rwnd;
		this.sstat_unackdata = sstat_unackdata;
		this.sstat_penddata = sstat_penddata;
		this.sstat_instrms = sstat_instrms;
		this.sstat_outstrms = sstat_outstrms;
		this.sstat_fragmentation_point = sstat_fragmentation_point;
		this.sstat_primary = sstat_primary;
	}

	@Override
	public String toString() {
		return "sctp_status [sstat_assoc_id=" + sstat_assoc_id + ", sstat_state=" + sstat_state + ", sstat_rwnd="
				+ sstat_rwnd + ", sstat_unackdata=" + sstat_unackdata + ", sstat_penddata=" + sstat_penddata
				+ ", sstat_instrms=" + sstat_instrms + ", sstat_outstrms=" + sstat_outstrms
				+ ", sstat_fragmentation_point=" + sstat_fragmentation_point + ", sstat_primary=" + sstat_primary + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(sstat_assoc_id);
		out.writeUTF(sstat_state.name());
		out.writeLong(sstat_rwnd);
		out.writeInt(sstat_unackdata);
		out.writeInt(sstat_penddata);
		out.writeInt(sstat_instrms);
		out.writeInt(sstat_outstrms);
		out.writeLong(sstat_fragmentation_point);
		out.writeObject(sstat_primary);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sstat_assoc_id = in.readInt();
		sstat_state = sctp_sstat_state.valueOf(in.readUTF());
		sstat_rwnd = in.readLong();
		sstat_unackdata = in.readInt();
		sstat_penddata = in.readInt();
		sstat_instrms = in.readInt();
		sstat_outstrms = in.readInt();
		sstat_fragmentation_point = in.readLong();
		sstat_primary = (sctp_paddrinfo) in.readObject();
	}

	public SctpSocketParam merge(SctpSocketParam other) {
		throw new UnsupportedOperationException("This options is GET only, can't be merged");
	}
}
