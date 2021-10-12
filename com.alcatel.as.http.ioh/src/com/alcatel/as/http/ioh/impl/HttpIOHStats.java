// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http.ioh.impl;

import com.alcatel.as.service.metering2.*;
import com.alcatel.as.util.config.*;
import com.alcatel_lucent.as.management.annotation.stat.*;

import org.osgi.framework.BundleContext;

import java.util.*;

@Stat(rootSnmpName = "alcatel.srd.a5350.HttpIOH", rootOid = { 637, 71, 6, 150 })
public class HttpIOHStats implements StatProvider {
    
    private HttpIOHEngine _engine;

    public HttpIOHStats (HttpIOHEngine engine){
	_engine = engine;
    }
    public HttpIOHStats register (BundleContext ctx){
	Dictionary props = new Hashtable ();
	props.put (ConfigConstants.MODULE_NAME, "HttpIOH/"+_engine.name ());
	ctx.registerService (StatProvider.class.getName (), this, props);
	return this;
    }
    
    public Monitorable getMonitorable() {
	return _engine.getIOHMeters ();
    }

    @Gauge(desc="http requests/second", snmpName = "NumReqSec", oid = 100)
    public final static String NumReqSec = "read.req.rate";
    
    @Gauge(desc="websocket incoming bytes/second", snmpName= "NumWsIncomingSec", oid = 139)
    public final static String NumWsIncomingSec = "read.tcp.ws.rate";
    
    @Gauge(desc="websocket outgoing bytes/second", snmpName= "NumWsOutgoingSec", oid = 140)
    public final static String NumWsOutgoingSec = "write.tcp.ws.rate";
    
    @Gauge(desc="regular clients", snmpName= "NumHttpClients", oid = 102)
    public final static String NumHttpClients = "channel.open.tcp.accept";
    
    @Gauge(desc="mux clients", snmpName= "NumMuxClients", oid = 103, defaultValue = 0)
    public final static String NumMuxClients = null;
    
    @Gauge(desc="http agents", snmpName= "NumAgents", oid = 104)
    public final static String NumAgents = "agent";
    
    @Counter(desc="socket cache hit", snmpName= "SockCacheHit", oid = 105, defaultValue = 0)
    public final static String SockCacheHit = null;
    
    @Counter(desc="socket cache lookup", snmpName= "SockCacheLookup", oid = 106, defaultValue = 0)
    public final static String SockCacheLookup = null;
    
    @Counter(desc="ssl socket cache hit", snmpName= "SslSockCacheHit", oid = 107, defaultValue = 0)
    public final static String SslSockCacheHit = null;
    
    @Counter(desc="ssl socket cache lookup", snmpName= "SslSockCacheLookup", oid = 108, defaultValue = 0)
    public final static String SslSockCacheLookup = null;
    
    @Counter(desc="ssl session cache hit", snmpName= "SslSessCacheHit", oid = 109, defaultValue = 0)
    public final static String SslSessCacheHit = null;
    
    @Counter(desc="ssl session cache lookup", snmpName= "SslSessCacheLookup", oid = 110, defaultValue = 0)
    public final static String SslSessCacheLookup = null;
    
    @Counter(desc="101 switching protocols", snmpName= "NumHttpReply101", oid = 141)
    public final static String NumHttpReply101 = "write.resp.101";
    
    @Counter(desc="200 success", snmpName= "NumHttpReply200", oid = 111)
    public final static String NumHttpReply200 = "write.resp.200";
    
    @Counter(desc="201 created", snmpName= "NumHttpReply201", oid = 112)
    public final static String NumHttpReply201 = "write.resp.201";
    
    @Counter(desc="202 accepted", snmpName= "NumHttpReply202", oid = 113)
    public final static String NumHttpReply202 = "write.resp.202";
    
    @Counter(desc="203 provional", snmpName= "NumHttpReply203", oid = 114)
    public final static String NumHttpReply203 = "write.resp.203";
    
    @Counter(desc="204 no content", snmpName= "NumHttpReply204", oid = 115)
    public final static String NumHttpReply204 = "write.resp.204";
    
    @Counter(desc="300 multiple choise", snmpName= "NumHttpReply300", oid = 116)
    public final static String NumHttpReply300 = "write.resp.300";
    
    @Counter(desc="301 moved permanently", snmpName= "NumHttpReply301", oid = 117)
    public final static String NumHttpReply301 = "write.resp.301";
    
    @Counter(desc="302 moved temporarily", snmpName= "NumHttpReply302", oid = 118)
    public final static String NumHttpReply302 = "write.resp.302";
    
    @Counter(desc="304 not modified", snmpName= "NumHttpReply304", oid = 119)
    public final static String NumHttpReply304 = "write.resp.304";
    
    @Counter(desc="400 bad request", snmpName= "NumHttpReply400", oid = 120)
    public final static String NumHttpReply400 = "write.resp.400";
    
    @Counter(desc="401 unauthorize", snmpName= "NumHttpReply401", oid = 121)
    public final static String NumHttpReply401 = "write.resp.401";
    
    @Counter(desc="402 payment required", snmpName= "NumHttpReply402", oid = 122)
    public final static String NumHttpReply402 = "write.resp.402";
    
    @Counter(desc="403 forbidden", snmpName= "NumHttpReply403", oid = 123)
    public final static String NumHttpReply403 = "write.resp.403";
    
    @Counter(desc="404 not found", snmpName= "NumHttpReply404", oid = 124)
    public final static String NumHttpReply404 = "write.resp.404";
    
    @Counter(desc="405 method not allowed", snmpName= "NumHttpReply405", oid = 125)
    public final static String NumHttpReply405 = "write.resp.405";
    
    @Counter(desc="406 none acceptable", snmpName= "NumHttpReply406", oid = 126)
    public final static String NumHttpReply406 = "write.resp.406";
    
    @Counter(desc="407 proxy authentication required", snmpName= "NumHttpReply407", oid = 127)
    public final static String NumHttpReply407 = "write.resp.407";
    
    @Counter(desc="408 request timeout", snmpName= "NumHttpReply408", oid = 128)
    public final static String NumHttpReply408 = "write.resp.408";
    
    @Counter(desc="409 conflict", snmpName= "NumHttpReply409", oid = 129)
    public final static String NumHttpReply409 = "write.resp.409";
    
    @Counter(desc="410 gone", snmpName= "NumHttpReply410", oid = 130)
    public final static String NumHttpReply410 = "write.resp.410";
    
    @Counter(desc="413 entity too long", snmpName= "NumHttpReply413", oid = 131)
    public final static String NumHttpReply413 = "write.resp.413";
    
    @Counter(desc="414 uri too long", snmpName= "NumHttpReply414", oid = 132)
    public final static String NumHttpReply414 = "write.resp.414";
    
    @Counter(desc="500 internal server error", snmpName= "NumHttpReply500", oid = 133)
    public final static String NumHttpReply500 = "write.resp.500";
    
    @Counter(desc="501 not implemented", snmpName= "NumHttp501", oid = 134)
    public final static String NumHttp501 = "write.resp.501";
    
    @Counter(desc="502 bad gateway", snmpName= "NumHttpReply502", oid = 135)
    public final static String NumHttpReply502 = "write.resp.502";
    
    @Counter(desc="503 service unavailable", snmpName= "NumHttpReply503", oid = 136)
    public final static String NumHttpReply503 = "write.resp.503";
    
    @Counter(desc="504 gateway timeout", snmpName= "NumHttpReply504", oid = 137)
    public final static String NumHttpReply504 = "write.resp.504";
    
    @Counter(desc="505 bad version", snmpName= "NumHttpReply505", oid = 138)
    public final static String NumHttpReply505 = "write.resp.505";
    
}
