// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.dns;

import javax.servlet.sip.SipURI;

/**  This is a set of tools method for making DNS SRV queries and checking */
public interface DNSHelper {
	// logger
	final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(com.alcatel.dns.DNSHelper.class);


	/**
	 *  enables host comparison through DNS SRV checking
	 *
	 *@param  uri          Description of the Parameter
	 *@param  myhost       Description of the Parameter
	 *@param  myport       Description of the Parameter
	 *@param  mytransport  Description of the Parameter
	 *@return              Description of the Return Value
	 */
	boolean matchHost(SipURI uri, String myhost, int myport, String mytransport);


	/**
	 *  Description of the Method
	 *
	 *@param  host1       Description of the Parameter
	 *@param  port1       Description of the Parameter
	 *@param  transport1  Description of the Parameter
	 *@param  host2       Description of the Parameter
	 *@param  port2       Description of the Parameter
	 *@param  transport2  Description of the Parameter
	 *@param  issecure    Description of the Parameter
	 *@return             Description of the Return Value
	 */
	boolean matchHost(String host1, int port1, String transport1, String host2, int port2, String transport2, boolean issecure);


	String NAPTR_TCP_SERVICE = "SIP+D2T";
	String NAPTR_TLS_SERVICE = "SIPS+D2T";
	String NAPTR_UDP_SERVICE = "SIP+D2U";
	String SRV_DEFAULT_UDP_REPLACEMENT_PREFIX = "_sip._udp.";
	String SRV_DEFAULT_TCP_REPLACEMENT_PREFIX = "_sip._tcp.";
	String SRV_DEFAULT_TLS_REPLACEMENT_PREFIX = "_sips._tcp.";
	String TLS = "TLS";
	String TCP = "TCP";
	String UDP = "UDP";

}

