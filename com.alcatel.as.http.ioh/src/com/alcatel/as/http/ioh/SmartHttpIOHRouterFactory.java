package com.alcatel.as.http.ioh;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;

import java.util.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

import com.alcatel.as.http.parser.*;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.alcatel.as.ioh.tools.ByteBufferUtils;
import com.alcatel.as.session.distributed.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.metering2.*;

import static com.alcatel.as.ioh.tools.ByteBufferUtils.getUTF8;

@Component(service={HttpIOHRouterFactory.class}, property={"router.id=smart"}, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class SmartHttpIOHRouterFactory extends HttpIOHRouterFactory {

    @Reference
    public void setSessionManager (SessionManager mgr){
	super.setSessionManager (mgr);
    }
    @Modified
    public void updated (Map<String, String> conf){
    }
    
    @Activate
    public void activate(Map<String, String> conf) {
    }
    
    @Deactivate
    public void stop() {	
    }

    @Override
    public String toString (){
	return "SmartHttpIOHRouterFactory";
    }
    
    @Override
    public HttpIOHRouter newHttpIOHRouter (){
	return new SmartHttpIOHRouter (this, LOGGER);
    }

    public static class SmartHttpIOHRouter extends HttpIOHRouter {

	protected String[] _smartHeaders;
	protected boolean _smartURLFirst, _smartURLLast;

	protected Meter _routedReqSmartMeter;

	public SmartHttpIOHRouter (SmartHttpIOHRouterFactory factory, Logger logger){
	    super (factory, logger);
	}
	@Override
	public String toString (){ return "SmartHttpIOHRouter";}
    
	/************************* The public methods called by the HttpIOH ********************************/
	
	@Override
	public void init (IOHEngine engine){
	    super.init (engine);
	    Map<String, Object> props = engine.getProperties ();
	    String s = getStringPropertyAsString ("http.ioh.router.smart.Headers", props, "cookie cookie2");
	    _smartHeaders = s != null ? s.split (" +") : new String[0];
	    String smartURL = getStringPropertyAsString ("http.ioh.router.smart.URL", props, "last").toLowerCase ();
	    _smartURLFirst = smartURL.equals ("first");
	    _smartURLLast = smartURL.equals ("last");

	    _routedReqSmartMeter = engine.getIOHMeters ().createIncrementalMeter ("router.routed.req.smart", null);
	}

	@Override
	public void initMuxClient (MuxClient agent){
	    agent.setContext (new SmartAgentContext ().init (agent));
	}
	protected class SmartAgentContext extends AgentContext {
	    protected Meter _routedReqSmartMeter;
	    @Override
	    protected AgentContext init (MuxClient agent){
		super.init (agent);
		_routedReqSmartMeter = agent.getIOHMeters ().createIncrementalMeter ("router.routed.req.smart", SmartHttpIOHRouter.this._routedReqSmartMeter);
		return this;
	    }
	}

	/****************************************************************************************************/
    
	/************************* routing methods ********************************/

	@Override
	protected boolean routeCustom (HttpIOHChannel channel, HttpMessage msg){
	    if (_smartURLFirst && routeBySmartKey (channel, msg, parseSmartKey (msg.getURL ())))
		return true;
	    for (String headerName : _smartHeaders){
		HttpMessage.Header header = msg.getHeader (headerName);
		while (header != null){
		    String value = header.getValue ();
		    if (value != null && routeBySmartKey (channel, msg, parseSmartKey (value)))
			return true;
		    header = header.getNext ();
		}
	    }
	    if (_smartURLLast && routeBySmartKey (channel, msg, parseSmartKey (msg.getURL ())))
		return true;
	    return false;
	}
	protected boolean routeBySmartKey (HttpIOHChannel channel, HttpMessage msg, String[] ids){
	    if (ids != null){
		MuxClient agent = null;
		for (int i=3; i<ids.length; i++){
		    String agentId = new StringBuilder ().append (ids[1]).append ('-').append (ids[i]).append ('-').append (ids[2]).toString ();
		    agent = channel.getAgent (agentId);
		    if (agent != null) break;
		}
	    
		// TODO if agent == null look up new agent
		if (agent == null) return false;
	    
		if (sendToAgent (channel, agent, msg, PRIORITY_SUBSEQUENT)){
		    SmartAgentContext ctx = agent.getContext ();
		    ctx._routedReqSmartMeter.inc (1);
		}
		return true;
	    }
	    return false;
	}

	protected String[] parseSmartKey (String value){
	    if (value == null) return null;
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : looking for smart key in : "+value);
	    return _factory.getSessionManager ().getIdsFromSmartKey (value);
	}
    }
}
