package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.sun.jna.Pointer;

/**
 * struct sctp_sndrcvinfo {
 *	__u16 		 sinfo_stream;
 *	__u16 		 sinfo_ssn;
 *	__u16 		 sinfo_flags;
 *	__u32 		 sinfo_ppid;
 *	__u32 		 sinfo_context;
 *	__u32 		 sinfo_timetolive;
 *	__u32 		 sinfo_tsn;
 *	__u32 		 sinfo_cumtsn;
 *	sctp_assoc_t sinfo_assoc_id;
 * };
 */

public class sctp_sndrcvinfo implements SctpSocketParam {
	
	public int			  sinfo_assoc_id;
	public int 			  sinfo_stream;
	public int 			  sinfo_ssn;
	public sctp_sinfo_flags sinfo_flags;
	public long 		  sinfo_ppid;
	public long			  sinfo_context;
	public long			  sinfo_timetolive;
	public long			  sinfo_tsn;
	public long 		  sinfo_cumtsn;
	
	public sctp_sndrcvinfo() { }
	
	public sctp_sndrcvinfo(int sinfo_assoc_id) {
		this(sinfo_assoc_id, 0, 0, new sctp_sinfo_flags(0), 0, 0, 0, 0, 0);
	}
	
	public sctp_sndrcvinfo(int sinfo_stream, int sinfo_ssn, sctp_sinfo_flags sinfo_flags, long sinfo_ppid, 
			   long sinfo_context, long sinfo_timetolive, long sinfo_tsn, long sinfo_cumtsn) {
		this(0, sinfo_stream, sinfo_ssn, sinfo_flags, sinfo_ppid, sinfo_context, 
				sinfo_timetolive, sinfo_tsn, sinfo_cumtsn);
	}

	public sctp_sndrcvinfo(int sinfo_stream, sctp_sinfo_flags sinfo_flags, long sinfo_ppid, 
						   long sinfo_context, long sinfo_timetolive) {
		this(0, sinfo_stream, 0, sinfo_flags, sinfo_ppid, sinfo_context, sinfo_timetolive, 0, 0);
	}

	public sctp_sndrcvinfo(int sinfo_assoc_id, int sinfo_stream, int sinfo_ssn, sctp_sinfo_flags sinfo_flags, long sinfo_ppid, 
						   long sinfo_context, long sinfo_timetolive, long sinfo_tsn, long sinfo_cumtsn) {
		
		this.sinfo_stream = sinfo_stream;
		this.sinfo_ssn = sinfo_ssn;
		this.sinfo_flags = sinfo_flags;
		this.sinfo_ppid = sinfo_ppid;
		this.sinfo_context = sinfo_context;
		this.sinfo_timetolive = sinfo_timetolive;
		this.sinfo_tsn = sinfo_tsn;
		this.sinfo_cumtsn = sinfo_cumtsn;
		this.sinfo_assoc_id = sinfo_assoc_id;
	}

	@Override
	public String toString() {
		return "sctp_sndrcvinfo [sinfo_assoc_id=" + sinfo_assoc_id + ", sinfo_stream=" + sinfo_stream + ", sinfo_ssn="
				+ sinfo_ssn + ", sinfo_flags=" + sinfo_flags + ", sinfo_ppid=" + sinfo_ppid + ", sinfo_context="
				+ sinfo_context + ", sinfo_timetolive=" + sinfo_timetolive + ", sinfo_tsn=" + sinfo_tsn
				+ ", sinfo_cumtsn=" + sinfo_cumtsn + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(sinfo_assoc_id);
		out.writeInt(sinfo_stream);
		out.writeInt(sinfo_ssn);
		out.writeLong(sinfo_flags.flags);
		out.writeLong(sinfo_ppid);
		out.writeLong(sinfo_context);
		out.writeLong(sinfo_timetolive);
		out.writeLong(sinfo_tsn);
		out.writeLong(sinfo_cumtsn);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sinfo_assoc_id = in.readInt();
		sinfo_stream = in.readInt();
		sinfo_ssn = in.readInt();
		sinfo_flags = new sctp_sinfo_flags(in.readLong());
		sinfo_ppid = in.readLong();
		sinfo_context = in.readLong();
		sinfo_timetolive = in.readLong();
		sinfo_tsn = in.readLong();
		sinfo_cumtsn = in.readLong();
	}

	public SctpSocketParam merge(SctpSocketParam other2) {
                if(!(other2 instanceof sctp_sndrcvinfo)) throw new IllegalArgumentException("Not an sctp_sndrcvinfo");
                sctp_sndrcvinfo other = (sctp_sndrcvinfo) other2;
		
		int stream;
		long ppid, context, timetolive;

		if(this.sinfo_stream == 0) stream = other.sinfo_stream;
		else if(other.sinfo_stream == 0) stream = this.sinfo_stream;
		else stream = other.sinfo_stream;	

		if(this.sinfo_ppid == 0) ppid = other.sinfo_ppid;
                else if(other.sinfo_ppid == 0) ppid = this.sinfo_ppid;
                else ppid = other.sinfo_ppid;

		if(this.sinfo_context == 0) context = other.sinfo_context;
                else if(other.sinfo_context == 0) context = this.sinfo_context;
                else context = other.sinfo_context;

		if(this.sinfo_timetolive == 0) timetolive = other.sinfo_timetolive;
                else if(other.sinfo_timetolive == 0) timetolive = this.sinfo_timetolive;
                else timetolive = other.sinfo_timetolive;

		return new sctp_sndrcvinfo(stream, other.sinfo_flags, ppid, context, timetolive);
	}
	
	public Pointer toJNA(Pointer p) {
		p.setShort(0, (short) sinfo_stream);
		p.setShort(2, (short) sinfo_ssn);
		p.setShort(4, (short) sinfo_flags.flags);
		p.setInt(8, (int) sinfo_ppid);
		p.setInt(12, (int) sinfo_context);
		p.setInt(16, (int) sinfo_timetolive);
		p.setInt(20, (int) sinfo_tsn);
		p.setInt(24, (int) sinfo_cumtsn);
		p.setInt(28, (int) sinfo_assoc_id);
		return p;
	}
	
	public sctp_sndrcvinfo fromJNA(Pointer p) {
		sinfo_stream = p.getShort(0);
		sinfo_ssn = p.getShort(2);
		sinfo_flags = new sctp_sinfo_flags(p.getShort(4));
		sinfo_ppid = p.getInt(8);
		sinfo_context = p.getInt(12);
		sinfo_timetolive = p.getInt(16);
		sinfo_tsn = p.getInt(20);
		sinfo_cumtsn = p.getInt(24);
		sinfo_assoc_id = p.getInt(28);
		return this;
	}
	
	public int jnaSize() {
		return 32; //3 shorts + 6 int + 2 padding
	}
}
