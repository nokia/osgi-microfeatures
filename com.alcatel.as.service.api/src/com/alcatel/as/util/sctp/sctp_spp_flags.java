package com.alcatel.as.util.sctp;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class sctp_spp_flags {
	private final int SPP_HB_ENABLE = 1 << 0;
	private final int SPP_HB_DISABLE = 1 << 1;
	
	private final int SPP_HB_DEMAND = 1 << 2;
	
	private final int SPP_PMTUD_ENABLE = 1 << 3;
	private final int SPP_PMTUD_DISABLE = 1 << 4;
	
	private final int SPP_SACKDELAY_ENABLE = 1 << 5;
	private final int SPP_SACKDELAY_DISABLE = 1 << 6;
	
	private final int SPP_HB_TIME_IS_ZERO = 1 << 7;
	
	public final long flags;
	
	public sctp_spp_flags(boolean spp_hb, 
						  boolean spp_hb_demand, 
						  boolean spp_pmtud, 
						  boolean spp_sackdelay, 
						  boolean spp_hb_time_is_zero) {
		long bitfield = 0;
		bitfield |= (spp_hb ? SPP_HB_ENABLE : SPP_HB_DISABLE);
		bitfield |= (spp_hb_demand ? SPP_HB_DEMAND : 0);
		bitfield |= (spp_pmtud ? SPP_PMTUD_ENABLE : SPP_PMTUD_DISABLE);
		bitfield |= (spp_sackdelay ? SPP_SACKDELAY_ENABLE : SPP_SACKDELAY_DISABLE);
		bitfield |= (spp_hb_time_is_zero ? SPP_HB_TIME_IS_ZERO : 0);
		
		flags = bitfield;
	}
	
	public sctp_spp_flags(long flags) {
		this.flags = flags;
	}
	
	public boolean isHeartbeatEnabled() {
		return ((flags & SPP_HB_ENABLE) == SPP_HB_ENABLE && 
				(flags & SPP_HB_DISABLE) != SPP_HB_DISABLE);
	}
	
	public boolean isHeartbeatDisabled() {
		return ((flags & SPP_HB_ENABLE) != SPP_HB_ENABLE && 
				(flags & SPP_HB_DISABLE) == SPP_HB_DISABLE);
	}
	
	public boolean sendHeartbeatImmediately() {
		return (flags & SPP_HB_DEMAND) == SPP_HB_DEMAND;
	}
	
	public boolean isPMTUDiscoveryEnabled() {
		return ((flags & SPP_PMTUD_ENABLE) == SPP_PMTUD_ENABLE && 
				(flags & SPP_PMTUD_DISABLE) != SPP_PMTUD_DISABLE);
	}
	
	public boolean isPMTUDiscoveryDisabled() {
		return ((flags & SPP_PMTUD_ENABLE) != SPP_PMTUD_ENABLE && 
				(flags & SPP_PMTUD_DISABLE) == SPP_PMTUD_DISABLE);
	}
	
	public boolean isSACKEnabled() {
		return ((flags & SPP_SACKDELAY_ENABLE) == SPP_SACKDELAY_ENABLE && 
				(flags & SPP_SACKDELAY_DISABLE) != SPP_SACKDELAY_DISABLE);
	}
	
	public boolean isSACKDisabled() {
		return ((flags & SPP_SACKDELAY_ENABLE) != SPP_SACKDELAY_ENABLE && 
				(flags & SPP_SACKDELAY_DISABLE) == SPP_SACKDELAY_DISABLE);
	}
	
	public boolean heartbeatTimeIsZero() {
		return (flags & SPP_HB_TIME_IS_ZERO) == SPP_HB_TIME_IS_ZERO;
	}

	@Override
	public String toString() {
		return "sctp_spp_flags [flags=" + String.format("%8s", Long.toBinaryString(flags)).replace(" ", "0") + "]";
	}
	
}	
