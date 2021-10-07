package com.nextenso.mux.impl.ioh;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.util.sctp.SctpSocketOption;
import com.alcatel.as.util.sctp.SctpSocketParam;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.MuxFactory.ConnectionListener;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.util.MuxHandlerMeters;
import com.nextenso.mux.util.MuxIdentification;

import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.util.FlowController;

@ProviderType
public class AgentSideMuxConnection extends ExtendedMuxConnection implements TcpClientChannelListener {

    public static final String PROP_MUX_READ_TIMEOUT = "ioh.mux.read.timeout";
    public static final String PROP_MUX_SCHEDULE_LOW_WM = "ioh.mux.schedule.lowWM";
    public static final String PROP_MUX_SCHEDULE_HIGH_WM = "ioh.mux.schedule.highWM";
    
    private static final int PING_NB = 2;
    private static final long PING_DELAY = Long.parseLong (System.getProperty (PROP_MUX_READ_TIMEOUT, "3000"));

    protected IOHMuxFactory _factory;
    protected TcpChannel _channel;
    protected MuxParser _parser;
    private int _pings = PING_NB;
    private boolean _threadSafe;
    private PlatformExecutors _execs;
    private FlowController _flowController;
    private MeteringService _meteringService;
    private BundleContext _bundleContext;
    private SimpleMonitorable _mon;
    private MuxConnectionMeters _meters = new DummyMuxConnectionMeters();
    private MuxHandler _wrappedHandler;
    private MuxHandlerProxy _extHandler; // same as _handler, but casted
    private String _protocol;
    
    // used by RemoteIOHEngine in ioh
    // no longer maintained : _factory is null --> NPE
    public AgentSideMuxConnection (MuxHandler mh, ConnectionListener listener, Logger logger){
	super (null, listener, logger);
	_wrappedHandler = mh;
	setMuxHandler (_extHandler = new MuxHandlerProxy ());
	_parser = new MuxParser (mh);
	_threadSafe = false; // the remote ioh engine handles multithread on its own
    }
    // used by Agent
    public AgentSideMuxConnection (IOHMuxFactory factory, MuxHandler mh, ConnectionListener listener, Logger logger){
	this (mh, listener, logger);
	_factory = factory;
	_execs = _factory.getPlatformExecutors ();
	_threadSafe = ((Boolean) mh.getMuxConfiguration ().get(MuxHandler.CONF_THREAD_SAFE)).booleanValue();
    }
    
    public void setMeteringService(MeteringService meteringService) {
    	this._meteringService = meteringService;
    }
    
    public void setBundleContext(BundleContext bundleContext) {
    	this._bundleContext = bundleContext;
    }
    
    /**
     * Set the name of the applicative protocol handled by this mux connection.
     * This is used for the name of the Monitorable object associated with this
     * connection.
     * @param proto the name of the protocol
     */
    public void setProtocol(String proto) {
   	 this._protocol = proto;
    }

    @Override
    public Object getMonitorable() {
       return  _mon;
    }
    
    @Override // this is important to override it - since it is called by the code that gave the MuxHandler in the constructor
    public MuxHandler getMuxHandler (){ return _wrappedHandler;}
  
    public void shutdown (){ close ();}
    public void close() {
	_channel.close ();
    }

    public IOHMuxFactory getMuxFactory (){ return _factory;}
    public TcpChannel getChannel (){ return _channel;}
    public void schedule (Object queue, Runnable r){ _execs.getProcessingThreadPoolExecutor (queue).execute (r);} // only works in an agent
    public FlowController getFlowController (){ return _flowController;} // just in case someone else re-schedules sthg

    public void connectionEstablished(TcpChannel cnx) {
	_channel = cnx;
	_channel.setSoTimeout (PING_DELAY);
	if (_threadSafe) _flowController = new FlowController (_channel,
							       Integer.parseInt (System.getProperty (PROP_MUX_SCHEDULE_LOW_WM, "10000")),
							       Integer.parseInt (System.getProperty (PROP_MUX_SCHEDULE_HIGH_WM, "100000")),
							       _execs.getCurrentThreadContext ().getCurrentExecutor ());
	// TODO check that currentExec is OK
	MuxParser.sendMuxVersionCommand (cnx); // call it before opened(false) which sends out the MuxIdentification
	startMeters(); 
	opened (false);
    }
    
    public void connectionFailed(TcpChannel cnx, Throwable err) {
	failed (err);
	_meters.stop();
    }
    
    public void connectionClosed(TcpChannel cnx) {
	_extHandler.closed ();
	closed ();
	_meters.stop();
    }
    
    public int messageReceived(TcpChannel cnx, ByteBuffer buff) {
	_pings = PING_NB;
	while (true){
	    MuxParser.MuxEvent event = _parser.parse (buff);
	    if (event == null) return 0;
	    if (_logger.isDebugEnabled () &&
		!(event instanceof MuxParser.MuxPingEvent) &&
		!(event instanceof MuxParser.MuxPingAckEvent)
		) _logger.debug (this+" : received : "+event);
	    event.run (_parser, _extHandler, this);
	}
    }
    
    public void receiveTimeout(TcpChannel cnx) {
	if (--_pings > 0){
	    MuxParser.sendMuxPingCommand (_channel);
	    return;
	}
	_logger.warn("receive last timeout on cnx: " + cnx +" : SHUTDOWN");
	cnx.shutdown ();
    }
    
    public void writeBlocked(TcpChannel cnx) {
	if (_logger.isDebugEnabled()) {
	    _logger.debug("Write blocked on cnx: " + cnx);
	}
    }
    
    public void writeUnblocked(TcpChannel cnx) {
	if (_logger.isDebugEnabled()) {
	    _logger.debug("Write unblocked on cnx: " + cnx);
	}
    }
    
    private boolean connectionListenerCallback(Supplier<Boolean> cb) {
    	try {
    		if (_connectionListener != null) {
    			return cb.get();
    		}
    	} catch (Throwable err) {
    		_logger.error("connection listener exception", err);
    	}
    	return false;
    }

    @Override
    public void setMuxVersion (int version){
	super.setMuxVersion (version);
	if (getMuxVersion () == (1 << 16)){ // this is an old ioh --> need to revert to older MuxVersion
	    _logger.warn ("Stack MUX version is 1.0 --> downgrading MUX version");
	    _parser.downgrade ();
	} else
	    _parser.upgrade ();
    }

    
	private void startMeters() {
		String monitorableName = MuxConnectionMeters.makeConnectionMonitorableName(false, getStackInstance(), hashCode(),
				_protocol);
		_mon = new SimpleMonitorable(monitorableName, this.toString());

		String[] protos = (String[]) _wrappedHandler.getMuxConfiguration().get(MuxHandler.CONF_L4_PROTOCOLS);
		MuxHandlerMeters handlerMeters = (MuxHandlerMeters) _wrappedHandler.getMuxConfiguration()
				.get(MuxHandler.CONF_HANDLER_METERS);

		if (handlerMeters != null) {
			_meters = new AggregatingMuxConnectionMetersImpl(_meteringService, _mon, handlerMeters).initMeters(protos);
		} else {
			_meters = new MuxConnectionMetersImpl(_meteringService, _mon).initMeters(protos);
		}
		_mon.start(_bundleContext);

		if (_logger.isDebugEnabled()) {
			_logger.info("Registering monitorable for connection %s", this);
		}
	}

    /************************* mux *************************/
    
    public void sendMuxPingAck (){
	MuxParser.sendMuxPingAckCommand (_channel);
    }
  
    public boolean sendMuxStart() {
    	if (!_open) return false;    	
    	if (connectionListenerCallback(() -> _connectionListener.muxStarted(this))) {
    		_logger.info("sendMuxStart: connectionListener acknoledged, sending mux start");
    		MuxParser.sendMuxStart (_channel);
    		_meters.setMuxStarted(false, true);
    	} else {
    		_logger.info("sendMuxSart: connectionListener rejected, sending mux stop");
    		MuxParser.sendMuxStop (_channel);
    		_meters.setMuxStarted(false, false);
    	}    	
    	return true;
    }
    public boolean sendMuxStop() {
    	if (!_open) return false;
    	if (connectionListenerCallback(() -> _connectionListener.muxStopped(this))) {
    		_logger.info("sendMuxStop: connectionListener acknoledged, sending mux stop");
        	MuxParser.sendMuxStop (_channel);
    		_meters.setMuxStarted(false, false);
    	} else {
    		_logger.info("sendMuxStop: connectionListener rejected, sending mux start");
    		MuxParser.sendMuxStart (_channel);
    		_meters.setMuxStarted(false, true);
    	}
    	return true;
    }
    public boolean sendMuxData(MuxHeader header, boolean copy, ByteBuffer ... buf) {
	if (!_open) return false;
	MuxParser.sendMuxData (_channel, header, copy, buf);
	return true;
    }
    public boolean sendMuxIdentification(MuxIdentification id) {
	if (!_open) return false;
	MuxParser.sendMuxIdentification (_channel, id);
	return true;
    }
    public boolean sendInternalMuxData (MuxHeader h, boolean copy, ByteBuffer... buff){
	if (!_open) return false;
	MuxParser.sendInternalMuxData (_channel, h, copy, buff);
	return true;
    }
    
    /************************* tcp *************************/
  
    public boolean sendTcpSocketListen(long listenId, String localIP, int localPort, boolean secure) {
	if (!_open) return false;
	_parser.sendTcpSocketListen (_channel, listenId, localIP, localPort, secure);
	return true;
    }
    public boolean sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, String localIP,
					int localPort, boolean secure, java.util.Map<String, String> params) {
	if (!_open) return false;
	_parser.sendTcpSocketConnect (_channel, connectionId, remoteHost, remotePort, localIP, localPort, secure, params);
	return true;
    }
    public boolean sendTcpSocketParams(int sockId, java.util.Map<String, String> params){
	if (!_open) return false;
	_parser.sendTcpSocketParams (_channel, sockId, params);
	return true;
    }
    public boolean sendTcpSocketReset(int sockId) {
	if (!_open) return false;
	_parser.sendTcpSocketReset (_channel, sockId);
	return true;
    }
    public boolean sendTcpSocketClose(int sockId) {
	if (!_open) return false;
	_parser.sendTcpSocketClose (_channel, sockId);
	return true;
    }
    public boolean sendTcpSocketAbort(int sockId) {
	if (!_open) return false;
	_parser.sendTcpSocketAbort (_channel, sockId);
	return true;
    }
    public boolean sendTcpSocketData(int sockId, boolean copy, ByteBuffer ... bufs) {
	if (!_open) return false;
	for(ByteBuffer buf : bufs) {
		_meters.sendTcpSocketData(buf.remaining());
	}
	
	_parser.sendTcpSocketData (_channel, sockId, copy, bufs);
	
	return true;
    }
  
    /************************* sctp *************************/
  
    public boolean sendSctpSocketListen(long listenId, String[] localAddrs, int localPort, int maxOutStreams,
					int maxInStreams, boolean secure) {
	if (!_open) return false;
	_parser.sendSctpSocketListen (_channel, listenId, localAddrs, localPort, maxOutStreams, maxInStreams, secure);
	return true;
    }
    public boolean sendSctpSocketConnect(long connectionId, String remoteHost, int remotePort,
                                         String[] localAddrs, int localPort, int maxOutStreams,
					 int maxInStreams, boolean secure, Map<SctpSocketOption, SctpSocketParam> options, Map<String, String> params){
	if (!_open) return false;
	_parser.sendSctpSocketConnect (_channel, connectionId, remoteHost, remotePort, localAddrs, localPort, maxOutStreams, maxInStreams, secure, options, params);
	return true;
    }
    public boolean sendSctpSocketData(int sockId, String addr, boolean unordered, boolean complete,
				      int ploadPID, int streamNumber, long timeToLive, boolean copy,
				      ByteBuffer ... data) {
	if (!_open) return false;
	
	for(ByteBuffer buf : data) {
		_meters.sendSctpSocketData(buf.remaining());
	}
	
	_parser.sendSctpSocketData (_channel, sockId, addr, unordered, complete, ploadPID, streamNumber, timeToLive, copy, data);
	
	return true;
    }
    public boolean sendSctpSocketReset(int sockId) {
	if (!_open) return false;
	_parser.sendSctpSocketReset (_channel, sockId);
	return true;
    }
    public boolean sendSctpSocketClose(int sockId) {
	if (!_open) return false;
	_parser.sendSctpSocketClose (_channel, sockId);
	return true;
    }
    public boolean sendSctpSocketOptions (int sockId, Map<SctpSocketOption, SctpSocketParam> params){
	if (!_open) return false;
	_parser.sendSctpSocketOptions (_channel, sockId, params);
	return true;
    }
    public boolean sendSctpSocketParams(int sockId, java.util.Map<String, String> params){
	if (!_open) return false;
	_parser.sendSctpSocketParams (_channel, sockId, params);
	return true;
    }
    
    /************************* udp *************************/
    public boolean sendUdpSocketBind(long bindId, String localIP, int localPort, boolean shared) {
	if (!_open) return false;
	_parser.sendUdpSocketBind (_channel, bindId, localIP, localPort, shared);
	return true;
    }
    public boolean sendUdpSocketClose(int sockId) {
	if (!_open) return false;
	_parser.sendUdpSocketClose (_channel, sockId);
	return true;
    }
    public boolean sendUdpSocketData(int sockId, String remoteIP, int remotePort, String virtualIP,
				     int virtualPort, boolean copy, ByteBuffer ... bufs) {
	if (!_open) return false;
	
	for(ByteBuffer buf : bufs) {
		_meters.sendUdpSocketData(buf.remaining());
	}
	
	_parser.sendUdpSocketData (_channel, sockId, remoteIP, remotePort, copy, bufs);

	return true;
    }
  
    /************************* dns *************************/
  
    public boolean sendDnsGetByAddr(long reqId, String addr) { throw new UnsupportedOperationException("method sendDnsGetByAddr() is not supported");}
    public boolean sendDnsGetByName(long reqId, String name) { throw new UnsupportedOperationException("method sendDnsGetByName() is not supported");}
    
    /************************* release *************************/
  
    public boolean sendRelease(final long sessionId) {
	if (!_open) return false;
	_parser.sendRelease (_channel, sessionId);
	return true;
    }
    public boolean sendReleaseAck(long sessionId, boolean confirm) {
	if (!_open) return false;
	_parser.sendReleaseAck (_channel, sessionId, confirm);
	return true;
    }
    
    /******************* read controls ************************/
  
    public void disableRead(int sockId) {
	if (!_open) return;
	_parser.disableRead (_channel, sockId);
    }
    public void enableRead(int sockId) {
	if (!_open) return;
	_parser.enableRead (_channel, sockId);
    }
    public void setInputExecutor(Executor inputExecutor) {
	_channel.setInputExecutor (inputExecutor);
    }


    /********************* mux handler proxy *****************/

    private class MuxHandlerProxy extends ExtendedMuxHandler {

	private List<Object> _trackings = new ArrayList<> ();

	private void closed (){
	    // must be cleaned even if no muxOpened/muxClosed
	    // since internalMuxData is called at any time, for ex before muxOpened
	    for (Object tracking : _trackings)
		_factory.getMeteringRegistry ().stopTracking (tracking, null);
	    _trackings.clear ();
	}
	
	@Override
	public void muxOpened (MuxConnection connection){
	    _factory.getMuxRecord ().record (new com.alcatel.as.service.recorder.Event ("mux opened : " + connection.getStackAddress() + ":" + connection.getStackPort()));
	    Boolean useMuxStart = ((Boolean) getMuxConfiguration ().get(MuxHandler.CONF_MUX_START));
	    if (useMuxStart == null ||
		useMuxStart.booleanValue () == false)
		connection.sendMuxStart ();
	    _meters.muxOpened(false);
	    _wrappedHandler.muxOpened (connection);
	}
	
	@Override
	public void muxClosed (MuxConnection connection){
		 _meters.muxClosed(false);
	    _wrappedHandler.muxClosed (connection);
	}

	@Override
	public Hashtable getMuxConfiguration (){ return _wrappedHandler.getMuxConfiguration ();}

	public int getMajorVersion (){ return -1;}
	public int getMinorVersion (){ return -1;}
	public int[] getCounters (){ return new int[0];}

	public void muxData(MuxConnection connection, MuxHeader header, byte[] data, int off, int len){
	    _wrappedHandler.muxData (connection, header, data, off, len);
	}

	public void muxData(MuxConnection connection, MuxHeader header, ByteBuffer data){
	    _wrappedHandler.muxData (connection, header, data);
	}
	    
	public void internalMuxData (MuxConnection connection, MuxHeader h, ByteBuffer buffer){
	    if (_logger.isDebugEnabled ()) _logger.debug ("internalMuxData "+h);
	    switch (h.getFlags ()){
	    case FLAG_MUX_METER_GET:
		_trackings.add (handleMuxMeterGet (AgentSideMuxConnection.this, h, buffer, _factory.getMeteringRegistry ()));
		return;
	    case FLAG_MUX_EXIT:
		getLogger ().warn ("Received EXIT command from : "+connection+" : shutting down");
		handleMuxExit (h, buffer, _factory.getEventAdmin ());
		return;
	    case FLAG_MUX_KILL:
		getLogger ().warn ("Received KILL command from : "+connection+" : exiting now");
		System.exit (0);
		return;
	    }
	}

	/************************* tcp socket mgmt *************************/

	public void tcpSocketListening(MuxConnection connection, int sockId, int localIP, int localPort,
				       boolean secure, long listenId, int errno){
	    _meters.tcpSocketListening();
	    _wrappedHandler.tcpSocketListening (connection, sockId, localIP, localPort, secure, listenId, errno);
	}

	public void tcpSocketListening(MuxConnection connection, int sockId, String localIP, int localPort,
				       boolean secure, long listenId, int errno){
    	_meters.tcpSocketListening();
	    _wrappedHandler.tcpSocketListening (connection, sockId, localIP, localPort, secure, listenId, errno);
	}

	public void tcpSocketConnected(final MuxConnection connection, final int sockId, final int remoteIP, final int remotePort,
				       final int localIP, final int localPort, final int virtualIP, final int virtualPort,
				       final boolean secure, final boolean clientSocket, final long connectionId, final int errno){
	    
    	if(errno == 0) {
    		_meters.tcpSocketConnected(sockId, clientSocket);
    	} else {
    		_meters.tcpSocketFailedConnect();
    	}
		
		if (_threadSafe){
		_flowController.acquireNow ();
		schedule (sockId, new Runnable (){
			public void run (){
			    try {_wrappedHandler.tcpSocketConnected (connection, sockId, remoteIP, remotePort, localIP, localPort, virtualIP, virtualPort, secure, clientSocket, connectionId, errno);} finally { _flowController.release ();}
			}
		    });
	    } else {
		_wrappedHandler.tcpSocketConnected (connection, sockId, remoteIP, remotePort, localIP, localPort, virtualIP, virtualPort, secure, clientSocket, connectionId, errno);
	    }
	}

	public void tcpSocketConnected(final MuxConnection connection,final int sockId,final String remoteIP,final int remotePort,
				       final String localIP,final int localPort,final String virtualIP,final int virtualPort,
				       final boolean secure, final boolean clientSocket, final long connectionId, final int errno){
    	if(errno == 0) {
    		_meters.tcpSocketConnected(sockId, clientSocket);
    	} else {
    		_meters.tcpSocketFailedConnect();
		}
    	
		if (_threadSafe){
		_flowController.acquireNow ();
		schedule (sockId, new Runnable (){
			public void run (){
			    try {_wrappedHandler.tcpSocketConnected (connection, sockId, remoteIP, remotePort, localIP, localPort, virtualIP, virtualPort, secure, clientSocket, connectionId, errno);} finally { _flowController.release ();}
			}
		    });
	    } else {
		_wrappedHandler.tcpSocketConnected (connection, sockId, remoteIP, remotePort, localIP, localPort, virtualIP, virtualPort, secure, clientSocket, connectionId, errno);
	    }
	}

	public void tcpSocketClosed(final MuxConnection connection, final int sockId){
	    _meters.tcpSocketClosed(sockId);

	    if (_threadSafe){
		_flowController.acquireNow ();
		schedule (sockId, new Runnable (){
			public void run (){ try {_wrappedHandler.tcpSocketClosed (connection, sockId);}finally{ _flowController.release ();} }
		    });
	    } else {
		_wrappedHandler.tcpSocketClosed (connection, sockId);
	    }
	}

	public void tcpSocketAborted(final MuxConnection connection, final int sockId){
	    _meters.tcpSocketAborted(sockId);

	    if (_threadSafe){
		_flowController.acquireNow ();
		schedule (sockId, new Runnable (){
			public void run (){ try {_wrappedHandler.tcpSocketAborted (connection, sockId);}finally{ _flowController.release ();}}
		    });
	    } else {
		_wrappedHandler.tcpSocketAborted (connection, sockId);
	    }
	}

	public void tcpSocketData(final MuxConnection connection, final int sockId, final long sessionId, byte[] data, int off, int len){
	    _meters.tcpSocketData(len);

	    if (_threadSafe){
		final byte[] clone = new byte[len];
		System.arraycopy (data, off, clone, 0, len);
		_flowController.acquireNow ();
		schedule (sockId, new Runnable (){
			public void run (){ try {_wrappedHandler.tcpSocketData (connection, sockId, sessionId, clone, 0, clone.length);}finally{ _flowController.release ();}}
		    });
	    } else {
		_wrappedHandler.tcpSocketData (connection, sockId, sessionId, data, off, len);
	    }
	}

	public void tcpSocketData(final MuxConnection connection, final int sockId, final long sessionId, ByteBuffer data){
	    _meters.tcpSocketData(data.remaining());

	    if (_threadSafe){
		final ByteBuffer clone = ByteBuffer.allocate (data.remaining ());
		clone.put (data);
		_flowController.acquireNow ();
		schedule (sockId, new Runnable (){
			public void run (){ clone.flip (); try {_wrappedHandler.tcpSocketData (connection, sockId, sessionId, clone);}finally{ _flowController.release ();}}
		    });
	    } else {
		_wrappedHandler.tcpSocketData (connection, sockId, sessionId, data);
	    }
	    
	}

	/************************* sctp socket mgmt ************************/

	public void sctpSocketListening(MuxConnection connexion, int sockId, long listenerId, String[] localAddrs,
					int localPort, boolean secure, int errno){
	    _meters.sctpSocketListening();
	    _wrappedHandler.sctpSocketListening (connexion, sockId, listenerId, localAddrs, localPort, secure, errno);
	}

	public void sctpSocketConnected(final MuxConnection connection, final int sockId, final long connectionId, final String[] remoteAddrs,
					final int remotePort, final String[] localAddrs, final int localPort, final int maxOutStreams,
					final int maxInStreams, final boolean fromClient, final boolean secure, final int errno){
      if(_meters != null) {
         if(errno == 0) {
       		_meters.sctpSocketConnected(sockId, fromClient);
       	} else {
       		_meters.sctpSocketFailedConnect();
       	}
      }
	    
		if (_threadSafe){
		_flowController.acquireNow ();
		schedule (sockId, new Runnable (){
			public void run (){ try {_wrappedHandler.sctpSocketConnected (connection, sockId, connectionId, remoteAddrs, remotePort, localAddrs, localPort, maxOutStreams, maxInStreams, fromClient, secure, errno);}finally{ _flowController.release ();}}
		    });
	    } else {
		_wrappedHandler.sctpSocketConnected (connection, sockId, connectionId, remoteAddrs, remotePort, localAddrs, localPort, maxOutStreams, maxInStreams, fromClient, secure, errno);
	    }
	}
	    
	public void sctpSocketData(final MuxConnection connection, final int sockId, final long sessionId, ByteBuffer data, final String addr,
				   final boolean isUnordered, final boolean isComplete, final int ploadPID, final int streamNumber){
	    _meters.sctpSocketData(data.remaining());

	    if (_threadSafe){
		final ByteBuffer clone = ByteBuffer.allocate (data.remaining ());
		clone.put (data);
		_flowController.acquireNow ();
		schedule (sockId, new Runnable (){
			public void run (){ clone.flip (); try {_wrappedHandler.sctpSocketData (connection, sockId, sessionId, clone, addr, isUnordered, isComplete, ploadPID, streamNumber);}finally{ _flowController.release ();}}
		    });
	    } else {
		_wrappedHandler.sctpSocketData (connection, sockId, sessionId, data, addr, isUnordered, isComplete, ploadPID, streamNumber);
	    }
	}

	public void sctpSocketClosed(final MuxConnection connection, final int sockId){
	    _meters.sctpSocketClosed(sockId);

	    if (_threadSafe){
		_flowController.acquireNow ();
		schedule (sockId, new Runnable (){
			public void run (){ try {_wrappedHandler.sctpSocketClosed (connection, sockId);}finally{ _flowController.release ();}}
		    });
	    } else {
		_wrappedHandler.sctpSocketClosed (connection, sockId);
	    }
	}

	public void sctpSocketSendFailed(final MuxConnection connection, final int sockId, final String addr, final int streamNumber,
					 ByteBuffer buf, final int errcode){
	    _meters.sctpSocketSendFailed();

	    if (_threadSafe){
		final ByteBuffer clone = ByteBuffer.allocate (buf.remaining ());
		clone.put (buf);
		_flowController.acquireNow ();
		schedule (sockId, new Runnable (){
			public void run (){ clone.flip ();  try {_wrappedHandler.sctpSocketSendFailed (connection, sockId, addr, streamNumber, clone, errcode);}finally{ _flowController.release ();}}
		    });
	    } else {
		_wrappedHandler.sctpSocketSendFailed (connection, sockId, addr, streamNumber, buf, errcode);
	    }
	}
    
	public void sctpPeerAddressChanged(final MuxConnection cnx, final int sockId, final String addr, final int port, final SctpAddressEvent event) {
	    _meters.sctpPeerAddressChanged();	

		if (_threadSafe){
		_flowController.acquireNow ();
		schedule (sockId, new Runnable (){
			public void run (){ try {_wrappedHandler.sctpPeerAddressChanged (cnx, sockId, addr, port, event);}finally{ _flowController.release ();}}
		    });
	    } else {
		_wrappedHandler.sctpPeerAddressChanged (cnx, sockId, addr, port, event);
	    }
	}

	/************************* udp socket mgmt *************************/

	public void udpSocketBound(MuxConnection connection, int sockId, int localIP, int localPort,
				   boolean shared, long bindId, int errno){
    	if(errno == 0) {
    		_meters.udpSocketBound();
    	} else {
    		_meters.udpSocketFailedBind();	
    	}
    	
	    _wrappedHandler.udpSocketBound (connection, sockId, localIP, localPort, shared, bindId, errno);
	}

	public void udpSocketBound(MuxConnection connection, int sockId, String localIP, int localPort,
				   boolean shared, long bindId, int errno){
    	if(errno == 0) {
    		_meters.udpSocketBound();
    	} else {
    		_meters.udpSocketFailedBind();	
    	}
    	
	    _wrappedHandler.udpSocketBound (connection, sockId, localIP, localPort, shared, bindId, errno);
	}

	public void udpSocketClosed(MuxConnection connection, int sockId){
    	_meters.udpSocketClosed();

	    _wrappedHandler.udpSocketClosed (connection, sockId);
	}

	public void udpSocketData(final MuxConnection connection, final int sockId, final long sessionId, final int remoteIP,
				  final int remotePort, final int virtualIP, final int virtualPort, final byte[] data, final int off, final int len){
		_meters.udpSocketData(len);

	    if (_threadSafe){
		_flowController.acquireNow ();
		schedule (sessionId, new Runnable (){
			public void run (){ try {_wrappedHandler.udpSocketData (connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, data, off, len);}finally{ _flowController.release ();}}
		    });
	    } else {
		_wrappedHandler.udpSocketData (connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, data, off, len);
	    }
	}

	public void udpSocketData(final MuxConnection connection, final int sockId, final long sessionId, final int remoteIP,
				  final int remotePort, final int virtualIP, final int virtualPort, final ByteBuffer data){
	    _meters.udpSocketData(data.remaining());

	    if (_threadSafe){
		_flowController.acquireNow ();
		schedule (sessionId, new Runnable (){
			public void run (){ try {_wrappedHandler.udpSocketData (connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, data);}finally{ _flowController.release ();}}
		    });
	    } else {
		_wrappedHandler.udpSocketData (connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, data);
	    }
	}

	public void udpSocketData(final MuxConnection connection, final int sockId, final long sessionId, final String remoteIP,
				  final int remotePort, final String virtualIP, final int virtualPort, final byte[] data, final int off, final int len){
	    _meters.udpSocketData(len);

	    if (_threadSafe){
		_flowController.acquireNow ();
		schedule (sessionId, new Runnable (){
			public void run (){ try {_wrappedHandler.udpSocketData (connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, data, off, len);}finally{ _flowController.release ();}}
		    });
	    } else {
		_wrappedHandler.udpSocketData (connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, data, off, len);
	    }
	}

	public void udpSocketData(final MuxConnection connection, final int sockId, final long sessionId, final String remoteIP,
				  final int remotePort, final String virtualIP, final int virtualPort, final ByteBuffer data){
	    _meters.udpSocketData(data.remaining());

	    if (_threadSafe){
		_flowController.acquireNow ();
		schedule (sockId, new Runnable (){
			public void run (){ try {_wrappedHandler.udpSocketData (connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, data);}finally{ _flowController.release ();}}
		    });
	    } else {
		_wrappedHandler.udpSocketData (connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, data);
	    }
	}

    };
}
