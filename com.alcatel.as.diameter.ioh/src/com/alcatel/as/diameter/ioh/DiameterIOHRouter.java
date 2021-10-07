package com.alcatel.as.diameter.ioh;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;

import java.util.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;
import java.nio.charset.Charset;

import com.alcatel.as.diameter.parser.*;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.alcatel.as.ioh.tools.ByteBufferUtils;
import com.alcatel.as.session.distributed.*;
import com.alcatel.as.session.distributed.smartkey.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;
import static com.alcatel.as.ioh.tools.ByteBufferUtils.getUTF8;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;

public class DiameterIOHRouter {

    protected static final int DIAMETER_AVP_SESSION_PRIORITY_CODE = 650;
    protected static final int DIAMETER_3GPP_VENDOR_ID = 10415;
    protected static Charset UTF8 = null;
    static {
	try{ UTF8 = Charset.forName ("utf-8");
	}catch(Exception e){}// cannot happen
    }
    protected static final AtomicInteger SEED_HOP_ID = new AtomicInteger (0);
    
    protected Logger _logger;

    protected DiameterIOHRouterFactory _factory;
    
    public static final Integer PRIORITY_INITIAL = 0;
    public static final Integer PRIORITY_INITIAL_RETRANSMISSION = 1;
    public static final Integer PRIORITY_SUBSEQUENT = 5;
    public static final Integer PRIORITY_RESPONSE = 9;
    public ChannelWriter.SendBufferMonitor CHANNEL_WRITER_REMOTE_AGENT, CHANNEL_WRITER_REMOTE_IOH, CHANNEL_WRITER_LOCAL_AGENT;
    
    protected Map<String, String> _cache = new ConcurrentHashMap <> ();
    protected boolean _test = false;
    protected boolean _localOnly = false;
    protected boolean _parseSessionId, _parseSessionIdDeep;
    protected String _toString;
    protected int _maxNbAgents, _agentHopIdOffset, _agentHopIdGetMask, _agentHopIdRemoveMask;
    
    protected Meter _respondMeter, _respondOverloadMeter;
    protected Meter _dropMeter, _dropReqMeter, _dropRespMeter, _dropOverloadReqMeter, _dropOverloadRespMeter, _dropOrphanRespMeter, _dropUnavailableReqMeter;
    
    protected Meter _routedReqMeter;
    protected Meter _routedRespMeter;
    
    public DiameterIOHRouter (DiameterIOHRouterFactory factory, Logger logger){
	_factory = factory;
	_logger = logger;
	_toString = new StringBuilder ().append ("DiameterIOHRouter[").append ('-').append (']').toString ();
    }

    @Override
    public String toString (){
	return _toString;
    }
    
    /************************* The public methods called by the DiameterIOH ********************************/
    
    public void init (IOHEngine engine){
	_toString = new StringBuilder ().append ("DiameterIOHRouter[").append (engine.name ()).append (']').toString ();
	
	Map<String, Object> props = engine.getProperties ();
	_test = getBooleanProperty ("diameter.ioh.router.test", props, false);
	_parseSessionId = getBooleanProperty ("diameter.ioh.router.session-id", props, false);
	_parseSessionIdDeep = getBooleanProperty ("diameter.ioh.router.session-id.deep", props, false);
	
	_localOnly = engine.useMuxAgent () == false;

	_logger.info (this+" : _localOnly="+_localOnly+", _test="+_test);

	CHANNEL_WRITER_REMOTE_AGENT = new ChannelWriter.ProgressiveSendBufferMonitorWithPriorities (getIntProperty ("diameter.ioh.router.mux.writer.lowWM", props, 1000000),
												    getIntProperty ("diameter.ioh.router.mux.writer.highWM", props, 2000000),
												    10);
	CHANNEL_WRITER_REMOTE_IOH = new ChannelWriter.ProgressiveSendBufferMonitorWithPriorities (getIntProperty ("diameter.ioh.router.remote.writer.lowWM", props, 5000000),
												  getIntProperty ("diameter.ioh.router.remote.writer.highWM", props, 10000000),
												  10);
	CHANNEL_WRITER_LOCAL_AGENT = new ChannelWriter.ProgressiveSendBufferMonitorWithPriorities (getIntProperty ("diameter.ioh.router.local.writer.lowWM", props, 10000),
												   getIntProperty ("diameter.ioh.router.local.writer.highWM", props, 20000),
												   10);

	_maxNbAgents = getIntProperty ("diameter.ioh.router.agent.max", props, 256);
	for (int i =0; i<32; i++){
	    int max = (int) Math.pow (2, i);
	    if (max >= _maxNbAgents){
		_maxNbAgents = max;
		_agentHopIdOffset = i;
		break;
	    }
	}
	_agentHopIdGetMask = 0XFFFFFFFF << (32 - _agentHopIdOffset);
	_agentHopIdRemoveMask = 0XFFFFFFFF >>> _agentHopIdOffset;
	_logger.warn ("Set Max Nb agents to : "+_maxNbAgents+"/"+_agentHopIdOffset);
	
	_respondMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.respond", null);
	_respondOverloadMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.respond.overload", _respondMeter);
	
	_dropMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.drop", null);
	_dropReqMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.drop.req", _dropMeter);
	_dropRespMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.drop.resp", _dropMeter);
	_dropOverloadReqMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.drop.req.overload", _dropReqMeter);
	_dropOverloadRespMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.drop.resp.overload", _dropRespMeter);
	_dropUnavailableReqMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.drop.req.unavailable", _dropReqMeter);

	_dropOrphanRespMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.drop.resp.orphan", _dropRespMeter);
	
	_routedReqMeter = engine.getIOHMeters ().createIncrementalMeter ("router.routed.req", null);
	_routedRespMeter = engine.getIOHMeters ().createIncrementalMeter ("router.routed.resp", null);
    }
    
    public void initMuxClient (MuxClient agent){
	agent.setContext (newAgentContext ().init (agent));
	if (agent.isRemoteIOHEngine ()) agent.setSendBufferMonitor (CHANNEL_WRITER_REMOTE_IOH);
	else if (agent.isLocalAgent ()) agent.setSendBufferMonitor (CHANNEL_WRITER_LOCAL_AGENT);
	else agent.setSendBufferMonitor (CHANNEL_WRITER_REMOTE_AGENT);
    }
    public void resetMuxClient (MuxClient agent){
	// to be overridden
    }
    protected AgentContext newAgentContext (){ // maybe overridden
	return new AgentContext ();
    }
    protected class AgentContext {
	public Meter _overloadReqMeter, _overloadRespMeter, _routedReqMeter, _routedRespMeter, _dismissMeter;
	public int _hopId;
	public AgentContext init (MuxClient agent){
	    _overloadReqMeter = agent.getIOHMeters ().createIncrementalMeter ("router.error.overload.req", null);
	    _overloadRespMeter = agent.getIOHMeters ().createIncrementalMeter ("router.error.overload.resp", null);
	    _routedReqMeter = agent.getIOHMeters ().createIncrementalMeter ("router.routed.req", DiameterIOHRouter.this._routedReqMeter);
	    _routedRespMeter = agent.getIOHMeters ().createIncrementalMeter ("router.routed.resp", DiameterIOHRouter.this._routedRespMeter);
	    _dismissMeter = agent.getIOHMeters ().createIncrementalMeter ("router.error.dismiss", null);
	    _hopId = SEED_HOP_ID.getAndIncrement () & 0x7FFFFFFF;
	    return this;
	}
    }
    
    /*************************************/
    public void clientOpened (DiameterIOHChannel client){}
    public void clientClosed (DiameterIOHChannel client){}
    
    public void doClientRequest (DiameterIOHChannel client, DiameterMessage msg){
    }
    public void doAgentRequest (DiameterIOHChannel client, MuxClient agent, DiameterMessage msg){
    }
    public void doClientResponse (DiameterIOHChannel client, DiameterMessage msg){
    }
    public void doAgentResponse (DiameterIOHChannel client, MuxClient agent, DiameterMessage msg){
    }

    protected Integer getPriority (DiameterMessage msg){
	if (msg.isRequest ()){
	    // NOTE let other implementations detect SUBSEQUENT
	    return msg.isRetransmitted () ? PRIORITY_INITIAL_RETRANSMISSION : PRIORITY_INITIAL;
	} else {
	    return PRIORITY_RESPONSE;
	}
    }

    protected long getSessionId (DiameterMessage msg){
	if (_parseSessionId){
	    int[] index = msg.getSessionId (_parseSessionIdDeep);
	    if (index != null){
		byte[] bytes = msg.getBytes ();
		long id = 0L;
		int off = index[2];
		for (int k=0; k<index[3]; k++){
		    long l = bytes[off++] & 0xFFL;
		    l <<= (k & 0x07) * 8; // (k%8)*8
		    id += l;
		}
		return id;
	    }
	}
	return 0L;
    }

    // returns 0 if OK - else the buffSize (useful for debug messages)
    public int checkAgentOverload (MuxClient agent, DiameterMessage msg){
	int buffSize = agent.getSendBufferSize ();
	if (_test){
	    int value = msg.getIntAvp (5, 123, -1);
	    if (value != -1){
		buffSize = value;
	    }
	}
	if (ChannelWriter.check (buffSize + msg.getBytes ().length, agent.getSendBufferMonitor (), getPriority (msg)))
	    return 0;
	if (_logger.isInfoEnabled ())
	    _logger.info (agent+" : Buffer Full : cannot send message (sendBufferSize="+buffSize+") isReq="+msg.isRequest()+", msgLen="+msg.getBytes().length+", prio="+getPriority(msg));
	return buffSize;
    }

    
    /*************************************/
    
    protected int getIntProperty (String name, Map props, int def){
	try{
	    String v = (String) props.get (name);
	    if (v == null) v = (String) _factory.getConf ().get (name);
	    if (v != null) return Integer.parseInt (v);
	}catch(Throwable t){}
	return def;
    }
    protected boolean getBooleanProperty (String name, Map props, boolean def){
	try{
	    String v = (String) props.get (name);
	    if (v == null) v = (String) _factory.getConf ().get (name);
	    if (v != null) return Boolean.parseBoolean (v);
	}catch(Throwable t){}
	return def;
    }
    protected String getStringProperty (String name, Map props, String def){
	String value = (String) props.get (name);
	if (value == null) value = (String) _factory.getConf ().get (name);
	if (value != null && (value = value.trim ()).length () > 0) return value;
	return def;
    }
    protected List<String> getStringListProperty (String name, Map props){
	Object value = props.get (name);
	if (value == null) return new ArrayList<> ();
	if (value instanceof String) {
	    List<String> list = new ArrayList<> ();
	    list.add ((String) value);
	    return list;
	}
	return (List<String>) value;
    }
}
