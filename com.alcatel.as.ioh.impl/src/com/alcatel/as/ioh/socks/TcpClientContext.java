package com.alcatel.as.ioh.socks;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.nio.*;
import java.net.InetSocketAddress;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.tools.*;

public class TcpClientContext implements TcpChannelListener {

    public Logger _logger = Logger.getLogger ("as.ioh.socks");

    protected TcpChannel _clientChannel, _destChannel;
    
    private boolean _stopReadClient = false; // sync in client exec
    private boolean _stopReadDest = false; // sync in dest exec
    private String _toString;
    private Meters _meters;
    private PlatformExecutor _exec;
    private STATE _state;    
    private SocksParser _parser;
    private SocksProxy _px;
    private SocksProxy.TcpServerContext _serverCtx;
    private ByteBuffer _buffer;
    private java.util.concurrent.Future _initTimeout;
    
    protected TcpClientContext (SocksProxy px, SocksProxy.TcpServerContext server, TcpChannel channel, Map<String, Object> props){
	_px = px;
	_serverCtx = server;
	_clientChannel = channel;
	_meters = server._meters;
	_exec = (PlatformExecutor)  props.get (TcpServer.PROP_READ_EXECUTOR);
	_parser = new SocksParser (this, _serverCtx._allow4, _serverCtx._allow5);
	_toString = new StringBuilder ().append ("TcpSocksClient[").append (server._name).append (':').append (channel.getRemoteAddress ()).append (']').toString ();
	_meters._clientChannelsTcpOpenMeter.inc (1);
    }

    public void start (){
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : clientConnectionAccepted");
	_state = STATE_INIT;
	_clientChannel.enableReading ();
	_initTimeout = _exec.schedule (this::timeout, _serverCtx._initTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    public void timeout (){
	_state.initTimeout ();
    }

    public String toString (){ return _toString; }

    public Logger logger (){ return _logger;}

    public boolean checkUserId (String userId){
	if (_serverCtx.checkUserId (userId)) return true;
	_meters._authFailed.inc (1);
	return false;
    }

    /************************************************
     ** TcpChannelListener for client connections **
     ************************************************/
	
    public int messageReceived (TcpChannel channel, ByteBuffer data){
	if (_stopReadClient){
	    data.position (data.limit ());
	    return 0;
	}
	try{
	    _state.clientMessageReceived (data);
	}catch(Exception e){
	    _logger.info (this+" : exception while handling client data", e);
	    data.position (data.limit ());
	    _clientChannel.close ();
	    _stopReadClient = true; // disable future reads until closed is called back
	}
	return 0;
    }
    
    public void receiveTimeout (TcpChannel channel){}
    public void writeBlocked (TcpChannel channel){}
    public void writeUnblocked (TcpChannel channel){}

    public void connectionClosed(TcpChannel channel){
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : clientConnectionClosed");
	_meters._clientChannelsTcpOpenMeter.inc (-1);
	_meters._clientChannelsTcpClosedMeter.inc (1);
	_state.clientClosed ();
    }

    /******************************** State machine ***************/

    protected void connect (InetSocketAddress dest){
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : connect to destination : "+dest);
	Map<ReactorProvider.TcpClientOption, Object> opts = new HashMap<> ();
	opts.putAll (_serverCtx._opts);
	opts.put (ReactorProvider.TcpClientOption.INPUT_EXECUTOR, _exec);
	_px.getReactorProvider ().tcpConnect (_serverCtx._reactor, dest, _destListener, opts);
    }

    // the dest listener is called in the dest exec : it is the client exec during the socks exchange
    private TcpClientChannelListener _destListener = new TcpClientChannelListener (){
	    public int messageReceived (TcpChannel channel, ByteBuffer data){
		if (_stopReadDest){
		    data.position (data.limit ());
		    return 0;
		}
		int remaining = data.remaining ();
		_meters._destReadTcpMeter.inc (remaining);
		if (_clientChannel.getSendBufferSize () > _serverCtx._clientMaxSendBuffer){
		    if (_logger.isInfoEnabled ())
			_logger.info (TcpClientContext.this+" : client send buffer size exceeded - closing");
		    _meters._clientOverloadTcpMeter.inc (1);
		    data.position (data.limit ());
		    _destChannel.close ();
		    _clientChannel.shutdown ();
		    _stopReadDest = true;
		    return 0;
		}
		_meters._clientSendTcpMeter.inc (remaining);
		_clientChannel.send (data, true);
		return 0;
	    }
    
	    public void receiveTimeout (TcpChannel channel){}
	    public void writeBlocked (TcpChannel channel){}
	    public void writeUnblocked (TcpChannel channel){}
	    public void connectionEstablished(TcpChannel channel){
		if (_logger.isDebugEnabled ()) _logger.debug (TcpClientContext.this+" : destConnected");
		_meters._destChannelsTcpOpenMeter.inc (1);
		_destChannel = channel;
		_destChannel.setInputExecutor (_px.createQueueExecutor ());
		_destChannel.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
		if (_serverCtx._destReadTimeout > 0)
		    _destChannel.setSoTimeout (_serverCtx._destReadTimeout);
		_state.destConnected ();
	    }
	    public void connectionFailed(TcpChannel channel, Throwable t) {
		if (_logger.isDebugEnabled ()) _logger.debug (TcpClientContext.this+" : destFailed : "+t.toString ());
		_meters._destChannelsTcpFailedMeter.inc (1);
		_state.destFailed ();
	    }
	    public void connectionClosed(TcpChannel channel){
		if (_logger.isDebugEnabled ()) _logger.debug (TcpClientContext.this+" : destClosed");
		_meters._destChannelsTcpOpenMeter.inc (-1);
		_meters._destChannelsTcpClosedMeter.inc (1);
		_exec.execute (() -> {_state.destClosed ();});
	    }
	};

    protected static class STATE {
	protected void clientMessageReceived (ByteBuffer data){}
	protected void clientClosed(){}
	protected void destConnected (){}
	protected void destFailed (){}
	protected void destClosed(){}

	protected void initTimeout (){}
    }
    
    protected STATE STATE_INIT = new STATE (){
	    @Override
	    protected void initTimeout(){
		if (_logger.isDebugEnabled ()) _logger.debug (TcpClientContext.this+" : init timeout : closing");
		_clientChannel.shutdown ();
		_state = STATE_CLOSED;
	    }
	    @Override
	    protected void clientClosed(){
		_state = STATE_CLOSED;
	    }
	    @Override
	    protected void clientMessageReceived (ByteBuffer data){
		InetSocketAddress dest = _parser.messageReceived (data);
		if (dest != null){
		    _initTimeout.cancel (true);
		    _clientChannel.disableReading ();
		    connect (dest);
		    _state = STATE_OPENING;
		}
		int remaining = data.remaining (); // in case we received more and need to buffer
		if (remaining > 0){
		    _meters._clientReadTcpMeter.inc (remaining);
		    ByteBuffer copy = ByteBuffer.allocate (remaining);
		    copy.put (data);
		    copy.flip ();
		    _buffer = copy;
		}
	    }
	};

    protected STATE STATE_OPENING = new STATE (){
	    @Override
	    protected void clientClosed(){
		_state = STATE_CLOSED;
	    }
	    @Override
	    protected void destConnected (){
		_parser.destConnected (_clientChannel);
		if (_buffer != null){
		    int remaining = _buffer.remaining ();		    
		    _meters._destSendTcpMeter.inc (remaining);
		    _destChannel.send (_buffer, false);
		    _buffer = null;
		}
		_clientChannel.enableReading ();
		_state = STATE_OPEN;
	    }
	    @Override
	    protected void destFailed (){
		_parser.destFailed (_clientChannel);
		_clientChannel.close ();
		_state = STATE_CLOSED;
	    }
	};

    protected STATE STATE_OPEN = new STATE (){
	    @Override
	    protected void clientMessageReceived (ByteBuffer data){
		int remaining = data.remaining ();
		_meters._clientReadTcpMeter.inc (remaining);
		if (_destChannel.getSendBufferSize () > _serverCtx._destMaxSendBuffer){
		    if (_logger.isInfoEnabled ())
			_logger.info (TcpClientContext.this+" : dest send buffer size exceeded - closing");
		    _meters._destOverloadTcpMeter.inc (1);
		    data.position (data.limit ());
		    _clientChannel.close ();
		    _destChannel.shutdown ();
		    _stopReadClient = true;
		    return;
		}
		_meters._destSendTcpMeter.inc (remaining);
		_destChannel.send (data, true);
	    }
	    @Override
	    protected void clientClosed(){
		_destChannel.close ();
		_state = STATE_CLOSED;
	    }
	    @Override
	    protected void destClosed(){
		_clientChannel.close ();
		_state = STATE_CLOSED;
	    }
	};

    protected STATE STATE_CLOSED = new STATE (){
	    @Override
	    protected void destConnected (){
		_destChannel.close ();
	    }
	};
}
