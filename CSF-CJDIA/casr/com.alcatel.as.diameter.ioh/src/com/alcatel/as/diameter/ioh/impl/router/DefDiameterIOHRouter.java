package com.alcatel.as.diameter.ioh.impl.router;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.apache.log4j.Logger;
import java.nio.charset.Charset;
import java.util.function.Function;
import java.util.regex.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.nextenso.mux.*;

import com.alcatel.as.diameter.ioh.*;
import com.alcatel.as.diameter.parser.*;
import com.alcatel.as.diameter.ioh.impl.*;
import com.alcatel.as.diameter.ioh.impl.utils.*;

import alcatel.tess.hometop.gateways.reactor.*;
import org.osgi.service.component.annotations.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel_lucent.as.management.annotation.stat.Stat;
import com.alcatel_lucent.as.management.annotation.alarm.*;
import com.alcatel.as.service.reporter.api.AlarmService;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.service.metering2.*;

@Stat
public class DefDiameterIOHRouter extends DiameterIOHRouter {

    private static boolean SET_ERROR_FLAG = !Boolean.getBoolean ("diameter.ioh.router.error.flag.clear"); // leave the option to not use it by precaution (compliancy with former behavior)
    
    protected static final byte[] _3004 = DiameterUtils.setIntValue (3004, new byte[4], 0);
    protected static final DiameterUtils.Avp _3004AVP = new DiameterUtils.Avp (268, 0, true, _3004);

    protected static final DiameterUtils.Avp _localOriginAVP = new DiameterUtils.Avp (999, 0, true, DiameterUtils.setIntValue (1, new byte[4], 0));
    
    protected boolean _useE2E;
    protected boolean _do3004, _do3004ErrorMessage;
    protected ConcurrentHashMap<Integer, MuxClient> _agentsByHopId = new ConcurrentHashMap<> ();
    protected DefDiameterIOHRouterFactory _defFactory;
    protected Map<Long, String> _groupByApp;
    protected String _defGroupByApp;
    protected boolean _groupByCommand;
    protected List<Function<DiameterMessage, String>> _groupByAVP;
    protected boolean _dropOnSendOverloadTcp, _dropOnSendOverloadSctp;
    protected boolean _respondOnUndeliver = false, _respondOnUndeliverResultIsError = true;
    protected DiameterUtils.Avp _respondOnUndeliverResultAVP;
    protected boolean _respondOnUndeliverErrMsg;
    protected Meter _undeliverMeter, _undeliverReqMeter, _undeliverRespMeter, _undeliverOverloadReqMeter, _undeliverOverloadRespMeter;
    public ChannelWriter.SendBufferMonitor CHANNEL_WRITER_TCP, CHANNEL_WRITER_SCTP;
    protected int _agentLoadSelect = 2;

    public DefDiameterIOHRouter (DiameterIOHRouterFactory factory, Logger logger){
	super (factory, logger);
	_defFactory = (DefDiameterIOHRouterFactory) factory;
    }

    @Override
    public void init (IOHEngine engine){
	super.init (engine);
	Map<String, Object> conf = engine.getProperties ();

	_dropOnSendOverloadTcp = getBooleanProperty ("ioh.tcp.overload.drop", conf, false);
	_dropOnSendOverloadSctp = getBooleanProperty ("ioh.sctp.overload.drop", conf, false);	

	_undeliverMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.undeliver", null);
	_undeliverReqMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.undeliver.req", _undeliverMeter);
	_undeliverRespMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.undeliver.resp", _undeliverMeter);
	_undeliverOverloadReqMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.undeliver.req.overload", _undeliverReqMeter);
	_undeliverOverloadRespMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.undeliver.resp.overload", _undeliverRespMeter);
	
	_respondOnUndeliver = getBooleanProperty (DefDiameterIOHRouterFactory.CONF_UNDELIVER_RESPOND, conf, false);
	int respondOnUndeliverResult = getIntProperty (DefDiameterIOHRouterFactory.CONF_UNDELIVER_RESPOND_RESULT, conf, 3002);
	_logger.warn (this+" : "+DefDiameterIOHRouterFactory.CONF_UNDELIVER_RESPOND+" : "+_respondOnUndeliver+", result="+respondOnUndeliverResult);
	_respondOnUndeliverResultIsError = respondOnUndeliverResult > 3000 && respondOnUndeliverResult < 4000 && SET_ERROR_FLAG;
	byte[] bytes = DiameterUtils.setIntValue (respondOnUndeliverResult, new byte[4], 0);
	_respondOnUndeliverErrMsg = getBooleanProperty (DefDiameterIOHRouterFactory.CONF_UNDELIVER_RESPOND_ERRMSG, conf, false);
	if (_respondOnUndeliver) _respondOnUndeliverResultAVP = new DiameterUtils.Avp (268, 0, true, bytes);
	
	_useE2E = getBooleanProperty (DefDiameterIOHRouterFactory.CONF_ROUTE_BY_E2E, conf, true);
	_logger.warn (this+" : "+DefDiameterIOHRouterFactory.CONF_ROUTE_BY_E2E+" : "+_useE2E); 
	
	_do3004 = getBooleanProperty (DefDiameterIOHRouterFactory.CONF_OVERLOAD_3004, conf, true);
	_logger.warn (this+" : "+DefDiameterIOHRouterFactory.CONF_OVERLOAD_3004+" : "+_do3004);

	_do3004ErrorMessage = getBooleanProperty (DefDiameterIOHRouterFactory.CONF_OVERLOAD_3004_ERROR_MSG_LOCAL, conf, false);
	_logger.warn (this+" : "+DefDiameterIOHRouterFactory.CONF_OVERLOAD_3004_ERROR_MSG_LOCAL+" : "+_do3004ErrorMessage);

	List<String> routeByApp = getStringListProperty (DefDiameterIOHRouterFactory.CONF_ROUTE_BY_APP, conf);
	for (String route : routeByApp){
	    route = route.trim ();
	    int index = route.indexOf (' ');
	    String appS = route.substring (0, index);
	    String group = route.substring (index+1).trim ();
	    if (appS.equals ("*") || appS.equals ("0")){
		_defGroupByApp = group;
		_logger.warn (this+" : "+DefDiameterIOHRouterFactory.CONF_ROUTE_BY_APP+" : app=* -> group="+_defGroupByApp);
		continue;
	    }
	    index = appS.indexOf ('/');
	    long app = 0L;
	    long command = 0L;
	    if (index == -1)
		app = Long.parseLong (appS);
	    else {
		app = Long.parseLong (appS.substring (0, index));
		String commandS = appS.substring (index+1);
		if (!commandS.equals ("*")){
		    command = Long.parseLong (commandS);
		    if (command != 0L) _groupByCommand = true;
		}
	    }
	    if (_groupByApp == null) _groupByApp = new HashMap<> (); // lazy instanciation to keep null if not needed
	    if (command != 0L) _groupByApp.put ((app << 32) | command, group);
	    else _groupByApp.put (app << 32, group);
	    _logger.warn (this+" : "+DefDiameterIOHRouterFactory.CONF_ROUTE_BY_APP+" : app="+app+"/command="+(command != 0L ? String.valueOf (command) : "*" )+" -> group="+group);
	}
	
	List<String> routeByAVP = getStringListProperty (DefDiameterIOHRouterFactory.CONF_ROUTE_BY_AVP, conf);
	for (String route : routeByAVP){
	    route = route.trim ();
	    int index = route.indexOf (' ');
	    String avpS = route.substring (0, index);
	    final String group = route.substring (index+1).trim ();
	    final boolean self = "_".equals (group);
	    index = avpS.indexOf ('=');
	    String value = avpS.substring (index + 1);	    
	    boolean numValue = value.startsWith ("0x");
	    long valueL = numValue ? Long.parseLong (value.substring (2), 16) : 0L;
	    avpS = avpS.substring (0, index);
	    index = avpS.indexOf ('/');
	    if (index == -1){
		index = avpS.length ();
		avpS = avpS+"/0";
	    }
	    final int code = Integer.parseInt (avpS.substring (0, index));
	    final int vid = Integer.parseInt (avpS.substring (index+1));
	    final Pattern p = numValue ? null : Pattern.compile(value);
	    Function<DiameterMessage, String> f = (msg) -> {
		byte[] b = msg.getAvp (code, vid);
		if (b == null) return null;
		if (numValue){
		    long l = 0L;
		    for (int i=0; i<b.length; i++){
			l = l << 8;
			l |= b[i] & 0xFFL;
		    }
		    if (l == valueL) return group;
		    return null;
		}
		String v = new String (b, UTF8);
		if (p.matcher (v).find ())
		    return self ? v : group;
		return null;
	    };
	    if (_groupByAVP == null) _groupByAVP = new ArrayList<> ();
	    _groupByAVP.add (f);
	    _logger.warn (this+" : "+DefDiameterIOHRouterFactory.CONF_ROUTE_BY_AVP+" : code="+code+"/vid="+vid+" value="+value+" -> group="+group);
	}

	int tcpLW = getIntProperty ("diameter.ioh.router.tcp.writer.lowWM", conf, -1);
	int tcpHW = getIntProperty ("diameter.ioh.router.tcp.writer.highWM", conf, -1);
	if (tcpLW >= 0 && tcpHW > 0 && tcpLW < tcpHW){
	    _logger.warn (this+" : applying low/high watermarks for tcp peers : "+tcpLW+"/"+tcpHW);
	    CHANNEL_WRITER_TCP = new ChannelWriter.ProgressiveSendBufferMonitorWithPriorities (tcpLW, tcpHW, 10);	    
	}
	int sctpLW = getIntProperty ("diameter.ioh.router.sctp.writer.lowWM", conf, -1);
	int sctpHW = getIntProperty ("diameter.ioh.router.sctp.writer.highWM", conf, -1);
	if (sctpLW >= 0 && sctpHW > 0 && sctpLW < sctpHW){
	    _logger.warn (this+" : applying low/high watermarks for sctp peers : "+sctpLW+"/"+sctpHW);
	    CHANNEL_WRITER_SCTP = new ChannelWriter.ProgressiveSendBufferMonitorWithPriorities (sctpLW, sctpHW, 10);	    
	}

	_agentLoadSelect = getIntProperty (IOHEngine.PROP_AGENT_LOAD_SELECT, conf, _agentLoadSelect);
    }

    @Override
    public void initMuxClient (MuxClient agent){
	super.initMuxClient (agent);
	AgentContext ctx = agent.getContext ();
	int attempts = 0;
	while (true){
	    ctx._hopId = ctx._hopId << (32 - _agentHopIdOffset);
	    Object existing = _agentsByHopId.putIfAbsent (ctx._hopId, agent);
	    if (existing == null) break;
	    if (++attempts == _maxNbAgents){
		_logger.error (this+" : rejecting agent : too many connected : "+agent);
		ctx._hopId = -1;
		agent.close ();
		return;
	    }
	    ctx._hopId = SEED_HOP_ID.getAndIncrement () & 0x7FFFFFFF;
	}
	MuxHeaderV0 h = new MuxHeaderV0 ();
	h.set (0, _agentHopIdRemoveMask, 0);
	agent.getMuxHandler ().muxData (agent, h, null);
	if (_logger.isInfoEnabled ()) _logger.info (this+" : "+agent+" : hopId = "+ctx._hopId);
    }
    @Override
    public void resetMuxClient (MuxClient agent){
	super.resetMuxClient (agent);
	AgentContext ctx = agent.getContext ();
	_agentsByHopId.remove (ctx._hopId);
    }
    
    protected class ClientState {
	protected DiameterIOHChannel _client;
	protected Meter _respondMeter, _respondOverloadMeter;
	protected Meter _dropMeter, _dropReqMeter, _dropRespMeter, _dropOverloadReqMeter, _dropOverloadRespMeter, _dropOrphanRespMeter, _dropUnavailableReqMeter;
	protected Meter _undeliverMeter, _undeliverReqMeter, _undeliverRespMeter, _undeliverOverloadReqMeter, _undeliverOverloadRespMeter;
	
	protected ClientState (DiameterIOHChannel client){
	    _client = client;
	    SimpleMonitorable monitorable = client.getMonitorable ();
	    MeteringService metering = _factory.getMeteringService ();

	    _respondMeter = monitorable.createIncrementalMeter (metering, "router.error.respond", null);
	    _respondOverloadMeter = monitorable.createIncrementalMeter (metering, "router.error.respond.overload", _respondMeter);

	    _dropMeter = monitorable.createIncrementalMeter (metering, "router.error.drop", null);
	    _dropReqMeter = monitorable.createIncrementalMeter (metering, "router.error.drop.req", _dropMeter);
	    _dropRespMeter = monitorable.createIncrementalMeter (metering, "router.error.drop.resp", _dropMeter);
	    _dropOverloadReqMeter = monitorable.createIncrementalMeter (metering, "router.error.drop.req.overload", _dropReqMeter);
	    _dropOverloadRespMeter = monitorable.createIncrementalMeter (metering, "router.error.drop.resp.overload", _dropRespMeter);
	    
	    _dropOrphanRespMeter = monitorable.createIncrementalMeter (metering, "router.error.drop.resp.orphan", _dropRespMeter);
	    _dropUnavailableReqMeter = monitorable.createIncrementalMeter (metering, "router.error.drop.req.unavailable", _dropRespMeter);

	    _undeliverMeter = monitorable.createIncrementalMeter (metering, "router.error.undeliver", null);
	    _undeliverReqMeter = monitorable.createIncrementalMeter (metering, "router.error.undeliver.req", _undeliverMeter);
	    _undeliverRespMeter = monitorable.createIncrementalMeter (metering, "router.error.undeliver.resp", _undeliverMeter);
	    _undeliverOverloadReqMeter = monitorable.createIncrementalMeter (metering, "router.error.undeliver.req.overload", _undeliverReqMeter);
	    _undeliverOverloadRespMeter = monitorable.createIncrementalMeter (metering, "router.error.undeliver.resp.overload", _undeliverRespMeter);
	}
    }

    @Override
    public void clientOpened (final DiameterIOHChannel client){
	client.attach (new ClientState (client));	
    }

    @Override
    protected Integer getPriority (DiameterMessage msg){
	return _defFactory.getPriority (msg);
    }
    
    @Override
    public void doClientRequest (DiameterIOHChannel client, DiameterMessage msg){
	if (routeCustom (client, msg)) return;

	routeDefault (client, msg);
    }
    protected boolean routeDefault (DiameterIOHChannel client, DiameterMessage msg){

	// self-overload check
	long overload = _defFactory.getOverload ();
	if (_test){
	    int value = msg.getIntAvp (6, 123, -1);
	    if (value != -1){
		overload = value;
	    }
	}
	if (overload > 0){
	    int prio = _defFactory.getMessageWeight (msg);
	    if (overload > prio){
		handleSelfOverload (client, msg, overload);
		return true;
	    }
	}

	String group = getDestinationGroup (msg);
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : getDestinationGroup="+group);
	
	// hash function defends against poor quality hash from msg identifier	
	MuxClient agent = client.pickAgent (group, _useE2E ? hash(msg.getEndIdentifier ()) : null);  
	if (agent == null){
	    // may happen when agents are in DPR transaction or if no route is found
	    dropUnavailableReq (client, msg);
	    return true;
	}
	if (!_useE2E){ // useE2E means stickiness --> no load factor
	    // use load factor
	    long load = agent.getLoadMeter ().getValue ();
	    if (load != 0L){
		for (int i=1; i<_agentLoadSelect; i++){
		    MuxClient agent2 = client.pickAgent (group, null);		    
		    if (agent2 == agent){
			continue;
		    }
		    long load2 = agent2.getLoadMeter ().getValue ();
		    if (load2 < load){
			AgentContext ctx = agent.getContext ();
			ctx._dismissMeter.inc (1);
			agent = agent2;
			load = load2;
			if (load == 0L) break;
		    }
		}
	    }
	}
	sendToAgent (client, agent, msg, getSessionId (msg));
	return true;
    }
    // to be overridden
    protected boolean routeCustom (DiameterIOHChannel client, DiameterMessage msg){
	return false;
    }

    protected String getDestinationGroup (DiameterMessage msg){
	if (_groupByAVP != null){
	    for (Function<DiameterMessage, String> f : _groupByAVP){
		String group = f.apply (msg);
		if (group != null) return group;
	    }
	}
	if (_groupByApp == null) return _defGroupByApp; // return null is ok (no route set)
	long app = msg.getApplicationID () << 32;
	if (_groupByCommand){
	    long command = msg.getCommandCode () & 0xFFFFFFFFL;
	    String group = _groupByApp.get (app | command);
	    if (group != null) return group;
	}
	String group = _groupByApp.get (app);
	if (group != null) return group;
	return _defGroupByApp != null ? _defGroupByApp : ""; // "" is used to generate an error (group not found)
    }
    
    
    protected void dropUnavailableReq (DiameterIOHChannel client, DiameterMessage msg){
	_dropUnavailableReqMeter.inc (1);
	ClientState state = client.attachment ();
	state._dropUnavailableReqMeter.inc (1);
    }
    
    
    @Override
    public void doAgentRequest (DiameterIOHChannel client, MuxClient agent, DiameterMessage msg){
	int hop = msg.getHopIdentifier ();
	AgentContext ctx = agent.getContext ();
	msg.updateHopIdentifier (hop | ctx._hopId);
	sendToClient (client, agent, msg); // if it fails, the client will be closed
    }

    @Override
    public void doClientResponse (DiameterIOHChannel client, DiameterMessage msg){
	int hop = msg.getHopIdentifier ();
	MuxClient agent = _agentsByHopId.get (hop & _agentHopIdGetMask);
	msg.updateHopIdentifier (hop & _agentHopIdRemoveMask);
	if (agent == null || agent.isOpened () == false){
	    if (_logger.isDebugEnabled ()) _logger.debug (this+" : no agent matching the response hop id : "+hop);
	    ClientState state = client.attachment ();
	    _dropOrphanRespMeter.inc (1);
	    state._dropOrphanRespMeter.inc (1);
	    return;
	}
	sendToAgent (client, agent, msg, 0L);
    }
    
    @Override
    public void doAgentResponse (DiameterIOHChannel client, MuxClient agent, DiameterMessage msg){
	sendToClient (client, agent, msg);
    }

    // returns true if check is ok
    public boolean checkClientOverload (DiameterIOHChannel client, ChannelWriter.SendBufferMonitor bufferMonitor, DiameterMessage msg){
	int buffSize = client.getChannel ().getSendBufferSize ();
	if (ChannelWriter.check (buffSize + msg.getBytes ().length, bufferMonitor, getPriority (msg)))
	    return true;
	if (_logger.isInfoEnabled ())
	    _logger.info (client+" : Buffer Full : cannot send message (sendBufferSize="+buffSize+") isReq="+msg.isRequest()+", msgLen="+msg.getBytes().length+", prio="+getPriority(msg));
	return false;
    }
    
    protected boolean sendToClient (DiameterIOHChannel client, MuxClient agent, DiameterMessage msg){
	boolean tcp = client.getType () == DiameterIOHChannel.TYPE.TCP;
	ChannelWriter.SendBufferMonitor bufferMonitor =  tcp ? CHANNEL_WRITER_TCP : CHANNEL_WRITER_SCTP;
	boolean dropOnSendOverload = tcp ? _dropOnSendOverloadTcp : _dropOnSendOverloadSctp;
	if (bufferMonitor == null){ // default case - no priority
	    if (client.sendOut (true, msg))
		return true;
	} else {
	    dropOnSendOverload = true;
	    if (checkClientOverload (client, bufferMonitor, msg)){
		client.sendOut (false, msg);
		return true;
	    }
	}
	// the msg was not sent
	if (dropOnSendOverload){
	    if (msg.isRequest ()){
		_undeliverOverloadReqMeter.inc (1);
		ClientState state = client.attachment ();
		state._undeliverOverloadReqMeter.inc (1);
		if (_respondOnUndeliver){
		    DiameterUtils.Avp errMsgAvp = null;
		    if (_respondOnUndeliverErrMsg){
			String txt = new StringBuilder ().append (client).append (" : overloaded : cannot send").toString ();
			errMsgAvp = new DiameterUtils.Avp (281, 0, false, txt.getBytes (UTF8));
		    }
		    DiameterMessage resp = errMsgAvp != null ?
			DiameterUtils.makeResponse (msg, _localOriginAVP, _respondOnUndeliverResultAVP, errMsgAvp) :
			DiameterUtils.makeResponse (msg, _localOriginAVP, _respondOnUndeliverResultAVP);
		    if (_respondOnUndeliverResultIsError) resp.setError ();
		    int hop = resp.getHopIdentifier ();
		    resp.updateHopIdentifier (hop & _agentHopIdRemoveMask);
		    sendToAgent (client, agent, resp, 0L);
		}
	    } else {
		_undeliverOverloadRespMeter.inc (1);
		ClientState state = client.attachment ();
		state._undeliverOverloadRespMeter.inc (1);
	    }
	}
	return false;
    }
    protected boolean sendToAgent (DiameterIOHChannel client, MuxClient agent, DiameterMessage msg, long sessionId){
	int check = checkAgentOverload (agent, msg);
	if (check == 0){
	    client.sendAgent (agent, msg, sessionId);
	    AgentContext ctx = agent.getContext ();
	    if (msg.isRequest ()) ctx._routedReqMeter.inc (1);
	    else ctx._routedRespMeter.inc (1);
	    return true;
	}
	handleAgentOverload (client, msg, agent, check);
	return false;
    }
    protected void handleAgentOverload (DiameterIOHChannel client, DiameterMessage msg, MuxClient agent, int overload){
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : Agent overload : "+agent+" : "+overload+" - rejecting : "+msg);
	AgentContext ctx = agent.getContext ();
	if (msg.isRequest () == false){
	    ctx._overloadRespMeter.inc (1);
	    _dropOverloadRespMeter.inc (1);
	    ClientState state = client.attachment ();
	    state._dropOverloadRespMeter.inc (1);
	    return;
	}
	ctx._overloadReqMeter.inc (1);
	if (_do3004){
	    DiameterUtils.Avp errMsgAvp = null;
	    if (_do3004ErrorMessage){
		String txt = new StringBuilder ().append (agent).append (" overload=").append (overload).toString ();
		errMsgAvp = new DiameterUtils.Avp (281, 0, false, txt.getBytes (UTF8));
	    }
	    DiameterMessage resp = errMsgAvp != null ?
		DiameterUtils.makeResponse (msg, client.getIOHOriginHost (), client.getIOHOriginRealm (), _3004AVP, errMsgAvp) :
		DiameterUtils.makeResponse (msg, client.getIOHOriginHost (), client.getIOHOriginRealm (), _3004AVP);
	    if (SET_ERROR_FLAG) resp.setError ();
	    if (_test){
		byte[] bytes = resp.getBytes ();
		bytes[5] = (byte)0xFF;
		bytes[6] = (byte)0xFF;
		bytes[7] = (byte)0xFF;
	    }
	    _respondOverloadMeter.inc (1);
	    ClientState state = client.attachment ();
	    state._respondOverloadMeter.inc (1);
	    sendToClient (client, agent, resp);
	} else {
	    _dropOverloadReqMeter.inc (1);
	    ClientState state = client.attachment ();
	    state._dropOverloadReqMeter.inc (1);
	}
    }
    protected void handleSelfOverload (DiameterIOHChannel client, DiameterMessage msg, long overload){
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : Self overload : "+overload+" - rejecting : "+msg);
	if (_do3004){
	    DiameterUtils.Avp errMsgAvp = null;
	    if (_do3004ErrorMessage){
		String txt = new StringBuilder ().append ("Self overload=").append (overload).toString ();
		errMsgAvp = new DiameterUtils.Avp (281, 0, false, txt.getBytes (UTF8));
	    }
	    DiameterMessage resp = errMsgAvp != null ?
		DiameterUtils.makeResponse (msg, client.getIOHOriginHost (), client.getIOHOriginRealm (), _3004AVP, errMsgAvp) :
		DiameterUtils.makeResponse (msg, client.getIOHOriginHost (), client.getIOHOriginRealm (), _3004AVP);
	    if (SET_ERROR_FLAG) resp.setError ();
	    if (_test){
		byte[] bytes = resp.getBytes ();
		bytes[5] = (byte)0xFF;
		bytes[6] = (byte)0xFF;
		bytes[7] = (byte)0xFF;
	    }
	    _respondOverloadMeter.inc (1);
	    ClientState state = client.attachment ();
	    state._respondOverloadMeter.inc (1);
	    sendToClient (client, null, resp);
	} else {
	    _dropOverloadReqMeter.inc (1);
	    ClientState state = client.attachment ();
	    state._dropOverloadReqMeter.inc (1);
	}
    }
    
    /**
     * Applies a supplemental hash function to a given msg identifier, which
     * defends against poor quality hash functions.  This is critical in order to
     * fairly distribute messages amongs agents, especially when msg identifiers only differs
     * by a small constant.
     */
	private int hash(int h) {
		// Ensures the number is unsigned.
		h = (h & 0X7FFFFFFF);

		// This function ensures that hashCodes that differ only by
		// constant multiples at each bit position have a bounded
		// number of collisions (approximately 8 at default load factor).
		h ^= (h >>> 20) ^ (h >>> 12);
		return h ^ (h >>> 7) ^ (h >>> 4);
	}
}
