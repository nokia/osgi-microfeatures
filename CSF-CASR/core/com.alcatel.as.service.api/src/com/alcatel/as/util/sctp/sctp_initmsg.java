package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

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

}
