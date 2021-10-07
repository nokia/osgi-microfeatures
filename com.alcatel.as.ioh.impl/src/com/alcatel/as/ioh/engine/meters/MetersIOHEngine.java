package com.alcatel.as.ioh.engine.meters;

import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.tools.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.tools.ChannelWriter.SendBufferMonitor;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel.as.service.discovery.*;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;
import com.nextenso.mux.*;
import com.nextenso.mux.util.MuxIdentification;

public class MetersIOHEngine extends IOHEngine {

    public static final int FLAG_GET_METERS = 50;
    public static final int FLAG_GET_METERS_IDENTICAL = 51;
    public static final int FLAG_VALUE = 61;
    public static final int FLAG_CREATE_MONITORABLE = 62;
    public static final int FLAG_CREATE_MONITORABLE_IDENTICAL = 63;
    public static final int FLAG_CREATE_METER = 64;
    public static final int FLAG_DESTROY_MONITORABLE = 65;
    public static final int FLAG_DESTROY_METER = 66;
    
    public static final AtomicInteger _seedMeter = new AtomicInteger (0);
    public static final AtomicInteger _seedMon = new AtomicInteger (0);

    public MetersIOHEngine (String name, IOHServices services){
	super (name, services);
    }
    
    public IOHEngine init (TcpServer server){
	server.getProperties ().put (PROP_TCP, "false");
	server.getProperties ().put (PROP_UDP, "false");
	server.getProperties ().put (PROP_SCTP, "false");
	server.getProperties ().put (PROP_EXT_SERVER_MIN, "0");
	super.init (server);
	return this;
    }
    @Override
    public void initMuxClient (MuxClient agent){
	super.initMuxClient (agent);
	agent.setContext (new MuxContext (agent)); // dont do it in agentConnected --> mux data may arrive in parallel
    }
    @Override
    public void resetMuxClient (MuxClient agent){
	super.resetMuxClient (agent);
	if (_logger.isInfoEnabled ()) _logger.info (this+" : "+agent+" : closed");
	MuxContext ctx = agent.getContext ();
	if (ctx != null) ctx.stop ();
    }
    
    @Override
    public boolean sendMuxData(final MuxClient agent, MuxHeader header, boolean copy, ByteBuffer ... bufs) {
	MuxContext ctx = agent.getContext ();
	ByteBuffer buffer = bufs[0];
	switch(header.getFlags ()){
	case FLAG_GET_METERS:
	case FLAG_GET_METERS_IDENTICAL:
	    byte[] bytes = new byte[buffer.remaining ()];
	    buffer.get (bytes);
	    String txt = null;
	    try { txt = new String (bytes, "ascii");}catch(Exception e){} // cannot happen
	    int index = txt.indexOf (' ');
	    if (_logger.isInfoEnabled ()) _logger.info (this+" : GET_METERS : "+txt+" : delay="+header.getChannelId ());
	    String monName = txt.substring (0, index);
	    String metersName = txt.substring (index+1);
	    int delay = header.getChannelId ();
	    GetRequest get = ctx.newGetRequest (monName, metersName, delay, header.getFlags () == FLAG_GET_METERS_IDENTICAL);
	    get.start ();
	    return true;	    
	}
	return false;
    }
    
    private  class MuxContext {
	MuxClient _agent;
	Map<String, Integer> _monitorables = new HashMap<> ();
	List<GetRequest> _reqs = new ArrayList<> ();
	private MuxContext (MuxClient agent){
	    _agent = agent;
	}
	private GetRequest newGetRequest (String monName, String metersName, int delay, boolean identical){
	    GetRequest get = new GetRequest (this, monName, metersName, delay, identical);
	    _reqs.add (get);
	    return get;
	}
	private int addMonitorable (GetRequest get, Monitorable monitorable){
	    Integer id = _monitorables.get (monitorable.getName ());
	    if (id != null) return id;
	    id = _seedMon.incrementAndGet ();
	    _monitorables.put (monitorable.getName (), id);
	    String s = monitorable.getName ()+" "+monitorable.getDescription ();
	    ByteBuffer data = ByteBuffer.wrap (s.getBytes ());
	    MuxHeaderV0 h = new MuxHeaderV0 ();
	    h.set (0, id, get._identical ? FLAG_CREATE_MONITORABLE_IDENTICAL : FLAG_CREATE_MONITORABLE);
	    if (_logger.isInfoEnabled ()) _logger.info (_agent+" : CREATE_MONITORABLE : "+monitorable.getName ()+" : alias="+id+" : identical="+get._identical);
	    _agent.getMuxHandler ().muxData (_agent, h, data);
	    return id;
	}
	private int getMonitorable (Monitorable monitorable){
	    return _monitorables.get (monitorable.getName ());
	}
	private boolean removeMonitorable (Monitorable monitorable){
	    Integer id = _monitorables.remove (monitorable.getName ());
	    if (id == null) return false;
	    ByteBuffer data = ByteBuffer.wrap (monitorable.getName ().getBytes ());
	    MuxHeaderV0 h = new MuxHeaderV0 ();
	    h.set (0, id, FLAG_DESTROY_MONITORABLE);
	    if (_logger.isInfoEnabled ()) _logger.info (_agent+" : DESTROY_MONITORABLE : "+monitorable.getName ()+" : alias="+id);
	    _agent.getMuxHandler ().muxData (_agent, h, data);
	    return true;
	}
	private void stop (){
	    for (GetRequest get : _reqs)
		get.stop ();
	    _reqs.clear ();
	}
    }
    private class GetRequest extends MeteringRegistry.MetersTracker {
	int _delay;
	MuxContext _ctx;
	String _monitorables;
	Map<Meter, MeterContext> _meters = new HashMap<> ();
	boolean _identical;
	private GetRequest (MuxContext ctx, String monName, String metersName, int delay, boolean identical){
	    super (metersName);
	    _ctx = ctx;
	    _monitorables = monName;
	    _delay = delay;
	    _identical = identical;
	}
	private void start (){
	    if (_logger.isDebugEnabled ()) _logger.debug (_ctx._agent+" : trackMonitorable : "+_monitorables);
	    getIOHServices ().getMeteringRegistry ().trackMonitorables (_monitorables, this, _ctx._agent.getPlatformExecutor ());
	}
	private void stop (){
	    if (_logger.isDebugEnabled ()) _logger.debug (_ctx._agent+" : untrack : "+_monitorables);
	    getIOHServices ().getMeteringRegistry ().stopTracking (_registration, null);
	}
	@Override
	public void addedMonitorable (Monitorable monitorable){
	    _ctx.addMonitorable (this, monitorable);
	}
	@Override
	public void addedMeter (Monitorable monitorable, Meter meter){
	    int monId = _ctx.getMonitorable (monitorable);
	    MeterContext ctx = new MeterContext (this, meter, monId);
	    _meters.put (meter, ctx);
	    ctx.start ();
	}
	@Override
	public void removedMeter (Monitorable monitorable, Meter meter){
	    MeterContext ctx = _meters.remove (meter);
	    ctx.removed ();
	}
	@Override
	public void removedMonitorable (Monitorable monitorable, List<Meter> meters){
	    _ctx.removeMonitorable (monitorable);
	    if (meters != null){
		for (Meter meter : meters)
		    _meters.remove (meter).stop ();
	    }
	}
	@Override
	public void destroyed (Object ctx){
	    super.destroyed (ctx);
	    for (MeterContext meter : _meters.values ())
		meter.stop ();
	    _meters.clear ();
	}
    }
    private class MeterContext implements MeterListener {
	int _id;
	int _delay;
	long _monId;
	long _lastValue = Long.MIN_VALUE;
	Meter _meter;
	MuxClient _agent;
	MonitoringJob _job;
	private MeterContext (GetRequest req, Meter meter, int monId){
	    _meter = meter;
	    _agent = req._ctx._agent;
	    _monId = ((long)monId) & 0xFFFFFFFFL;
	    _delay = req._delay;
	}
	private void start (){
	    _id = _seedMeter.incrementAndGet ();
	    ByteBuffer data = ByteBuffer.wrap (_meter.getName ().getBytes ());
	    MuxHeaderV0 h = new MuxHeaderV0 ();
	    h.set (_monId, _id, FLAG_CREATE_METER);
	    if (_logger.isInfoEnabled ()) _logger.info (_agent+" : CREATE_METER : "+_meter.getName ()+" : alias="+_id);
	    _agent.getMuxHandler ().muxData (_agent, h, data);
	    if (_delay == 0L){
		_job = _meter.startJob (this, null, _agent.getPlatformExecutor ());
	    } else {
		_job = _meter.startScheduledJob (this, null, _agent.getPlatformExecutor (), _delay, 0);
	    }
	}
	private void stop (){ _job.stop ();}
	private void removed (){
	    stop ();
	    MuxHeaderV0 h = new MuxHeaderV0 ();
	    h.set (_monId, _id, FLAG_DESTROY_METER);
	    if (_logger.isInfoEnabled ()) _logger.info (_agent+" : DESTROY_METER : "+_meter.getName ()+" : alias="+_id);
	    _agent.getMuxHandler ().muxData (_agent, h, null);
	}
	public Object updated (Meter meter, Object ctx){
	    long value = _meter.getValue ();
	    //_logger.debug (_meter.getName ()+"  --> "+value);
	    if (_lastValue != value){
		_lastValue = value;
		MuxHeaderV0 h = new MuxHeaderV0 ();
		h.set (value, _id, FLAG_VALUE);
		if (_logger.isDebugEnabled ()) _logger.debug (_agent+" : VALUE : "+_meter.getName ()+" : alias="+_id+" : value="+value);
		_agent.getMuxHandler ().muxData (_agent, h, null);
	    }
	    return null;
	}
    }
}
