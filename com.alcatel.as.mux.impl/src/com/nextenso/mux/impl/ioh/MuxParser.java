// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl.ioh;

import java.nio.ByteBuffer;

import java.util.Map;
import java.io.*;

import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import com.alcatel.as.util.sctp.*;

import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.MuxHeaderV0;
import com.nextenso.mux.MuxHeaderV2;
import com.nextenso.mux.MuxHeaderV3;
import com.nextenso.mux.MuxHeaderV4;
import com.nextenso.mux.MuxHeaderV5;
import com.nextenso.mux.MuxHeaderV6;
import com.nextenso.mux.util.AbstractMuxConnection;
import com.nextenso.mux.util.MuxIdentification;
import com.nextenso.mux.util.MuxUtils;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public class MuxParser {

    public static final int MUX_VERSION_MAJOR = 2;
    public static final int MUX_VERSION_MINOR = 0;
    public static final int MUX_VERSION = (MUX_VERSION_MAJOR << 16) | MUX_VERSION_MINOR;

    private static final int MAGIC_COMMAND_HEADER = 0x12340000;
    private static final int MAGIC_EVENT_HEADER = 0x76540000;
    
    public static abstract class MuxMessage {
	protected ByteBuffer _data;
	protected boolean _copyDataOnSend = true;
	protected MuxMessage (){}
	protected boolean checkBufferSize (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    int len = buffer.getInt ();
	    if (buffer.remaining () < len){
		buffer.position (buffer.position () - 4);
		return false;
	    }
	    return true;
	}
	public boolean read (ByteBuffer buffer){ return true;}
	public boolean copyData (){
	    if (_data == null) return false;
	    _data = ByteBuffer.allocate (_data.remaining ()).put (_data);
	    _data.flip ();
	    _copyDataOnSend = false;
	    return true;
	}
	public abstract int getCode ();
    }
    // a command is sent from the agent to the stack
    public static abstract class MuxCommand extends MuxMessage {
	// this method is run on the stack side upon the reception of a MuxCommand
	public abstract void run (ExtendedMuxConnection connection);
    }
    // an event is sent from the stack to the agent
    public static abstract class MuxEvent extends MuxMessage {
	// this method is run on the agent side upon the reception of a MuxEvent
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){}
    }
    
    private static int CACHE_SIZE = 64*1024;
    private ByteBuffer _cache = ByteBuffer.allocate (CACHE_SIZE);
    private MuxMessage _currentMsg = null;
    private boolean _downgraded = true;
    
    // on the stack side
    public MuxParser (){
    }
    // on the agent side : we read some props from the muxhandler's config
    private boolean _byteBufferMode, _ipv6Support;
    public MuxParser (MuxHandler handler){
	// note that if _byteBufferMode=false, then the i/o read buffer cannot be a direct buffer
	_byteBufferMode = ((Boolean) handler.getMuxConfiguration ().get(MuxHandler.CONF_USE_NIO)).booleanValue();
	_ipv6Support = ((Boolean) handler.getMuxConfiguration ().get(MuxHandler.CONF_IPV6_SUPPORT)).booleanValue();
    }

    private ByteBuffer cache (ByteBuffer buffer){
	if (buffer.remaining () > _cache.remaining ()){
	    ByteBuffer tmp = ByteBuffer.allocate (_cache.position () + buffer.remaining ());
	    _cache.flip ();
	    tmp.put (_cache);
	    _cache = tmp;
	}
	_cache.put (buffer);
	return _cache;
    }

    public void downgrade (){ _downgraded = true;}
    public void upgrade (){ _downgraded = false;}
    
    public <T> T parse(ByteBuffer buffer){
	// _cache is in WRITE mode
	if (_cache.position () > 0){
	    cache (buffer);
	    _cache.flip ();
	    // _cache is in READ mode
	    buffer = _cache;
	}
	if (_currentMsg == null){
	    if (buffer.remaining () < 4){
		if (buffer == _cache) _cache.compact ();
		else cache (buffer);
		// _cache is in WRITE mode
		return null;
	    }
	    int code = buffer.getInt ();
	    //System.out.println ("code = "+code+" "+(code&0XFFFF));
	    switch (code){
	    case MuxVersionCommand.CODE : _currentMsg = new MuxVersionCommand (); break;
	    case MuxVersionEvent.CODE : _currentMsg = new MuxVersionEvent (); break;
	    case MuxPingCommand.CODE : _currentMsg = MuxPingCommand.INSTANCE; break;
	    case MuxPingEvent.CODE : _currentMsg = MuxPingEvent.INSTANCE; break;
	    case MuxPingAckCommand.CODE : _currentMsg = MuxPingAckCommand.INSTANCE; break;
	    case MuxPingAckEvent.CODE : _currentMsg = MuxPingAckEvent.INSTANCE; break;
	    case MuxStartCommand.CODE : _currentMsg = MuxStartCommand.INSTANCE; break;
	    case MuxStartEvent.CODE : _currentMsg = MuxStartEvent.INSTANCE; break;
	    case MuxStopCommand.CODE : _currentMsg = MuxStopCommand.INSTANCE; break;
	    case MuxIdentificationCommand.CODE : _currentMsg = new MuxIdentificationCommand (); break;
	    case MuxDataCommand.CODE : _currentMsg = new MuxDataCommand (); break;
	    case SendTcpSocketListenCommand.CODE : _currentMsg = new SendTcpSocketListenCommand (); break;
	    case SendTcpSocketConnectCommand.CODE : _currentMsg = new SendTcpSocketConnectCommand (); break;
	    case SendTcpSocketConnectCommandV10.CODE : _currentMsg = new SendTcpSocketConnectCommandV10 (); break;
	    case SendTcpSocketParamsCommand.CODE : _currentMsg = new SendTcpSocketParamsCommand (); break;
	    case SendTcpSocketResetCommand.CODE : _currentMsg = new SendTcpSocketResetCommand (); break;
	    case SendTcpSocketCloseCommand.CODE : _currentMsg = new SendTcpSocketCloseCommand (); break;
	    case SendTcpSocketAbortCommand.CODE : _currentMsg = new SendTcpSocketAbortCommand (); break;
	    case SendTcpSocketDataCommand.CODE : _currentMsg = new SendTcpSocketDataCommand (); break;
	    case SendUdpSocketBindCommand.CODE : _currentMsg = new SendUdpSocketBindCommand (); break;
	    case SendUdpSocketCloseCommand.CODE : _currentMsg = new SendUdpSocketCloseCommand (); break;
	    case SendUdpSocketDataCommand.CODE : _currentMsg = new SendUdpSocketDataCommand (); break;
	    case SendSctpSocketListenCommand.CODE : _currentMsg = new SendSctpSocketListenCommand (); break;
	    case SendSctpSocketConnectCommand.CODE : _currentMsg = new SendSctpSocketConnectCommand (); break;
	    case SendSctpSocketConnectCommandV10.CODE : _currentMsg = new SendSctpSocketConnectCommandV10 (); break;
	    case SendSctpSocketResetCommand.CODE : _currentMsg = new SendSctpSocketResetCommand (); break;
	    case SendSctpSocketCloseCommand.CODE : _currentMsg = new SendSctpSocketCloseCommand (); break;
	    case SendSctpSocketDataCommand.CODE : _currentMsg = new SendSctpSocketDataCommand (); break;
	    case SendSctpSocketOptionsCommand.CODE : _currentMsg = new SendSctpSocketOptionsCommand (); break;
	    case SendSctpSocketParamsCommand.CODE : _currentMsg = new SendSctpSocketParamsCommand (); break;
	    case SendReleaseCommand.CODE : _currentMsg = new SendReleaseCommand (); break;
	    case SendReleaseAckCommand.CODE : _currentMsg = new SendReleaseAckCommand (); break;
	    case DisableReadCommand.CODE : _currentMsg = new DisableReadCommand (); break;
	    case EnableReadCommand.CODE : _currentMsg = new EnableReadCommand (); break;
	    case ReleaseAckEvent.CODE : _currentMsg = new ReleaseAckEvent (); break;
	    case TcpSocketAbortedEvent.CODE : _currentMsg = new TcpSocketAbortedEvent (); break;
	    case TcpSocketClosedEvent.CODE : _currentMsg = new TcpSocketClosedEvent (); break;
	    case TcpSocketListeningEvent.CODE : _currentMsg = new TcpSocketListeningEvent (); break;
	    case TcpSocketConnectedEvent.CODE : _currentMsg = new TcpSocketConnectedEvent (); break;
	    case TcpSocketDataEvent.CODE_W_SESSION_ID : _currentMsg = new TcpSocketDataEvent (TcpSocketDataEvent.CODE_W_SESSION_ID); break;
	    case TcpSocketDataEvent.CODE_WO_SESSION_ID : _currentMsg = new TcpSocketDataEvent (TcpSocketDataEvent.CODE_WO_SESSION_ID); break;
	    case UdpSocketBoundEvent.CODE : _currentMsg = new UdpSocketBoundEvent (); break;
	    case UdpSocketClosedEvent.CODE : _currentMsg = new UdpSocketClosedEvent (); break;
	    case UdpSocketDataEvent.CODE_W_SESSION_ID : _currentMsg = new UdpSocketDataEvent (UdpSocketDataEvent.CODE_W_SESSION_ID); break;
	    case UdpSocketDataEvent.CODE_WO_SESSION_ID : _currentMsg = new UdpSocketDataEvent (UdpSocketDataEvent.CODE_WO_SESSION_ID); break;
	    case SctpSocketClosedEvent.CODE : _currentMsg = new SctpSocketClosedEvent (); break;
	    case SctpSocketListeningEvent.CODE : _currentMsg = new SctpSocketListeningEvent (); break;
	    case SctpSocketConnectedEvent.CODE : _currentMsg = new SctpSocketConnectedEvent (); break;
	    case SctpSocketDataEvent.CODE_W_SESSION_ID : _currentMsg = new SctpSocketDataEvent (SctpSocketDataEvent.CODE_W_SESSION_ID); break;
	    case SctpSocketDataEvent.CODE_WO_SESSION_ID : _currentMsg = new SctpSocketDataEvent (SctpSocketDataEvent.CODE_WO_SESSION_ID); break;
	    case SctpSocketSendFailedEvent.CODE : _currentMsg = new SctpSocketSendFailedEvent (); break;
	    case SctpPeerAddressChangedEvent.CODE : _currentMsg = new SctpPeerAddressChangedEvent (); break;
	    case MuxDataEvent.CODE : _currentMsg = new MuxDataEvent (); break;
	    case InternalDataEvent.CODE : _currentMsg = new InternalDataEvent (); break;
	    case InternalDataCommand.CODE : _currentMsg = new InternalDataCommand (); break;
	    default: throw new RuntimeException ("Invalid Mux Protocol : "+code+"/"+(code&0XFFFF));
	    }
	}
	if (_currentMsg.read (buffer)){
	    try{
		return (T) _currentMsg;
	    }finally {
		if (buffer == _cache){
		    _currentMsg.copyData ();
		    _cache.compact ();
		    // _cache is in WRITE mode
		    if (_cache.remaining () == 0 && _cache.capacity () > CACHE_SIZE)
			_cache = ByteBuffer.allocate (CACHE_SIZE);
		}
		_currentMsg = null;
	    }
	}
	// we have an incomplete msg : the code was read : the rest of the buffer is intact
	if (buffer == _cache) _cache.compact ();
	else cache (buffer);
	// _cache is in WRITE mode
	return null;
    }

    public static void sendMuxPingCommand (TcpChannel channel){
	MuxPingCommand.write (channel);
    }
    public static void sendMuxPingEvent (TcpChannel channel){
	MuxPingEvent.write (channel);
    }
    public static void sendMuxPingAckCommand (TcpChannel channel){
	MuxPingAckCommand.write (channel);
    }
    public static void sendMuxPingAckEvent (TcpChannel channel){
	MuxPingAckEvent.write (channel);
    }
    public static void sendMuxVersionCommand (TcpChannel channel){
	MuxVersionCommand.write (channel);
    }
    public static void sendMuxVersionEvent (TcpChannel channel){
	MuxVersionEvent.write (channel);
    }
    
    public static class MuxPingCommand extends MuxCommand {
	public static final MuxPingCommand INSTANCE = new MuxPingCommand ();
	public static final int CODE = 0xFFF0 | MAGIC_COMMAND_HEADER;
	private static final byte[] PAYLOAD = makeIntArray (CODE);
	public void run (ExtendedMuxConnection connection){
	    connection.sendMuxPingAck ();
	}
	public static void write (TcpChannel channel){ channel.send (PAYLOAD, 0, 4, false);}
	public String toString (){
	    return "MuxPingCommand";
	}
	public int getCode (){ return CODE;}
    }
    public static class MuxPingEvent extends MuxEvent {
	public static final MuxPingEvent INSTANCE = new MuxPingEvent ();
	public static final int CODE = 0xFFF1 | MAGIC_EVENT_HEADER;
	private static final byte[] PAYLOAD = makeIntArray (CODE);
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    connection.sendMuxPingAck ();
	}
	public static void write (TcpChannel channel){ channel.send (PAYLOAD, 0, 4, false);}
	public String toString (){
	    return "MuxPingEvent";
	}
	public int getCode (){ return CODE;}
    }
    public static class MuxPingAckCommand extends MuxCommand {
	public static final MuxPingAckCommand INSTANCE = new MuxPingAckCommand ();
	public static final int CODE = 0xFFF2 | MAGIC_COMMAND_HEADER;
	private static final byte[] PAYLOAD = makeIntArray (CODE);
	public void run (ExtendedMuxConnection connection){}
	public static void write (TcpChannel channel){ channel.send (PAYLOAD, 0, 4, false);}
	public String toString (){
	    return "MuxPingAckCommand";
	}
	public int getCode (){ return CODE;}
    }
    public static class MuxPingAckEvent extends MuxEvent {
	public static final MuxPingAckEvent INSTANCE = new MuxPingAckEvent ();
	public static final int CODE = 0xFFF3 | MAGIC_EVENT_HEADER;
	private static final byte[] PAYLOAD = makeIntArray (CODE);
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){}
	public static void write (TcpChannel channel){ channel.send (PAYLOAD, 0, 4, false);}
	public String toString (){
	    return "MuxPingAckEvent";
	}
	public int getCode (){ return CODE;}
    }
    public static class MuxVersionCommand extends MuxCommand {
	public static final int CODE = 0 | MAGIC_COMMAND_HEADER;
	private static final byte[] PAYLOAD = makeIntArray (CODE, MUX_VERSION);
	private int _version;
	public void run (ExtendedMuxConnection connection){
	    connection.setMuxVersion (_version);
	}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _version = buffer.getInt ();
	    return true;
	}
	public static void write (TcpChannel channel){ channel.send (PAYLOAD, 0, 8, false);}
	public String toString (){
	    return "MuxVersionCommand["+(_version >>> 16)+"."+(_version & 0xFFFF)+"]";
	}
	public int getCode (){ return CODE;}
    }
    public static class MuxVersionEvent extends MuxEvent {
	public static final int CODE = 0 | MAGIC_EVENT_HEADER;
	private static final byte[] PAYLOAD = makeIntArray (CODE, MUX_VERSION);
	private int _version;
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    connection.setMuxVersion (_version);
	}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _version = buffer.getInt ();
	    return true;
	}
	public static void write (TcpChannel channel){ channel.send (PAYLOAD, 0, 8, false);}
	public String toString (){
	    return "MuxVersionEvent["+(_version >>> 16)+"."+(_version & 0xFFFF)+"]";
	}
	public int getCode (){ return CODE;}
    }

    /**************************** MuxCommand ********************/

    public static void sendMuxStart (TcpChannel channel){
	MuxStartCommand.write (channel);
    }
    public static void sendMuxStop(TcpChannel channel) {
	MuxStopCommand.write (channel);
    }
    public static void sendMuxData(TcpChannel channel, MuxHeader header, boolean copy, ByteBuffer ... buf) {
	MuxDataCommand.write (channel, header, copy, buf);
    }
    public static void sendMuxIdentification(TcpChannel channel, MuxIdentification id) {
	MuxIdentificationCommand.write (channel, id);
    }
    public static void sendInternalMuxData (TcpChannel channel, MuxHeader h, boolean copy, ByteBuffer... buff){
	InternalDataCommand.write (channel, h, copy, buff);
    }

    public static class MuxStartCommand extends MuxCommand {
	public static final MuxStartCommand INSTANCE = new MuxStartCommand ();
	public static final int CODE = 1 | MAGIC_COMMAND_HEADER;
	private static final byte[] PAYLOAD = makeIntArray (CODE);
	public void run (ExtendedMuxConnection connection){connection.sendMuxStart ();}
	public static void write (TcpChannel channel){ channel.send (PAYLOAD, 0, 4, false);}
	public String toString (){
	    return "MuxStart";
	}
	public int getCode (){ return CODE;}
    }
    public static class MuxStopCommand extends MuxCommand {
	public static final MuxStopCommand INSTANCE = new MuxStopCommand ();
	public static final int CODE = 2 | MAGIC_COMMAND_HEADER;
	private static final byte[] PAYLOAD = makeIntArray (CODE);
	public void run (ExtendedMuxConnection connection){connection.sendMuxStop ();}
	public static void write (TcpChannel channel){ channel.send (PAYLOAD, 0, 4, false);}
	public String toString (){
	    return "MuxStop";
	}
	public int getCode (){ return CODE;}
    }
    public static class MuxDataCommand extends MuxCommand {
	public static final int CODE = 3 | MAGIC_COMMAND_HEADER;
	protected MuxHeader _header;
	public void run (ExtendedMuxConnection connection){
	    connection.sendMuxData (_header, _copyDataOnSend, _data);
	}
    	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    int len = buffer.getInt ();
	    if (buffer.remaining () < len){
		buffer.position (buffer.position () - 4);
		return false;
	    }
	    _header = getMuxHeader (buffer);
	    len -= getMuxHeaderLength (_header);
	    _data = wrap (buffer, len);
	    return true;
	}
	public static void write (TcpChannel channel, MuxHeader header, boolean copy, ByteBuffer ... bufs){
	    write (channel, CODE, header, copy, bufs);
	}
	public static void write (TcpChannel channel, int code, MuxHeader header, boolean copy, ByteBuffer ... bufs){
	    int hlen = getMuxHeaderLength (header);
	    int len = hlen;
	    if (bufs == null || bufs.length == 0 || (bufs.length == 1 && bufs[0] == null)){
		bufs = null;
		copy = false;
	    } else 
		len += remaining (bufs);
	    if (copy){
		ByteBuffer buffer = ByteBuffer.allocate (8+len);
		buffer.putInt (code).putInt (len);
		putMuxHeader (buffer, header);
		for (ByteBuffer b : bufs) buffer.put (b);
		buffer.flip ();
		channel.send (buffer, false);
	    } else {
		ByteBuffer buffer = ByteBuffer.allocate (8+hlen);
		buffer.putInt (code).putInt (len);
		putMuxHeader (buffer, header);
		buffer.flip ();
		if (bufs != null) channel.send (prepend (buffer, bufs), false);
		else channel.send (buffer, false);
	    }
	}
	public String toString (){
	    return new StringBuilder ().append ("MuxDataCommand").toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class MuxIdentificationCommand extends MuxCommand {
	public static final int CODE = 4 | MAGIC_COMMAND_HEADER;
	private MuxIdentification _id = new MuxIdentification ();
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    _id.setAgentID (buffer.getLong ()).setAppName (getString (buffer)).setHostName (getString (buffer)).setContainerIndex (buffer.getInt ()).setGroupID (buffer.getLong ()).setIdleFactor (buffer.getLong ()).setInstanceName (getString (buffer)).setKeepAlive (buffer.getLong ()).setRingID (buffer.getLong ());
	    return true;
	}
	public void run (ExtendedMuxConnection connection){ connection.sendMuxIdentification (_id); }
	public static void write (TcpChannel channel, MuxIdentification id){
	    if (id.getHostName () == null) id.setHostName ("localhost"); // not expected - to avoid a NPE in standalone mode
	    ByteBuffer buffer = ByteBuffer.allocate (58+id.getAppName ().length ()+id.getInstanceName ().length ()+id.getHostName ().length ());
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putLong (id.getAgentID ());
	    putString (buffer, id.getAppName ());
	    putString (buffer, id.getHostName ());
	    buffer.putInt (id.getContainerIndex ()).putLong (id.getGroupID ()).putLong (id.getIdleFactor ());
	    putString (buffer, id.getInstanceName ());
	    buffer.putLong (id.getKeepAlive ()).putLong (id.getRingID ());
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("MuxIdentificationCommand[").append (_id).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public void sendTcpSocketListen(TcpChannel channel, long listenId, String localIP, int localPort, boolean secure) {
	SendTcpSocketListenCommand.write (channel, listenId, localIP, localPort, secure);
    }
    public void sendTcpSocketConnect(TcpChannel channel, long connectionId, String remoteHost, int remotePort, String localIP, int localPort, boolean secure, Map<String, String> params) {
	if (_downgraded)
	    SendTcpSocketConnectCommandV10.write (channel, connectionId, remoteHost, remotePort, localIP, localPort, secure);
	else
	    SendTcpSocketConnectCommand.write (channel, connectionId, remoteHost, remotePort, localIP, localPort, secure, params);
    }
    public void sendTcpSocketParams(TcpChannel channel, int sockId, Map<String, String> params){
	if (_downgraded) return;
	SendTcpSocketParamsCommand.write (channel, sockId, params);
    }
    public void sendTcpSocketReset(TcpChannel channel, int sockId) {
	SendTcpSocketResetCommand.write (channel, sockId);
    }
    public void sendTcpSocketClose(TcpChannel channel, int sockId) {
	SendTcpSocketCloseCommand.write (channel, sockId);
    }
    public void sendTcpSocketAbort(TcpChannel channel, int sockId) {
	SendTcpSocketAbortCommand.write (channel, sockId);
    }
    public void sendTcpSocketData(TcpChannel channel, int sockId, boolean copy, ByteBuffer ... bufs) {
	SendTcpSocketDataCommand.write (channel, sockId, copy, bufs);
    }
    
    public static class SendTcpSocketListenCommand extends MuxCommand {
	public static final int CODE = 10 | MAGIC_COMMAND_HEADER;
	private long _listenId;
	private String _localIP;
	private int _localPort;
	private byte _secure;
	public SendTcpSocketListenCommand set (long listenId, String localIP, int localPort, byte secure){
	    _listenId = listenId;
	    _localIP = localIP;
	    _localPort = localPort;
	    _secure = secure;
	    return this;
	}
	public void run (ExtendedMuxConnection connection){connection.sendTcpSocketListen (_listenId, _localIP, _localPort, _secure == 1);}
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    set (buffer.getLong (), getIPString (buffer), buffer.getInt (), buffer.get ());
	    return true;
	}
	public static void write (TcpChannel channel, long listenId, String localIP, int localPort, boolean secure){
	    if (localIP == null) localIP = "";
	    ByteBuffer buffer = ByteBuffer.allocate (23+localIP.length ());
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putLong (listenId);
	    putString (buffer, localIP);
	    buffer.putInt (localPort).put (secure ? (byte)1 : (byte)0);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendTcpSocketListenCommand[").append (_localIP).append ('/').append (_localPort).append ('/').append (_secure).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendTcpSocketConnectCommand extends MuxCommand {
	public static final int CODE = 17 | MAGIC_COMMAND_HEADER;
	private long _connectionId;
	private String _remoteIP, _localIP;
	private int _remotePort, _localPort;
	private byte _secure;
	private Map<String, String> _params;
	public SendTcpSocketConnectCommand set (long connectionId, String remoteIP, int remotePort, String localIP, int localPort, byte secure){
	    _connectionId = connectionId;
	    _remoteIP = remoteIP;
	    _remotePort = remotePort;
	    _localIP = localIP;
	    _localPort = localPort;
	    _secure = secure;
	    return this;
	}
	public void run (ExtendedMuxConnection connection){connection.sendTcpSocketConnect (_connectionId, _remoteIP, _remotePort, _localIP, _localPort, _secure == 1, _params);}
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    set (buffer.getLong (), getIPString (buffer), buffer.getShort () & 0xFFFF, getIPString (buffer), buffer.getShort () & 0xFFFF, buffer.get ());
	    if (buffer.get () == (byte)1)
		_params = getMap (buffer);
	    return true;
	}
	public static void write (TcpChannel channel, long connectionId, String remoteIP, int remotePort, String localIP, int localPort, boolean secure, Map<String, String> params){
	    if (localIP == null) localIP = "";
	    // remoteIP cannot be null
	    int paramsSize = sizeof (params);
	    ByteBuffer buffer = ByteBuffer.allocate (26+remoteIP.length ()+localIP.length ()+paramsSize);
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putLong (connectionId);
	    putString (buffer, remoteIP);
	    buffer.putShort ((short)remotePort);
	    putString (buffer, localIP);
	    buffer.putShort ((short)localPort).put (secure ? (byte)1 : (byte)0);
	    putBoolean (buffer, paramsSize > 0);
	    if (paramsSize > 0) putMap (buffer, params);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendTcpSocketConnectCommand[").append (_remoteIP).append ('/').append (_remotePort).append ('/').append (_secure).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public static class SendTcpSocketConnectCommandV10 extends MuxCommand {
	public static final int CODE = 11 | MAGIC_COMMAND_HEADER;
	private long _connectionId;
	private String _remoteIP, _localIP;
	private int _remotePort, _localPort;
	private byte _secure;
	public SendTcpSocketConnectCommandV10 set (long connectionId, String remoteIP, int remotePort, String localIP, int localPort, byte secure){
	    _connectionId = connectionId;
	    _remoteIP = remoteIP;
	    _remotePort = remotePort;
	    _localIP = localIP;
	    _localPort = localPort;
	    _secure = secure;
	    return this;
	}
	public void run (ExtendedMuxConnection connection){connection.sendTcpSocketConnect (_connectionId, _remoteIP, _remotePort, _localIP, _localPort, _secure == 1);}
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    set (buffer.getLong (), getIPString (buffer), buffer.getShort () & 0xFFFF, getIPString (buffer), buffer.getShort () & 0xFFFF, buffer.get ());
	    return true;
	}
	public static void write (TcpChannel channel, long connectionId, String remoteIP, int remotePort, String localIP, int localPort, boolean secure){
	    if (localIP == null) localIP = "";
	    // remoteIP cannot be null
	    ByteBuffer buffer = ByteBuffer.allocate (25+remoteIP.length ()+localIP.length ());
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putLong (connectionId);
	    putString (buffer, remoteIP);
	    buffer.putShort ((short)remotePort);
	    putString (buffer, localIP);
	    buffer.putShort ((short)localPort).put (secure ? (byte)1 : (byte)0);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendTcpSocketConnectCommandV10[").append (_remoteIP).append ('/').append (_remotePort).append ('/').append (_secure).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendTcpSocketAbortCommand extends MuxCommand {
	public static final int CODE = 12 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	public void run (ExtendedMuxConnection connection){connection.sendTcpSocketAbort (_sockId);}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _sockId = buffer.getInt ();
	    return true;
	}
	public static void write (TcpChannel channel, int sockId){
	    ByteBuffer buffer = ByteBuffer.allocate (8);
	    buffer.putInt (CODE).putInt (sockId);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendTcpSocketAbortCommand[").append (_sockId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendTcpSocketCloseCommand extends MuxCommand {
	public static final int CODE = 13 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	public void run (ExtendedMuxConnection connection){connection.sendTcpSocketClose (_sockId);}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _sockId = buffer.getInt ();
	    return true;
	}
	public static void write (TcpChannel channel, int sockId){
	    ByteBuffer buffer = ByteBuffer.allocate (8);
	    buffer.putInt (CODE).putInt (sockId);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendTcpSocketCloseCommand[").append (_sockId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendTcpSocketResetCommand extends MuxCommand {
	public static final int CODE = 14 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	public void run (ExtendedMuxConnection connection){connection.sendTcpSocketReset (_sockId);}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _sockId = buffer.getInt ();
	    return true;
	}
	public static void write (TcpChannel channel, int sockId){
	    ByteBuffer buffer = ByteBuffer.allocate (8);
	    buffer.putInt (CODE).putInt (sockId);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendTcpSocketResetCommand[").append (_sockId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendTcpSocketDataCommand extends MuxCommand {
	public static final int CODE = 15 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	public void run (ExtendedMuxConnection connection){
	    connection.sendTcpSocketData (_sockId, _copyDataOnSend, _data);
	}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    int len = buffer.getInt ();
	    if (buffer.remaining () < len){
		buffer.position (buffer.position () - 4);
		return false;
	    }
	    _sockId = buffer.getInt ();
	    len -= 4;
	    _data = wrap (buffer, len);
	    return true;
	}
	public static void write (TcpChannel channel, int sockId, boolean copy, ByteBuffer ... bufs){
	    int len = remaining (bufs);
	    if (copy){
		ByteBuffer buffer = ByteBuffer.allocate (12+len);
		buffer.putInt (CODE).putInt (buffer.capacity () - 8).putInt (sockId);
		for (ByteBuffer b : bufs) buffer.put (b);
		buffer.flip ();
		channel.send (buffer, false);
	    } else {
		ByteBuffer buffer = ByteBuffer.allocate (12);
		buffer.putInt (CODE).putInt (4 + len).putInt (sockId);
		buffer.flip ();
		channel.send (prepend (buffer, bufs), false);
	    }
	}
	public String toString (){
	    return new StringBuilder ().append ("SendTcpSocketDataCommand[").append (_sockId).append ('/').append (_data.remaining ()).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendTcpSocketParamsCommand extends MuxCommand {
	public static final int CODE = 16 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	private Map<String, String> _params;
	public void run (ExtendedMuxConnection connection){connection.sendTcpSocketParams (_sockId, _params);}
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    _sockId = buffer.getInt ();
	    _params = getMap (buffer);
	    return true;
	}
	public static void write (TcpChannel channel, int sockId, Map<String, String> params){
	    int paramsSize = sizeof (params);
	    if (paramsSize < 1) return;
	    ByteBuffer buffer = ByteBuffer.allocate (12+paramsSize);
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putInt (sockId);
	    putMap (buffer, params);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendTcpSocketParamsCommand[").append (_sockId).append ('/').append (_params).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public void sendUdpSocketBind(TcpChannel channel, long bindId, String localIP, int localPort, boolean shared) {
	SendUdpSocketBindCommand.write (channel, bindId, localIP, localPort, shared);
    }
    public void sendUdpSocketClose(TcpChannel channel, int sockId) {
	SendUdpSocketCloseCommand.write (channel, sockId);
    }
    public void sendUdpSocketData(TcpChannel channel, int sockId, String remoteIP, int remotePort, boolean copy, ByteBuffer ... bufs) {
	SendUdpSocketDataCommand.write (channel, sockId, remoteIP, remotePort, copy, bufs);
    }

    public static class SendUdpSocketBindCommand extends MuxCommand {
	public static final int CODE = 20 | MAGIC_COMMAND_HEADER;
	private long _bindId;
	private String _localIP;
	private int _localPort;
	private byte _shared;
	public SendUdpSocketBindCommand set (long bindId, String localIP, int localPort, byte shared){
	    _bindId = bindId;
	    _localIP = localIP;
	    _localPort = localPort;
	    _shared = shared;
	    return this;
	}
	public void run (ExtendedMuxConnection connection){connection.sendUdpSocketBind (_bindId, _localIP, _localPort, _shared == 1);}
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    set (buffer.getLong (), getIPString (buffer), buffer.getInt (), buffer.get ());
	    return true;
	}
	public static void write (TcpChannel channel, long bindId, String localIP, int localPort, boolean shared){
	    if (localIP == null) localIP = "";
	    ByteBuffer buffer = ByteBuffer.allocate (23+localIP.length ());
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putLong (bindId);
	    putString (buffer, localIP);
	    buffer.putInt (localPort).put (shared ? (byte)1 : (byte)0);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendUdpSocketBindCommand[").append (_localIP).append ('/').append (_localPort).append ('/').append (_shared).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendUdpSocketCloseCommand extends MuxCommand {
	public static final int CODE = 21 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	public void run (ExtendedMuxConnection connection){connection.sendUdpSocketClose (_sockId);}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _sockId = buffer.getInt ();
	    return true;
	}
	public static void write (TcpChannel channel, int sockId){
	    ByteBuffer buffer = ByteBuffer.allocate (8);
	    buffer.putInt (CODE).putInt (sockId);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendUdpSocketCloseCommand[").append (_sockId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendUdpSocketDataCommand extends MuxCommand {
	public static final int CODE = 22 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	private String _remoteIP;
	private int _remotePort;
	public void run (ExtendedMuxConnection connection){
	    connection.sendUdpSocketData (_sockId, _remoteIP, _remotePort, null, 0, _copyDataOnSend, _data);
	}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    int len = buffer.getInt ();
	    if (buffer.remaining () < len){
		buffer.position (buffer.position () - 4);
		return false;
	    }
	    _sockId = buffer.getInt ();
	    _remoteIP = getIPString (buffer);
	    _remotePort = buffer.getShort () & 0xFFFF;
	    len = buffer.getInt ();
	    _data = wrap (buffer, len);
	    return true;
	}
	public static void write (TcpChannel channel, int sockId, String remoteIP, int remotePort, boolean copy, ByteBuffer ... bufs){
	    // remoteIP cannot be null
	    int len = remaining (bufs);
	    if (copy){
		ByteBuffer buffer = ByteBuffer.allocate (20+remoteIP.length ()+len);
		buffer.putInt (CODE).putInt (buffer.capacity () - 8).putInt (sockId);
		putString (buffer, remoteIP);
		buffer.putShort ((short)remotePort).putInt (len);
		for (ByteBuffer b : bufs) buffer.put (b);
		buffer.flip ();
		channel.send (buffer, false);
	    } else {
		ByteBuffer buffer = ByteBuffer.allocate (20+remoteIP.length ());
		buffer.putInt (CODE).putInt (buffer.capacity () - 8 + len).putInt (sockId);
		putString (buffer, remoteIP);
		buffer.putShort ((short)remotePort).putInt (len);
		buffer.flip ();
		channel.send (prepend (buffer, bufs), false);
	    }
	}
	public String toString (){
	    return new StringBuilder ().append ("SendUdpSocketDataCommand[").append (_sockId).append ('/').append (_remoteIP).append ('/').append (_remotePort).append ('/').append (_data.remaining ()).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public void sendRelease(TcpChannel channel, long sessionId) {
	SendReleaseCommand.write (channel, sessionId);
    }
    public void sendReleaseAck(TcpChannel channel, long sessionId, boolean confirm) {
	SendReleaseAckCommand.write (channel, sessionId, confirm);
    }

    public static class SendReleaseCommand extends MuxCommand {
	public static final int CODE = 40 | MAGIC_COMMAND_HEADER;
	private long _sessionId;
	public void run (ExtendedMuxConnection connection){connection.sendRelease (_sessionId);}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 8) return false;
	    _sessionId = buffer.getLong ();
	    return true;
	}
	public static void write (TcpChannel channel, long sessionId){
	    ByteBuffer buffer = ByteBuffer.allocate (12);
	    buffer.putInt (CODE).putLong (sessionId);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendReleaseCommand[").append (_sessionId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendReleaseAckCommand extends MuxCommand {
	public static final int CODE = 41 | MAGIC_COMMAND_HEADER;
	private long _sessionId;
	private boolean _confirm;
	public void run (ExtendedMuxConnection connection){connection.sendReleaseAck (_sessionId, _confirm);}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 9) return false;
	    _sessionId = buffer.getLong ();
	    _confirm = buffer.get () == 1;
	    return true;
	}
	public static void write (TcpChannel channel, long sessionId, boolean confirm){
	    ByteBuffer buffer = ByteBuffer.allocate (13);
	    buffer.putInt (CODE).putLong (sessionId).put (confirm ? (byte)1 : (byte)0);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendReleaseAckCommand[").append (_sessionId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public void disableRead(TcpChannel channel, int sockId) {
	DisableReadCommand.write (channel, sockId);
    }
    public void enableRead(TcpChannel channel, int sockId) {
	EnableReadCommand.write (channel, sockId);
    }
    
    public static class DisableReadCommand extends MuxCommand {
	public static final int CODE = 50 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	public void run (ExtendedMuxConnection connection){connection.disableRead (_sockId);}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _sockId = buffer.getInt ();
	    return true;
	}
	public static void write (TcpChannel channel, int sockId){
	    ByteBuffer buffer = ByteBuffer.allocate (8);
	    buffer.putInt (CODE).putInt (sockId);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("DisableReadCommand[").append (_sockId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class EnableReadCommand extends MuxCommand {
	public static final int CODE = 51 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	public void run (ExtendedMuxConnection connection){connection.enableRead (_sockId);}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _sockId = buffer.getInt ();
	    return true;
	}
	public static void write (TcpChannel channel, int sockId){
	    ByteBuffer buffer = ByteBuffer.allocate (8);
	    buffer.putInt (CODE).putInt (sockId);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("EnableReadCommand[").append (_sockId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class InternalDataCommand extends MuxDataCommand {
	public static final int CODE = 60 | MAGIC_COMMAND_HEADER;
	@Override
	public void run (ExtendedMuxConnection connection){
	    connection.sendInternalMuxData (_header, _copyDataOnSend, _data);
	}
	public static void write (TcpChannel channel, MuxHeader header, boolean copy, ByteBuffer ... bufs){
	    write (channel, CODE, header, copy, bufs);
	}
	public String toString (){
	    return new StringBuilder ().append ("InternalDataCommand[").append (_header).append (']').toString ();
	}
	@Override
	public int getCode (){ return CODE;}
    }
    
    public void sendSctpSocketListen(TcpChannel channel, long listenId, String[] localIPs, int localPort, int maxOutStreams, int maxInStreams, boolean secure){
	SendSctpSocketListenCommand.write (channel, listenId, localIPs, localPort, maxOutStreams, maxInStreams, secure);
    }
    public void sendSctpSocketConnect(TcpChannel channel, long connectionId, String remoteHost, int remotePort, String[] localIPs, int localPort, int maxOutStreams, int maxInStreams, boolean secure, Map<SctpSocketOption, SctpSocketParam> options, Map<String, String> params) {
	if (_downgraded)
	    SendSctpSocketConnectCommandV10.write (channel, connectionId, remoteHost, remotePort, localIPs, localPort, maxOutStreams, maxInStreams, secure, options);
	else
	    SendSctpSocketConnectCommand.write (channel, connectionId, remoteHost, remotePort, localIPs, localPort, maxOutStreams, maxInStreams, secure, options, params);
    }
    public void sendSctpSocketReset(TcpChannel channel, int sockId) {
	SendSctpSocketResetCommand.write (channel, sockId);
    }
    public void sendSctpSocketClose(TcpChannel channel, int sockId) {
	SendSctpSocketCloseCommand.write (channel, sockId);
    }
    public void sendSctpSocketData(TcpChannel channel, int sockId, String addr, boolean unordered, boolean complete, int ploadPID, int streamNumber, long timeToLive,boolean copy, ByteBuffer ... bufs) {
	SendSctpSocketDataCommand.write (channel, sockId, addr, unordered, complete, ploadPID, streamNumber, timeToLive, copy, bufs);
    }
    public void sendSctpSocketOptions(TcpChannel channel, int sockId, Map<SctpSocketOption, SctpSocketParam> options){
	SendSctpSocketOptionsCommand.write (channel, sockId, options);
    }
    public void sendSctpSocketParams(TcpChannel channel, int sockId, Map<String, String> params){
	if (_downgraded) return;
	SendSctpSocketParamsCommand.write (channel, sockId, params);
    }
    
    public static class SendSctpSocketListenCommand extends MuxCommand {
	public static final int CODE = 70 | MAGIC_COMMAND_HEADER;
	private long _listenId;
	private String[] _localIPs;
	private int _localPort;
	private int _maxOutStreams, _maxInStreams;
	private byte _secure;
	public SendSctpSocketListenCommand set (long listenId, String[] localIPs, int localPort, int maxOutStreams, int maxInStreams, byte secure){
	    _listenId = listenId;
	    _localIPs = localIPs;
	    _localPort = localPort;
	    _maxOutStreams = maxOutStreams;
	    _maxInStreams = maxInStreams;
	    _secure = secure;
	    return this;
	}
	public void run (ExtendedMuxConnection connection){connection.sendSctpSocketListen (_listenId, _localIPs, _localPort, _maxOutStreams, _maxInStreams, _secure == 1);}
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    set (buffer.getLong (), getStrings (buffer, true), buffer.getInt (), buffer.getInt (), buffer.getInt (), buffer.get ());
	    return true;
	}
	public static void write (TcpChannel channel, long listenId, String[] localIPs, int localPort, int maxOutStreams, int maxInStreams, boolean secure){
	    ByteBuffer buffer = ByteBuffer.allocate (29+sizeof (localIPs));
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putLong (listenId);
	    putStrings (buffer, localIPs);
	    buffer.putInt (localPort).putInt (maxOutStreams).putInt (maxInStreams);
	    putBoolean (buffer, secure);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendSctpSocketListenCommand[").append (_localIPs[0]).append ('/').append (_localPort).append ('/').append (_secure).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    
    public static class SendSctpSocketConnectCommand extends MuxCommand {
	private static final String[] EMPTY = new String[0];
	public static final int CODE = 77 | MAGIC_COMMAND_HEADER;
	private long _connectionId;
	private String _remoteIP;
	private String[] _localIPs;
	private int _remotePort, _localPort, _maxInStreams, _maxOutStreams;
	private byte _secure;
	private Map<SctpSocketOption, SctpSocketParam> _options;
	private Map<String, String> _params;
	public SendSctpSocketConnectCommand set (long connectionId, String remoteIP, int remotePort, String[] localIPs, int localPort, int maxOutStreams, int maxInStreams, byte secure){
	    _connectionId = connectionId;
	    _remoteIP = remoteIP;
	    _remotePort = remotePort;
	    _localIPs = localIPs;
	    _localPort = localPort;
	    _maxOutStreams = maxOutStreams;
	    _maxInStreams = maxInStreams;
	    _secure = secure;
	    return this;
	}
	public void run (ExtendedMuxConnection connection){connection.sendSctpSocketConnect (_connectionId, _remoteIP, _remotePort, _localIPs, _localPort, _maxOutStreams, _maxInStreams, _secure == 1, _options, _params);}
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    set (buffer.getLong (), getIPString (buffer), buffer.getShort () & 0xFFFF, getStrings (buffer, true), buffer.getShort () & 0xFFFF, buffer.getShort () & 0xFFFF, buffer.getShort () & 0xFFFF, buffer.get ());
	    if (buffer.get () == (byte)1){
		_options = readSctpOptions (buffer);
	    }
	    if (buffer.get () == (byte)1){
		_params = getMap (buffer);
	    }
	    return true;
	}
	public static void write (TcpChannel channel, long connectionId, String remoteIP, int remotePort, String[] localIPs, int localPort, int maxOutStreams, int maxInStreams, boolean secure, Map<SctpSocketOption, SctpSocketParam> options, Map<String, String> params){
	    if (localIPs == null || (localIPs.length == 1 && localIPs[0] == null)) localIPs = EMPTY;
	    // remoteIP cannot be null
	    ByteBuffer optionsBuffer = writeSctpOptions (options);
	    int optionsBufferSize = optionsBuffer != null ? optionsBuffer.remaining () : 0;
	    int paramsSize = sizeof (params);
	    ByteBuffer buffer = ByteBuffer.allocate (27+sizeof (remoteIP)+sizeof (localIPs)+optionsBufferSize+paramsSize);
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putLong (connectionId);
	    putString (buffer, remoteIP);
	    buffer.putShort ((short)remotePort);
	    putStrings (buffer, localIPs);
	    buffer.putShort ((short)localPort).putShort ((short)maxOutStreams).putShort ((short)maxInStreams);
	    putBoolean (buffer, secure);
	    putBoolean (buffer, optionsBuffer != null);
	    if (optionsBuffer != null) buffer.put (optionsBuffer);
	    putBoolean (buffer, paramsSize > 0);
	    if (paramsSize > 0) putMap (buffer, params);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendSctpSocketConnectCommand[").append (_remoteIP).append ('/').append (_remotePort).append ('/').append (_secure).append ('/').append (_options).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendSctpSocketConnectCommandV10 extends MuxCommand {
	private static final String[] EMPTY = new String[0];
	public static final int CODE = 71 | MAGIC_COMMAND_HEADER;
	private long _connectionId;
	private String _remoteIP;
	private String[] _localIPs;
	private int _remotePort, _localPort, _maxInStreams, _maxOutStreams;
	private byte _secure;
	private Map<SctpSocketOption, SctpSocketParam> _params;
	public SendSctpSocketConnectCommandV10 set (long connectionId, String remoteIP, int remotePort, String[] localIPs, int localPort, int maxOutStreams, int maxInStreams, byte secure){
	    _connectionId = connectionId;
	    _remoteIP = remoteIP;
	    _remotePort = remotePort;
	    _localIPs = localIPs;
	    _localPort = localPort;
	    _maxOutStreams = maxOutStreams;
	    _maxInStreams = maxInStreams;
	    _secure = secure;
	    return this;
	}
	public void run (ExtendedMuxConnection connection){connection.sendSctpSocketConnect (_connectionId, _remoteIP, _remotePort, _localIPs, _localPort, _maxOutStreams, _maxInStreams, _secure == 1, _params, null);}
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    set (buffer.getLong (), getIPString (buffer), buffer.getShort () & 0xFFFF, getStrings (buffer, true), buffer.getShort () & 0xFFFF, buffer.getShort () & 0xFFFF, buffer.getShort () & 0xFFFF, buffer.get ());
	    if (buffer.get () == (byte)1){
		_params = readSctpOptions (buffer);
	    }
	    return true;
	}
	public static void write (TcpChannel channel, long connectionId, String remoteIP, int remotePort, String[] localIPs, int localPort, int maxOutStreams, int maxInStreams, boolean secure){
	    write(channel, connectionId, remoteIP, remotePort, localIPs, localPort, maxOutStreams, maxInStreams, secure, null);
	}
	public static void write (TcpChannel channel, long connectionId, String remoteIP, int remotePort, String[] localIPs, int localPort, int maxOutStreams, int maxInStreams, boolean secure, Map<SctpSocketOption, SctpSocketParam> params){
	    if (localIPs == null || (localIPs.length == 1 && localIPs[0] == null)) localIPs = EMPTY;
	    // remoteIP cannot be null
	    ByteBuffer paramsBuffer = writeSctpOptions (params);
	    int paramsBufferSize = paramsBuffer != null ? paramsBuffer.remaining () : 0;
	    ByteBuffer buffer = ByteBuffer.allocate (26+sizeof (remoteIP)+sizeof (localIPs)+paramsBufferSize);
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putLong (connectionId);
	    putString (buffer, remoteIP);
	    buffer.putShort ((short)remotePort);
	    putStrings (buffer, localIPs);
	    buffer.putShort ((short)localPort).putShort ((short)maxOutStreams).putShort ((short)maxInStreams);
	    putBoolean (buffer, secure);
	    putBoolean (buffer, paramsBuffer != null);
	    if (paramsBuffer != null) buffer.put (paramsBuffer);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendSctpSocketConnectCommandV10[").append (_remoteIP).append ('/').append (_remotePort).append ('/').append (_secure).append ('/').append (_params).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendSctpSocketCloseCommand extends MuxCommand {
	public static final int CODE = 72 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	public void run (ExtendedMuxConnection connection){connection.sendSctpSocketClose (_sockId);}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _sockId = buffer.getInt ();
	    return true;
	}
	public static void write (TcpChannel channel, int sockId){
	    ByteBuffer buffer = ByteBuffer.allocate (8);
	    buffer.putInt (CODE).putInt (sockId);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendSctpSocketCloseCommand[").append (_sockId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendSctpSocketResetCommand extends MuxCommand {
	public static final int CODE = 73 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	public void run (ExtendedMuxConnection connection){connection.sendSctpSocketReset (_sockId);}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _sockId = buffer.getInt ();
	    return true;
	}
	public static void write (TcpChannel channel, int sockId){
	    ByteBuffer buffer = ByteBuffer.allocate (8);
	    buffer.putInt (CODE).putInt (sockId);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendSctpSocketResetCommand[").append (_sockId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendSctpSocketDataCommand extends MuxCommand {
	public static final int CODE = 74 | MAGIC_COMMAND_HEADER;
	private int _sockId, _ploadPID, _streamNb, _ttl;
	private boolean _unordered, _complete;
	private String _addr;
	public void run (ExtendedMuxConnection connection){
	    connection.sendSctpSocketData (_sockId, _addr, _unordered, _complete, _ploadPID, _streamNb, ((long)_ttl) & 0xFFFFFFFFL, _copyDataOnSend, _data);
	}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    int len = buffer.getInt ();
	    if (buffer.remaining () < len){
		buffer.position (buffer.position () - 4);
		return false;
	    }
	    _sockId = buffer.getInt ();
	    byte flags = buffer.get ();
	    _unordered = (flags & (byte)0x10) == (byte)0x10;
	    _complete = (flags & (byte)0x01) == (byte)0x01;
	    _streamNb = buffer.getShort () & 0xFFFF;
	    if ((flags & (byte)0x80) == (byte)0x80){ // optimal
		len -= 7;
	    } else {
		int pos = buffer.position ();
		_addr = getIPString (buffer);
		_ploadPID = buffer.getInt ();
		_ttl = buffer.getInt ();
		int diff = buffer.position () - pos;
		len -= 7 + diff;
	    }
	    _data = wrap (buffer, len);
	    return true;
	}
	public static void write (TcpChannel channel,
				  int sockId,
				  String addr,
				  boolean unordered,
				  boolean complete,
				  int ploadPID,
				  int streamNumber,
				  long timeToLive,
				  boolean copy,
				  ByteBuffer... data){
	    int len = remaining (data);
	    boolean optimal = (addr == null && ploadPID == 0 && timeToLive == 0L);
	    byte flags = unordered ? (complete ? (byte)0x11 : (byte)0x10) : (complete ? (byte)0x1 : (byte)0x0);
	    if (optimal){
		flags |= (byte) 0x80;
		if (copy){
		    ByteBuffer buffer = ByteBuffer.allocate (15+len);
		    buffer.putInt (CODE).putInt (7 + len).putInt (sockId);
		    buffer.put (flags).putShort ((short)streamNumber);
		    for (ByteBuffer b : data) buffer.put (b);
		    buffer.flip ();
		    channel.send (buffer, false);
		} else {
		    ByteBuffer buffer = ByteBuffer.allocate (15);
		    buffer.putInt (CODE).putInt (7 + len).putInt (sockId).put (flags).putShort ((short)streamNumber);
		    buffer.flip ();
		    channel.send (prepend (buffer, data), false);
		}
		return;
	    }
	    if (addr == null) addr = "";
	    if (copy){
		ByteBuffer buffer = ByteBuffer.allocate (23+sizeof (addr)+len);
		buffer.putInt (CODE).putInt (buffer.capacity () - 8).putInt (sockId);
		buffer.put (flags).putShort ((short)streamNumber);
		putString (buffer, addr).putInt ((int) ploadPID).putInt ((int) timeToLive); // assume ploadPID and timeToLive fit in an int
		for (ByteBuffer b : data) buffer.put (b);
		buffer.flip ();
		channel.send (buffer, false);
	    } else {
		ByteBuffer buffer = ByteBuffer.allocate (23+sizeof (addr));
		buffer.putInt (CODE).putInt (buffer.capacity () - 8 + len).putInt (sockId).put (flags).putShort ((short)streamNumber);
		putString (buffer, addr).putInt (ploadPID).putInt ((int) timeToLive); // assume timeToLive fit in an int
		buffer.flip ();
		channel.send (prepend (buffer, data), false);
	    }
	}
	public String toString (){
	    return new StringBuilder ().append ("SendSctpSocketDataCommand[").append (_sockId).append ('/').append (_data.remaining ()).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public static class SendSctpSocketOptionsCommand extends MuxCommand {
	public static final int CODE = 75 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	private Map<SctpSocketOption, SctpSocketParam> _options;
	public void run (ExtendedMuxConnection connection){
	    connection.sendSctpSocketOptions (_sockId, _options);
	}
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    _sockId = buffer.getInt ();
	    _options = readSctpOptions (buffer);
	    return true;
	}
	public static void write (TcpChannel channel,
				  int sockId,
				  Map<SctpSocketOption, SctpSocketParam> options){
	    ByteBuffer optionsBuffer = writeSctpOptions (options);
	    if (optionsBuffer == null) return;
	    ByteBuffer buffer = ByteBuffer.allocate (12+optionsBuffer.remaining ());
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putInt (sockId);
	    buffer.put (optionsBuffer);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendSctpSocketOptionsCommand[").append (_sockId).append ('/').append (_options).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SendSctpSocketParamsCommand extends MuxCommand {
	public static final int CODE = 76 | MAGIC_COMMAND_HEADER;
	private int _sockId;
	private Map<String, String> _params;
	public void run (ExtendedMuxConnection connection){
	    connection.sendSctpSocketParams (_sockId, _params);
	}
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    _sockId = buffer.getInt ();
	    _params = getMap (buffer);
	    return true;
	}
	public static void write (TcpChannel channel,
				  int sockId,
				  Map<String, String> params){
	    int paramsSize = sizeof (params);
	    if (paramsSize < 1) return;
	    ByteBuffer buffer = ByteBuffer.allocate (12+paramsSize);
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putInt (sockId);
	    putMap (buffer, params);
	    buffer.flip ();
	    channel.send (buffer, false);
	}
	public String toString (){
	    return new StringBuilder ().append ("SendSctpSocketParamsCommand[").append (_sockId).append ('/').append (_params).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    
    /**************************** MuxEvent ********************/

    public static class ReleaseAckEvent extends MuxEvent {
	public static final int CODE = 100 | MAGIC_EVENT_HEADER;
	private long _sessionId;
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 8) return false;
	    _sessionId = buffer.getLong ();
	    return true;
	}
	public static ByteBuffer makeBuffer (long sessionId){
	    return (ByteBuffer) ByteBuffer.allocate (12).putInt (CODE).putLong (sessionId).flip ();
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){handler.releaseAck (connection, _sessionId);}
	public String toString (){
	    return new StringBuilder ().append ("ReleaseAckEvent[").append (_sessionId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public static class TcpSocketAbortedEvent extends MuxEvent {
	public static final int CODE = 110 | MAGIC_EVENT_HEADER;
	private int _sockId;
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _sockId = buffer.getInt ();
	    return true;
	}
	public static ByteBuffer makeBuffer (int sockId){
	    return (ByteBuffer) ByteBuffer.allocate (12).putInt (CODE).putInt (sockId).flip ();
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){handler.tcpSocketAborted (connection, _sockId);}
	public String toString (){
	    return new StringBuilder ().append ("TcpSocketAbortedEvent[").append (_sockId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public static class TcpSocketClosedEvent extends MuxEvent {
	public static final int CODE = 111 | MAGIC_EVENT_HEADER;
	private int _sockId;
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _sockId = buffer.getInt ();
	    return true;
	}
	public static ByteBuffer makeBuffer (int sockId){
	    return (ByteBuffer) ByteBuffer.allocate (8).putInt (CODE).putInt (sockId).flip ();
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){handler.tcpSocketClosed (connection, _sockId);}
	public String toString (){
	    return new StringBuilder ().append ("TcpSocketClosedEvent[").append (_sockId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public static class TcpSocketConnectedEvent extends MuxEvent {
	public static final int CODE = 112 | MAGIC_EVENT_HEADER;
	private long _connectionId;
	private int _sockId, _remotePort, _localPort, _errno;
	private String _remoteIP, _localIP;
	private byte _clientSocket, _secure;
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    _sockId = buffer.getInt ();
	    _remoteIP = getIPString (buffer);
	    _remotePort = buffer.getShort () & 0xFFFF;
	    _localIP = getIPString (buffer);
	    _localPort = buffer.getShort () & 0xFFFF;
	    _secure = buffer.get ();
	    _clientSocket = buffer.get ();
	    _connectionId = buffer.getLong ();
	    _errno = buffer.getInt ();
	    return true;
	}
	public static ByteBuffer makeBuffer (int sockId, String remoteIP, int remotePort, String localIP, int localPort,
					     boolean secure, boolean clientSocket, long connectionId, int errno){
	    if (localIP == null) localIP = "";
	    if (remoteIP == null) remoteIP = "";
	    ByteBuffer buffer =  ByteBuffer.allocate (34+localIP.length ()+remoteIP.length ());
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putInt (sockId);
	    putString (buffer, remoteIP).putShort ((short)remotePort);
	    putString (buffer, localIP).putShort ((short)localPort);
	    putBoolean (buffer, secure);
	    putBoolean (buffer, clientSocket);
	    return (ByteBuffer) buffer.putLong (connectionId).putInt (errno).flip ();
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    if (parser._ipv6Support){
		handler.tcpSocketConnected (connection, _sockId, _remoteIP, _remotePort, _localIP, _localPort, _localIP, _localPort, _secure == 1, _clientSocket == 1, _connectionId, _errno);
	    } else {
		handler.tcpSocketConnected (connection, _sockId, getIPAsInt (_remoteIP), _remotePort, getIPAsInt (_localIP), _localPort, getIPAsInt (_localIP), _localPort, _secure == 1, _clientSocket == 1, _connectionId, _errno);
	    }
	}
	public String toString (){
	    return new StringBuilder ().append ("TcpSocketConnectedEvent[").append (_connectionId).append ('/').append (_sockId).append ('/').append (_errno).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public static class TcpSocketListeningEvent extends MuxEvent {
	public static final int CODE = 113 | MAGIC_EVENT_HEADER;
	private long _listenId;
	private int _sockId, _localPort, _errno;
	private String _localIP;
	private byte _secure;
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    _sockId = buffer.getInt ();
	    _localIP = getIPString (buffer);
	    _localPort = buffer.getInt ();
	    _secure = buffer.get ();
	    _listenId = buffer.getLong ();
	    _errno = buffer.getInt ();
	    return true;
	}
	public static ByteBuffer makeBuffer (int sockId, String localIP, int localPort, boolean secure, long listenId, int errno){
	    if (localIP == null) localIP = "";
	    ByteBuffer buffer =  ByteBuffer.allocate (31+localIP.length ());
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putInt (sockId);
	    putString (buffer, localIP).putInt (localPort);
	    putBoolean (buffer, secure);
	    return (ByteBuffer) buffer.putLong (listenId).putInt (errno).flip ();
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    handler.tcpSocketListening (connection, _sockId, _localIP, _localPort, _secure == 1, _listenId, _errno);
	}
	public String toString (){
	    return new StringBuilder ().append ("TcpSocketListeningEvent[").append (_listenId).append ('/').append (_sockId).append ('/').append (_localIP).append ('/').append (_localPort).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public static class TcpSocketDataEvent extends MuxEvent {
	public static final int CODE_W_SESSION_ID = 114 | MAGIC_EVENT_HEADER;
	public static final int CODE_WO_SESSION_ID = 115 | MAGIC_EVENT_HEADER;
	private long _sessionId;
	private int _sockId;
	private int _code;
	private TcpSocketDataEvent (int code){ _code = code;}
	public boolean read (ByteBuffer buffer){
	    // for this one, we read in method run to avoid recopy
	    if (buffer.remaining () < 4) return false;
	    int len = buffer.getInt ();
	    if (buffer.remaining () < len){
		buffer.position (buffer.position () - 4);
		return false;
	    }
	    _sockId = buffer.getInt ();
	    if (_code == CODE_W_SESSION_ID){
		_sessionId = buffer.getLong ();
		len -= 12;
	    } else {
		len -= 4;
	    }
	    _data = wrap (buffer, len);
	    return true;
	}
	public static ByteBuffer[] makeBuffer (int sockId, long sessionId, ByteBuffer data){
	    return new ByteBuffer[]{makeBufferHeader (sockId, sessionId, data.remaining ()), data};
	}
	public static ByteBuffer[] makeBuffer (int sockId, long sessionId, ByteBuffer... data){
	    return prepend (makeBufferHeader (sockId, sessionId, remaining (data)), data);
	}
	protected static ByteBuffer makeBufferHeader (int sockId, long sessionId, int len){
	    ByteBuffer buffer =  null;
	    if (sessionId == 0L) {
		buffer = ByteBuffer.allocate (12);
		buffer.putInt (CODE_WO_SESSION_ID).putInt (4 + len).putInt (sockId).flip ();
	    } else {
		buffer = ByteBuffer.allocate (20);
		buffer.putInt (CODE_W_SESSION_ID).putInt (12 + len).putInt (sockId).putLong (sessionId).flip ();
	    }
	    return buffer;
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    if (parser._byteBufferMode){
		handler.tcpSocketData (connection, _sockId, _sessionId, _data);
	    } else {
		handler.tcpSocketData (connection, _sockId, _sessionId, _data.array (), _data.position (), _data.remaining ());
	    }
	}
	public String toString (){
	    return new StringBuilder ().append ("TcpSocketDataEvent[").append (_sockId).append ('/').append (_sessionId).append ('/').append (_data.remaining ()).append (']').toString ();
	}
	public int getCode (){ return _code;}
    }

    public static class UdpSocketClosedEvent extends MuxEvent {
	public static final int CODE = 120 | MAGIC_EVENT_HEADER;
	private int _sockId;
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _sockId = buffer.getInt ();
	    return true;
	}
	public static ByteBuffer makeBuffer (int sockId){
	    return (ByteBuffer) ByteBuffer.allocate (8).putInt (CODE).putInt (sockId).flip ();
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){handler.udpSocketClosed (connection, _sockId);}
	public String toString (){
	    return new StringBuilder ().append ("UdpSocketClosedEvent[").append (_sockId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    
    public static class UdpSocketBoundEvent extends MuxEvent {
	public static final int CODE = 121 | MAGIC_EVENT_HEADER;
	private long _bindId;
	private int _sockId, _localPort, _errno;
	private String _localIP;
	private byte _shared;
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    _sockId = buffer.getInt ();
	    _localIP = getIPString (buffer);
	    _localPort = buffer.getInt ();
	    _bindId = buffer.getLong ();
	    _shared = buffer.get ();
	    _errno = buffer.getInt ();
	    return true;
	}
	public static ByteBuffer makeBuffer (int sockId, String localIP, int localPort, boolean shared, long bindId, int errno){
	    if (localIP == null) localIP = "";
	    ByteBuffer buffer =  ByteBuffer.allocate (31+localIP.length ());
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putInt (sockId);
	    putString (buffer, localIP).putInt (localPort);
	    return (ByteBuffer) buffer.putLong (bindId).put (shared ? (byte)1 : (byte)0).putInt (errno).flip ();
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    if (parser._ipv6Support)
		handler.udpSocketBound (connection, _sockId, _localIP, _localPort, _shared == 1, _bindId, _errno);
	    else
		handler.udpSocketBound (connection, _sockId, getIPAsInt (_localIP), _localPort, _shared == 1, _bindId, _errno);
	}
	public String toString (){
	    return new StringBuilder ().append ("UdpSocketBoundEvent[").append (_bindId).append ('/').append (_sockId).append ('/').append (_localIP).append ('/').append (_localPort).append ('/').append (_errno).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public static class UdpSocketDataEvent extends MuxEvent {
	public static final int CODE_W_SESSION_ID = 122 | MAGIC_EVENT_HEADER;
	public static final int CODE_WO_SESSION_ID = 123 | MAGIC_EVENT_HEADER;
	private long _sessionId;
	private int _sockId;
	private String _remoteIP;
	private int _remotePort;
	private int _code;
	private UdpSocketDataEvent (int code){ _code = code;}
	public boolean read (ByteBuffer buffer){
	    // for this one, we read in method run to avoid recopy
	    if (buffer.remaining () < 4) return false;
	    int len = buffer.getInt ();
	    if (buffer.remaining () < len){
		buffer.position (buffer.position () - 4);
		return false;
	    }
	    _sockId = buffer.getInt ();
	    if (_code == CODE_W_SESSION_ID) _sessionId = buffer.getLong ();
	    _remoteIP = getIPString (buffer);
	    _remotePort = buffer.getShort () & 0xFFFF;
	    len = buffer.getInt ();
	    _data = wrap (buffer, len);
	    return true;
	}
	public static ByteBuffer[] makeBuffer (int sockId, long sessionId, String remoteIP, int remotePort, ByteBuffer data){
	    return new ByteBuffer[]{makeBufferHeader (sockId, sessionId, remoteIP, remotePort, data.remaining ()), data};
	}
	public static ByteBuffer[] makeBuffer (int sockId, long sessionId, String remoteIP, int remotePort, ByteBuffer... data){
	    return prepend (makeBufferHeader (sockId, sessionId, remoteIP, remotePort, remaining (data)), data);
	}
	protected static ByteBuffer makeBufferHeader (int sockId, long sessionId, String remoteIP, int remotePort, int len){
	    // remoteIP cannot be null
	    ByteBuffer buffer = null;
	    if (sessionId == 0L){
		buffer = ByteBuffer.allocate (20+remoteIP.length ());
		buffer.putInt (CODE_WO_SESSION_ID).putInt (buffer.capacity () - 8 + len).putInt (sockId);
	    } else {
		buffer = ByteBuffer.allocate (28+remoteIP.length ());
		buffer.putInt (CODE_W_SESSION_ID).putInt (buffer.capacity () - 8 + len).putInt (sockId).putLong (sessionId);
	    }
	    putString (buffer, remoteIP).putShort ((short)remotePort).putInt (len).flip ();
	    return buffer;
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    if (parser._byteBufferMode){
		if (parser._ipv6Support)
		    handler.udpSocketData (connection, _sockId, _sessionId, _remoteIP, _remotePort, _remoteIP, _remotePort, _data);
		else {
		    int remoteIP = getIPAsInt (_remoteIP);
		    handler.udpSocketData (connection, _sockId, _sessionId, remoteIP, _remotePort, remoteIP, _remotePort, _data);
		}
	    } else {
		if (parser._ipv6Support)
		    handler.udpSocketData (connection, _sockId, _sessionId, _remoteIP, _remotePort, _remoteIP, _remotePort, _data.array (), _data.position (), _data.remaining ());
		else {
		    int remoteIP = getIPAsInt (_remoteIP);
		    handler.udpSocketData (connection, _sockId, _sessionId, remoteIP, _remotePort, remoteIP, _remotePort, _data.array (), _data.position (), _data.remaining ());
		}
	    }
	}
	public String toString (){
	    return new StringBuilder ().append ("UdpSocketDataEvent[").append (_sockId).append ('/').append (_remoteIP).append ('/').append (_remotePort).append ('/').append (_data.remaining ()).append (']').toString ();
	}
	public int getCode (){ return _code;}
    }

    public static class MuxDataEvent extends MuxEvent {
	public static final int CODE = 130 | MAGIC_EVENT_HEADER;
	protected MuxHeader _header;
    	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    int len = buffer.getInt ();
	    if (buffer.remaining () < len){
		buffer.position (buffer.position () - 4);
		return false;
	    }
	    _header = getMuxHeader (buffer);
	    len -= getMuxHeaderLength (_header);
	    _data = wrap (buffer, len);
	    return true;
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    if (parser._byteBufferMode){
		handler.muxData (connection, _header, _data);
	    } else {
		handler.muxData (connection, _header, _data.array (), _data.position (), _data.remaining ());
	    }
	}
	public static ByteBuffer[] makeBuffer (MuxHeader header, ByteBuffer data){
	    return makeBuffer (header, data, CODE);
	}
	public static ByteBuffer[] makeBuffer (MuxHeader header, ByteBuffer data, int code){
	    int hlen = getMuxHeaderLength (header);
	    ByteBuffer buffer =  ByteBuffer.allocate (8+hlen);
	    buffer.putInt (code).putInt (hlen + (data != null ? data.remaining () : 0));
	    putMuxHeader (buffer, header);
	    buffer.flip ();
	    if (data == null || data.remaining () == 0)
		return new ByteBuffer[] {buffer};
	    else
		return new ByteBuffer[]{buffer, data};
	}
	public String toString (){
	    return new StringBuilder ().append ("MuxDataEvent[").append (_header).append ("/len=").append (_data.remaining ()).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public static class MuxStartEvent extends MuxEvent {
	public static final MuxStartEvent INSTANCE = new MuxStartEvent ();
	public static final int CODE = 131 | MAGIC_EVENT_HEADER;
	private static final byte[] PAYLOAD = makeIntArray (CODE);
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    connection.opened (true);
	}
	public static ByteBuffer makeBuffer (){
	    return ByteBuffer.wrap (PAYLOAD);
	}
	public String toString (){
	    return "MuxStartEvent";
	}
	public int getCode (){ return CODE;}
    }

    public static class InternalDataEvent extends MuxDataEvent {
	public static final int CODE = 140 | MAGIC_EVENT_HEADER;
	@Override
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    handler.internalMuxData (connection, _header, _data);
	}
	public static ByteBuffer[] makeBuffer (MuxHeader header, ByteBuffer data){
	    return makeBuffer (header, data, CODE);
	}
	public String toString (){
	    return new StringBuilder ().append ("InternalDataEvent[").append (_header).append ("/len=").append (_data.remaining ()).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    
    public static class SctpSocketClosedEvent extends MuxEvent {
	public static final int CODE = 150 | MAGIC_EVENT_HEADER;
	private int _sockId;
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    _sockId = buffer.getInt ();
	    return true;
	}
	public static ByteBuffer makeBuffer (int sockId){
	    return (ByteBuffer) ByteBuffer.allocate (8).putInt (CODE).putInt (sockId).flip ();
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){handler.sctpSocketClosed (connection, _sockId);}
	public String toString (){
	    return new StringBuilder ().append ("SctpSocketClosedEvent[").append (_sockId).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public static class SctpSocketConnectedEvent extends MuxEvent {
	public static final int CODE = 151 | MAGIC_EVENT_HEADER;
	private long _connectionId;
	private int _sockId, _remotePort, _localPort, _errno, _maxOutStreams, _maxInStreams;
	private String[] _remoteIPs, _localIPs;
	private byte _clientSocket, _secure;
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    _sockId = buffer.getInt ();
	    _connectionId = buffer.getLong ();
	    _remoteIPs = getStrings (buffer, true);
	    _remotePort = buffer.getShort () & 0xFFFF;
	    _localIPs = getStrings (buffer, true);
	    _localPort = buffer.getShort () & 0xFFFF;
	    _maxOutStreams = buffer.getShort () & 0xFFFF;
	    _maxInStreams = buffer.getShort () & 0xFFFF;
	    _clientSocket = buffer.get ();
	    _secure = buffer.get ();
	    _errno = buffer.getInt ();
	    return true;
	}
	public static ByteBuffer makeBuffer (int sockId,
					     long connectionId,
					     String[] remoteAddrs,
					     int remotePort,
					     String[] localAddrs,
					     int localPort,
					     int maxOutStreams,
					     int maxInStreams,
					     boolean fromClient,
					     boolean secure,
					     int errno){
	    ByteBuffer buffer =  ByteBuffer.allocate (34+sizeof (remoteAddrs)+sizeof (localAddrs));
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putInt (sockId);
	    buffer.putLong (connectionId);
	    putStrings (buffer, remoteAddrs).putShort ((short) remotePort);
	    putStrings (buffer, localAddrs).putShort ((short) localPort);
	    buffer.putShort ((short) maxOutStreams).putShort ((short) maxInStreams);
	    putBoolean (buffer, fromClient);
	    putBoolean (buffer, secure);
	    buffer.putInt (errno);
	    buffer.flip ();
	    return buffer;
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    handler.sctpSocketConnected (connection, _sockId, _connectionId, _remoteIPs, _remotePort, _localIPs, _localPort, _maxOutStreams, _maxInStreams, _clientSocket == 1, _secure == 1, _errno);
	}
	public String toString (){
	    return new StringBuilder ().append ("SctpSocketConnectedEvent[").append (_connectionId).append ('/').append (_sockId).append ('/').append (_errno).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public static class SctpSocketListeningEvent extends MuxEvent {
	public static final int CODE = 152 | MAGIC_EVENT_HEADER;
	private long _listenId;
	private int _sockId, _localPort, _errno;
	private String[] _localIPs;
	private byte _secure;
	public boolean read (ByteBuffer buffer){
	    if (!checkBufferSize (buffer)) return false;
	    _sockId = buffer.getInt ();
	    _listenId = buffer.getLong ();
	    _localIPs = getStrings (buffer, true);
	    _localPort = buffer.getInt ();
	    _secure = buffer.get ();
	    _errno = buffer.getInt ();
	    return true;
	}
	public static ByteBuffer makeBuffer (int sockId,
					     long listenerId,
					     String[] localAddrs,
					     int localPort,
					     boolean secure,
					     int errno){
	    ByteBuffer buffer =  ByteBuffer.allocate (29+sizeof (localAddrs));
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putInt (sockId);
	    buffer.putLong (listenerId);
	    putStrings (buffer, localAddrs).putInt (localPort);
	    putBoolean (buffer, secure);
	    buffer.putInt (errno).flip ();
	    return buffer;
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    handler.sctpSocketListening (connection, _sockId, _listenId, _localIPs, _localPort, _secure == 1, _errno);
	}
	public String toString (){
	    return new StringBuilder ().append ("SctpSocketListeningEvent[").append (_listenId).append ('/').append (_sockId).append ('/').append (_localIPs[0]).append ('/').append (_localPort).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }

    public static class SctpSocketDataEvent extends MuxEvent {
	public static final int CODE_W_SESSION_ID = 153 | MAGIC_EVENT_HEADER;
	public static final int CODE_WO_SESSION_ID = 154 | MAGIC_EVENT_HEADER;
	private long _sessionId;
	private int _sockId, _ploadPID, _streamNb;
	private int _code;
	private boolean _unordered, _complete;
	private String _addr;
	private SctpSocketDataEvent (int code){ _code = code;}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    int len = buffer.getInt ();
	    if (buffer.remaining () < len){
		buffer.position (buffer.position () - 4);
		return false;
	    }
	    int pos = buffer.position ();
	    _sockId = buffer.getInt ();
	    _addr = getIPString (buffer);
	    byte flags = buffer.get ();
	    _unordered = (flags & (byte)0x10) == (byte)0x10;
	    _complete = (flags & (byte)0x1) == (byte)0x1;
	    _ploadPID = buffer.getShort () & 0xFFFF;
	    _streamNb = buffer.getShort () & 0xFFFF;
	    if (_code == CODE_W_SESSION_ID)
		_sessionId = buffer.getLong ();
	    len -= buffer.position () - pos;
	    _data = wrap (buffer, len);
	    return true;
	}
	public static ByteBuffer[] makeBuffer (int sockId, long sessionId, ByteBuffer data, String addr, boolean isUnordered, boolean isComplete, int ploadPID, int streamNumber){
	    return new ByteBuffer[]{makeBufferHeader (sockId, sessionId, data.remaining (), addr, isUnordered, isComplete, ploadPID, streamNumber), data};
	}
	public static ByteBuffer[] makeBuffer (int sockId, long sessionId, ByteBuffer[] data, String addr, boolean isUnordered, boolean isComplete, int ploadPID, int streamNumber){
	    return prepend (makeBufferHeader (sockId, sessionId, remaining (data), addr, isUnordered, isComplete, ploadPID, streamNumber), data);
	}
	public static ByteBuffer makeBufferHeader (int sockId, long sessionId, int len, String addr, boolean isUnordered, boolean isComplete, int ploadPID, int streamNumber){
	    if (addr == null) addr = "";
	    ByteBuffer buffer =  null;
	    byte flags = isUnordered ? (isComplete ? (byte)0x11 : (byte)0x10) : (isComplete ? (byte)0x1 : (byte)0x0);
	    if (sessionId == 0L) {
		buffer = ByteBuffer.allocate (17+sizeof (addr));
		buffer.putInt (CODE_WO_SESSION_ID).putInt (buffer.capacity () - 8 + len).putInt (sockId);
		putString (buffer, addr).put (flags).putShort ((short)ploadPID).putShort ((short)streamNumber);
	    } else {
		buffer = ByteBuffer.allocate (25+sizeof (addr));
		buffer.putInt (CODE_W_SESSION_ID).putInt (buffer.capacity () - 8 + len).putInt (sockId);
		putString (buffer, addr).put (flags).putShort ((short)ploadPID).putShort ((short)streamNumber).putLong (sessionId);
	    }
	    buffer.flip ();
	    return buffer;
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    handler.sctpSocketData (connection, _sockId, _sessionId, _data, _addr, _unordered, _complete, _ploadPID, _streamNb);
	}
	public String toString (){
	    return new StringBuilder ().append ("SctpSocketDataEvent[").append (_sockId).append ('/').append (_sessionId).append ('/').append (_data.remaining ()).append (']').toString ();
	}
	public int getCode (){ return _code;}
    }
    public static class SctpSocketSendFailedEvent extends MuxEvent {
	public static final int CODE = 155;
	private int _sockId, _errCode, _streamNb;
	private String _addr;
	private SctpSocketSendFailedEvent (){}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    int len = buffer.getInt ();
	    if (buffer.remaining () < len){
		buffer.position (buffer.position () - 4);
		return false;
	    }
	    int pos = buffer.position ();
	    _sockId = buffer.getInt ();
	    _addr = getIPString (buffer);
	    _streamNb = buffer.getShort () & 0xFFFF;
	    _errCode = buffer.getInt ();
	    len -= buffer.position () - pos;
	    _data = wrap (buffer, len);
	    return true;
	}
	public static ByteBuffer[] makeBuffer (int sockId, String addr, int streamNumber, ByteBuffer data, int errCode){
	    return new ByteBuffer[]{makeBufferHeader (sockId, data.remaining (), addr, streamNumber, errCode), data};
	}
	public static ByteBuffer makeBufferHeader (int sockId, int len, String addr, int streamNumber, int errCode){
	    if (addr == null) addr = "";
	    ByteBuffer buffer = ByteBuffer.allocate (17+sizeof (addr));
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8 + len).putInt (sockId);
	    putString (buffer, addr).putShort ((short)streamNumber).putInt (errCode);
	    buffer.flip ();
	    return buffer;
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    handler.sctpSocketSendFailed (connection, _sockId, _addr, _streamNb, _data, _errCode);
	}
	public String toString (){
	    return new StringBuilder ().append ("SctpSocketSendFailedEvent[").append (_sockId).append ('/').append (_errCode).append ('/').append (_data.remaining ()).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    public static class SctpPeerAddressChangedEvent extends MuxEvent {
	public static final int CODE = 156;
	private int _sockId, _port, _event;
	private String _addr;
	private SctpPeerAddressChangedEvent (){}
	public boolean read (ByteBuffer buffer){
	    if (buffer.remaining () < 4) return false;
	    int len = buffer.getInt ();
	    if (buffer.remaining () < len){
		buffer.position (buffer.position () - 4);
		return false;
	    }
	    _sockId = buffer.getInt ();
	    _addr = getIPString (buffer);
	    _port = buffer.getShort () & 0xFFFF;
	    _event = buffer.getShort () & 0xFFFF;
	    return true;
	}
	public static ByteBuffer makeBuffer (int sockId, String addr, int port, int event){
	    // addr cannot be null
	    ByteBuffer buffer = ByteBuffer.allocate (16+sizeof (addr));
	    buffer.putInt (CODE).putInt (buffer.capacity () - 8).putInt (sockId);
	    putString (buffer, addr).putShort ((short)port).putShort ((short)event);
	    buffer.flip ();
	    return buffer;
	}
	public void run (MuxParser parser, ExtendedMuxHandler handler, ExtendedMuxConnection connection){
	    MuxHandler.SctpAddressEvent event = null;
	    switch (_event){
	    case 0 : event = MuxHandler.SctpAddressEvent.ADDR_ADDED; break;
	    case 1 : event = MuxHandler.SctpAddressEvent.ADDR_AVAILABLE; break;
	    case 2 : event = MuxHandler.SctpAddressEvent.ADDR_CONFIRMED; break;
	    case 3 : event = MuxHandler.SctpAddressEvent.ADDR_MADE_PRIMARY; break;
	    case 4 : event = MuxHandler.SctpAddressEvent.ADDR_REMOVED; break;
	    case 5 : event = MuxHandler.SctpAddressEvent.ADDR_UNREACHABLE; break;
	    }
	    handler.sctpPeerAddressChanged (connection, _sockId, _addr, _port, event);
	}
	public String toString (){
	    return new StringBuilder ().append ("SctpPeerAddressChangedEvent[").append (_sockId).append ('/').append (_addr).append ('/').append (_event).append (']').toString ();
	}
	public int getCode (){ return CODE;}
    }
    
    /******************************* Utilities ****************************/

    private static final ByteBuffer VOID = ByteBuffer.allocate (0);

    private static ByteBuffer wrap (ByteBuffer buffer, int len){
	if (len == 0) return VOID;
	int newPos = buffer.position () + len;
	try{
	    ByteBuffer newBuffer = buffer.duplicate ();
	    newBuffer.limit (newPos);
	    return newBuffer;
	}finally{
	    buffer.position (newPos);
	}
    }

    private static final byte[] makeIntArray (int... values){
	byte[] bytes = new byte[values.length * 4];
	ByteBuffer buff = ByteBuffer.wrap (bytes);
	for (int i : values)
	    buff.putInt (i);
	return bytes;
    }

    public static ByteBuffer putBoolean (ByteBuffer buffer, boolean value){
	if (value) return buffer.put ((byte)1);
	return buffer.put ((byte)0);
    }
    public static int sizeof (String s){
	return 2 + s.length ();
    }
    public static ByteBuffer putString (ByteBuffer buffer, String value){
	int size = value.length ();
	buffer.putShort ((short)size);
	for (int i=0; i<size; i++)
	    buffer.put ((byte)value.charAt (i));
	return buffer;
    }
    public static int sizeof (String[] values){
	if (values == null) return 1;
	int res = 1;
	for (String value : values) res += sizeof (value);
	return res;
    }
    public static ByteBuffer putStrings (ByteBuffer buffer, String[] values){
	if (values == null){
	    buffer.put ((byte)0);
	    return buffer;
	}
	buffer.put ((byte)values.length);
	for (String value : values) // assume cannot be null
	    putString (buffer, value);
	return buffer;
    }
    public static String getString (ByteBuffer buffer){
	return getString (buffer, false);
    }
    public static String[] getStrings (ByteBuffer buffer, boolean ip){
	String[] values = new String[buffer.get () & 0xFF];
	for (int i=0; i<values.length; i++)
	    values[i] = getString (buffer, ip);
	return values;
    }
    public static String getIPString (ByteBuffer buffer){
	 return getString (buffer, true);
    }
    private static String getString (ByteBuffer buffer, boolean ip){
	int size = buffer.getShort () & 0xFFFF;
	if (size == 0) return ip ? null : "";
	StringBuilder sb = new StringBuilder (size);
	int x = buffer.get () & 0xFF;
	if (!ip || x != 0x2F) sb.append ((char)x); // skip leading / (in IP address)
	for (int i=1; i<size; i++){
	    x = buffer.get () & 0xFF;
	    sb.append ((char)x);
	}
	return sb.toString ();
    }
    private static int getIPAsInt (String ip){
	// handle null
	if (ip == null) return 0;
	return MuxUtils.getIPAsInt (ip);
    }
    public static int remaining (ByteBuffer... buffs){
	int n = 0;
	for (ByteBuffer b : buffs) n += b.remaining ();
	return n;
    }
    public static ByteBuffer[] prepend (ByteBuffer prefix, ByteBuffer... buffs){
	ByteBuffer[] res = new ByteBuffer[1 + buffs.length];
	res[0] = prefix;
	System.arraycopy (buffs, 0, res, 1, buffs.length);
	return res;
    }

    public static int getMuxHeaderLength (MuxHeader header){
	switch (header.getVersion ()){
	case 0:
	case 3:
	case 6:
	    return 17;
	case 2:
	    return 23;
	case 4:
	case 5:
	    return 5;
	}
	return -1; // EXCEPTION !
    }
    public static void putMuxHeader (ByteBuffer buffer, MuxHeader header){
	buffer.put ((byte) header.getVersion ());
	switch (header.getVersion ()){
	case 0:
	case 3:
	case 6:
	    buffer.putLong (header.getSessionId ()).putInt (header.getChannelId ()).putInt (header.getFlags ());
	    return;
	case 2:
	    MuxHeaderV2 v2 = (MuxHeaderV2) header;
	    buffer.putLong (header.getSessionId ()).putInt (header.getChannelId ()).putInt (header.getFlags ());
	    buffer.putInt (v2.getRemoteIP ()).putShort ((short) v2.getRemotePort ());
	    return;
	case 4:
	case 5:
	    buffer.put ((byte) header.getFlags ());
	    return;
	}
    }
    public static MuxHeader getMuxHeader (ByteBuffer buffer){
	switch (buffer.get () & 0xFF){
	case 0:
	    MuxHeaderV0 v0 = new MuxHeaderV0 ();
	    v0.set (buffer.getLong (), buffer.getInt (), buffer.getInt ());
	    return v0;
	case 2:
	    MuxHeaderV2 v2 = new MuxHeaderV2 ();
	    v2.set (buffer.getLong (), buffer.getInt (), buffer.getInt (), buffer.getInt (), buffer.getShort () & 0xFFFF, 0, 0);
	    return v2;
	case 3:
	    MuxHeaderV3 v3 = new MuxHeaderV3 ();
	    v3.set (buffer.getLong (), buffer.getInt (), buffer.getInt ());
	    return v3;
	case 4:
	    MuxHeaderV4 v4 = new MuxHeaderV4 ();
	    v4.set (0, 0, buffer.get () & 0xFF);
	    return v4;
	case 5:
	    MuxHeaderV5 v5 = new MuxHeaderV5 ();
	    v5.set (buffer.get () & 0xFF);
	    return v5;
	case 6:
	    MuxHeaderV6 v6 = new MuxHeaderV6 ();
	    v6.set (buffer.getLong (), buffer.getInt (), buffer.getInt ());
	    return v6;
	}
	return null;
    }
    public static ByteBuffer writeSctpOptions (Map<SctpSocketOption, SctpSocketParam> options){
	if (options == null || options.size () == 0) return null;
	ByteArrayOutputStream baos = new ByteArrayOutputStream (128);
	baos.write ((byte) options.size ()); // assume less than 128 options !
	try{
	    ObjectOutputStream oos = new ObjectOutputStream (baos);
	    for (SctpSocketOption opt : options.keySet ()){
		oos.writeUTF (opt.name ());
		oos.writeObject (options.get (opt));
	    }
	    oos.close ();
	    baos.close ();
	}catch(IOException e){}   // cannot happen with ByteArrayOutputStream
	ByteBuffer buffer = ByteBuffer.wrap (baos.toByteArray ()); // returned buffer in read mode
	return buffer;
    }
    public static Map<SctpSocketOption, SctpSocketParam> readSctpOptions (final ByteBuffer buffer){
	Map<SctpSocketOption, SctpSocketParam> options = new java.util.HashMap<> (); 
	int size = buffer.get () & 0xFF;
	try{
	    InputStream in = new InputStream (){
		    public int read (){ return buffer.get () & 0xFF;}
		};
	    ObjectInputStream ois = new com.alcatel.as.util.cl.ObjectInputStreamWithClassLoader(in, MuxParser.class.getClassLoader());
	    for (int i = 0; i<size ; i++){
		String name = ois.readUTF ();
		SctpSocketOption opt = SctpSocketOption.valueOf (name);
		SctpSocketParam param = (SctpSocketParam) ois.readObject ();
		options.put (opt, param);
	    }
	    ois.close ();
	}catch(Exception e){} // cannot happen here
	return options;
    }
    public static int sizeof (Map<String, String> map){
	if (map == null || map.size () == 0) return 0;
	int i = 1;
	for (String s : map.keySet ()){
	    i += sizeof (s);
	    i += sizeof (map.get (s));
	}
	return i;
    }
    public static ByteBuffer putMap (ByteBuffer buffer, Map<String, String> map){
	buffer.put ((byte) map.size ()); // assume less than 128 entries !
	for (String s : map.keySet ()){
	    putString (buffer, s);
	    putString (buffer, map.get (s));
	}
	return buffer;
    }
    public static Map<String, String> getMap (ByteBuffer buffer){
	Map<String, String> map = new java.util.HashMap<> ();
	int size = buffer.get () & 0xFF;
	for (int i = 0; i<size ; i++)
	    map.put (getString (buffer), getString (buffer));
	return map;
    }
}
