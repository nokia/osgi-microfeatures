// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl.ioh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.*;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;
import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpClientOption;
import alcatel.tess.hometop.gateways.utils.IntHashtable;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.recorder.RecorderService;
import com.alcatel.as.service.recorder.Record;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.util.MeteringRegistry;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.socket.TcpMessageParser;

public class IOHMuxFactory extends MuxFactory {

    // Our logger
    private final static Logger _logger = Logger.getLogger("as.service.mux.IOHMuxFactory");
  
    // Reactor Provider used to create Reactors
    protected volatile ReactorProvider _reactorProvider; // injected

    // RecorderService
    protected volatile RecorderService _recorderService; // injected

    // EventAdmin service
    protected volatile EventAdmin _eventAdmin; // injected
  
    // socket connection timeout
    protected long _cnxTimeout = 15000L;

    protected volatile PlatformExecutors _execs; // injected
    protected volatile MeteringService _metering; // injected
    protected volatile MeteringRegistry _meteringRegistry; // injected
    protected volatile BundleContext _bctx;

    protected Record _muxRecord;

    private static class ConnectionAttachment {
	private long _connectionTimeout;
	private Reactor _reactor;
	private Object _attachment;
	private IntHashtable _flags;
	private Map _opts;
    }

    public void start (){
	_muxRecord = _recorderService.newRecord("agent.mux", null, false);
    }
  
    @SuppressWarnings("rawtypes")
    @Override
    public MuxConnection newMuxConnection(Reactor reactor, ConnectionListener listener, MuxHandler muxHandler,
					  InetSocketAddress to, int stackId, String stackName,
					  String stackHost, String stackInstance, Map opts) {
	ConnectionAttachment connectionAttcht = new ConnectionAttachment ();
	connectionAttcht._reactor = reactor;
	connectionAttcht._opts = opts;
	Logger logger = (opts != null) ? (Logger) opts.get(OPT_LOGGER) : _logger;
	InetSocketAddress from = (opts != null) ? (InetSocketAddress) opts.get(OPT_LOCAL_ADDR) : null;
	connectionAttcht._connectionTimeout = _cnxTimeout;
	if (opts != null && opts.get(OPT_CONNECTION_TIMEOUT) != null) {
	    connectionAttcht._connectionTimeout = (Long) opts.get(OPT_CONNECTION_TIMEOUT);
	}
    
	connectionAttcht._attachment = (opts != null) ? (Object) opts.get(OPT_ATTACH) : null;
	connectionAttcht._flags = (opts != null) ? (IntHashtable) opts.get(OPT_FLAGS) : null;
    
	AgentSideMuxConnection cnx = new AgentSideMuxConnection (this, muxHandler, listener, logger);
	cnx.setStackInfo (stackId, stackName, stackInstance, stackHost);
	cnx.setAddresses (to, from);
	cnx.setMeteringService(_metering);
	cnx.setBundleContext(_bctx);
	cnx.setProtocol((String) opts.get(PROTOCOL));
	cnx.attach (connectionAttcht);
	return cnx;
    }
  
    //   @Deprecated use the MuxFactory service with the OSGI service property "local=true".
    @Override
    public MuxConnection newLocalMuxConnection(Reactor reactor, MuxHandler mh, int stackAppId,
					       String stackAppName, String stackInstance, TcpMessageParser parser, Logger logger) {
	return null;
    }
  
    @Override
    public void connect(MuxConnection cnx) {
	if (!(cnx instanceof AgentSideMuxConnection)) {
	    return;
	}
	AgentSideMuxConnection impl = (AgentSideMuxConnection) cnx;
	ConnectionAttachment connectionAttcht = (ConnectionAttachment) impl.attachment ();
	impl.attach (null);
	
	Executor inExec = null;
	Map muxOpts = connectionAttcht._opts;
	inExec = (Executor) muxOpts.get(OPT_INPUT_EXECUTOR);
    
	Map<TcpClientOption, Object> opts = new HashMap<TcpClientOption, Object>();
	opts.put(TcpClientOption.FROM_ADDR, cnx.getLocalAddress());
	opts.put(TcpClientOption.TIMEOUT, connectionAttcht._connectionTimeout);
	opts.put(TcpClientOption.PRIORITY, AsyncChannel.MAX_PRIORITY);
	opts.put(TcpClientOption.TCP_NO_DELAY, Boolean.TRUE);
	if (inExec != null) {
	    opts.put(TcpClientOption.INPUT_EXECUTOR, inExec);
	}
	
	_reactorProvider.tcpConnect(connectionAttcht._reactor, cnx.getRemoteAddress(), impl, opts);
    }
  
    @SuppressWarnings("rawtypes")
    @Override
    public InetSocketAddress accept(Reactor r, ConnectionListener l, MuxHandler mh, InetSocketAddress from,
				    Map opts) throws IOException {
	return null;
    }

    public EventAdmin getEventAdmin (){ return _eventAdmin;}
    public PlatformExecutors getPlatformExecutors (){ return _execs;}
    public MeteringService getMeteringService (){ return _metering;}
    public MeteringRegistry getMeteringRegistry (){ return _meteringRegistry;}

    public Record getMuxRecord (){ return _muxRecord;}
}
