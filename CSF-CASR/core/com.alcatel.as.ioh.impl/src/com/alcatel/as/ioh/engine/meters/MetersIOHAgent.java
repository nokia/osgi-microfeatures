package com.alcatel.as.ioh.engine.meters;

import java.nio.ByteBuffer;
import java.util.*;
import java.io.*;
import java.util.concurrent.atomic.*;
import org.osgi.service.component.annotations.*;

import org.apache.log4j.Logger;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxContext;
import com.nextenso.mux.*;
import com.nextenso.mux.socket.Socket;
import com.nextenso.mux.util.MuxConnectionManager;
import com.nextenso.mux.util.MuxUtils;
import com.alcatel.as.service.metering2.*;
import org.osgi.framework.*;


@Component(service={MuxHandler.class}, property={"protocol=meters", "autoreporting=false"})
public class MetersIOHAgent extends MuxHandler {

    final static Logger LOGGER = Logger.getLogger("meters.ioh.agent");
    private final static int VERSION = ((1 << 16) | 0);
  
    /** our MuxConnectionManager */
    private List<MuxConnection> _muxConnections = new ArrayList<> ();
    private MeteringService _metering;
    private BundleContext _osgi;
    private List<ConfEntry> _config = new ArrayList<> ();
    
    private final static int[] METERS_IOH_ID = new int[] {
	324
    };
    
    @Reference
    public void setMetering (MeteringService metering){
	_metering = metering;
    }
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target="(service.pid=com.alcatel.as.service.metering2.impl.MeteringServiceImpl)")
    public synchronized void setConfig (Dictionary<String, String> conf){
	boolean reset = false;
	if (_config.size () > 0){
	    _config.clear ();
	    reset = true;
	}
	String list = conf.get ("meters.remote");
	try{
	    BufferedReader reader = new BufferedReader (new StringReader (list));
	    String line = null;
	    while ((line=reader.readLine ()) != null){
		line = line.trim ();
		if (line.startsWith ("#") || line.length () == 0) continue;
		ConfEntry entry = null;
		if (line.startsWith("get")) {
		    entry = new GetEntry ();
		    entry._monitorable = getParam(line, "-m", "-mon", "-monitorable");
		    entry._monitorables = getParam(line, "-ms", "-mons", "-monitorables");
		    entry._meter = getParam(line, "-mt", "-meter");
		    entry._meters = getParam(line, "-mts", "-meters");
		    entry._periodS = getParam(line, "-p", "-period");
		    entry._identical = getFlagParam(line, "-identical");
		}
		if (entry == null || !entry.check ()) throw new Exception ("invalid config line : "+line);
		_config.add (entry);
	    }
	    reader.close ();
	}catch(Throwable t){
	    LOGGER.error ("Exception in initialization", t);
	}
	if (reset){
	    // force re-sync
	    for (MuxConnection cnx : _muxConnections)
		cnx.close ();
	    _muxConnections.clear ();
	}
    }
    public void unsetConfig (Dictionary<String, String> conf){
	LOGGER.warn("MetersIOHAgent : unsetConfig");
    }
  
    @Activate
    protected void activate(BundleContext osgi, Map<String, String> conf) {
	LOGGER.debug("MetersIOH Agent activated");
	_osgi = osgi;
    }

    // ---------------- MuxHandler interface -----------------------------------------------------------
  
    /** Called by the CalloutAgent when it has seen our MuxHandler */
    @SuppressWarnings("unchecked")
    @Override
    public void init(int appId, String appName, String appInstance, MuxContext muxContext) {
	// Don't forget to call the super.init method !
	super.init(appId, appName, appInstance, muxContext);
	LOGGER.info("Initializing MetersIOH agent");
	
	// Configure our MUX handler for the Web protocol
	getMuxConfiguration().put(CONF_STACK_ID, METERS_IOH_ID);
	getMuxConfiguration().put(CONF_USE_NIO, true);
	getMuxConfiguration().put(CONF_THREAD_SAFE, true);
    }

    @Override
    public void muxOpened (final MuxConnection connection) {
	LOGGER.info("mux opened: " + connection);
	StackContext ctx = new StackContext (connection);
	connection.attach (ctx);
	synchronized (this){
	    _muxConnections.add (connection);
	    for (ConfEntry entry : _config){
		entry.init (connection, ctx);	    
	    }
	}
    }

    @Override
    public void muxClosed(MuxConnection connection) {
	LOGGER.warn("Mux closed: " + connection);
	synchronized (this){
	    _muxConnections.remove (connection);
	}
	StackContext ctx = connection.attachment ();
	for (SimpleMonitorable monitorable : ctx._monitorables.values ())
	    monitorable.stop ();
    }

    @Override
    public void muxData(MuxConnection connection,
			MuxHeader header,
			java.nio.ByteBuffer buffer){
	StackContext ctx = connection.attachment ();
	switch (header.getFlags ()){
	case MetersIOHEngine.FLAG_CREATE_MONITORABLE:
	case MetersIOHEngine.FLAG_CREATE_MONITORABLE_IDENTICAL:
	    int monId = header.getChannelId ();
	    byte[] bytes = new byte[buffer.remaining ()];
	    buffer.get (bytes);
	    String s = null;
	    try { s = new String (bytes, "ascii");}catch(Exception e){} // cannot happen
	    int index = s.indexOf (' ');
	    String monName = s.substring (0, index);
	    String monDesc = s.substring (index+1);
	    if (LOGGER.isInfoEnabled ()) LOGGER.info (connection.getStackInstance ()+" : FLAG_CREATE_MONITORABLE : "+monName+" : alias="+monId);
	    if (header.getFlags () == MetersIOHEngine.FLAG_CREATE_MONITORABLE &&
		monName.indexOf ("@remote.") == -1)
		monName = monName + "@remote."+connection.getStackInstance ().replace ("-MetersIOH", ""); // the suffix -MetersIOH is not needed here
	    SimpleMonitorable monitorable = new SimpleMonitorable (monName, monDesc);
	    monitorable.start (_osgi);
	    ctx._monitorables.put (monId, monitorable);
	    return;
	case MetersIOHEngine.FLAG_DESTROY_MONITORABLE:
	    monId = header.getChannelId ();
	    monitorable = ctx._monitorables.remove (monId);
	    if (LOGGER.isInfoEnabled ()) LOGGER.info (connection.getStackInstance ()+" : FLAG_DESTROY_MONITORABLE : "+monitorable.getName ());
	    monitorable.stop ();
	    return;
	case MetersIOHEngine.FLAG_CREATE_METER:
	    monId = (int) header.getSessionId ();
	    int meterId = header.getChannelId ();
	    bytes = new byte[buffer.remaining ()];
	    buffer.get (bytes);
	    String name = null;
	    try { name = new String (bytes, "ascii");}catch(Exception e){} // cannot happen
	    monitorable = ctx._monitorables.get (monId);
	    if (LOGGER.isInfoEnabled ()) LOGGER.info (connection.getStackInstance ()+" : FLAG_CREATE_METER : "+monitorable.getName ()+" / "+name+" : alias="+meterId);
	    Meter meter = _metering.createAbsoluteMeter (name);
	    ctx._meters.put (meterId, meter);
	    monitorable.getMeters ().put (name, meter);
	    monitorable.updated ();
	    return;
	case MetersIOHEngine.FLAG_DESTROY_METER:
	    monId = (int) header.getSessionId ();
	    meterId = header.getChannelId ();
	    monitorable = ctx._monitorables.get (monId);
	    meter = ctx._meters.remove (meterId);
	    if (LOGGER.isInfoEnabled ()) LOGGER.info (connection.getStackInstance ()+" : FLAG_DESTROY_METER : "+monitorable.getName ()+" / "+meter.getName ());
	    monitorable.getMeters ().remove (meter.getName ());
	    monitorable.updated ();
	    return;
	case MetersIOHEngine.FLAG_VALUE:
	    meterId = header.getChannelId ();
	    meter = ctx._meters.get (meterId);
	    long value = header.getSessionId ();
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (connection.getStackInstance ()+" : FLAG_VALUE : "+meter.getName ()+" : value="+value);
	    meter.set (value);
	    return;
	}
    }

    public static class StackContext {
	protected MuxConnection _stack;
	protected Map<Integer, SimpleMonitorable> _monitorables = new HashMap<> ();
	protected Map<Integer, Meter> _meters = new HashMap<> ();
	public StackContext (MuxConnection stack){
	    _stack = stack;
	}
    }

    public void destroy () {
	// The Callout is gone and asks us to destroy ourself ...
	LOGGER.info("Destroying MetersIOH Agent");
    }

    @Override
    public int getMinorVersion() {
	return VERSION & 0xFFFF;
    }

    @Override
    public int getMajorVersion() {
	return VERSION >>> 16;
    }

    @Override
    public int[] getCounters() {
	throw new RuntimeException("deprecated method, should not be used anymore");
    }

    @Override
    public void commandEvent(int command, int[] intParams, String[] strParams) {
    }


    private abstract class ConfEntry {
	protected String _monitorable, _monitorables, _meter, _meters;
	protected String _periodS;
	protected boolean _identical;
	protected int getPeriod (){ return _periodS == null ? 3000 : Integer.parseInt (_periodS);}
	protected abstract void init (MuxConnection connection, StackContext ctx);
	protected boolean check (){
	    if (_monitorable != null && _monitorables != null) return false;
	    if (_meter != null && _meters != null) return false;
	    try{if (getPeriod () < 0) return false;}catch(Exception e){ return false;}
	    return true;
	}
    }
    private class GetEntry extends ConfEntry {
	protected void init (MuxConnection connection, StackContext ctx){
	    if (_monitorable == null && _monitorables == null) _monitorables = "*";
	    if (_meter == null && _meters == null) _meters = "*";
	    String monName = _monitorable != null ? "="+_monitorable : _monitorables;
	    String meterName = _meter != null ? "="+_meter : _meters;
	    String body = monName+" "+meterName;
	    ByteBuffer ret = ByteBuffer.wrap (body.getBytes ());
	    MuxHeaderV0 h = new MuxHeaderV0 ();
	    h.set (0L, getPeriod (), _identical ? MetersIOHEngine.FLAG_GET_METERS_IDENTICAL : MetersIOHEngine.FLAG_GET_METERS);
	    connection.sendMuxData (h, ret);
	    LOGGER.info (connection+ " : requesting GET_METERS : "+body);
	}
    }
    
    public static final String getParam(String line, String ... pnames) {
	for (String pname : pnames) {
	    String p = " " + pname + " ";
	    int index = line.indexOf(p);
	    if (index == -1)
		continue;
	    index += p.length();
	    if (index == line.length())
		return "";
	    int end = line.indexOf(" ", index);
	    if (end == -1)
		end = line.indexOf("\t", index);
	    if (end == -1)
		end = line.length();
	    return line.substring(index, end);
	}
	return null;
    }
    public static final boolean getFlagParam(String line, String ... pnames) {
	for (String pname : pnames) {
	    String p = " " + pname;
	    int index = line.indexOf(p);
	    if (index == -1)
		return false;
	    int end = index + p.length ();
	    if (end == line.length ())
		return true;
	    return line.charAt (end) == ' ';
	}
	return false;
    }
}
