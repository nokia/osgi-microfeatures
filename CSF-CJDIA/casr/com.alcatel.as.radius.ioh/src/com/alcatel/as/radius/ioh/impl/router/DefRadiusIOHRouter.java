package com.alcatel.as.radius.ioh.impl.router;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.apache.log4j.Logger;
import java.nio.charset.Charset;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.nextenso.mux.*;

import com.alcatel.as.radius.ioh.*;
import com.alcatel.as.radius.parser.*;
import com.alcatel.as.radius.ioh.impl.*;

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
public class DefRadiusIOHRouter extends RadiusIOHRouter {

    protected boolean _useAuthenticator;

    public DefRadiusIOHRouter (RadiusIOHRouterFactory factory, Logger logger){
	super (factory, logger);
    }

    @Override
    public void init (IOHEngine engine){
	super.init (engine);
	_useAuthenticator = getBooleanProperty (DefRadiusIOHRouterFactory.CONF_ROUTE_BY_AUTHENTICATOR, engine.getProperties (), true);
	_logger.warn (this+" : "+DefRadiusIOHRouterFactory.CONF_ROUTE_BY_AUTHENTICATOR+" : "+_useAuthenticator);
    }

    @Override
    public void doClientRequest (RadiusIOHChannel channel, RadiusMessage msg){
	if (routeCustom (channel, msg)) return;

	routeDefault (channel, msg);
    }
    protected boolean routeDefault (RadiusIOHChannel channel, RadiusMessage msg){
	MuxClient agent = null;
	if (_useAuthenticator){
	    // for stickiness, we rely on the authenticator
	    int stickiness = 0;
	    byte[] bytes = msg.getBytes ();
	    for (int i=4; i<12; i++) stickiness += bytes[i]; // take only 8 bytes for speed
	    agent = channel.pickAgent (hash(stickiness));
	} else {
	    agent = channel.pickAgent (null);
	    if (agent != null){
		long load = agent.getLoadMeter ().getValue ();
		if (load != 0L){
		    MuxClient agent2 = channel.pickAgent (null);
		    if (agent != agent2){
			long load2 = agent2.getLoadMeter ().getValue ();
			if (load2 < load){
			    AgentContext ctx = agent.getContext ();
			    ctx._dismissMeter.inc (1);
			    agent = agent2;		    
			}
		    }
		}
	    }
	}
	
	if (agent == null){
	    dropRequest (channel, msg, null);
	    return true;
	}
	sendToAgent (channel, agent, msg);
	return true;
    }
    // to be overridden
    protected boolean routeCustom (RadiusIOHChannel channel, RadiusMessage msg){
	return false;
    }
    
    protected void dropRequest (RadiusIOHChannel channel, RadiusMessage msg, MuxClient agent){
	_droppedMeters.getMeter (msg).inc (1);
	if (agent != null)
	    getAgentContext (agent).getDroppedMeters ().getMeter (msg).inc (1);
    }
    
    protected boolean sendToAgent (RadiusIOHChannel channel, MuxClient agent, RadiusMessage msg){
	if (checkAgentOverload (agent, msg)){
	    channel.sendAgent (agent, msg);
	    _routedMeters.getMeter (msg).inc (1);
	    getAgentContext (agent).getRoutedMeters ().getMeter (msg).inc (1);
	    return true;
	}
	dropRequest (channel, msg, agent);
	return false;
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
