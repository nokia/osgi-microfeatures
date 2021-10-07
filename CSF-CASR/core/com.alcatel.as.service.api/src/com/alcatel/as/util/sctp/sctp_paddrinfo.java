package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.sun.jna.Pointer;

/**
 * struct sctp_paddrinfo {
 *	sctp_assoc_t			spinfo_assoc_id;
 *	struct sockaddr_storage	spinfo_address;
 *	__s32					spinfo_state;
 *	__u32					spinfo_cwnd;
 *	__u32					spinfo_srtt;
 *	__u32					spinfo_rto;
 *	__u32					spinfo_mtu;
 * } __attribute__((packed, aligned(4)));
 */
public class sctp_paddrinfo implements SctpSocketParam {
	
	public enum sctp_spinfo_state {
		SCTP_INACTIVE,
		SCTP_PF,
		SCTP_ACTIVE,
		SCTP_UNCONFIRMED,
		SCTP_UNKNOWN;
	}
	
	public int 				spinfo_assoc_id;
	public InetSocketAddress 	spinfo_address;
	public sctp_spinfo_state	spinfo_state;
	public long				spinfo_cwnd;
	public long				spinfo_srtt;
	public long				spinfo_rto;
	public long				spinfo_mtu;
	
	public sctp_paddrinfo() { }
	
	public sctp_paddrinfo(int spinfo_assoc_id, InetSocketAddress spinfo_address) {
		this(spinfo_assoc_id, spinfo_address, sctp_spinfo_state.SCTP_INACTIVE, 0, 0, 0, 0);
	}

	public sctp_paddrinfo(InetSocketAddress spinfo_address, sctp_spinfo_state spinfo_state,
			long spinfo_cwnd, long spinfo_srtt, long spinfo_rto, long spinfo_mtu) {
		this(0, spinfo_address, spinfo_state, spinfo_cwnd, spinfo_srtt, spinfo_rto, spinfo_mtu);
	}

	public sctp_paddrinfo(int spinfo_assoc_id, InetSocketAddress spinfo_address, sctp_spinfo_state spinfo_state,
			long spinfo_cwnd, long spinfo_srtt, long spinfo_rto, long spinfo_mtu) {
		this.spinfo_assoc_id = spinfo_assoc_id;
		this.spinfo_address = spinfo_address;
		this.spinfo_state = spinfo_state;
		this.spinfo_cwnd = spinfo_cwnd;
		this.spinfo_srtt = spinfo_srtt;
		this.spinfo_rto = spinfo_rto;
		this.spinfo_mtu = spinfo_mtu;
	}

	@Override
	public String toString() {
		return "sctp_paddrinfo [spinfo_assoc_id=" + spinfo_assoc_id + ", spinfo_address=" + spinfo_address
				+ ", spinfo_state=" + spinfo_state + ", spinfo_cwnd=" + spinfo_cwnd + ", spinfo_srtt=" + spinfo_srtt
				+ ", spinfo_rto=" + spinfo_rto + ", spinfo_mtu=" + spinfo_mtu + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(spinfo_assoc_id);
		out.writeUTF(spinfo_address.getHostName());
		out.writeInt(spinfo_address.getPort());
		out.writeUTF(spinfo_state.name());
		out.writeLong(spinfo_cwnd);
		out.writeLong(spinfo_srtt);
		out.writeLong(spinfo_rto);
		out.writeLong(spinfo_mtu);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		spinfo_assoc_id = in.readInt();

		String hostname = in.readUTF();
		int port = in.readInt();
		spinfo_address = new InetSocketAddress(hostname, port);

		spinfo_state = sctp_spinfo_state.valueOf(in.readUTF());
		spinfo_cwnd = in.readLong();
		spinfo_srtt = in.readLong();
		spinfo_rto = in.readLong();
		spinfo_mtu = in.readLong();
	}

	public SctpSocketParam merge(SctpSocketParam other) {
		throw new UnsupportedOperationException("This options is GET only, can't be merged");
	}
	
	public Pointer toJNA(Pointer p, long offset) {
		p.setInt(offset, spinfo_assoc_id);
		sockaddr_storage.toJNA(spinfo_address, p, offset + 4);
		return p; //no need to set the other values, only getsockopt is permitted on this struct
	}
	
	public sctp_paddrinfo fromJNA(Pointer p, long offset) throws UnknownHostException {
		spinfo_assoc_id = p.getInt(offset);
		spinfo_address = sockaddr_storage.fromJNA(p, offset + 4);
		int size = sockaddr_storage.jnaSize();
		
		int state = p.getInt(offset + size + 4);
		switch(state) {
			case 0: spinfo_state = sctp_spinfo_state.SCTP_INACTIVE; break;
			case 1: spinfo_state = sctp_spinfo_state.SCTP_PF; break;
			case 2: spinfo_state = sctp_spinfo_state.SCTP_ACTIVE; break;
			case 3: spinfo_state = sctp_spinfo_state.SCTP_UNCONFIRMED; break;
			default: spinfo_state = sctp_spinfo_state.SCTP_UNKNOWN;
		}
		
		spinfo_cwnd = p.getInt(offset + size + 8);
		spinfo_srtt = p.getInt(offset + size + 12);
		spinfo_rto = p.getInt(offset + size + 16);
		spinfo_mtu = p.getInt(offset + size + 20);
		return this;
	}
	
	public int jnaSize() {
		return 24 + sockaddr_storage.jnaSize(); //6 int + 128 sockaddr_storage
	}
	
}
