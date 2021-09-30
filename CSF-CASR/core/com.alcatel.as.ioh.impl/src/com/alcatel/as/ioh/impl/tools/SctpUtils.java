package com.alcatel.as.ioh.impl.tools;

import static com.alcatel.as.ioh.server.SctpServer.*;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import com.alcatel.as.ioh.impl.conf.Property;
import com.alcatel.as.util.sctp.SctpSocketOption;
import com.alcatel.as.util.sctp.SctpSocketParam;
import com.alcatel.as.util.sctp.sctp_boolean;
import com.alcatel.as.util.sctp.sctp_assoc_value;
import com.alcatel.as.util.sctp.sctp_assocparams;
import com.alcatel.as.util.sctp.sctp_initmsg;
import com.alcatel.as.util.sctp.sctp_paddrparams;
import com.alcatel.as.util.sctp.sctp_rtoinfo;
import com.alcatel.as.util.sctp.sctp_sack_info;
import com.alcatel.as.util.sctp.sctp_spp_flags;

public class SctpUtils {
	public static Map<SctpSocketOption, SctpSocketParam> createSctpOptions (Map<String, Object> config) {
		return createSctpOptions(config, "");
	}
	
	public static Map<SctpSocketOption, SctpSocketParam> createSctpOptions (Map<String, Object> config, String prefix) {
		Map<SctpSocketOption, SctpSocketParam> sctpOptions = new HashMap<>();
		
		/** SCTP_RTOINFO **/
		int rtoInit = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_RTO_INIT, config, 0, false);
		int rtoMin = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_RTO_MIN, config, 0, false);
		int rtoMax = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_RTO_MAX, config, 0, false);
		if(rtoInit != 0 || rtoMin != 0 || rtoMax != 0)
			sctpOptions.put(SctpSocketOption.SCTP_RTOINFO, new sctp_rtoinfo(rtoInit, rtoMax, rtoMin));

		/** SCTP_MAX_BURST **/
		int maxBurst = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_MAX_BURST, config, -1, false);
		if(maxBurst != -1)
		    sctpOptions.put(SctpSocketOption.SCTP_MAX_BURST, new sctp_assoc_value(maxBurst));
		
		/** SCTP_ASSOCINFO **/
		int cookieLife = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_COOKIE_LIFE, config, 0, false);
		int assocMaxRt = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_ASSOC_MAX_RXT, config, 0, false);
		if (cookieLife != 0 || assocMaxRt != 0)
			sctpOptions.put(SctpSocketOption.SCTP_ASSOCINFO, new sctp_assocparams(assocMaxRt, cookieLife));
		
		/** SCTP_PADDRPARAMS **/
		int pathMaxRt = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_PATH_MAX_RXT, config, 0, false);
		int hbInterval = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_HB_INTERVAL, config, -1, false); //hbinterval can be set to 0
		int pathMTU = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_PATH_MTU, config, 0, false);
		boolean hbEnable = Property.getBooleanProperty(prefix + PROP_SCTP_SOCK_OPT_HB_ENABLE, config, true, false);
		boolean pmtudEnable = Property.getBooleanProperty(prefix + PROP_SCTP_SOCK_OPT_PMTUD_ENABLE, config, true, false);
		boolean sackDelayEnable = Property.getBooleanProperty(prefix + PROP_SCTP_SOCK_OPT_SACK_DELAY_ENABLE, config, true, false);
		if(pathMaxRt != 0 || hbInterval != -1 || pathMTU != 0 || !hbEnable || !pmtudEnable || !sackDelayEnable) {
		    InetSocketAddress spp_address = new InetSocketAddress(0);
		    int spp_hbinterval = hbInterval == -1 ? 0 : hbInterval;
		    int spp_sackdelay = 0;

		    sctp_spp_flags spp_flags = new sctp_spp_flags(hbEnable, 
								      					  false,
								      					  pmtudEnable,
								      					  sackDelayEnable,
								      					  (hbInterval == 0));
		    
		    sctpOptions.put(SctpSocketOption.SCTP_PEER_ADDR_PARAMS, 
		    				new sctp_paddrparams(spp_address, spp_hbinterval, pathMaxRt, pathMTU, spp_sackdelay, spp_flags));
		}
		
		/** SCTP_DISABLEFRAGMENTS **/
		if(config.containsKey(prefix + PROP_SCTP_SOCK_OPT_DISABLE_FRAGMENTS)) {
			boolean disableFragments = Property.getBooleanProperty(prefix + PROP_SCTP_SOCK_OPT_DISABLE_FRAGMENTS, config, false, false);
			sctpOptions.put(SctpSocketOption.SCTP_DISABLEFRAGMENTS, sctp_boolean.getSctpBoolean (disableFragments));
		}
		
		/** SCTP_DELAYED_SACK **/
		int sackDelay = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_SACK_DELAY, config, 0, false);
		int sackFreq = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_SACK_FREQ, config, 0, false);
		if(sackDelay != 0 || sackFreq != 0)
			sctpOptions.put(SctpSocketOption.SCTP_DELAYED_SACK, new sctp_sack_info(sackDelay, sackFreq));
		
		/** SCTP_INITMSG **/
		int maxInitRxt = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_MAX_INIT_RXT, config, 0, false);
		int maxInitTimeout = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_MAX_INIT_TIMEOUT, config, 0, false);
		int maxInStreams = Property.getIntProperty(prefix + PROP_STREAM_IN, config, 0, false);
		int maxOutStreams = Property.getIntProperty(prefix + PROP_STREAM_OUT, config, 0, false);
		if(maxInitRxt != 0 || maxInitTimeout != 0 || maxInStreams != 0 || maxOutStreams != 0)
			sctpOptions.put(SctpSocketOption.SCTP_INITMSG, new sctp_initmsg(maxOutStreams, maxInStreams, maxInitRxt, maxInitTimeout));
		
		/** SCTP_MAXSEG **/
		int maxSeg = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_MAX_SEG, config, -1, false);
		if(maxSeg != -1)
		    sctpOptions.put(SctpSocketOption.SCTP_MAXSEG, new sctp_assoc_value(maxSeg));
		
		/** SCTP_FRAGMENT_INTERLEAVE **/
		if(config.containsKey(prefix + PROP_SCTP_SOCK_OPT_FRAGMENT_INTERLEAVE)) {
			boolean fragmentInterleave = Property.getBooleanProperty(prefix + PROP_SCTP_SOCK_OPT_FRAGMENT_INTERLEAVE, config, false, false);
			sctpOptions.put(SctpSocketOption.SCTP_FRAGMENT_INTERLEAVE, sctp_boolean.getSctpBoolean (fragmentInterleave));
		}
		
		/** SCTP_PARTIAL_DELIVERY_POINT **/
		int partialDeliveryPoint = Property.getIntProperty(prefix + PROP_SCTP_SOCK_OPT_PARTIAL_DELIVERY_POINT, config, -1, false);
		if(partialDeliveryPoint != -1)
			sctpOptions.put(SctpSocketOption.SCTP_PARTIAL_DELIVERY_POINT, new sctp_assoc_value ((long) partialDeliveryPoint));

		/** SCTP_SO_REUSEADDR **/
		if(config.containsKey(prefix + PROP_SCTP_SOCK_OPT_REUSEADDR)) {
			boolean reuseaddr = Property.getBooleanProperty(prefix + PROP_SCTP_SOCK_OPT_REUSEADDR, config, false, false);
			sctpOptions.put(SctpSocketOption.SCTP_SO_REUSEADDR, sctp_boolean.getSctpBoolean (reuseaddr));
		}
		
		return sctpOptions;
	}
}
