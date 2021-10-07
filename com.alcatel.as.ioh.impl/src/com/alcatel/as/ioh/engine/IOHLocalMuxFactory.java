package com.alcatel.as.ioh.engine;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.ioh.tools.ByteBufferUtils;
import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.recorder.Record;
import com.alcatel.as.util.sctp.SctpSocketOption;
import com.alcatel.as.util.sctp.SctpSocketParam;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.impl.ioh.AggregatingMuxConnectionMetersImpl;
import com.nextenso.mux.impl.ioh.DummyMuxConnectionMeters;
import com.nextenso.mux.impl.ioh.ExtendedMuxHandler;
import com.nextenso.mux.impl.ioh.MuxConnectionMeters;
import com.nextenso.mux.impl.ioh.MuxConnectionMetersImpl;
import com.nextenso.mux.socket.TcpMessageParser;
import com.nextenso.mux.util.MuxHandlerMeters;
import com.nextenso.mux.util.MuxUtils;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import jsr166e.LongAdder;

public class IOHLocalMuxFactory extends MuxFactory {
    
    protected IOHEngine _engine;
    protected String _id, _toString;
    private BundleContext _bctx;
    private MeteringService _meteringService;

    public IOHLocalMuxFactory (String id, IOHEngine engine){
	_id = id;
	_engine = engine;
	_toString = "IOHLocalMuxFactory["+_id+"]";
    }
    public String toString (){ return _toString;}
    public String getType (){ return _id;}

    public IOHLocalMuxFactory register (BundleContext ctx){
	Dictionary props = new Hashtable ();
	props.put ("type", getType ());
	ctx.registerService (MuxFactory.class.getName (), this, props);
	return this;
    }
    
    public void setMeteringService(MeteringService metering, BundleContext bctx) {
    	this._meteringService = metering;
    	this._bctx = bctx;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public MuxConnection newMuxConnection(Reactor reactor, ConnectionListener listener, MuxHandler muxHandler,
					  InetSocketAddress to, int stackId, String stackName,
					  String stackHost, String stackInstance, Map opts) {
	IOHLocalMuxConnection cnx = _engine.newLocalMuxClient (muxHandler, listener, opts);
	// we need to update the stackInstance : else 2 local mux endpoints would look like the same stacks
	// we replace the instance with this _id
	// in blueprint : getStackInstance returns platform.group__component.instance --> hence instance is the last token
	int index = stackInstance.lastIndexOf("__"); // or indexOf should work too
	if (index >= 0) {
	    index+=2;
	    int nextIndex = stackInstance.indexOf ('.', index);
	    if (nextIndex != -1)
		index = nextIndex;
	    stackInstance = stackInstance.substring(0, index)+"."+_id;
	}
	cnx.setStackInfo (stackId, stackName, stackInstance, stackHost);
	cnx.setAddresses (to, null);	
	cnx.setProtocol((String) opts.get(PROTOCOL));
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
      if (cnx instanceof IOHLocalMuxConnection) {
         if (_bctx != null && _meteringService != null) {
            ((IOHLocalMuxConnection) cnx).startMeters(_meteringService, _bctx);
         }
         ((IOHLocalMuxConnection) cnx).open();
      }
   }
  
    @SuppressWarnings("rawtypes")
    @Override
    public InetSocketAddress accept(Reactor r, ConnectionListener l, MuxHandler mh, InetSocketAddress from,
				    Map opts) throws java.io.IOException {
	return null;
    }

     public static class IOHLocalMuxConnection extends IOHEngine.MuxClient {

	protected final AtomicInteger _bufferSize = new AtomicInteger (0);
	protected MuxConnectionMeters _cnxMeters = new DummyMuxConnectionMeters();
	protected SimpleMonitorable _mon;
	protected String _protocol;
	protected Record _muxRecord;
	protected boolean _closed; // this one is not volatile and kept up to date in _exec (while _open is volatile and checked in misc threads)
	
	protected IOHLocalMuxConnection (IOHEngine engine, MuxHandler handler, ConnectionListener listener, Map opts){
	    super ();
	    _sendBufferMonitor = engine.getSendLocalAgentBufferMonitor ();
	    _props = engine.getProperties ();
	    Logger logger = (opts != null) ? (Logger) opts.get(OPT_LOGGER) : null;
	    if (logger == null) logger = engine.getLogger ();
	    setLogger (logger);
	    _connectionListener = listener;
	    _socketManager = new com.nextenso.mux.util.SocketManagerImpl ();
	    _engine = engine;
	    _muxRecord = engine.getIOHServices ().getRecorderService ().newRecord ("agent.mux", null, false);
	    _exec = (PlatformExecutor) opts.get(OPT_INPUT_EXECUTOR);
	    if (_exec == null) _exec = _engine.createQueueExecutor ();
	    _handler = _extHandler = new MuxHandlerWrapper (handler);
	    _toString = new StringBuilder ().append ("IOHLocalMuxConnection[").append (handler.getAppName ()).append (']').toString ();
	}
	
		void startMeters(MeteringService meteringService, BundleContext bctx) {
			String monitorableName = MuxConnectionMeters.makeConnectionMonitorableName(true, getStackInstance(),
					hashCode(), _protocol);
			_mon = new SimpleMonitorable(monitorableName, this.toString());

			String[] protos = (String[]) getMuxHandler().getMuxConfiguration().get(MuxHandler.CONF_L4_PROTOCOLS);

			MuxHandlerMeters handlerMeters = (MuxHandlerMeters) getMuxHandler().getMuxConfiguration()
					.get(MuxHandler.CONF_HANDLER_METERS);

			if (handlerMeters != null) {
				_cnxMeters = new AggregatingMuxConnectionMetersImpl(meteringService, _mon, handlerMeters)
						.initMeters(protos);
			} else {
				_cnxMeters = new MuxConnectionMetersImpl(meteringService, _mon).initMeters(protos);
			}
			_mon.start(bctx);

			if (_logger.isInfoEnabled()) {
				_logger.info("Registering monitorable for connection %s", this);
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
	public boolean isLocalAgent (){ return true;}
	@Override
	public int getSendBufferSize (){
	    return _bufferSize.get ();
	}
	
	@Override
	public Object getMonitorable() {
	   return _mon;
	}
	
   /**
    * Set the name of the applicative protocol handled by this mux connection.
    * This is used for the name of the Monitorable object associated with this
    * connection.
    * @param proto the name of the protocol
    */
	public void setProtocol(String protocol) {
		this._protocol = protocol;
	}
	
	public void open (){
	    // lets sync
	    synchronized (this){
		_logger4j.isDebugEnabled ();
	    }
	    Runnable r = new Runnable (){
		    public void run (){
			synchronized (this){
			    _logger4j.isDebugEnabled ();
			}
			opened (false);
		    }};
	    _exec.execute (r);
	}
	
	public void close (){
	    _blacklist = false;
	    
	    _cnxMeters.stop();

	    _exec.execute (new Runnable (){
		    public void run (){
			if (_closed) return; // idempotent by safety
			_closed = true;
			closed ();
		    }
		});
	}

	public boolean sendMuxStart (){
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			if (connectionListenerCallback(() -> _connectionListener.muxStarted(IOHLocalMuxConnection.this))) {
			    _logger.info("sendMuxStart: connectionListener acknoledged, sending mux start");
			    IOHLocalMuxConnection.super.sendMuxStart ();
			    _cnxMeters.setMuxStarted(true, true);
			} else {
			    _logger.info("sendMuxSart: connectionListener rejected, sending mux stop");
			    IOHLocalMuxConnection.super.sendMuxStop ();
			    _cnxMeters.setMuxStarted(true, false);
			}    	
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}

	public boolean sendMuxStop (){
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			if (connectionListenerCallback(() -> _connectionListener.muxStopped(IOHLocalMuxConnection.this))) {
			    _logger.info("sendMuxStop: connectionListener acknoledged, sending mux stop");
			    IOHLocalMuxConnection.super.sendMuxStop ();
			    _cnxMeters.setMuxStarted(true, false);
			} else {
			    _logger.info("sendMuxStop: connectionListener rejected, sending mux start");
			    IOHLocalMuxConnection.super.sendMuxStart ();
			    _cnxMeters.setMuxStarted(true, true);
			}
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}

	public boolean sendMuxData(final MuxHeader header, boolean copy, final ByteBuffer ... bufs) {
	    if (bufs == null || bufs.length == 0 || (bufs.length == 1 && bufs[0] == null)){
		copy = false;
	    }
	    if (copy)
		return sendMuxData (header, false, ByteBufferUtils.aggregate (true, true, bufs));
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendMuxData (header, false, bufs);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	public boolean sendInternalMuxData (final MuxHeader header, boolean copy, final ByteBuffer... bufs){
	    if (bufs == null || bufs.length == 0 || (bufs.length == 1 && bufs[0] == null)){
		copy = false;
	    }
	    if (copy)
		return sendInternalMuxData (header, false, ByteBufferUtils.aggregate (true, true, bufs));
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendInternalMuxData (header, false, bufs);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}

	/******************* START implement TCP IO operations **************/
	@Override
	public boolean sendTcpSocketListen(long listenId, String localIP, int localPort, boolean secure) {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendTcpSocketListen(listenId, localIP, localPort, secure);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendTcpSocketConnect(final long connectionId, final String remoteHost, final int remotePort, String localIP, int localPort, boolean secure, Map<String, String> params) {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendTcpSocketConnect(connectionId, remoteHost, remotePort, localIP, localPort, secure, params);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendTcpSocketReset(final int sockId) {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendTcpSocketReset (sockId);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendTcpSocketClose(final int sockId) {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendTcpSocketClose (sockId);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendTcpSocketData(final int sockId, boolean copy, final ByteBuffer ... bufs) {
	    for(ByteBuffer buf : bufs) {
	    	_cnxMeters.sendTcpSocketData(buf.remaining());
	    }
		
		if (copy)
		return sendTcpSocketData (sockId, false, ByteBufferUtils.aggregate (true, true, bufs));
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendTcpSocketData (sockId, false, bufs);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendTcpSocketParams (int sockId, Map<String, String> params){
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendTcpSocketParams (sockId, params);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	/******************* START implement SCTP IO operations **************/
	@Override
	public boolean sendSctpSocketListen (long listenId, String[] localAddrs, int localPort, int maxOutStreams, int maxInStreams, boolean secure){
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendSctpSocketListen (listenId, localAddrs, localPort, maxOutStreams, maxInStreams, secure);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendSctpSocketConnect(final long connectionId, final java.lang.String remoteHost, final int remotePort, java.lang.String[] localIPs, int localPort, int maxOutStreams, int maxInStreams, boolean secure, Map<SctpSocketOption, SctpSocketParam> options, Map<String, String> params){
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendSctpSocketConnect(connectionId, remoteHost, remotePort, localIPs, localPort, maxOutStreams, maxInStreams, secure, options, params);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendSctpSocketReset(final int sockId) {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendSctpSocketReset (sockId);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendSctpSocketClose(final int sockId) {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendSctpSocketClose (sockId);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendSctpSocketData(final int sockId, final String addr, final boolean unordered, final boolean complete, final int ploadPID, final int streamNumber, final long timeToLive, boolean copy, final ByteBuffer... data){
	    for(ByteBuffer buf : data) {
	    	_cnxMeters.sendSctpSocketData(buf.remaining());
	    }
		
		if (copy)
		return sendSctpSocketData (sockId, addr, unordered, complete, ploadPID, streamNumber, timeToLive, false, ByteBufferUtils.aggregate (true, true, data));
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendSctpSocketData (sockId, addr, unordered, complete, ploadPID, streamNumber, timeToLive, false, data);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendSctpSocketOptions(final int sockId, final Map<SctpSocketOption, SctpSocketParam> params){
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendSctpSocketOptions (sockId, params);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendSctpSocketParams (int sockId, Map<String, String> params){
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendSctpSocketParams (sockId, params);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	/******************* START implement UDP IO operations **************/
	@Override
	public boolean sendUdpSocketBind(final long bindId, final String localIP, final int localPort, final boolean shared) {
	    // not stricty needed to schedule if shared : lets keep it the same for all cases (rare call)
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendUdpSocketBind (bindId, localIP, localPort, shared);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendUdpSocketClose(final int sockId) {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendUdpSocketClose (sockId);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendUdpSocketData(final int sockId, final String remoteIP, final int remotePort, final String virtualIP,
					 final int virtualPort, boolean copy, final ByteBuffer ... bufs) {
	    for(ByteBuffer buf : bufs) {
	    	_cnxMeters.sendUdpSocketData(buf.remaining());
	    }
		
		if (copy)
		return sendUdpSocketData (sockId, remoteIP, remotePort, virtualIP, virtualPort, false, ByteBufferUtils.aggregate (true, true, bufs));
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendUdpSocketData (sockId, remoteIP, remotePort, virtualIP, virtualPort, false, bufs);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	/********************* Misc. operations ************************/
	@Override
	public void disableRead(final int sockId) {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.disableRead (sockId);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	}
	@Override
	public void enableRead(final int sockId) {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.enableRead (sockId);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	}
	@Override
	public void setInputExecutor(Executor inputExecutor) {
	    // not sure if this is actually used
	    _exec = (PlatformExecutor) inputExecutor;
	}
	@Override
	public boolean sendRelease(final long sessionId) {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendRelease (sessionId);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	@Override
	public boolean sendReleaseAck(final long sessionId, final boolean confirm) {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			IOHLocalMuxConnection.super.sendReleaseAck (sessionId, confirm);
		    }};
	    _exec.execute (r, ExecutorPolicy.INLINE);
	    return _open;
	}
	/******************* END implement UDP IO operations **************/
	
	protected class MuxHandlerWrapper extends ExtendedMuxHandler {

	    protected MuxHandler _handler;
	    protected boolean _ipv6Support, _byteBufferMode, _threadSafe;
	    protected int _sendBufferSize;
	    protected LongAdder _qSize = new LongAdder ();
	    protected Future _qWatcher;
	    protected List<Object> _trackings = new ArrayList<> ();
	    protected PlatformExecutors _execs;
	    
	    protected MuxHandlerWrapper (MuxHandler handler){
		_handler = handler;
		_ipv6Support = ((Boolean) getMuxConfiguration ().get(MuxHandler.CONF_IPV6_SUPPORT)).booleanValue();
		_byteBufferMode = ((Boolean) getMuxConfiguration ().get(MuxHandler.CONF_USE_NIO)).booleanValue();
		_threadSafe = ((Boolean) getMuxConfiguration ().get(MuxHandler.CONF_THREAD_SAFE)).booleanValue();
		// there is no need for a FlowController : the agent' overload is assessed via _qSize
		if (_threadSafe) _execs = _engine.getIOHServices ().getPlatformExecutors ();
	    }
	    @Override
	    public Hashtable getMuxConfiguration (){ return _handler.getMuxConfiguration ();}
	    
	    public void schedule (Object queue, Runnable r){ _execs.getProcessingThreadPoolExecutor (queue).execute (r);} // used in _threadSafe mode

	    // we override since getMuxHandler is used by CalloutServer to do lookups
	    public int hashCode (){ return _handler.hashCode ();}
	    public boolean equals (Object o){ return _handler.equals (o);}	

	    public int[] getCounters(){return null;}
	    public int getMajorVersion(){return -1;}
	    public int getMinorVersion(){return -1;}
	    public void commandEvent(int command,
				     int[] intParams,
				     java.lang.String[] strParams){}
	    @Override
	    public void muxOpened (final MuxConnection cnx){
		_muxRecord.record (new com.alcatel.as.service.recorder.Event ("mux opened : " + cnx.getStackAddress() + ":" + cnx.getStackPort()));
	 	_cnxMeters.muxOpened(true);	
		Runnable q = new Runnable (){
			public void run (){
			    _bufferSize.lazySet ((int)_qSize.sum ());
			}};
		_qWatcher = _exec.scheduleAtFixedRate (q, 50, 50, TimeUnit.MILLISECONDS);
		Runnable r = new Runnable (){
			public void run (){
			    Boolean useMuxStart = ((Boolean) getMuxConfiguration ().get(MuxHandler.CONF_MUX_START));
			    if (useMuxStart == null ||
				useMuxStart.booleanValue () == false)
				cnx.sendMuxStart ();
			    _handler.muxOpened (cnx);
			}
		    };
		_exec.execute (r);
	    }
	    @Override
	    public void muxClosed (final MuxConnection cnx){
	 	_cnxMeters.muxClosed(true);	
		Runnable r = new Runnable (){
			public void run (){_handler.muxClosed (cnx);}
		    };
		_exec.execute (r);
		_qWatcher.cancel (false);
		for (Object tracking : _trackings)
		    _engine.getIOHServices ().getMeteringRegistry ().stopTracking (tracking, null);
		_trackings.clear ();
	    }
	    @Override
	    public void internalMuxData (MuxConnection connection,
					 MuxHeader h,
					 ByteBuffer buffer){
		switch (h.getFlags ()){
		case FLAG_MUX_METER_GET:
		    _trackings.add (handleMuxMeterGet (IOHLocalMuxConnection.this, h, buffer, _engine.getIOHServices ().getMeteringRegistry ()));
		    return;
		case FLAG_MUX_EXIT:
		    handleMuxExit (h, buffer, _engine.getIOHServices ().getEventAdmin ());
		    return;
		}
	    }
	    public void tcpSocketListening(final MuxConnection connection,
					   final int sockId,
					   final String localIP,
					   final int localPort,
					   final boolean secure,
					   final long listenId,
					   final int errno){
	    if(errno == 0) {
	    	_cnxMeters.tcpSocketListening();
	    }
	    
		Runnable r = new Runnable (){
			public void run (){
			    if (_ipv6Support)
				_handler.tcpSocketListening (connection, sockId, localIP, localPort, secure, listenId, errno);
			    else
				_handler.tcpSocketListening (connection, sockId, MuxUtils.getIPAsInt (localIP), localPort, secure, listenId, errno);
			}
		    };
		_exec.execute (r);
	    }
	    public void tcpSocketConnected(final MuxConnection connection,
					   final int sockId,
					   final java.lang.String remoteIP,
					   final int remotePort,
					   final java.lang.String localIP,
					   final int localPort,
					   final java.lang.String virtualIP,
					   final int virtualPort,
					   final boolean secure,
					   final boolean clientSocket,
					   final long connectionId,
					   final int errno){
		if(errno == 0) {
			_cnxMeters.tcpSocketConnected(sockId, clientSocket);
		} else {
			_cnxMeters.tcpSocketFailedConnect();
		}
		Runnable r = new Runnable (){
			public void run (){
			    if (_ipv6Support)
				_handler.tcpSocketConnected (connection, sockId, remoteIP, remotePort, localIP, localPort, localIP, localPort, secure, clientSocket, connectionId, errno);
			    else
				_handler.tcpSocketConnected (connection, sockId, MuxUtils.getIPAsInt (remoteIP), remotePort, MuxUtils.getIPAsInt (localIP), localPort, MuxUtils.getIPAsInt (localIP), localPort, secure, clientSocket, connectionId, errno);
			
			}
		    };
		if (_threadSafe)
		    schedule (sockId, r);
		else
		    _exec.execute (r);
		
	    }
	    public void tcpSocketClosed(final MuxConnection connection,
					final int sockId){
	    _cnxMeters.tcpSocketClosed(sockId);
	    
		Runnable r = new Runnable (){
			public void run (){
			    _handler.tcpSocketClosed (connection, sockId);
			}
		    };
		if (_threadSafe)
		    schedule (sockId, r);
		else
		    _exec.execute (r);	
	    }
	    public void tcpSocketAborted(final MuxConnection connection,
					 final int sockId){
	    _cnxMeters.tcpSocketAborted(sockId);	
	    
		Runnable r = new Runnable (){
			public void run (){
			    _handler.tcpSocketAborted (connection, sockId);
			}
		    };
		if (_threadSafe)
		    schedule (sockId, r);
		else
		    _exec.execute (r);
	    }
	    public void tcpSocketData(final MuxConnection connection,
				      final int sockId,
				      final long sessionId,
				      final java.nio.ByteBuffer data){
	    _cnxMeters.tcpSocketData(data.remaining());
	    	
		Runnable r = new Runnable (){
			public void run (){
			    try{
				if (_byteBufferMode)
				    _handler.tcpSocketData (connection, sockId, sessionId, data);
				else
				    _handler.tcpSocketData (connection, sockId, sessionId, data.array (), data.position (), data.remaining ());
			    }finally{
				_qSize.decrement ();
			    }
			}
		    };
		_qSize.increment ();
		if (_threadSafe)
		    schedule (sockId, r);
		else
		    _exec.execute (r);
	    }
	    public void tcpSocketData(MuxConnection connection,
				      int sockId,
				      long sessionId,
				      java.nio.ByteBuffer[] data){
		tcpSocketData (connection, sockId, sessionId, ByteBufferUtils.aggregate (false, false, data));
	    }
	    public void udpSocketBound(final MuxConnection connection,
				       final int sockId,
				       final String localIP,
				       final int localPort,
				       final boolean shared,
				       final long bindId,
				       final int errno){
	    if(errno == 0) {
	    	_cnxMeters.udpSocketBound();
	    } else {
	    	_cnxMeters.udpSocketFailedBind();
	    }
	    	
		Runnable r = new Runnable (){
			public void run (){
			    if (_ipv6Support)
				_handler.udpSocketBound (connection, sockId, localIP, localPort, shared, bindId, errno);
			    else
				_handler.udpSocketBound (connection, sockId, MuxUtils.getIPAsInt (localIP), localPort, shared, bindId, errno);
			}
		    };
		_exec.execute (r);
	    }
	    public void udpSocketClosed (final MuxConnection connection, final int sockId){
    	_cnxMeters.udpSocketClosed();
    	
    	Runnable r = new Runnable (){
			public void run (){
			    _handler.udpSocketClosed (connection, sockId);
			}
		    };
		_exec.execute (r);	
	    }
	    public void udpSocketData(final MuxConnection connection,
				      final int sockId,
				      final long sessionId,
				      final String remoteIP,
				      final int remotePort,
				      final String virtualIP,
				      final int virtualPort,
				      final ByteBuffer data){
	    _cnxMeters.udpSocketData(data.remaining());
	    
		Runnable r = new Runnable (){
			public void run (){
			    try{
				if (_byteBufferMode){
				    if (_ipv6Support)
					_handler.udpSocketData (connection, sockId, sessionId, remoteIP, remotePort, remoteIP, remotePort, data);
				    else {
					int remoteIP_int = MuxUtils.getIPAsInt (remoteIP);
					_handler.udpSocketData (connection, sockId, sessionId, remoteIP_int, remotePort, remoteIP_int, remotePort, data);
				    }
				} else {
				    if (_ipv6Support)
					_handler.udpSocketData (connection, sockId, sessionId, remoteIP, remotePort, remoteIP, remotePort, data.array (), data.position (), data.remaining ());
				    else {
					int remoteIP_int = MuxUtils.getIPAsInt (remoteIP);
					_handler.udpSocketData (connection, sockId, sessionId, remoteIP_int, remotePort, remoteIP_int, remotePort, data.array (), data.position (), data.remaining ());
				    }
				}
			    }finally{ _qSize.decrement ();}
			}};
		_qSize.increment ();
		if (_threadSafe){
		    schedule (sessionId, r);
		}
		else
		    _exec.execute (r);
	    }
	    public void udpSocketData(MuxConnection connection,
				      int sockId,
				      long sessionId,
				      String remoteIP,
				      int remotePort,
				      String virtualIP,
				      int virtualPort,
				      ByteBuffer[] buff){
		udpSocketData (connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, ByteBufferUtils.aggregate (false, false, buff));
	    }
	    
	    public void sctpSocketListening(final MuxConnection connexion, final int sockId, final long listenerId, final String[] localAddrs,
					    final int localPort, final boolean secure, final int errno){
    	if(errno == 0) {
    		_cnxMeters.sctpSocketListening();
    	}
	    	
		Runnable r = new Runnable (){
			public void run (){
			    _handler.sctpSocketListening (connexion, sockId, listenerId, localAddrs, localPort, secure, errno);
			}};
		_exec.execute (r);
	    }

	    public void sctpSocketConnected(final MuxConnection connection, final int sockId, final long connectionId, final String[] remoteAddrs,
					    final int remotePort, final String[] localAddrs, final int localPort, final int maxOutStreams,
					    final int maxInStreams, final boolean fromClient, final boolean secure, final int errno){
		if(errno == 0) {
			_cnxMeters.sctpSocketConnected(sockId, fromClient);
		} else {
			_cnxMeters.sctpSocketFailedConnect();
		}
	    	
    	Runnable r = new Runnable (){
			public void run (){
			    _handler.sctpSocketConnected (connection, sockId, connectionId, remoteAddrs, remotePort, localAddrs, localPort, maxOutStreams, maxInStreams, fromClient, secure, errno);
			}};
		if (_threadSafe)
		    schedule (sockId, r);
		else
		    _exec.execute (r);
	    }
	    
	    public void sctpSocketData(final MuxConnection connection, final int sockId, final long sessionId, final ByteBuffer data, final String addr,
				       final boolean isUnordered, final boolean isComplete, final int ploadPID, final int streamNumber){
		_cnxMeters.sctpSocketData(data.remaining());
	    	
    	Runnable r = new Runnable (){
			public void run (){
			    try{
				_handler.sctpSocketData (connection, sockId, sessionId, data, addr, isUnordered, isComplete, ploadPID, streamNumber);
			    }finally{ _qSize.decrement ();}
			}};
		_qSize.increment ();
		if (_threadSafe)
		    schedule (sockId, r);
		else
		    _exec.execute (r);
	    }

	    public void sctpSocketData(final MuxConnection connection, final int sockId, final long sessionId, final ByteBuffer[] data, final String addr,
				       final boolean isUnordered, final boolean isComplete, final int ploadPID, final int streamNumber){
		sctpSocketData (connection, sockId, sessionId, ByteBufferUtils.aggregate (false, false, data), addr, isUnordered, isComplete, ploadPID, streamNumber);
	    }

	    public void sctpSocketClosed(final MuxConnection connection, final int sockId){
	    _cnxMeters.sctpSocketClosed(sockId);
	    	
		Runnable r = new Runnable (){
			public void run (){
			    _handler.sctpSocketClosed (connection, sockId);
			}};
		if (_threadSafe)
		    schedule (sockId, r);
		else
		    _exec.execute (r);
	    }

	    public void sctpSocketSendFailed(final MuxConnection connection, final int sockId, final String addr, final int streamNumber,
					     final ByteBuffer buf, final int errcode){
    	_cnxMeters.sctpSocketSendFailed();
	    	
		Runnable r = new Runnable (){
			public void run (){
			    _handler.sctpSocketSendFailed (connection, sockId, addr, streamNumber, buf, errcode);
			}};
		if (_threadSafe)
		    schedule (sockId, r);
		else
		    _exec.execute (r);
	    }
    
	    public void sctpPeerAddressChanged(final MuxConnection cnx, final int sockId, final String addr, final int port, final SctpAddressEvent event) {
		_cnxMeters.sctpPeerAddressChanged();
	    	
    	Runnable r = new Runnable (){
			public void run (){
			    _handler.sctpPeerAddressChanged (cnx, sockId, addr, port, event);
			}};
		if (_threadSafe)
		    schedule (sockId, r);
		else
		    _exec.execute (r);
	    }
	    public void muxData(final MuxConnection connection, final MuxHeader header, final ByteBuffer data){
		Runnable r = new Runnable (){
			public void run (){
			    if (_byteBufferMode)
				_handler.muxData (connection, header, data);
			    else
				_handler.muxData (connection, header, data != null ? data.array () : null, data != null ? data.position () : 0, data != null ? data.remaining () : 0);
			}};
		_exec.execute (r);
	    }
    
	    public void releaseAck(final MuxConnection connection, final long sessionId){
		Runnable r = new Runnable (){
			public void run (){
			    _handler.releaseAck (connection, sessionId);
			}
		    };
		_exec.execute (r);	
	    }
	}
    }
}
