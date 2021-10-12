// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb.mux.agent;

import java.util.*;
import java.util.concurrent.*;
import java.nio.*;
import java.net.*;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.apache.log4j.Logger;
import org.osgi.framework.*;

import com.nextenso.mux.*;
import com.alcatel.as.ioh.server.*;

@Component(service={}, immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class LbMuxHandler {

    static final Logger LOGGER = Logger.getLogger("as.ioh.lb.mux.agent");

    private List<TcpAgent> _agents = new ArrayList<> ();
    private BundleContext _osgi;
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setTcpAgent (TcpAgent agent, Map<String, String> properties){
	LOGGER.info ("@Reference setTcpAgent : "+agent.id ()+" : "+agent);
	_agents.add (agent);
	initAgents ();
    }
    public void unsetTcpAgent (TcpAgent agent){
    }
    
    @Activate
    public synchronized void init (BundleContext ctx, Map<String, String> conf){
	_osgi = ctx;
	initAgents ();
    }

    private void initAgents (){
	if (_osgi == null) return;
	for (TcpAgent agent : _agents){
	    TcpAgentMuxHandler handler = new TcpAgentMuxHandler (this, agent);
	    handler.register ();
	}
        _agents.clear ();
    }

    private static class TcpAgentMuxHandler extends MuxHandler {

	private TcpAgent _agent;
	private LbMuxHandler _mh;
	private String _toString;
	private String _id;

	private Map<Integer, TcpClientImpl> _clients = new ConcurrentHashMap<> ();

	private TcpAgentMuxHandler (LbMuxHandler mh, TcpAgent agent){
	    _mh = mh;
	    _agent = agent;
	    _id = agent.protocol () + "-" + agent.id ();
	    _toString = "TcpAgentMuxHandler["+_id+"]";
	}
	public String toString (){ return _toString;}
	protected void register (){
	    LOGGER.info (this+" : register");
	    // the following props will be set in the opened TcpServers, all the processor.advertize in particular will be used in advertizing
	    Dictionary props = new Hashtable ();
	    props.put ("protocol", _id);
	    props.put ("autoreporting", "false");
	    props.put ("hidden", "true");
	    _mh._osgi.registerService (MuxHandler.class.getName (), this, props);
	}

	// ---------------- MuxHandler interface
	// -----------------------------------------------------------

	/** Called by the CalloutAgent when it has seen our MuxHandler */
	@SuppressWarnings("unchecked")
	@Override
	public void init(int appId, String appName, String appInstance, MuxContext muxContext) {
	    // Don't forget to call the super.init method !
	    super.init(appId, toString (), appInstance, muxContext);

	    // Configure our MUX handler for the Web protocol
	    getMuxConfiguration().put(CONF_STACK_ID, new int[]{_agent.id ()});
	    getMuxConfiguration().put(CONF_USE_NIO, true);
	    getMuxConfiguration().put(CONF_THREAD_SAFE, true);
	    getMuxConfiguration().put(CONF_IPV6_SUPPORT, true);
	    getMuxConfiguration().put(CONF_L4_PROTOCOLS, new String[]{"tcp"});
	}
	
	@Override
	public void muxOpened(final MuxConnection connection) {
	    LOGGER.warn (this+" : muxOpened : "+connection);
	}

	@Override
	public void muxClosed(MuxConnection connection) {
	    LOGGER.warn (this+" : muxClosed : "+connection);
	}
	
	@Override
	public void tcpSocketConnected(MuxConnection connection, int sockId, String remoteIP, int remotePort,
				       String localIP, int localPort, String virtualIP, int virtualPort, boolean secure, boolean clientSocket,
				       long connectionId, int errno) {
	    TcpClientImpl client = new TcpClientImpl (connection, sockId, localIP, localPort, remoteIP, remotePort);
	    if (LOGGER.isInfoEnabled ())
		LOGGER.info (this+" : tcpSocketConnected : "+client);
	    _clients.put (sockId, client);
	    _agent.clientOpened (client);
	}

	@Override
	public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer buf) {
	    TcpClientImpl client = _clients.get (sockId);
	    if (client == null){
		LOGGER.warn (this+" : tcpSocketData : no client found : closing");
		connection.sendTcpSocketClose (sockId);
		return;
	    }
	    if (LOGGER.isDebugEnabled ())
		LOGGER.debug (this+" : tcpSocketData : "+client+" : "+buf.remaining ());
	    _agent.clientData (client, buf);
	}

	@Override
	public void tcpSocketClosed(MuxConnection connection, int sockId) {
	    TcpClientImpl client = _clients.remove (sockId);
	    if (client == null){
		LOGGER.warn (this+" : tcpSocketClosed : no client found");
		return;
	    }
	    if (LOGGER.isDebugEnabled ())
		LOGGER.debug (this+" : tcpSocketClosed : "+client);
	    _agent.clientClosed (client);
	}

	public void destroy() {
	}

	@Override
	public int getMinorVersion() {
	    return 0;
	}

	@Override
	public int getMajorVersion() {
	    return 1;
	}

	@Override
	public int[] getCounters() {
	    throw new RuntimeException("deprecated method, should not be used anymore");
	}

	@Override
	public void commandEvent(int command, int[] intParams, String[] strParams) {
	}
    }

    /********************************
     * Utils *
     *******************************/

    private static class TcpClientImpl implements TcpAgent.TcpClient {
	private int _id;
	private MuxConnection _mux;
	private Object _attachment;
	private InetSocketAddress _local, _remote;

	private TcpClientImpl (MuxConnection mux, int id, String localIP, int localPort, String remoteIP, int remotePort){
	    _mux = mux;
	    _id = id;
	    try{
		_local = new InetSocketAddress (InetAddress.getByName (localIP), localPort);
		_remote = new InetSocketAddress (InetAddress.getByName (remoteIP), remotePort);
	    }catch(Exception e){
		// cannot happen
	    }
	}

	public String toString (){ return "TcpClient[id="+id()+"/"+getRemoteAddress ()+"]";}
	
	public int id (){ return _id;}

	public void attach (Object x){ _attachment = x;}
	public <T> T attachment (){ return (T) _attachment;}

	public InetSocketAddress getRemoteAddress (){ return _local;}
	public InetSocketAddress getLocalAddress (){ return _remote;}

	
	public void close (){
	    _mux.sendTcpSocketClose (_id);
	}
	public void send (boolean copy, ByteBuffer... data){
	    _mux.sendTcpSocketData (_id, copy, data);
	}
    }
}
