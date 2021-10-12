// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.diameter.lb.impl.monitor.Monitor;
import com.alcatel.as.diameter.lb.impl.monitor.MonitorFactory;
import com.alcatel_lucent.as.management.annotation.stat.Counter;
import com.alcatel_lucent.as.management.annotation.stat.Gauge;
import com.alcatel_lucent.as.management.annotation.stat.Stat;

import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

import com.alcatel.as.service.concurrent.*;
import alcatel.tess.hometop.gateways.reactor.*;

public class Counters {
  
  private static final long EXACT = 1L;
  private static final long THOUSANDS = 1000L;
  private static final long MILLIONS = 1000000L;
  
  public static class Definition {
    private long _unit;
    private String _name, _meteringName, _toString;
    private Definition (String name, String meteringName, long unit){
      _name = name;
      _meteringName = meteringName;
      _unit = unit;
      _toString = new StringBuilder ().append ("Definition[").append (_name).append ('/').append (_meteringName).append (']').toString ();
    }
    @Override
    public String toString (){ return _toString;}
  }
  // used by gogo commands
  public static String getName (Meter m){
    Definition d = m.attachment ();
    return d._name;
  }
  // used by gogo commands
  public static String getRateName (Meter m){ // cannot attach an Object to a rate meter
    switch (m.getName ()){
    case "client:read.reqs.rate": return "CLIENT_IN_REQUESTS_RATE";
    case "client:read.resps.rate": return "CLIENT_IN_RESPONSES_RATE";
    case "server:read.reqs.rate": return "SERVER_IN_REQUESTS_RATE";
    case "server:read.resps.rate": return "SERVER_IN_RESPONSES_RATE";
    case "client:write.reqs.rate": return "CLIENT_OUT_REQUESTS_RATE";
    case "client:write.resps.rate": return "CLIENT_OUT_RESPONSES_RATE";
    case "server:write.reqs.rate": return "SERVER_OUT_REQUESTS_RATE";
    case "server:write.resps.rate": return "SERVER_OUT_RESPONSES_RATE";
    }
    return null;
  }
  
  public static final Logger LOGGER = Logger.getLogger("as.diameter.lb.stats");
  public static Definition UPTIME_DEF = new Definition ("UPTIME", MeteringConstants.SYSTEM_UPTIME, EXACT);
  
  public static Definition CLIENT_TCP_CONNECTIONS_DEF = new Definition ("CLIENT_TCP_CONNECTIONS", "client:connection.tcp", EXACT);
  public static Definition CLIENT_SCTP_CONNECTIONS_DEF = new Definition ("CLIENT_SCTP_CONNECTIONS", "client:connection.sctp", EXACT);
  public static Definition SERVER_TCP_CONNECTIONS_DEF = new Definition ("SERVER_TCP_CONNECTIONS", "connection.tcp", EXACT);
  public static Definition SERVER_LB_CONNECTIONS_DEF = new Definition ("SERVER_LB_CONNECTIONS", "lb.connection.tcp", EXACT);
  public static Definition CLIENT_SCTP_DISCONNECTIONS_DEF = new Definition ("CLIENT_SCTP_DISCONNECTIONS", "client:connection.sctp.event.disconnect", EXACT);
  public static Definition CLIENT_SCTP_UNREACHABLE_EVTS_DEF = new Definition ("CLIENT_SCTP_UNREACHABLE_EVTS", "client:connection.sctp.event.unreachable", EXACT);
  public static Definition CLIENT_TCP_DISCONNECTIONS_DEF = new Definition ("CLIENT_TCP_DISCONNECTIONS", "client:connection.tcp.event.disconnect", EXACT);
  
  public static Definition CLIENT_IN_BYTES_DEF = new Definition ("CLIENT_IN_BYTES", "client:read.bytes", EXACT);
  public static Definition CLIENT_IN_REQUESTS_DEF = new Definition ("CLIENT_IN_REQUESTS", "client:read.reqs", EXACT);
  public static Definition CLIENT_IN_RESPONSES_DEF = new Definition ("CLIENT_IN_RESPONSES", "client:read.resps", EXACT);
  public static Definition CLIENT_IN_CER_DEF = new Definition ("CLIENT_IN_CER", "client:read.cer", EXACT);
  public static Definition CLIENT_IN_DWR_DEF = new Definition ("CLIENT_IN_DWR", "client:read.dwr", EXACT);
  public static Definition CLIENT_IN_DPR_DEF = new Definition ("CLIENT_IN_DPR", "client:read.dpr", EXACT);
  public static Definition CLIENT_IN_CEA_DEF = new Definition ("CLIENT_IN_CEA", "client:read.cea", EXACT);
  public static Definition CLIENT_IN_DWA_DEF = new Definition ("CLIENT_IN_DWA", "client:read.dwa", EXACT);
  public static Definition CLIENT_IN_DPA_DEF = new Definition ("CLIENT_IN_DPA", "client:read.dpa", EXACT);
  
  public static Definition CLIENT_OUT_BYTES_DEF = new Definition ("CLIENT_OUT_BYTES", "client:write.cer", EXACT);
  public static Definition CLIENT_OUT_REQUESTS_DEF = new Definition ("CLIENT_OUT_REQUESTS", "client:write.reqs", EXACT);
  public static Definition CLIENT_OUT_RESPONSES_DEF = new Definition ("CLIENT_OUT_RESPONSES", "client:write.resps", EXACT);
  public static Definition CLIENT_OUT_CEA_DEF = new Definition ("CLIENT_OUT_CEA", "client:write.cea", EXACT);
  public static Definition CLIENT_OUT_DWA_DEF = new Definition ("CLIENT_OUT_DWA", "client:write.dwa", EXACT);
  public static Definition CLIENT_OUT_DPA_DEF = new Definition ("CLIENT_OUT_DPA", "client:write.dpa", EXACT);
  public static Definition CLIENT_OUT_CER_DEF = new Definition ("CLIENT_OUT_CER", "client:write.cer", EXACT);
  public static Definition CLIENT_OUT_DWR_DEF = new Definition ("CLIENT_OUT_DWR", "client:write.dwr", EXACT);
  public static Definition CLIENT_OUT_DPR_DEF = new Definition ("CLIENT_OUT_DPR", "client:write.dpr", EXACT);
  
  public static Definition SERVER_IN_BYTES_DEF = new Definition ("SERVER_IN_BYTES", "read.bytes", EXACT);
  public static Definition SERVER_IN_REQUESTS_DEF = new Definition ("SERVER_IN_REQUESTS", "read.reqs", EXACT);
  public static Definition SERVER_IN_RESPONSES_DEF = new Definition ("SERVER_IN_RESPONSES", "read.resps", EXACT);
  public static Definition SERVER_IN_CEA_DEF = new Definition ("SERVER_IN_CEA", "read.cea", EXACT);
  public static Definition SERVER_IN_DWA_DEF = new Definition ("SERVER_IN_DWA", "read.dwa", EXACT);
  public static Definition SERVER_IN_DPA_DEF = new Definition ("SERVER_IN_DPA", "read.dpa", EXACT);
  public static Definition SERVER_IN_CER_DEF = new Definition ("SERVER_IN_CER", "read.cer", EXACT);
  public static Definition SERVER_IN_DWR_DEF = new Definition ("SERVER_IN_DWR", "read.dwr", EXACT);
  public static Definition SERVER_IN_DPR_DEF = new Definition ("SERVER_IN_DPR", "read.dpr", EXACT);
  
  public static Definition SERVER_OUT_BYTES_DEF = new Definition ("SERVER_OUT_BYTES", "write.bytes", EXACT);
  public static Definition SERVER_OUT_REQUESTS_DEF = new Definition ("SERVER_OUT_REQUESTS", "write.reqs", EXACT);
  public static Definition SERVER_OUT_RESPONSES_DEF = new Definition ("SERVER_OUT_RESPONSES", "write.resps", EXACT);
  public static Definition SERVER_OUT_CER_DEF = new Definition ("SERVER_OUT_CER", "write.cer", EXACT);
  public static Definition SERVER_OUT_DWR_DEF = new Definition ("SERVER_OUT_DWR", "write.dwr", EXACT);
  public static Definition SERVER_OUT_DPR_DEF = new Definition ("SERVER_OUT_DPR", "write.dpr", EXACT);
  public static Definition SERVER_OUT_CEA_DEF = new Definition ("SERVER_OUT_CEA", "write.cea", EXACT);
  public static Definition SERVER_OUT_DWA_DEF = new Definition ("SERVER_OUT_DWA", "write.dwa", EXACT);
  public static Definition SERVER_OUT_DPA_DEF = new Definition ("SERVER_OUT_DPA", "write.dpa", EXACT);

  public static Definition CLIENT_IN_REQUESTS_RATE_DEF = new Definition (CLIENT_IN_REQUESTS_DEF._name+"_RATE", CLIENT_IN_REQUESTS_DEF._meteringName+".rate", EXACT);
  public static Definition CLIENT_IN_RESPONSES_RATE_DEF = new Definition (CLIENT_IN_RESPONSES_DEF._name+"_RATE", CLIENT_IN_RESPONSES_DEF._meteringName+".rate", EXACT);
  public static Definition CLIENT_OUT_REQUESTS_RATE_DEF = new Definition (CLIENT_OUT_REQUESTS_DEF._name+"_RATE", CLIENT_OUT_REQUESTS_DEF._meteringName+".rate", EXACT);
  public static Definition CLIENT_OUT_RESPONSES_RATE_DEF = new Definition (CLIENT_OUT_RESPONSES_DEF._name+"_RATE", CLIENT_OUT_RESPONSES_DEF._meteringName+".rate", EXACT);
  public static Definition SERVER_IN_REQUESTS_RATE_DEF = new Definition (SERVER_IN_REQUESTS_DEF._name+"_RATE", SERVER_IN_REQUESTS_DEF._meteringName+".rate", EXACT);
  public static Definition SERVER_IN_RESPONSES_RATE_DEF = new Definition (SERVER_IN_RESPONSES_DEF._name+"_RATE", SERVER_IN_RESPONSES_DEF._meteringName+".rate", EXACT);
  public static Definition SERVER_OUT_REQUESTS_RATE_DEF = new Definition (SERVER_OUT_REQUESTS_DEF._name+"_RATE", SERVER_OUT_REQUESTS_DEF._meteringName+".rate", EXACT);
  public static Definition SERVER_OUT_RESPONSES_RATE_DEF = new Definition (SERVER_OUT_RESPONSES_DEF._name+"_RATE", SERVER_OUT_RESPONSES_DEF._meteringName+".rate", EXACT);
  
  public static Counters AGGREGATED;

  private static PlatformExecutors _executors;
  private static TimerService _timerService;
  private static MeteringService _metering;
  private static BundleContext _osgi;
  public static void init (PlatformExecutors executors, TimerService timer, MeteringService metering, BundleContext osgi){
    _executors = executors;
    _timerService = timer;
    _metering = metering;
    _osgi = osgi;
    AGGREGATED = new Counters ("diameter.lb");
    AGGREGATED.start ("Total");
    _timerService.scheduleWithFixedDelay (_executors.getProcessingThreadPoolExecutor (), DUMP_RUNNABLE, 3000, 3000, java.util.concurrent.TimeUnit.MILLISECONDS);
  }
  
  private String _id;
  private SimpleMonitorable _monitorable;
  private boolean _clientCounters;
  
  private Counters (String id){
    this (id, null);
  }
  public Counters (String id, Counters aggregate){
    this (id, aggregate, true);
  }
  public Counters (String id, Counters aggregate, boolean clientCounters){
    _id = id;
    init (aggregate, clientCounters);
  }
  public String toString (){ return _id;}
  public Counters start (String description){
    _monitorable.setDescription (description);
    _monitorable.start (_osgi);
    return this;
  }
  public Counters stop (){
    if (_clientCounters){
      _monitorable.stop ();
    } else {
      _monitorable.removeMeter (METERS_ALL);
      _monitorable.updated ();
    }
    return this;
  }
  public Counters abort (){ // only for clientCounters - which were not yet started
    _monitorable.removeMeter (METERS_ALL);
    return this;
  }
  public Meter getMeter (Definition def){
    return MONITORS_BY_DEF.get (def);
  }
  public SimpleMonitorable getMonitorable (){ return _monitorable;}
  
  public Meter CLIENT_TCP_CONNECTIONS;
  public Meter CLIENT_SCTP_CONNECTIONS;
  public Meter SERVER_TCP_CONNECTIONS;
  public Meter SERVER_LB_CONNECTIONS;
  public Meter CLIENT_TCP_DISCONNECTIONS;
  public Meter CLIENT_SCTP_DISCONNECTIONS;
  public Meter CLIENT_SCTP_UNREACHABLE_EVTS;
  
  public Meter CLIENT_IN_BYTES;
  public Meter CLIENT_IN_REQUESTS;
  public Meter CLIENT_IN_RESPONSES;
  public Meter CLIENT_IN_CER;
  public Meter CLIENT_IN_DWR;
  public Meter CLIENT_IN_DPR;
  public Meter CLIENT_IN_CEA;
  public Meter CLIENT_IN_DWA;
  public Meter CLIENT_IN_DPA;
  
  public Meter CLIENT_OUT_BYTES;
  public Meter CLIENT_OUT_REQUESTS;
  public Meter CLIENT_OUT_RESPONSES;
  public Meter CLIENT_OUT_CEA;
  public Meter CLIENT_OUT_DWA;
  public Meter CLIENT_OUT_DPA;
  public Meter CLIENT_OUT_CER;
  public Meter CLIENT_OUT_DWR;
  public Meter CLIENT_OUT_DPR;
  
  public Meter SERVER_IN_BYTES;
  public Meter SERVER_IN_REQUESTS;
  public Meter SERVER_IN_RESPONSES;
  public Meter SERVER_IN_CEA;
  public Meter SERVER_IN_DWA;
  public Meter SERVER_IN_DPA;
  public Meter SERVER_IN_CER;
  public Meter SERVER_IN_DWR;
  public Meter SERVER_IN_DPR;
  
  public Meter SERVER_OUT_BYTES;
  public Meter SERVER_OUT_REQUESTS;
  public Meter SERVER_OUT_RESPONSES;
  public Meter SERVER_OUT_CER;
  public Meter SERVER_OUT_DWR;
  public Meter SERVER_OUT_DPR;
  public Meter SERVER_OUT_CEA;
  public Meter SERVER_OUT_DWA;
  public Meter SERVER_OUT_DPA;
  
  public Meter[] METERS_ALL;
  private Map<Definition, Meter> MONITORS_BY_DEF = new HashMap<Definition, Meter> ();

  private Meter createClientSideMeter (Definition def, Meter parent){
    String name = def._meteringName;
    Meter meter = _monitorable.createIncrementalMeter (_metering, name, parent);
    meter.attach (def);
    MONITORS_BY_DEF.put (def, meter);
    return meter;
  }
  private Meter createServerSideMeter (Definition def, Meter parent){
    String name = def._meteringName;
    if (_clientCounters){
      name = "server:*:"+name;
    } else {
      name = _id+":"+name;
    }
    Meter meter = _monitorable.createIncrementalMeter (_metering, name, parent);
    meter.attach (def);
    MONITORS_BY_DEF.put (def, meter);
    return meter;
  }
  private void init (Counters aggregate, boolean clientCounters){ // clientCounters=true for AGGREGATED, endpoint and client
    boolean aggregated = aggregate != null;
    _clientCounters = clientCounters;
    if (clientCounters)
      _monitorable = new SimpleMonitorable (_id, _id);
    else
      _monitorable = aggregate._monitorable;
    ArrayList<Meter> metersAll = new ArrayList<>();
    
    Meter uptimeMeter = null;
    if (clientCounters) _monitorable.addMeter (uptimeMeter = Meters.createUptimeMeter (_metering, UPTIME_DEF._meteringName));
    else _monitorable.addMeter (uptimeMeter = Meters.createUptimeMeter (_metering, _id+":"+UPTIME_DEF._meteringName));
    uptimeMeter.attach (UPTIME_DEF);
    metersAll.add (uptimeMeter);

    if (_clientCounters){
      metersAll.add (CLIENT_TCP_CONNECTIONS = createClientSideMeter (CLIENT_TCP_CONNECTIONS_DEF, aggregated ? aggregate.CLIENT_TCP_CONNECTIONS : null));
      metersAll.add (CLIENT_SCTP_CONNECTIONS = createClientSideMeter (CLIENT_SCTP_CONNECTIONS_DEF, aggregated ? aggregate.CLIENT_SCTP_CONNECTIONS : null));
      metersAll.add (SERVER_TCP_CONNECTIONS = createServerSideMeter (SERVER_TCP_CONNECTIONS_DEF, aggregated ? aggregate.SERVER_TCP_CONNECTIONS : null));
      metersAll.add (SERVER_LB_CONNECTIONS = createServerSideMeter (SERVER_LB_CONNECTIONS_DEF, aggregated ? aggregate.SERVER_LB_CONNECTIONS : null));
      metersAll.add (CLIENT_TCP_DISCONNECTIONS = createClientSideMeter (CLIENT_TCP_DISCONNECTIONS_DEF, aggregated ? aggregate.CLIENT_TCP_DISCONNECTIONS : null));
      metersAll.add (CLIENT_SCTP_DISCONNECTIONS = createClientSideMeter (CLIENT_SCTP_DISCONNECTIONS_DEF, aggregated ? aggregate.CLIENT_SCTP_DISCONNECTIONS : null));
      metersAll.add (CLIENT_SCTP_UNREACHABLE_EVTS = createClientSideMeter (CLIENT_SCTP_UNREACHABLE_EVTS_DEF, aggregated ? aggregate.CLIENT_SCTP_UNREACHABLE_EVTS : null));
      
      metersAll.add (CLIENT_IN_BYTES = createClientSideMeter(CLIENT_IN_BYTES_DEF, aggregated ? aggregate.CLIENT_IN_BYTES : null));
      metersAll.add (CLIENT_IN_REQUESTS = createClientSideMeter(CLIENT_IN_REQUESTS_DEF, aggregated ? aggregate.CLIENT_IN_REQUESTS : null));
      metersAll.add (CLIENT_IN_RESPONSES = createClientSideMeter(CLIENT_IN_RESPONSES_DEF, aggregated ? aggregate.CLIENT_IN_RESPONSES : null));
      metersAll.add (CLIENT_IN_CER = createClientSideMeter(CLIENT_IN_CER_DEF, aggregated ? aggregate.CLIENT_IN_CER : null));
      metersAll.add (CLIENT_IN_DWR = createClientSideMeter(CLIENT_IN_DWR_DEF, aggregated ? aggregate.CLIENT_IN_DWR : null));
      metersAll.add (CLIENT_IN_DPR = createClientSideMeter(CLIENT_IN_DPR_DEF, aggregated ? aggregate.CLIENT_IN_DPR : null));
      metersAll.add (CLIENT_IN_CEA = createClientSideMeter(CLIENT_IN_CEA_DEF, aggregated ? aggregate.CLIENT_IN_CEA : null));
      metersAll.add (CLIENT_IN_DWA = createClientSideMeter(CLIENT_IN_DWA_DEF, aggregated ? aggregate.CLIENT_IN_DWA : null));
      metersAll.add (CLIENT_IN_DPA = createClientSideMeter(CLIENT_IN_DPA_DEF, aggregated ? aggregate.CLIENT_IN_DPA : null));
      
      metersAll.add (CLIENT_OUT_BYTES = createClientSideMeter(CLIENT_OUT_BYTES_DEF, aggregated ? aggregate.CLIENT_OUT_BYTES : null));
      metersAll.add (CLIENT_OUT_REQUESTS = createClientSideMeter(CLIENT_OUT_REQUESTS_DEF, aggregated ? aggregate.CLIENT_OUT_REQUESTS : null));
      metersAll.add (CLIENT_OUT_RESPONSES = createClientSideMeter(CLIENT_OUT_RESPONSES_DEF, aggregated ? aggregate.CLIENT_OUT_RESPONSES : null));
      metersAll.add (CLIENT_OUT_CEA = createClientSideMeter(CLIENT_OUT_CEA_DEF, aggregated ? aggregate.CLIENT_OUT_CEA : null));
      metersAll.add (CLIENT_OUT_DWA = createClientSideMeter(CLIENT_OUT_DWA_DEF, aggregated ? aggregate.CLIENT_OUT_DWA : null));
      metersAll.add (CLIENT_OUT_DPA = createClientSideMeter(CLIENT_OUT_DPA_DEF, aggregated ? aggregate.CLIENT_OUT_DPA : null));
      metersAll.add (CLIENT_OUT_CER = createClientSideMeter(CLIENT_OUT_CER_DEF, aggregated ? aggregate.CLIENT_OUT_CER : null));
      metersAll.add (CLIENT_OUT_DWR = createClientSideMeter(CLIENT_OUT_DWR_DEF, aggregated ? aggregate.CLIENT_OUT_DWR : null));
      metersAll.add (CLIENT_OUT_DPR = createClientSideMeter(CLIENT_OUT_DPR_DEF, aggregated ? aggregate.CLIENT_OUT_DPR : null));
    }
    
    metersAll.add (SERVER_IN_BYTES = createServerSideMeter(SERVER_IN_BYTES_DEF, aggregated ? aggregate.SERVER_IN_BYTES : null));
    metersAll.add (SERVER_IN_REQUESTS = createServerSideMeter(SERVER_IN_REQUESTS_DEF, aggregated ? aggregate.SERVER_IN_REQUESTS : null));
    metersAll.add (SERVER_IN_RESPONSES = createServerSideMeter(SERVER_IN_RESPONSES_DEF, aggregated ? aggregate.SERVER_IN_RESPONSES : null));
    metersAll.add (SERVER_IN_CEA = createServerSideMeter(SERVER_IN_CEA_DEF, aggregated ? aggregate.SERVER_IN_CEA : null));
    metersAll.add (SERVER_IN_DWA = createServerSideMeter(SERVER_IN_DWA_DEF, aggregated ? aggregate.SERVER_IN_DWA : null));
    metersAll.add (SERVER_IN_DPA = createServerSideMeter(SERVER_IN_DPA_DEF, aggregated ? aggregate.SERVER_IN_DPA : null));
    metersAll.add (SERVER_IN_CER = createServerSideMeter(SERVER_IN_CER_DEF, aggregated ? aggregate.SERVER_IN_CER : null));
    metersAll.add (SERVER_IN_DWR = createServerSideMeter(SERVER_IN_DWR_DEF, aggregated ? aggregate.SERVER_IN_DWR : null));
    metersAll.add (SERVER_IN_DPR = createServerSideMeter(SERVER_IN_DPR_DEF, aggregated ? aggregate.SERVER_IN_DPR : null));
    
    metersAll.add (SERVER_OUT_BYTES = createServerSideMeter(SERVER_OUT_BYTES_DEF, aggregated ? aggregate.SERVER_OUT_BYTES : null));
    metersAll.add (SERVER_OUT_REQUESTS = createServerSideMeter(SERVER_OUT_REQUESTS_DEF, aggregated ? aggregate.SERVER_OUT_REQUESTS : null));
    metersAll.add (SERVER_OUT_RESPONSES = createServerSideMeter(SERVER_OUT_RESPONSES_DEF, aggregated ? aggregate.SERVER_OUT_RESPONSES : null));
    metersAll.add (SERVER_OUT_CER = createServerSideMeter(SERVER_OUT_CER_DEF, aggregated ? aggregate.SERVER_OUT_CER : null));
    metersAll.add (SERVER_OUT_DWR = createServerSideMeter(SERVER_OUT_DWR_DEF, aggregated ? aggregate.SERVER_OUT_DWR : null));
    metersAll.add (SERVER_OUT_DPR = createServerSideMeter(SERVER_OUT_DPR_DEF, aggregated ? aggregate.SERVER_OUT_DPR : null));
    metersAll.add (SERVER_OUT_CEA = createServerSideMeter(SERVER_OUT_CEA_DEF, aggregated ? aggregate.SERVER_OUT_CEA : null));
    metersAll.add (SERVER_OUT_DWA = createServerSideMeter(SERVER_OUT_DWA_DEF, aggregated ? aggregate.SERVER_OUT_DWA : null));
    metersAll.add (SERVER_OUT_DPA = createServerSideMeter(SERVER_OUT_DPA_DEF, aggregated ? aggregate.SERVER_OUT_DPA : null));
    
    if (_clientCounters){
      metersAll.add (IN_REQUESTS_RATE = newRate(CLIENT_IN_REQUESTS, CLIENT_IN_REQUESTS_RATE_DEF));
      metersAll.add (IN_RESPONSES_RATE = newRate(CLIENT_IN_RESPONSES, CLIENT_IN_RESPONSES_RATE_DEF));
      metersAll.add (OUT_REQUESTS_RATE = newRate(CLIENT_OUT_REQUESTS, CLIENT_OUT_REQUESTS_RATE_DEF));
      metersAll.add (OUT_RESPONSES_RATE = newRate(CLIENT_OUT_RESPONSES, CLIENT_OUT_RESPONSES_RATE_DEF));
    } else { // used when monitoring server side connections
      metersAll.add (IN_REQUESTS_RATE = newRate(SERVER_IN_REQUESTS, SERVER_IN_REQUESTS_RATE_DEF));
      metersAll.add (IN_RESPONSES_RATE = newRate(SERVER_IN_RESPONSES, SERVER_IN_RESPONSES_RATE_DEF));
      metersAll.add (OUT_REQUESTS_RATE = newRate(SERVER_OUT_REQUESTS, SERVER_OUT_REQUESTS_RATE_DEF));
      metersAll.add (OUT_RESPONSES_RATE = newRate(SERVER_OUT_RESPONSES, SERVER_OUT_RESPONSES_RATE_DEF));
    }

    METERS_ALL = metersAll.toArray (new Meter[metersAll.size ()]);
      
    if (clientCounters == false)
      _monitorable.updated ();
  }
  
  public void dump() {
    if (LOGGER.isInfoEnabled() == false)
      return;
    StringBuilder sb = new StringBuilder("\n");
    for (Meter meter : METERS_ALL){
      Definition def = meter.attachment ();
      sb.append (def._name).append ('=').append (meter.getValue ()).append ('\n');
    }
    LOGGER.info(sb.toString());
  }

  private Meter newRate (Meter meter, Definition rateDef){
    Meter rateMeter = Meters.createRateMeter (_metering, meter, 1000L);
    rateMeter.attach (rateDef);
    _monitorable.addMeter (rateMeter);
    return rateMeter;
  }
  
  public Meter IN_REQUESTS_RATE;
  public Meter IN_RESPONSES_RATE;
  public Meter OUT_REQUESTS_RATE;
  public Meter OUT_RESPONSES_RATE;
  public Meter[] RATES_ALL;
    
  public static int[] getStatistics() {
    return AGGREGATED.getInstanceStatistics ();
  }
  private int[] getInstanceStatistics (){
    return new int[] { (int)CLIENT_TCP_CONNECTIONS.getValue (), (int)CLIENT_SCTP_CONNECTIONS.getValue (),
		       (int)SERVER_TCP_CONNECTIONS.getValue (), getAsGb(CLIENT_IN_BYTES), getAs(CLIENT_IN_REQUESTS, MILLIONS),
		       getAs(CLIENT_IN_RESPONSES, MILLIONS), getAs(CLIENT_IN_CER, THOUSANDS),
		       getAs(CLIENT_IN_DWR, THOUSANDS), getAs(CLIENT_IN_DPR, THOUSANDS), getAs(CLIENT_IN_CEA, THOUSANDS),
		       getAs(CLIENT_IN_DWA, THOUSANDS), getAs(CLIENT_IN_DPA, THOUSANDS), getAsGb(CLIENT_OUT_BYTES),
		       getAs(CLIENT_OUT_REQUESTS, MILLIONS), getAs(CLIENT_OUT_RESPONSES, MILLIONS),
		       getAs(CLIENT_OUT_CER, THOUSANDS), getAs(CLIENT_OUT_DWR, THOUSANDS), getAs(CLIENT_OUT_DPR, THOUSANDS),
		       getAs(CLIENT_OUT_CEA, THOUSANDS), getAs(CLIENT_OUT_DWA, THOUSANDS), getAs(CLIENT_OUT_DPA, THOUSANDS),
		       getAsGb(SERVER_IN_BYTES), getAs(SERVER_IN_REQUESTS, MILLIONS), getAs(SERVER_IN_RESPONSES, MILLIONS),
		       getAs(SERVER_IN_CER, THOUSANDS), getAs(SERVER_IN_DWR, THOUSANDS), getAs(SERVER_IN_DPR, THOUSANDS),
		       getAs(SERVER_IN_CEA, THOUSANDS), getAs(SERVER_IN_DWA, THOUSANDS), getAs(SERVER_IN_DPA, THOUSANDS),
		       getAsGb(SERVER_OUT_BYTES), getAs(SERVER_OUT_REQUESTS, MILLIONS),
		       getAs(SERVER_OUT_RESPONSES, MILLIONS), getAs(SERVER_OUT_CER, THOUSANDS),
		       getAs(SERVER_OUT_DWR, THOUSANDS), getAs(SERVER_OUT_DPR, THOUSANDS), getAs(SERVER_OUT_CEA, THOUSANDS),
		       getAs(SERVER_OUT_DWA, THOUSANDS), getAs(SERVER_OUT_DPA, THOUSANDS),
		       (int)IN_REQUESTS_RATE.getValue (), (int)IN_RESPONSES_RATE.getValue (),
		       (int)OUT_REQUESTS_RATE.getValue (), (int)OUT_RESPONSES_RATE.getValue (),
		       (int)SERVER_LB_CONNECTIONS.getValue (), (int)CLIENT_TCP_DISCONNECTIONS.getValue (),
		       (int)CLIENT_SCTP_DISCONNECTIONS.getValue (), (int)CLIENT_SCTP_UNREACHABLE_EVTS.getValue ()
    };
  }
  
  private static int getAsGb(Meter counter) {
    long l = counter.getValue ();
    l >>= 30;
    return (int) l;
  }
  
  private static int getAs(AtomicLong counter, long seed) {
    long l = counter.get ();
    l /= seed;
    return (int) l;
  }
  
  private static int getAs(Meter monitor, long seed) {
    long l = monitor.getValue ();
    l /= seed;
    return (int) l;
  }
  
  public static Runnable DUMP_RUNNABLE = new Runnable() {
      public void run() {
	AGGREGATED.dump();
      }
    };

  @Stat(rootSnmpName = "alcatel.srd.a5350.DiameterLB", rootOid = { 637, 71, 6, 1120 })
  public static class Instance {
    
    @Gauge(index = 0, snmpName = "NumClientTcp", oid = 100, desc = "Client-side TCP connections")
    public int getNumClientTcp() {
      return (int)AGGREGATED.CLIENT_TCP_CONNECTIONS.getValue ();
    }
    
    @Gauge(index = 1, snmpName = "NumClientSctp", oid = 101, desc = "Client-side SCTP connections")
    public int getNumClientSctp() {
      return (int)AGGREGATED.CLIENT_SCTP_CONNECTIONS.getValue ();
    }
    
    @Gauge(index = 2, snmpName = "NumServerTcp", oid = 102, desc = "Server-side TCP connections")
    public int getNumServerTcp() {
      return (int)AGGREGATED.SERVER_TCP_CONNECTIONS.getValue ();
    }
    
    @Counter(index = 3, snmpName = "NumClientInBytes", oid = 110, desc = "Incoming Client-side Bytes (GB)")
    public int getNumClientInBytes() {
      return getAsGb(AGGREGATED.CLIENT_IN_BYTES);
    }
    
    @Counter(index = 4, snmpName = "NumClientInReqs", oid = 111,
	     desc = "Incoming Client-side Requests (Millions)")
    public int getNumClientInReqs() {
      return getAs(AGGREGATED.CLIENT_IN_REQUESTS, MILLIONS);
    }
    
    @Counter(index = 5, snmpName = "NumClientInResps", oid = 112,
	     desc = "Incoming Client-side Responses (Millions)")
    public int getNumClientInResps() {
      return getAs(AGGREGATED.CLIENT_IN_RESPONSES, MILLIONS);
    }
    
    @Counter(index = 6, snmpName = "NumClientInCER", oid = 113, desc = "Incoming Client-side CER (Thousands)")
    public int getNumClientInCER() {
      return getAs(AGGREGATED.CLIENT_IN_CER, THOUSANDS);
    }
    
    @Counter(index = 7, snmpName = "NumClientInDWR", oid = 114, desc = "Incoming Client-side DWR (Thousands)")
    public int getNumClientInDWR() {
      return getAs(AGGREGATED.CLIENT_IN_DWR, THOUSANDS);
    }
    
    @Counter(index = 8, snmpName = "NumClientInDPR", oid = 115, desc = "Incoming Client-side DPR (Thousands)")
    public int getNumClientInDPR() {
      return getAs(AGGREGATED.CLIENT_IN_DPR, THOUSANDS);
    }
    
    @Counter(index = 9, snmpName = "NumClientInCEA", oid = 116, desc = "Incoming Client-side CEA (Thousands)")
    public int getNumClientInCEA() {
      return getAs(AGGREGATED.CLIENT_IN_CEA, THOUSANDS);
    }
    
    @Counter(index = 10, snmpName = "NumClientInDWA", oid = 117,
	     desc = "Incoming Client-side DWA (Thousands)")
    public int getNumClientInDWA() {
      return getAs(AGGREGATED.CLIENT_IN_DWA, THOUSANDS);
    }
    
    @Counter(index = 11, snmpName = "NumClientInDPA", oid = 118,
	     desc = "Incoming Client-side DPA (Thousands)")
    public int getNumClientInDPA() {
      return getAs(AGGREGATED.CLIENT_IN_DPA, THOUSANDS);
    }
    
    @Counter(index = 12, snmpName = "NumClientOutBytes", oid = 120,
	     desc = "Outgoing Client-side Bytes (in GB)")
    public int getNumClientOutBytes() {
      return getAsGb(AGGREGATED.CLIENT_OUT_BYTES);
    }
    
    @Counter(index = 13, snmpName = "NumClientOutReqs", oid = 121,
	     desc = "Outgoing Client-side Requests (Millions)")
    public int getNumClientOutReqs() {
      return getAs(AGGREGATED.CLIENT_OUT_REQUESTS, MILLIONS);
    }
    
    @Counter(index = 14, snmpName = "NumClientOutResps", oid = 122,
	     desc = "Outgoing Client-side Responses (Millions)")
    public int getNumClientOutResps() {
      return getAs(AGGREGATED.CLIENT_OUT_RESPONSES, MILLIONS);
    }
    
    @Counter(index = 15, snmpName = "NumClientOutCER", oid = 126,
	     desc = "Outgoing Client-side CER (Thousands)")
    public int getNumClientOutCER() {
      return getAs(AGGREGATED.CLIENT_OUT_CER, THOUSANDS);
    }
    
    @Counter(index = 16, snmpName = "NumClientOutDWR", oid = 127,
	     desc = "Outgoing Client-side DWR (Thousands)")
    public int getNumClientOutDWR() {
      return getAs(AGGREGATED.CLIENT_OUT_DWR, THOUSANDS);
    }
    
    @Counter(index = 17, snmpName = "NumClientOutDPR", oid = 128,
	     desc = "Outgoing Client-side DPR (Thousands)")
    public int getNumClientOutDPR() {
      return getAs(AGGREGATED.CLIENT_OUT_DPR, THOUSANDS);
    }
    
    @Counter(index = 18, snmpName = "NumClientOutCEA", oid = 123,
	     desc = "Outgoing Client-side CEA (Thousands)")
    public int getNumClientOutCEA() {
      return getAs(AGGREGATED.CLIENT_OUT_CEA, THOUSANDS);
    }
    
    @Counter(index = 19, snmpName = "NumClientOutDWA", oid = 124,
	     desc = "Outgoing Client-side DWA (Thousands)")
    public int getNumClientOutDWA() {
      return getAs(AGGREGATED.CLIENT_OUT_DWA, THOUSANDS);
    }
    
    @Counter(index = 20, snmpName = "NumClientOutDPA", oid = 125,
	     desc = "Outgoing Client-side DPA (Thousands)")
    public int getNumClientOutDPA() {
      return getAs(AGGREGATED.CLIENT_OUT_DPA, THOUSANDS);
    }
    
    @Counter(index = 21, snmpName = "NumServerInBytes", oid = 130,
	     desc = "Incoming Server-side Bytes (in GB)")
    public int getNumServerInBytes() {
      return getAsGb(AGGREGATED.SERVER_IN_BYTES);
    }
    
    @Counter(index = 22, snmpName = "NumServerInReqs", oid = 131,
	     desc = "Incoming Server-side Requests (Millions)")
    public int getNumServerInReqs() {
      return getAs(AGGREGATED.SERVER_IN_REQUESTS, MILLIONS);
    }
    
    @Counter(index = 23, snmpName = "NumServerInResps", oid = 132,
	     desc = "Incoming Server-side Responses (Millions)")
    public int getNumServerInResps() {
      return getAs(AGGREGATED.SERVER_IN_RESPONSES, MILLIONS);
    }
    
    @Counter(index = 24, snmpName = "NumServerInCER", oid = 136,
	     desc = "Incoming Server-side CER (Thousands)")
    public int getNumServerInCER() {
      return getAs(AGGREGATED.SERVER_IN_CER, THOUSANDS);
    }
    
    @Counter(index = 25, snmpName = "NumServerInDWR", oid = 137,
	     desc = "Incoming Server-side DWR (Thousands)")
    public int getNumServerInDWR() {
      return getAs(AGGREGATED.SERVER_IN_DWR, THOUSANDS);
    }
    
    @Counter(index = 26, snmpName = "NumServerInDPR", oid = 138,
	     desc = "Incoming Server-side DPR (Thousands)")
    public int getNumServerInDPR() {
      return getAs(AGGREGATED.SERVER_IN_DPR, THOUSANDS);
    }
    
    @Counter(index = 27, snmpName = "NumServerInCEA", oid = 133,
	     desc = "Incoming Server-side CEA (Thousands)")
    public int getNumServerInCEA() {
      return getAs(AGGREGATED.SERVER_IN_CEA, THOUSANDS);
    }
    
    @Counter(index = 28, snmpName = "NumServerInDWA", oid = 134,
	     desc = "Incoming Server-side DWA (Thousands)")
    public int getNumServerInDWA() {
      return getAs(AGGREGATED.SERVER_IN_DWA, THOUSANDS);
    }
    
    @Counter(index = 29, snmpName = "NumServerInDPA", oid = 135,
	     desc = "Incoming Server-side DPA (Thousands)")
    public int getNumServerInDPA() {
      return getAs(AGGREGATED.SERVER_IN_DPA, THOUSANDS);
    }
    
    @Counter(index = 30, snmpName = "NumServerOutBytes", oid = 140,
	     desc = "Outgoing Server-side Bytes (in GB)")
    public int getNumServerOutBytes() {
      return getAsGb(AGGREGATED.SERVER_OUT_BYTES);
    }
    
    @Counter(index = 31, snmpName = "NumServerOutReqs", oid = 141,
	     desc = "Outgoing Server-side Requests (Millions)")
    public int getNumServerOutReqs() {
      return getAs(AGGREGATED.SERVER_OUT_REQUESTS, MILLIONS);
    }
    
    @Counter(index = 32, snmpName = "NumServerOutResps", oid = 142,
	     desc = "Outgoing Server-side Responses (Millions)")
    public int getNumServerOutResps() {
      return getAs(AGGREGATED.SERVER_OUT_RESPONSES, MILLIONS);
    }
    
    @Counter(index = 33, snmpName = "NumServerOutCER", oid = 143,
	     desc = "Outgoing Server-side CER (Thousands)")
    public int getNumServerOutCER() {
      return getAs(AGGREGATED.SERVER_OUT_CER, THOUSANDS);
    }
    
    @Counter(index = 34, snmpName = "NumServerOutDWR", oid = 144,
	     desc = "Outgoing Server-side DWR (Thousands)")
    public int getNumServerOutDWR() {
      return getAs(AGGREGATED.SERVER_OUT_DWR, THOUSANDS);
    }
    
    @Counter(index = 35, snmpName = "NumServerOutDPR", oid = 145,
	     desc = "Outgoing Server-side DPR (Thousands)")
    public int getNumServerOutDPR() {
      return getAs(AGGREGATED.SERVER_OUT_DPR, THOUSANDS);
    }
    
    @Counter(index = 36, snmpName = "NumServerOutCEA", oid = 146,
	     desc = "Outgoing Server-side CEA (Thousands)")
    public int getNumServerOutCEA() {
      return getAs(AGGREGATED.SERVER_OUT_CEA, THOUSANDS);
    }
    
    @Counter(index = 37, snmpName = "NumServerOutDWA", oid = 147,
	     desc = "Outgoing Server-side DWA (Thousands)")
    public int getNumServerOutDWA() {
      return getAs(AGGREGATED.SERVER_OUT_DWA, THOUSANDS);
    }
    
    @Counter(index = 38, snmpName = "NumServerOutDPA", oid = 148,
	     desc = "Outgoing Server-side DPA (Thousands)")
    public int getNumServerOutDPA() {
      return getAs(AGGREGATED.SERVER_OUT_DPA, THOUSANDS);
    }
    
    @Gauge(index = 39, snmpName = "NumClientInReqsRate", oid = 150,
	   desc = "Incoming Client-side Requests Rate (msg/sec)")
    public int getNumClientInReqsRate() {
      return (int) AGGREGATED.IN_REQUESTS_RATE.getValue ();
    }
    
    @Gauge(index = 40, snmpName = "NumClientInRespsRate", oid = 151,
	   desc = "Incoming Client-side Responses Rate (msg/sec)")
    public int getNumClientInRespsRate() {
      return (int) AGGREGATED.IN_RESPONSES_RATE.getValue ();
    }
    
    @Gauge(index = 41, snmpName = "NumClientOutReqsRate", oid = 160,
	   desc = "Outgoing Client-side Requests Rate (msg/sec)")
    public int getNumClientOutReqsRate() {
      return (int)AGGREGATED.OUT_REQUESTS_RATE.getValue ();
    }
    
    @Gauge(index = 42, snmpName = "NumClientOutRespsRate", oid = 161,
	   desc = "Outgoing Client-side Responses Rate (msg/sec)")
    public int getNumClientOutRespsRate() {
      return (int) AGGREGATED.OUT_RESPONSES_RATE.getValue ();
    }

    @Gauge(index = 43, snmpName = "NumServerLBTcp", oid = 170, desc = "Server-side Remote LB connections")
    public int getNumServerLBTcp() {
      return (int)AGGREGATED.SERVER_LB_CONNECTIONS.getValue ();
    }

    @Counter(index = 44, snmpName = "NumClientTcpDisc", oid = 171, desc = "Client-side TCP disconnections")
    public int getNumClientTcpDisc() {
      return (int)AGGREGATED.CLIENT_TCP_DISCONNECTIONS.getValue ();
    }

    @Counter(index = 45, snmpName = "NumClientSctpDisc", oid = 172, desc = "Client-side SCTP disconnections")
    public int getNumClientSctpDisc() {
      return (int)AGGREGATED.CLIENT_SCTP_DISCONNECTIONS.getValue ();
    }

    @Counter(index = 46, snmpName = "NumClientSctpUnreach", oid = 173, desc = "Client-side SCTP address unreachable events")
    public int getNumClientSctpUnreach() {
      return (int)AGGREGATED.CLIENT_SCTP_UNREACHABLE_EVTS.getValue ();
    }
  }
}
