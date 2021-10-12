// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.httpgw;

import com.nextenso.proxylet.diameter.*;

import java.util.List;

public class ClientPeer implements DiameterPeer {

    private java.util.Map<Object, Object> _attributes;

    private String _originHost, _originRealm, _port, _proto;
    private List<String> _ips;
    
    public ClientPeer (String originHost, String orginRealm,
		       List<String> ips, String port, String proto
		       ){
	_originHost = originHost;
	_originRealm = orginRealm;
	_ips = ips;
	_port = port;
	_proto = proto != null ? proto.toLowerCase () : null;
    }

    public String toString (){
	return new StringBuilder ()
	    .append ("H2ClientPeer[")
	    .append (_originHost).append ('@').append (_originRealm)
	    .append (" ips=").append (_ips)
	    .append (" port=").append (_port)
	    .append (" proto=").append (_proto)
	    .append (']')
	    .toString ();
    }
    
    public long getId(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public String getLocalDiameterPeerName (){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public List<DiameterApplication> getSupportedApplications(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public void setSupportedApplications(List<DiameterApplication> applications){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public void setRelay(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public boolean isRelay(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}

    public boolean isLocalDiameterPeer(){ return false;}
    public String getOriginHost(){ return _originHost;}
    public String getOriginRealm(){ return _originRealm;}

    public byte[][] getHostIPAddresses(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public long getVendorId(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public String getProductName(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public long getOriginStateId(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public long[] getSupportedVendorIds(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public long[] getAuthApplications(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public long[] getAcctApplications(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public long[] getVendorSpecificApplications(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public List<DiameterApplication> getSpecificApplications(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public long[] getInbandSecurityId(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public long getFirmwareRevision(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}

    public String getHost(){ return _ips.get (0);}
    public List<String> getHosts(){ return _ips;}
    public List<String> getConfiguredHosts(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public int getPort(){
	if (_port == null) return 0;
	return Integer.parseInt (_port);
    }
    public boolean isEncrypted(){
	if (_proto == null) return false; // no better choice
	switch (_proto){
	case "tcp":
	case "sctp": return false;
	default: return true;
	}
    }
    public void disconnect(int disconnectCause){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public void connect(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public void connect(int localPort, String... localIPs){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public boolean isConnected(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public boolean isLocalInitiator(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public DiameterPeer getLocalDiameterPeer(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public void addListener(DiameterPeerListener listener){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public void removeListener(DiameterPeerListener listener){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public void setRetryTimeout(Integer seconds){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public void setRetryTimeoutInMs(Integer milliseconds){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public Integer getRetryTimeout(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public Integer getRetryTimeoutInMs(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public void setNbRetries(Integer nb){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public Integer getNbRetries(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public Protocol getProtocol(){
	if (_proto == null) return Protocol.TCP; // no better choice
	switch (_proto){
	case "tcp":
	case "tls": return Protocol.TCP;
	default: return Protocol.SCTP;
	}
    }
    public Long getQuarantineDelay(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public void setQuarantineDelay(Long delayInMs){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public boolean isQuarantined(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public void quarantine(){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public DiameterAVP[] getCapabilitiesExchangeAVPs (){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public void setSctpSocketOptions (java.util.Map options){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    public java.util.Map<Object, Object> getAttributes (){ // best effort...
	if (_attributes == null) _attributes = new java.util.concurrent.ConcurrentHashMap<> ();
	return _attributes;
    }
    public void setParameters (java.util.Map<String, String> params){ throw new RuntimeException ("Method Not Implemented in H2 mode");}
    
}
