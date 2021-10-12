// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl.ioh;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.TcpChannel;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.util.MuxUtils;

public class StackSideMuxHandler extends ExtendedMuxHandler {

    protected TcpChannel _channel;
    protected Logger _logger;
    public StackSideMuxHandler (TcpChannel channel, Logger logger){
	_channel = channel;
	_logger = logger;
	sendMuxVersion ();
    }
    public TcpChannel getChannel (){ return _channel;}
    public void setLogger (Logger logger){ _logger=logger;}
    public int[] getCounters(){return null;}
    public int getMajorVersion(){return -1;}
    public int getMinorVersion(){return -1;}
    public void commandEvent(int command,
			     int[] intParams,
			     java.lang.String[] strParams){}

    public void sendMuxVersion (){
	MuxParser.sendMuxVersionEvent (_channel);
    }

    public void sendMuxPingAck (){
	MuxParser.sendMuxPingAckEvent (_channel);
    }

    public void muxOpened (MuxConnection connection){
	_channel.send (MuxParser.MuxStartEvent.makeBuffer (), false);
    }

    public void tcpSocketListening(MuxConnection connection,
				   int sockId,
				   int localIP,
				   int localPort,
				   boolean secure,
				   long listenId,
				   int errno){
	tcpSocketListening (connection, sockId, MuxUtils.getIPAsString (localIP), localPort, secure, listenId, errno);
    }
    public void tcpSocketListening(MuxConnection connection,
				   int sockId,
				   java.lang.String localIP,
				   int localPort,
				   boolean secure,
				   long listenId,
				   int errno){
	_channel.send (MuxParser.TcpSocketListeningEvent.makeBuffer (sockId, localIP, localPort, secure, listenId, errno), false);
    }
    public void tcpSocketConnected(MuxConnection connection,
				   int sockId,
				   int remoteIP,
				   int remotePort,
				   int localIP,
				   int localPort,
				   int virtualIP,
				   int virtualPort,
				   boolean secure,
				   boolean clientSocket,
				   long connectionId,
				   int errno){
	tcpSocketConnected (connection, sockId, MuxUtils.getIPAsString (remoteIP), remotePort, MuxUtils.getIPAsString (localIP), localPort, null, virtualPort, secure, clientSocket, connectionId, errno);
    }
    public void tcpSocketConnected(MuxConnection connection,
				   int sockId,
				   java.lang.String remoteIP,
				   int remotePort,
				   java.lang.String localIP,
				   int localPort,
				   java.lang.String virtualIP,
				   int virtualPort,
				   boolean secure,
				   boolean clientSocket,
				   long connectionId,
				   int errno){
	_channel.send (MuxParser.TcpSocketConnectedEvent.makeBuffer (sockId, remoteIP, remotePort, localIP, localPort, secure, clientSocket, connectionId, errno), false);
    }
    public void tcpSocketClosed(MuxConnection connection,
				int sockId){
	_channel.send (MuxParser.TcpSocketClosedEvent.makeBuffer (sockId), false);
    }
    public void tcpSocketAborted(MuxConnection connection,
				 int sockId){
	_channel.send (MuxParser.TcpSocketAbortedEvent.makeBuffer (sockId), false);
    }
    public void tcpSocketData(MuxConnection connection,
			      int sockId,
			      long sessionId,
			      byte[] data,
			      int off,
			      int len){
	tcpSocketData (connection, sockId, sessionId, ByteBuffer.wrap (data, off, len));
    }
    public void tcpSocketData(MuxConnection connection,
			      int sockId,
			      long sessionId,
			      java.nio.ByteBuffer data){
	_channel.send (MuxParser.TcpSocketDataEvent.makeBuffer (sockId, sessionId, data), false);
    }
    public void tcpSocketData(MuxConnection connection,
			      int sockId,
			      long sessionId,
			      java.nio.ByteBuffer[] data){
	_channel.send (MuxParser.TcpSocketDataEvent.makeBuffer (sockId, sessionId, data), false);
    }
    public void udpSocketBound(MuxConnection connection,
			       int sockId,
			       int localIP,
			       int localPort,
			       boolean shared,
			       long bindId,
			       int errno){
	udpSocketBound (connection, sockId, MuxUtils.getIPAsString (localIP), localPort, shared, bindId, errno);
    }
    public void udpSocketBound(MuxConnection connection,
			       int sockId,
			       String localIP,
			       int localPort,
			       boolean shared,
			       long bindId,
			       int errno){
	_channel.send (MuxParser.UdpSocketBoundEvent.makeBuffer (sockId, localIP, localPort, shared, bindId, errno), false);
    }
    public void udpSocketClosed (MuxConnection connection, int sockId){
	_channel.send (MuxParser.UdpSocketClosedEvent.makeBuffer (sockId), false);
    }
    public void udpSocketData(MuxConnection connection,
			      int sockId,
			      long sessionId,
			      String remoteIP,
			      int remotePort,
			      String virtualIP,
			      int virtualPort,
			      ByteBuffer buff){
	_channel.send (MuxParser.UdpSocketDataEvent.makeBuffer (sockId, sessionId, remoteIP, remotePort, buff), false);
    }
    public void udpSocketData(MuxConnection connection,
			      int sockId,
			      long sessionId,
			      String remoteIP,
			      int remotePort,
			      String virtualIP,
			      int virtualPort,
			      ByteBuffer[] buff){
	_channel.send (MuxParser.UdpSocketDataEvent.makeBuffer (sockId, sessionId, remoteIP, remotePort, buff), false);
    }


    public void muxData(MuxConnection connection, MuxHeader header, byte[] data, int off, int len){
	if (len > 0)
	    muxData (connection, header, ByteBuffer.wrap (data, off, len));
	else
	    muxData (connection, header, null);
    }

    public void muxData(MuxConnection connection, MuxHeader header, ByteBuffer data){
	_channel.send (MuxParser.MuxDataEvent.makeBuffer (header, data), false);
    }
    
    public void releaseAck(MuxConnection connection, long sessionId){
	_channel.send (MuxParser.ReleaseAckEvent.makeBuffer (sessionId), false);
    }

    public void internalMuxData (MuxConnection connection, MuxHeader header, ByteBuffer data){
	_channel.send (MuxParser.InternalDataEvent.makeBuffer (header, data), false);
    }

    public void sctpSocketListening(MuxConnection connexion, int sockId, long listenerId, String[] localAddrs,
                                    int localPort, boolean secure, int errno){
	_channel.send (MuxParser.SctpSocketListeningEvent.makeBuffer (sockId, listenerId, localAddrs, localPort, secure, errno), false);
    }

    public void sctpSocketConnected(MuxConnection connection, int sockId, long connectionId, String[] remoteAddrs,
                                    int remotePort, String[] localAddrs, int localPort, int maxOutStreams,
                                    int maxInStreams, boolean fromClient, boolean secure, int errno){
	_channel.send (MuxParser.SctpSocketConnectedEvent.makeBuffer (sockId, connectionId, remoteAddrs, remotePort, localAddrs, localPort, maxOutStreams, maxInStreams, fromClient, secure, errno), false);
    }

    public void sctpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer data, String addr,
                               boolean isUnordered, boolean isComplete, int ploadPID, int streamNumber){
	_channel.send (MuxParser.SctpSocketDataEvent.makeBuffer (sockId, sessionId, data, addr, isUnordered, isComplete, ploadPID, streamNumber), false);
    }

    public void sctpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer[] data, String addr,
                               boolean isUnordered, boolean isComplete, int ploadPID, int streamNumber){
	_channel.send (MuxParser.SctpSocketDataEvent.makeBuffer (sockId, sessionId, data, addr, isUnordered, isComplete, ploadPID, streamNumber), false);
    }

    public void sctpSocketClosed(MuxConnection connection, int sockId){
	_channel.send (MuxParser.SctpSocketClosedEvent.makeBuffer (sockId), false);
    }

    public void sctpSocketSendFailed(MuxConnection connection, int sockId, String addr, int streamNumber,
                                     ByteBuffer buf, int errcode){
	_channel.send (MuxParser.SctpSocketSendFailedEvent.makeBuffer (sockId, addr, streamNumber, buf, errcode), false);
    }
    
    public void sctpPeerAddressChanged(MuxConnection cnx, int sockId, String addr, int port, SctpAddressEvent event) {
	_channel.send (MuxParser.SctpPeerAddressChangedEvent.makeBuffer (sockId, addr, port, event.ordinal ()), false);
    }
}
