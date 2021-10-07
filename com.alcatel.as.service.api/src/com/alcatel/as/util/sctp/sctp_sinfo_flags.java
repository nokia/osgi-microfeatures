package com.alcatel.as.util.sctp;

public class sctp_sinfo_flags {

	private final int SCTP_UNORDERED = 1 << 0;
	private final int SCTP_ADDR_OVER = 1 << 1;
	private final int SCTP_ABORT = 1 << 2;
	private final int SCTP_EOF = 0x200;

	public final long flags;

	public sctp_sinfo_flags(boolean sctp_unordered, 
				boolean sctp_addr_over, 
				boolean sctp_abort, 
				boolean sctp_eof) {
		long bitfield = 0;
		bitfield |= (sctp_unordered ? SCTP_UNORDERED : 0);
		bitfield |= (sctp_addr_over ? SCTP_ADDR_OVER : 0);
		bitfield |= (sctp_abort ? SCTP_ABORT : 0);
		bitfield |= (sctp_eof ? SCTP_EOF : 0);

		flags = bitfield;
	}

	public sctp_sinfo_flags(long flags) {
		this.flags = flags;
	}
	
	public boolean hasSctpUnordered() {
		return (flags & SCTP_UNORDERED) == SCTP_UNORDERED;
	}
	
	public boolean hasSctpAddrOver() {
		return (flags & SCTP_ADDR_OVER) == SCTP_ADDR_OVER;
	}
	
	public boolean hasSctpAbort() {
		return (flags & SCTP_ABORT) == SCTP_ABORT;
	}
	
	public boolean hasSctpEof() {
		return (flags & SCTP_EOF) == SCTP_EOF;
	}
	
	@Override
	public String toString() {
		return "sctp_sinfo_flags [flags=" + String.format("%10s", Long.toBinaryString(flags)).replace(" ", "0") + "]";
	}

}
