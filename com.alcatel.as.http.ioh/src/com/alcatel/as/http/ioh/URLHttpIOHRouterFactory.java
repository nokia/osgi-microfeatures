// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http.ioh;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;

import java.util.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;

import com.alcatel.as.http.parser.*;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClientState;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.alcatel.as.ioh.tools.ByteBufferUtils;
import com.alcatel.as.session.distributed.*;
import com.alcatel.as.service.concurrent.*;

import static com.alcatel.as.ioh.tools.ByteBufferUtils.getUTF8;

import java.util.regex.*;

@Component(service={HttpIOHRouterFactory.class}, property={"router.id=url"}, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class URLHttpIOHRouterFactory extends HttpIOHRouterFactory {
    
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
	return "URLHttpIOHRouterFactory";
    }
    
    @Override
    public HttpIOHRouter newHttpIOHRouter (){
	return new URLHttpIOHRouter (this, LOGGER);
    }

    public static class URLHttpIOHRouter extends HttpIOHRouter {

	private static BiFunction<HttpIOHChannel, HttpMessage, String> byURL = (ch, msg) -> {return msg.getURL ();};
	private static BiFunction<HttpIOHChannel, HttpMessage, String> byVersion = (ch, msg) -> {return String.valueOf (msg.getVersion ());};
	private static BiFunction<HttpIOHChannel, HttpMessage, String> byMethod = (ch, msg) -> {return msg.getMethod ();};
	private static BiFunction<HttpIOHChannel, HttpMessage, String> byPort = (ch, msg) -> {
	    TcpChannel tcp = ch.getChannel ();
	    return String.valueOf (tcp.getLocalAddress ().getPort ());
	};
	private static BiFunction<HttpIOHChannel, HttpMessage, String> byIP = (ch, msg) -> {
	    TcpChannel tcp = ch.getChannel ();
	    return tcp.getRemoteAddress ().getAddress ().getHostAddress ();
	};

	private List<BiFunction<HttpIOHChannel, HttpMessage, Boolean>> _evaluators = new ArrayList<> ();
	private List<String> _destinations = new ArrayList<> ();
	private boolean _byProtocol = false, _byGroup = false;;

	private AtomicInteger SEED = new AtomicInteger (0);

	public URLHttpIOHRouter (URLHttpIOHRouterFactory factory, Logger logger){
	    super (factory, logger);
	}

	@Override
	public void init (IOHEngine engine){
	    super.init (engine);
	    Map<String, Object> props = engine.getProperties ();
	    for (String rule : getStringListProperty ("http.ioh.router.URL.rule", props)){
		int index = rule.indexOf (' ');
		if (index == -1 || index == 0 || index == rule.length () - 1){
		    _logger.error (this+" : Invalid http.ioh.router.URL.rule : "+rule);
		    continue;
		}
		String regex = rule.substring (0, index);
		String dest = rule.substring (index).trim ();
		if (dest.length () == 0){
		    _logger.error (this+" : Invalid http.ioh.router.URL.rule : "+rule);
		    continue;
		}
		if (dest.startsWith ("protocol:")){
		    if (dest.length () == "protocol:".length ()){
			_logger.error (this+" : Invalid http.ioh.router.URL.rule : "+rule);
			continue;
		    }
		    _byProtocol = true;
		} else {
		    _byGroup = true;
		}
		BiFunction<HttpIOHChannel, HttpMessage, String> extractor = byURL;
		index = regex.indexOf (':');
		if (index > 0){
		    String by = regex.substring (0, index);
		    regex = regex.substring (index+1);
		    switch (by){
		    case "header":
			index = regex.indexOf ('=');
			String name = regex.substring (0, index).toLowerCase ();
			regex = regex.substring (index+1);
			extractor = (ch, msg) -> {
			    return msg.getHeaderValue (name);
			};
			break;
		    case "version":
			extractor = byVersion;
			break;
		    case "port":
			extractor = byPort;
			break;
		    case "ip":
			extractor = byIP;
			break;
		    case "method":
			extractor = byMethod;
			break;
		    }
		}
		Pattern p = Pattern.compile(regex);
		final BiFunction<HttpIOHChannel, HttpMessage, String> fextractor = extractor;
		BiFunction<HttpIOHChannel, HttpMessage, Boolean> f = (ch, msg) -> {
		    String s = fextractor.apply (ch, msg);
		    if (s == null) return false;
		    return p.matcher (s).find ();
		};
		// add them in order
		_evaluators.add (f);
		_destinations.add (dest);
		_logger.debug (this+" : added rule : "+regex+" to "+dest);
	    }
	}
    
	/************************* The public methods called by the HttpIOH ********************************/

	@Override
	public void agentConnected (HttpIOHChannel channel, MuxClient agent, MuxClientState state){
	    if (state.stopped ()) return;
	    Map<String, List<MuxClient>> agentsMap = channel.attachment ();
	    if (agentsMap == null)
		channel.attach (agentsMap = new HashMap<String, List<MuxClient>>());
	    if (_byGroup){
		List<MuxClient> agentsList = agentsMap.get (agent.getGroupName ());
		if (agentsList == null)
		    agentsMap.put (agent.getGroupName (), agentsList = new ArrayList<MuxClient> ());
		agentsList.add (agent);
	    }
	    if (_byProtocol){
		String protocol = "protocol:"+agent.getApplicationParam ("agent.protocol", "na");
		List<MuxClient> agentsList = agentsMap.get (protocol);
		if (agentsList == null)
		    agentsMap.put (protocol, agentsList = new ArrayList<MuxClient> ());
		agentsList.add (agent);
	    }
	}
	@Override
	public void agentClosed (HttpIOHChannel channel, MuxClient agent){
	    Map<String, List<MuxClient>> agentsMap = channel.attachment ();
	    if (agentsMap == null) // should not happen
		return;
	    if (_byGroup){
		List<MuxClient> agentsList = agentsMap.get (agent.getGroupName ());
		if (agentsList != null) // null should not happen
		    agentsList.remove (agent);
	    }
	    if (_byProtocol){
		String protocol = "protocol:"+agent.getApplicationParam ("agent.protocol", "na");
		List<MuxClient> agentsList = agentsMap.get (protocol);
		if (agentsList != null) // null should not happen
		    agentsList.remove (agent);
	    }
	}
	@Override
	public void agentStopped (HttpIOHChannel channel, MuxClient agent){
	    agentClosed (channel, agent);
	}
	@Override
	public void agentUnStopped (HttpIOHChannel channel, MuxClient agent){
	    agentConnected (channel, agent, new MuxClientState ().stopped (false));
	}

	/****************************************************************************************************/
    
	/************************* routing methods ********************************/

	@Override
	protected boolean routeCustom (HttpIOHChannel channel, HttpMessage msg){
	    int i=0;
	    loop : for (BiFunction<HttpIOHChannel, HttpMessage, Boolean> f : _evaluators){
		if (f.apply (channel, msg)){
		    String dest = _destinations.get (i);
		    Map<String, List<MuxClient>> agentsMap = channel.attachment ();
		    if (agentsMap == null) // should not happen
			break loop;
		    List<MuxClient> agentsList = agentsMap.get (dest);
		    int size = 0;
		    if (agentsList == null || (size = agentsList.size ()) == 0)
			break loop;
		    
		    MuxClient agent = null;
		    if (size == 1){
			agent = agentsList.get (0);
		    } else {
			int rnd = ThreadLocalRandom.current ().nextInt (size);
			agent = agentsList.get (rnd);
			long load = agent.getLoadMeter ().getValue ();
			if (load != 0L){
			    select : for (int k=1; k<_agentLoadSelect; k++){
				rnd = ThreadLocalRandom.current ().nextInt (size);
				MuxClient agent2 = agentsList.get (rnd);
				if (agent2 == agent){
				    continue select;
				}
				long load2 = agent2.getLoadMeter ().getValue ();
				if (load2 < load){
				    AgentContext ctx = agent.getContext ();
				    ctx._dismissMeter.inc (1);
				    agent = agent2;
				    load = load2;
				    if (load == 0L) break select;
				}
			    }
			}
		    }
		    
		    sendToAgent (channel, agent, msg, PRIORITY_INITIAL);
		    return true;
		}
		i++;
	    }
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : no matching destination for URL : "+msg.getURL ());
	    // TODO add a meter
	    return returnError (channel, msg, _errorUnavailable, "No matching destination");
	}
    }
}
