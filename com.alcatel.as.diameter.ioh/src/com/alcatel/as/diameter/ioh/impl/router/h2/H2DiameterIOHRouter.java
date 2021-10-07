package com.alcatel.as.diameter.ioh.impl.router.h2;

import java.util.*;
import org.apache.log4j.Logger;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.alcatel.as.diameter.ioh.*;
import com.alcatel.as.diameter.parser.*;
import com.nextenso.mux.*;

import com.alcatel.as.ioh.engine.IOHEngine;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;

import com.alcatel.as.http2.client.api.*;

public class H2DiameterIOHRouter extends DiameterIOHRouter {
    
    protected static final byte[] _3002 = DiameterUtils.setIntValue (3002, new byte[4], 0);
    protected static final DiameterUtils.Avp _3002AVP = new DiameterUtils.Avp (268, 0, true, _3002);    
    
    protected H2DiameterIOHRouterFactory _defH2Factory;
    protected HttpClientFactory _h2ClientF;
    protected HttpClient _h2Client;
    protected URI _uri;
    protected ConcurrentHashMap<Integer, MuxClient> _agentsByHopId = new ConcurrentHashMap<> ();
    protected Map<Long, URI> _uris = new HashMap<> ();
    protected int MAX_REDIRECT = 1;
    
    public H2DiameterIOHRouter (DiameterIOHRouterFactory factory, Logger logger){
	super (factory, logger);
	_defH2Factory = (H2DiameterIOHRouterFactory) factory;
	_h2ClientF = _defH2Factory.getH2ClientFactory ();
    }

    @Override
    public void init (IOHEngine engine){
	super.init (engine);
	Map<String, Object> props = engine.getProperties ();
	Object o = props.get (H2DiameterIOHRouterFactory.CONF_H2_URI);
	if (o == null) _logger.error (engine+" : CONFIGURATION MISSING : "+H2DiameterIOHRouterFactory.CONF_H2_URI, new Exception ("Failed to initialize"));
	else{
	    if (o instanceof String){
		String s = (String) o;
		try{
		    _uri = new URI (s);
		    if (_logger.isDebugEnabled ()) _logger.debug (engine+" : default http2 URI is : "+_uri);
		}
		catch(Exception e){
		    _logger.error (engine+" : INVALID CONFIGURATION : "+H2DiameterIOHRouterFactory.CONF_H2_URI+" : invalid uri : "+s, new Exception ("Failed to initialize"));
		}
	    }else{
		List<String> list = (List<String>) o;
		for (String s : list){
		    try{
			int index = s.indexOf (' ');
			if (index == -1){
			    _uri = new URI (s);
			    if (_logger.isDebugEnabled ()) _logger.debug (engine+" : default http2 URI is : "+_uri);
			}else{
			    long appId = Long.parseLong (s.substring (0, index).trim ());
			    URI uri = new URI (s.substring (index+1).trim ());
			    _uris.put (appId, uri);
			    if (_logger.isDebugEnabled ()) _logger.debug (engine+" : defined http2 URI : "+uri+" for appId : "+appId);
			}
		    }
		    catch(Exception e){
			_logger.error (engine+" : INVALID CONFIGURATION : "+H2DiameterIOHRouterFactory.CONF_H2_URI+" : invalid entry : "+s, new Exception ("Failed to initialize"));
		    }
		}
	    }
	}
	_h2Client = _h2ClientF.newHttpClient ();
	String s = (String) props.get (H2DiameterIOHRouterFactory.CONF_REDIRECT_MAX);
	if (s != null) MAX_REDIRECT = Integer.parseInt (s);
	if (_logger.isDebugEnabled ()) _logger.debug (engine+" : max http2 redirect : "+MAX_REDIRECT);
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

    @Override
    public void doClientRequest (DiameterIOHChannel client, DiameterMessage msg){
	long appId = msg.getApplicationID ();
	URI uri = _uris.get (appId);
	if (uri == null) uri = _uri;
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : doClientRequest : appId="+appId+" : uri="+uri);
	if (uri == null){
	    failed (client, msg, new Exception ("No http2 URI for appId="+appId));
	    return;
	}
	doClientRequest (client, msg, uri, 0, null);
    }
    private void doClientRequest (DiameterIOHChannel client, DiameterMessage msg, URI uri, int nbredirects, HttpHeaders redirectHeaders){
	if (nbredirects > MAX_REDIRECT){
	    failed (client, msg, new Exception ("Max redirect reached"));
	    return;
	}
	HttpRequest.Builder builder = _h2ClientF.newHttpRequestBuilder()
	    .uri(uri)
	    .header("content-type", "application/octet-stream");

	try{
	    _defH2Factory.getHeaders ().decorate (client, msg, builder);
	}catch(Exception e){
	    failed (client, msg, e);
	    return;
	}

	if (redirectHeaders != null){
	    Map<String, List<String>> headers = redirectHeaders.map();
	    for (String name : headers.keySet ()){
		if (name.toLowerCase ().startsWith ("x-redirect-")){
		    String n = name.substring ("x-redirect-".length ());
		    String v = redirectHeaders.firstValue (name).get ();
		    builder.header (n, v);
		    if (_logger.isDebugEnabled ())
			_logger.debug ("setting redirect header : name = "+n+" value = "+v);
		}
	    }
	}
	
	builder.POST(_h2ClientF.bodyPublishers().ofByteArray(msg.getBytes ()));
	
	HttpRequest post = builder.build();
	
	CompletableFuture<HttpResponse<byte[]>> cf =
	    _h2Client.sendAsync(post, _h2ClientF.bodyHandlers().ofByteArray())
	    .whenCompleteAsync((response, exception) -> {
                    if (exception != null) {
			failed (client, msg, exception);
                    } if (response != null) {
			//_logger.warn ("x-test-1 = "+response.headers().firstValue("x-test-1").orElse ("----"));
			if (response.statusCode () == 200)
			    respond (client, msg, response.body ());
			else {
			    if (response.statusCode () == 302){
				String location = response.headers().firstValue("Location").orElse (uri.toString ()); // we allow a null location -> we re-use the same (used by SPS)
				if (location.startsWith ("http")){
				    try{
					URI nuri = new URI (location);
					if (_logger.isDebugEnabled ())
					    _logger.debug (this+" : doClientRequest : redirecting to "+location);
					doClientRequest (client, msg, nuri, nbredirects+1, response.headers ());
					return;
				    }catch(Exception e){
					if (_logger.isInfoEnabled ())
					    _logger.info (this+" : doClientRequest : invalid redirect URL : "+location);
					failed (client, msg, new Exception ("Server responded "+response.statusCode ()+" - with invalid redirect : "+location));
					return;
				    }
				}
			    }
			    failed (client, msg, new Exception ("Server responded "+response.statusCode ()+" - Warning = ["+response.headers().firstValue("Warning").orElse ("")+"]"));		    
			}
		    } else {
			// response is null this should not happen with this API
                    }
		    // It is better to specify your executor where the lambda will be executed
		    // to prevent concurrent access to your internal state.
                }, client.getPlatformExecutor ());
    }

    private void respond (DiameterIOHChannel client, DiameterMessage req, byte[] respData){
	DiameterMessage resp = new DiameterMessage (respData);
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : response received : "+resp.getResultCode ());
	client.sendOut (true, resp);
    }

    private void failed (DiameterIOHChannel client, DiameterMessage req, Throwable t){
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : failed to send request : "+client+" : "+req+" : "+t);
	DiameterUtils.Avp errMsgAvp = null;
	DiameterMessage resp = DiameterUtils.makeResponse (req, client.getIOHOriginHost (), client.getIOHOriginRealm (), _3002AVP);
	client.sendOut (true, resp);
    }

    public void doAgentRequest (DiameterIOHChannel client, MuxClient agent, DiameterMessage msg){
	int hop = msg.getHopIdentifier ();
	AgentContext ctx = agent.getContext ();
	msg.updateHopIdentifier (hop | ctx._hopId);
	client.sendOut (true, msg);
    }
    public void doClientResponse (DiameterIOHChannel client, DiameterMessage msg){
	int hop = msg.getHopIdentifier ();
	MuxClient agent = _agentsByHopId.get (hop & _agentHopIdGetMask);
	msg.updateHopIdentifier (hop & _agentHopIdRemoveMask);
	if (agent == null || agent.isOpened () == false){
	    if (_logger.isDebugEnabled ()) _logger.debug (this+" : no agent matching the response hop id : "+hop);
	    return;
	}
	sendToAgent (client, agent, msg, 0L);
    }
    
    public void doAgentResponse (DiameterIOHChannel client, MuxClient agent, DiameterMessage msg){
	throw new IllegalStateException (this+" : doAgentResponse not expected from "+agent);
    }

    protected boolean sendToAgent (DiameterIOHChannel client, MuxClient agent, DiameterMessage msg, long sessionId){
	int check = checkAgentOverload (agent, msg);
	if (check == 0){
	    client.sendAgent (agent, msg, sessionId);
	    return true;
	}
	// only resps for now
	return false;
    }
}
