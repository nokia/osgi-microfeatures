// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2;

import java.nio.ByteBuffer;
import com.alcatel.as.http2.frames.*;
import com.alcatel.as.http2.hpack.*;
import com.alcatel.as.http2.headers.*;
import com.alcatel.as.http2.StreamStateMachine.State;
import com.alcatel.as.http2.StreamStateMachine.Event;
import org.apache.log4j.Logger;
import com.alcatel.as.service.concurrent.*;
import java.util.concurrent.Executor;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import com.alcatel.as.http.parser.AccessLog;
import com.alcatel.as.http.parser.HttpMeters;
import com.alcatel.as.http2.client.*;

public class SendReqStream extends Stream implements Http2Request, HPACKDecoder.DecoderSouth {

    protected Http2ResponseListener _respListener;
    protected HPACKDecoder _hpackDecoder;
    protected AccessLog _accessLog = new AccessLog ().version (2);
    protected Http2ClientImpl.Http2ConnectionImpl _clientConnection;
    protected int _sendFCWindowInitial;
    protected long _respContentLen = -1L;
    protected long _respDataReceived = 0L;
    
    protected SendReqStream (Http2ClientImpl.Http2ConnectionImpl clientConnection, Connection connection, int id){
	super (connection, id, SendReqStateMachine.INSTANCE);
	_clientConnection = clientConnection;
    }
    public String toString (){
	return new StringBuilder ().append ("SendReqStream[").append (_id).append (',').append (_state).append (']').toString ();
    }
    protected SendReqStream init (Http2ResponseListener listener){
	_sendFCWindowInitial = (int) _connection.remoteSettings ().INITIAL_WINDOW_SIZE;
	_sendFCWindow = _sendFCWindowInitial;
	_respListener = listener;
	_hpackDecoder = _connection.hpackDecoder ();
	return this;
    }
    protected int initialSendFCWindow (){
	return _sendFCWindowInitial;
    }
    protected long respDataPending (){
	if (_respContentLen == -1L) return Long.MAX_VALUE;
	return _respContentLen - _respDataReceived; // can be negative ...
    }
    
    /*************** State Machine callbacks ***********/

    private boolean _inTrailers;

    // in STATE_OPEN
    protected void recvHeaders (ByteBuffer buffer){
	_hpackDecoder.decode (buffer, this);
    }
    //  in STATE_OPEN
    protected void recvHeaders (){
	_hpackDecoder.last_or_end_or_finish_or_stop_or_complete (this);
	_inTrailers = true;
    }
    protected boolean hasRecvHeaders (){ return _inTrailers;}
    // in STATE_OPEN
    protected void recvData (ByteBuffer data){
	_respDataReceived += data.remaining ();
	_respListener.recvRespData (this, data, recvEndStream ());
    }
    protected void recvResponse (){
	_respListener.endResponse (this);
    }
    // when entering STATE_CLOSED
    @Override
    protected void closed (){
	super.closed ();
	if (closedEvent () != Event.RecvES &&
	    closedEvent () != Event.SendR)
	    _respListener.abortRequest (this);
    }
    @Override
    protected void closedInWriteExec (){
	super.closedInWriteExec ();
	_clientConnection.closed (this);
    }

    /***************** DecoderSouth ************/

    public void add(Header h){
	switch (h){
	case STATUS_200: _respListener.recvRespStatus (this, 200); return;
	case STATUS_204: _respListener.recvRespStatus (this, 204); return;
	case STATUS_206: _respListener.recvRespStatus (this, 206); return;
	case STATUS_304: _respListener.recvRespStatus (this, 304); return;
	case STATUS_400: _respListener.recvRespStatus (this, 400); return;
	case STATUS_404: _respListener.recvRespStatus (this, 404); return;
	case STATUS_500: _respListener.recvRespStatus (this, 500); return;
	case CONTENT_LENGTH:
	    _respContentLen = Long.parseLong (h.getValue ());
	}
	recvRespHeader (h.getName (), h.getValue ());
    }
    
    public void add(Header h, String value){
	switch (h){
	case STATUS:
	    _respListener.recvRespStatus (this, Integer.parseInt (value)); return;
	case CONTENT_LENGTH:
	    _respContentLen = Long.parseLong (value);
	}
	recvRespHeader (h.getName (), value);
    }

    public void add(String name, String value){
	recvRespHeader (name, value);
    }

    private void recvRespHeader (String name, String value){
	if (_inTrailers)
	    _respListener.recvRespTrailer (this, name, value);
	else{
	    if (_respContentLen == -1L &&
		name.equalsIgnoreCase ("content-length"))
		_respContentLen = Long.parseLong (value);
	    _respListener.recvRespHeader (this, name, value);
	}
    }

    public void last_or_end_or_finish_or_stop_or_complete(){
	if (!_inTrailers)
	    _respListener.recvRespHeaders (this, recvEndStream ());
    }

    public void error(){
	_connection.error (new ConnectionError (Http2Error.Code.COMPRESSION_ERROR, "Error in HPACKDecoder"));
    }

    /***************** Http2Request ***********/

    protected Object _attachment;
    protected boolean _authSet, _defSet;

    public Http2Connection getConnection (){ return _clientConnection;}
    
    public Logger logger (){ return _logger;}
    
    public long channelId (){ return _connection.id ();}

    public <T> T attachment (){ return (T) _attachment;}

    public void attach (Object o){ _attachment = o;}

    // note that they are opposite to reqListener
    public Executor requestExecutor (){ return _connection.writeExecutor ();}
    public Executor responseExecutor (){ return _connection.readExecutor ();}

    public Http2Request setPriority (boolean exclusive, int streamDepId, int weight){
	_clientConnection.sendPriority (id (), exclusive, streamDepId, weight);
	return this;
    }
    
    public Http2Request setReqMethod (String method){
	switch (method){
	case "GET": _connection.encodeHeader (Header.METHOD_GET); return this;
	case "POST": _connection.encodeHeader (Header.METHOD_POST); return this;
	default: _connection.encodeHeader (Header.METHOD, method); return this;
	}
    }
    
    public Http2Request setReqPath (String path){
	_connection.encodeHeader (Header.PATH, path); return this;
    }
    
    public Http2Request setReqAuth (String auth){
	_authSet = true;
	_connection.encodeHeader (Header.AUTHORITY, auth); return this;
    }

    public Http2Request setReqScheme (String scheme){
	switch (scheme.toLowerCase ()){
	case "http": _connection.encodeHeader (Header.SCHEME_HTTP); return this;
	case "https": _connection.encodeHeader (Header.SCHEME_HTTPS); return this;
	}
	return this; // not reachable
    }
    
    public Http2Request setReqHeader (String name, String value){
	setDefHeaders ();
	_connection.encodeHeader (name, value); return this;
    }

    public Http2Request sendReqHeaders (boolean done){
	setDefHeaders ();
	_connection.sendHeaders (this, done);
	return this;
    }
    
    public Http2Request sendReqData (ByteBuffer data, boolean copy, boolean done){
	sendData (data, copy, done);
	return this;
    }
    
    public void abort (int code){
	ResetFrame rf = Frame.newFrame (ResetFrame.TYPE);
	rf.set (0, _id);
	rf.set (code);
	_connection.sendNow (this, rf);
	_closed = true;
    }

    public SendReqBuffer newSendReqBuffer (int maxBufferSize){
	return new SendBuffer (this, maxBufferSize);
    }

    private void setDefHeaders (){
	if (_defSet) return;
	if (!_authSet) setReqAuth (new StringBuilder () // TODO CHECK if relevant
				   .append (_connection.channel ().getRemoteAddress ().getAddress ().getHostAddress ())
				   .append (':')
				   .append (_connection.channel ().getRemoteAddress ().getPort ())
				   .toString ());	
	_defSet = true;
    }

}
