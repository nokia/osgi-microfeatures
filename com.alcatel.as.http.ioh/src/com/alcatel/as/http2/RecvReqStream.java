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

public class RecvReqStream extends Stream implements Http2RequestListener.RequestContext, HPACKDecoder.DecoderSouth {

    protected Http2RequestListener _reqListener;
    protected HPACKDecoder _hpackDecoder;
    protected AccessLog _accessLog = new AccessLog ().version (2);
    protected boolean _resp100Continue = false;
    
    protected RecvReqStream (Connection connection, int id){
	super (connection, id, RecvReqStateMachine.INSTANCE);
    }
    public String toString (){
	return new StringBuilder ().append ("RecvReqStream[").append (_id).append (',').append (_state).append (']').toString ();
    }
    @Override
    protected Stream accept (){
	super.accept ();
	// call listener once constructor is done
	if (_connection.commonLogFormat ().isEnabled ())
	    _accessLog.touch ().remoteIP (_connection.channel ().getRemoteAddress ().getAddress ());
	_hpackDecoder = _connection.hpackDecoder ();
	_reqListener = _connection.reqListener ();
	_reqListener.newRequest (this);
	return this;
    }
    
    /*************** State Machine callbacks ***********/

    private String _method; // we store it for the meters
    private boolean _inTrailers;

    // aggregate headers in IDLE state
    protected void recvHeaders (ByteBuffer buffer){
	_hpackDecoder.decode (buffer, this);
    }
    //  when entering STATE_OPEN
    protected void recvHeaders (){
	_hpackDecoder.last_or_end_or_finish_or_stop_or_complete (this);
    }
    // in STATE_OPEN
    protected void recvData (ByteBuffer data){
	_reqListener.recvReqData (this, data, recvEndStream ());
    }
    // in STATE_OPEN
    protected void recvTrailers (ByteBuffer buffer){
	_inTrailers = true;
	_hpackDecoder.decode (buffer, this);
    }
    // in STATE_OPEN before entering STATE_HALF_CLOSED_REMOTE
    protected void recvTrailers (){
	_inTrailers = true;
	_hpackDecoder.last_or_end_or_finish_or_stop_or_complete (this);
    }
    // when entering STATE_HALF_CLOSED_REMOTE
    protected void recvRequest (){
	HttpMeters meters = _connection.meters ();
	if (meters != null) meters.getReadReqMeter (_method).inc (1);
	_reqListener.endRequest (this);
    }
    // when entering STATE_CLOSED
    protected void closed (){
	super.closed ();
	if (closedEvent () != Event.SendES &&
	    closedEvent () != Event.SendR)
	    _reqListener.abortRequest (this);
    }

    /***************** DecoderSouth ************/

    public void add(Header h){
	switch (h){
	case METHOD_GET:
	case METHOD_POST:
	    _accessLog.method (_method = h.getValue ());
	    _reqListener.recvReqMethod (this, h.getValue ()); return;
	case PATH_SLASH:
	case PATH_SLASH_INDEX_HTML:
	    _accessLog.url (h.getValue ());
	    _reqListener.recvReqPath (this, h.getValue ()); return;
	case SCHEME_HTTP:
	case SCHEME_HTTPS:
	    _reqListener.recvReqScheme (this, h.getValue ()); return;
	case AUTHORITY: authority (h.getValue ()); return;
	}
	recvReqHeader (h.getName (), h.getValue ());
    }
    
    public void add(Header h, String value){
	switch (h){
	case METHOD:
	    _accessLog.method (_method = value);
	    _reqListener.recvReqMethod (this, value); return;
	case PATH:
	    _accessLog.url (value);
	    _reqListener.recvReqPath (this, value); return;
	case SCHEME: _reqListener.recvReqScheme (this, value); return;
	case AUTHORITY: authority (value); return;
	}
	recvReqHeader (h.getName (), value);
    }

    private void authority (String value){
	if (_connection.sniMatch (value)){
	    _reqListener.recvReqAuthority (this, value);
	}else{
	    throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR, "Invalid authority : "+value+" : SNI mismatch");
	}
    }

    public void add(String name, String value){
	recvReqHeader (name, value);
    }

    private void recvReqHeader (String name, String value){
	if (!_accessLog.isEmpty () &&
	    name.equals ("authorization"))
	    _accessLog.username (value);
	if (_inTrailers)
	    _reqListener.recvReqTrailer (this, name, value);
	else {
	    if (name.equals ("expect")){
		if (value != null && value.contains ("100-continue")){
		    // hide it and respond 100
		    _resp100Continue = true;
		    return;
		}
	    }
	    _reqListener.recvReqHeader (this, name, value);
	}
    }

    public void last_or_end_or_finish_or_stop_or_complete(){
	if (!_inTrailers){
	    // send 100 Continue if needed
	    if (_resp100Continue){
		responseExecutor ().execute (() -> {
			_connection.encodeHeader (Header.STATUS, "100");
			_connection.sendHeaders (RecvReqStream.this, false);
		    });
	    }
	    _reqListener.recvReqHeaders (this, recvEndStream ());
	}
    }

    public void error(){
	_connection.error (new ConnectionError (Http2Error.Code.COMPRESSION_ERROR, "Error in HPACKDecoder"));
    }

    /***************** Http2ReqListener Context ***********/

    protected Object _attachment;
    
    public Logger logger (){ return _logger;}
    
    public TcpChannel channel (){ return _connection.channel ();}

    public long channelId (){ return _connection.id ();}

    public <T> T attachment (){ return (T) _attachment;}

    public void attach (Object o){ _attachment = o;}

    public Executor requestExecutor (){ return _connection.readExecutor ();}
    public Executor responseExecutor (){ return _connection.writeExecutor ();}

    public int sendWindow (){ return sendFCWindow ();}
    
    public void setRespStatus (int status){
	_accessLog.responseStatus (status);
	HttpMeters meters = _connection.meters ();
	if (meters != null) meters.getWriteRespMeter (status).inc (1);
	switch (status){
	case 200: _connection.encodeHeader (Header.STATUS_200); return;
	case 204: _connection.encodeHeader (Header.STATUS_204); return;
	case 206: _connection.encodeHeader (Header.STATUS_206); return;
	case 304: _connection.encodeHeader (Header.STATUS_304); return;
	case 400: _connection.encodeHeader (Header.STATUS_400); return;
	case 404: _connection.encodeHeader (Header.STATUS_404); return;
	case 500: _connection.encodeHeader (Header.STATUS_500); return;
	}
	_connection.encodeHeader (Header.STATUS, String.valueOf (status));
    }

    public void setRespHeader (String name, String value){
	_connection.encodeHeader (name.toLowerCase (), value);
    }
    
    public void sendRespHeaders (boolean done){
	_connection.sendHeaders (this, done);
	if (done){
	    _accessLog.responseSize (0);
	    _connection.commonLogFormat ().log (_accessLog);
	}
    }
    
    public void sendRespData (ByteBuffer data, boolean copy, boolean done){
	sendData (data, copy, done);
    }
    @Override
    public void sendData (ByteBuffer data, boolean copy, boolean done){
	_accessLog.incResponseSize (data != null ? data.remaining () : 0);
	super.sendData (data, copy, done);
	if (done)
	    _connection.commonLogFormat ().log (_accessLog);
    }

    public SendBuffer newSendRespBuffer (int maxBufferSize){
	return new SendBuffer (this, maxBufferSize);
    }

    // same as reset() but reset() is called from _readExec while here it is from _writeExec
    public void abortStream (Http2Error.Code code, String msg){
	if (code == null) code = Http2Error.Code.INTERNAL_ERROR;
	ResetFrame rf = Frame.newFrame (ResetFrame.TYPE);
	rf.set (0, _id);
	rf.set (code.value ());
	_connection.sendNow (this, rf);
	_closed = true;
    }
    public void abortConnection (Http2Error.Code code, String msg){
	if (code == null) code = Http2Error.Code.INTERNAL_ERROR;
	_connection.close (code, msg);
	_closed = true;
    }
}
