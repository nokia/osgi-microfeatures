package com.nextenso.mux.util;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import alcatel.tess.hometop.gateways.utils.Log;

import com.alcatel.as.util.sctp.*;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.MuxFactory.ConnectionListener;
import com.nextenso.mux.MuxHandler.SctpAddressEvent;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.socket.SocketManager;
import com.nextenso.mux.socket.TcpMessageParser;
import com.nextenso.mux.util.MuxIdentification;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.mux.util.TimeoutManager;

public abstract class AbstractMuxConnection implements MuxConnection {
    
    protected boolean _ipv6Support;
    protected boolean _byteBufferMode;
    protected SocketManager _socketManager;
    protected TimeoutManager _timeoutManager;
    protected MuxHandler _handler;
    protected ConnectionListener _connectionListener;
    protected Object[] _attributes;
    protected Object _attachment;
    protected int _id;
    protected static AtomicInteger ID = new AtomicInteger(1);
    protected boolean _threadSafe;
    protected Log _logger;
    protected Logger _logger4j;
    protected volatile boolean _open;
    protected boolean _muxOpened;

    // used on the stack side
    public AbstractMuxConnection (Logger logger){
	setLogger (logger);
	_id = ID.getAndIncrement();
    }
    
    // used on the agent side
    public AbstractMuxConnection (MuxHandler mh, ConnectionListener listener, Logger logger){
	this (logger);
	_connectionListener = listener;
	_socketManager = new SocketManagerImpl ();
	setMuxHandler (mh);
    }

    // may be set after the constructor
    public void setMuxHandler (MuxHandler mh){
	_handler = mh;
	if (mh != null){
	    @SuppressWarnings("rawtypes")
		java.util.Hashtable muxConf = _handler.getMuxConfiguration();
	    _ipv6Support = ((Boolean) muxConf.get(MuxHandler.CONF_IPV6_SUPPORT)).booleanValue();
	    _byteBufferMode = ((Boolean) muxConf.get(MuxHandler.CONF_USE_NIO)).booleanValue();
	    _threadSafe = ((Boolean) muxConf.get(MuxHandler.CONF_THREAD_SAFE)).booleanValue();
	}
    }

    public void shutdown (){ close ();}
    
    public void opened (){
	opened (true);
    }
    public void opened (boolean notifyMuxHandler){
	// the goal is to be able to call this method twice to be able to delay _handler.muxOpened if needed
	if (_open == false){
	    _open = true;
	    if (_connectionListener != null)
		_connectionListener.muxConnected (this, null);
	}
	if (notifyMuxHandler){
	    if (_muxOpened) return; // make it idempotent
	    _muxOpened = true;
	    _handler.muxOpened (this);
	}
    }
    public void failed (Throwable err){
	if (_connectionListener != null)
	    _connectionListener.muxConnected (this, err);
    }
    public void closed() {
	_open = false;
	if (_connectionListener != null)
	    _connectionListener.muxClosed (this);
	if (_muxOpened) _handler.muxClosed (this);
    }
    public boolean isOpened() { return _open;}
  
    public int getId() { return _id;}
    public int getInputChannel() { return 0;}
    public Logger getLogger (){ return _logger4j;}
    public void setLogger (Logger logger){ _logger = Log.getLogger (_logger4j = logger);}
    
    public boolean setKeepAlive(int interval, int idleFactor){ throw new UnsupportedOperationException("method setKeepAlive() is not supported");}
    public boolean useKeepAlive() {return false;}
    
    
    /***********************/
    
    protected String _stackAppName, _stackInstance, _stackHost, _stackAddress;
    protected int _stackAppId = -1, _stackPort = -1;
    protected InetSocketAddress _remoteAddress, _localAddress;
    public AbstractMuxConnection setStackInfo (int stackAppId, String stackAppName, String stackInstance, String stackHost){
	_stackAppId = stackAppId;
	_stackAppName = stackAppName;
	_stackInstance = stackInstance;
	_stackHost = stackHost;
	return this;
    }
    public AbstractMuxConnection setAddresses (InetSocketAddress remoteAddress, InetSocketAddress localAddress){
	_remoteAddress = remoteAddress;
	_localAddress = localAddress;
	_stackAddress = _remoteAddress.getAddress().getHostAddress();
	_stackPort = _remoteAddress.getPort ();
	return this;
    }
    
    public int getStackAppId() { return _stackAppId;}
    public String getStackAppName() { return _stackAppName;}
    public String getStackInstance() { return _stackInstance;}
    public String getStackHost() { return _stackHost;}
    public String getStackAddress() { return _stackAddress;}
    public int getStackPort() {return _stackPort;}
    public InetSocketAddress getRemoteAddress() { return _remoteAddress;}
    public InetSocketAddress getLocalAddress() {return _localAddress;}
    
    public SocketManager getSocketManager() { return _socketManager;}
    public void setTimeoutManager(TimeoutManager manager) { _timeoutManager = manager;}
    public TimeoutManager getTimeoutManager() {	return _timeoutManager;}
    public MuxHandler getMuxHandler() {	return _handler;}
    public void setAttributes(Object[] attributes) {_attributes = attributes;}
    public Object[] getAttributes() {return _attributes;}
    public void attach(Object attachment) {_attachment = attachment;}
    public <T> T attachment() {	return (T) _attachment;}

    @Override
    public String toString() {
	StringBuilder buff = new StringBuilder();
	buff.append("MuxConnection [id=");
	buff.append(_id);
	buff.append(", stackId=");
	buff.append(_stackAppId);
	buff.append(", stackName=");
	buff.append(_stackAppName);
	buff.append(", stackInstance=");
	buff.append(_stackInstance);
	buff.append(", stackHost=");
	buff.append(_stackHost);
	buff.append(", stackAddr=");
	buff.append(_stackAddress);
	buff.append(", stackPort=");
	buff.append(_stackPort);
	buff.append(']');
	return buff.toString();
    }
    
    /************************* mux *************************/

    @Override
    public boolean sendMuxData(MuxHeader header, byte[] data, int off, int len, boolean copy) {
	if (data == null || len == 0) return sendMuxData (header, null);
	return sendMuxData (header, copy, ByteBuffer.wrap (data, off, len));
    }
    @Override
    public boolean sendMuxData(MuxHeader header, ByteBuffer buf) {
	boolean copy = (buf != null && buf.remaining () > 0);
	return sendMuxData (header, copy, buf);
    }
  
    @Override
    public boolean sendMuxStart() { return _open;}
    @Override
    public boolean sendMuxStop() { return _open;}
    @Override
    public boolean sendMuxData(MuxHeader header, boolean copy, ByteBuffer ... buf) { return _open;}
    @Override
    public boolean sendMuxIdentification(MuxIdentification id) { return _open;}

  
    /************************* tcp *************************/
  
    @Override
    public boolean sendTcpSocketListen(long listenId, int localIP, int localPort, boolean secure) {
	String ip = (localIP == 0) ? "0.0.0.0" : MuxUtils.getIPAsString(localIP);
	return sendTcpSocketListen(listenId, ip, localPort, secure);
    }
    @Override
    public boolean sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, int localIP,
					int localPort, boolean secure) {
	return sendTcpSocketConnect(connectionId, remoteHost, remotePort, MuxUtils.getIPAsString(localIP),
				    localPort, secure, null);
    }
    @Override
    public boolean sendTcpSocketData(int sockId, byte[] data, int off, int len, boolean copy) {
	if (len == 0 || data == null)
	    return _open;
	return sendTcpSocketData(sockId, copy, ByteBuffer.wrap(data, off, len));
    }
    @Override
    public boolean sendTcpSocketListen(long listenId, String localIP, int localPort, boolean secure) {return _open;}
    @Override
    public boolean sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, String localIP,
					int localPort, boolean secure) {
	return sendTcpSocketConnect (connectionId, remoteHost, remotePort, localIP, localPort, secure, null);
    }
    @Override
    public boolean sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, String localIP,
                                        int localPort, boolean secure, java.util.Map<String, String> params){ return _open;}
    @Override
    public boolean sendTcpSocketReset(int sockId) {return _open;}
    @Override
    public boolean sendTcpSocketClose(int sockId) {return _open;}
    @Override
    public boolean sendTcpSocketAbort(int sockId) {return _open;}
    @Override
    public boolean sendTcpSocketData(int sockId, boolean copy, ByteBuffer ... bufs) {return _open;}
    @Override
    public boolean sendTcpSocketParams(int sockId, java.util.Map<String, String> params){return _open;}
  
    /************************* sctp *************************/

    @Override
    public boolean sendSctpSocketListen(long listenId, String[] localAddrs, int localPort, int maxOutStreams,
					int maxInStreams, boolean secure) {return _open;}
    @Override
    public boolean sendSctpSocketConnect(long connectionId, String remoteHost, int remotePort,
					 String[] localAddrs, int localPort, int maxOutSreams, int maxInStreams, boolean secure) {
	return sendSctpSocketConnect (connectionId, remoteHost, remotePort, localAddrs, localPort, maxOutSreams, maxInStreams, secure, null, null);
    }
    @Override
    public boolean sendSctpSocketConnect(long connectionId, String remoteHost, int remotePort,
					 String[] localAddrs, int localPort, int maxOutSreams, int maxInStreams, boolean secure,
					 java.util.Map<SctpSocketOption, SctpSocketParam> sctpSocketOptions, java.util.Map<String, String> params) {return _open;}
    @Override
    public boolean sendSctpSocketData(int sockId, String addr, boolean unordered, boolean complete,
				      int ploadPID, int streamNumber, long timeToLive, boolean copy,
				      ByteBuffer ... data) {return _open;}
    @Override
    public boolean sendSctpSocketReset(int sockId) {return _open;}
    @Override
    public boolean sendSctpSocketClose(int sockId) {return _open;}
    @Override
    public boolean sendSctpSocketOptions(int sockId, java.util.Map<SctpSocketOption, SctpSocketParam> sctpSocketOptions){ return _open;}
    @Override
    public boolean sendSctpSocketParams(int sockId, java.util.Map<String, String> params){ return _open;}
  
    /************************* udp *************************/
    @Override
    public boolean sendUdpSocketBind(long bindId, String localIP, int localPort, boolean shared) {return _open;}
    @Override
    public boolean sendUdpSocketClose(int sockId) {return _open;}
    @Override
    public boolean sendUdpSocketData(int sockId, String remoteIP, int remotePort, String virtualIP,
				     int virtualPort, boolean copy, ByteBuffer ... bufs) {return _open;}

    
    @Override
    public boolean sendUdpSocketBind(long bindId, int localIP, int localPort, boolean shared) {
	return sendUdpSocketBind (bindId, MuxUtils.getIPAsString (localIP), localPort, shared);
    }
    @Override
    public boolean sendUdpSocketData(int sockId, int remoteIP, int remotePort, int virtualIP, int virtualPort,
				     byte[] data, int off, int len, boolean copy) {
	return sendUdpSocketData(sockId, MuxUtils.getIPAsString(remoteIP), remotePort,
				 MuxUtils.getIPAsString(virtualIP), virtualPort, data, off, len, copy);
    }
    @Override
    public boolean sendUdpSocketData(int sockId, String remoteIP, int remotePort, String virtualIP,
				     int virtualPort, byte[] data, int off, int len, boolean copy) {
	if (len == 0 || data == null)
	    return _open;
	return sendUdpSocketData(sockId, remoteIP, remotePort, virtualIP, virtualPort, copy,
				 ByteBuffer.wrap(data, off, len));
    }
  
    @Override
    public boolean sendUdpSocketData(int sockId, int remoteIP, int remotePort, int virtualIP, int virtualPort,
				     boolean copy, ByteBuffer ... bufs) {
	return sendUdpSocketData(sockId, MuxUtils.getIPAsString(remoteIP), remotePort,
				 MuxUtils.getIPAsString(virtualIP), virtualPort, copy, bufs);
    }
  
    /************************* dns *************************/
  
    @Override
    public boolean sendDnsGetByAddr(long reqId, String addr) { throw new UnsupportedOperationException("method sendDnsGetByAddr() is not supported");}
    @Override
    public boolean sendDnsGetByName(long reqId, String name) { throw new UnsupportedOperationException("method sendDnsGetByName() is not supported");}
    
    /************************* release *************************/
  
    @Override
    public boolean sendRelease(final long sessionId) { return _open;}
    @Override
    public boolean sendReleaseAck(long sessionId, boolean confirm) { return _open;}
    
    /******************* read controls ************************/
  
    @Override
    public void disableRead(int sockId) {}
    @Override
    public void enableRead(int sockId) {}
    @Override
    public void setInputExecutor(Executor inputExecutor) {}
}
