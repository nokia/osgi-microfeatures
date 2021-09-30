package com.alcatel.as.http2;

import java.nio.ByteBuffer;
import com.alcatel.as.http2.frames.*;
import com.alcatel.as.http2.StreamStateMachine.State;
import com.alcatel.as.http2.StreamStateMachine.Event;
import org.apache.log4j.Logger;
import com.alcatel.as.service.concurrent.*;
import java.util.concurrent.*;

public abstract class Stream {

    protected int _id;
    protected State _state;
    protected Connection _connection;
    protected Logger _logger;
    protected Executor _writeExec;
    protected boolean _closed;
    protected int _sendFCWindow;
    protected OnAvailable _onAvailable;
    
    protected static Stream newRecvStream (Connection connection, Frame frame){
	switch (frame.type ()){
	case HeadersFrame.TYPE :
	    return new RecvReqStream (connection, frame.streamId ());
	    //case PushFrame.TYPE : return newRecvClientReqStream (connection, frame.streamId ());
	}
	return null;
    }
    
    protected Stream (Connection connection, int id, StreamStateMachine sm){
	_connection = connection;
	_id = id;
	_logger = connection.logger ();
	_writeExec = connection.writeExecutor ();
	_state = sm.getIdleState ();
	_onAvailable = new OnAvailable (this::isAvailable, connection::scheduleInWriteExecutor, connection.logger ());
    }

    public int id (){ return _id;}

    public Connection connection (){ return _connection;}
    
    protected Stream accept (){
	// init _sendFCWindow
	_connection.writeExecutor ().execute (() -> {
		_sendFCWindow = (int) _connection.remoteSettings ().INITIAL_WINDOW_SIZE;
	    });
	return this;
    }
    protected Stream reject (){
	return event (Event.StreamReject, null);
    }

    /********************** SendFC mgmt : called in _writeExec **************/

    protected int sendFCWindow (){
	return Math.min (_sendFCWindow, _connection.sendFCWindow ());
    }
    public int sendWindow (){ return sendFCWindow ();} // this one is the public API
    public boolean isAvailable (){ return sendFCWindow () > 0;}
    public void onWriteAvailable (Runnable success, Runnable failure, long delay){
	if (isAvailable ()){
	    success.run ();
	    return;
	}
	if (_closed){
	    failure.run ();
	    return;
	}
	long initTime = System.currentTimeMillis ();
	Runnable successWrapper = new Runnable (){
		public void run (){
		    long spent = System.currentTimeMillis () - initTime;
		    long newdelay = delay - spent;
		    if (newdelay <= 0L){
			failure.run ();
			return;
		    }
		    onWriteAvailable (success, failure, newdelay);
		}
	    };
	if (_sendFCWindow > 0){
	    // the connection is full
	    _connection.onWriteAvailable (successWrapper, failure, delay);
	} else {
	    // the stream is full
	    _onAvailable.add (successWrapper, failure, delay);
	}
    }
    public void sendData (ByteBuffer data, boolean copy, boolean done){
	if (data != null){
	    _sendFCWindow -=  data.remaining ();
	    // TODO handle negative value
	    if (_logger.isDebugEnabled ())
		_logger.debug (this+" : decrease flow control window to : "+_sendFCWindow);
	}
	_connection.sendData (this, data, copy, done);
    }
    
    /*************** called by Connection in _readExec ***********/

    public void received (Frame frame) throws ConnectionError {
	frame.received (this); // to avoid a switch
    }
    public void sent (Frame frame) {
	frame.sent (this);
    }
    public void connectionClosed (){
	event (Event.ConnR, null);
    }
    public void error (StreamError se){
	_state.error (this, se);
    }

    /*************** called by Frames ***********/
    
    public Stream event (Event event, Frame frame){
	_state.event (this, event, frame);
	return this;
    }
    
    /*************** State Machine callbacks ***********/

    // when we recv WindowUpdateFrame
    public void updateSendFCWindow (final WindowUpdateFrame wuf){
	_connection.writeExecutor ().execute (() -> {
		int maxCurrent = 0x7FFFFFFF - wuf.increment ();
		if (_sendFCWindow > maxCurrent){
		    // dont try to be too smart: close the connection for now / TODO reset the stream only...
		    _connection.close (new ConnectionError (Http2Error.Code.FLOW_CONTROL_ERROR, "Max Flow Control Window exceeded for stream : "+_id));
		} else {
		    boolean wasBlocked = _sendFCWindow <= 0;
		    _sendFCWindow += wuf.increment ();
		    if (_logger.isDebugEnabled ())
			_logger.debug (Stream.this+" : increase flow control window to : "+_sendFCWindow);
		    if (wasBlocked && _sendFCWindow > 0)
			_onAvailable.available ();
		}
	    });
    }
    // when new remote settings indicate a new INITIAL_WINDOW_SIZE
    public void updateSendFCWindow (int diff){
	_connection.writeExecutor ().execute (() -> {
		int newSendFCWindow = Math.min (_sendFCWindow + diff, 0x7FFFFFFF);
		boolean wasBlocked = _sendFCWindow <= 0;
		_sendFCWindow = newSendFCWindow;
		if (_logger.isDebugEnabled ())
		    _logger.debug (Stream.this+" : new INITIAL_WINDOW_SIZE : update flow control window to : "+_sendFCWindow);
		if (wasBlocked && _sendFCWindow > 0)
		    _onAvailable.available ();
	    });
    }

    // State storage
    protected StreamStateMachine.State state (){ return _state;}
    protected void state (StreamStateMachine.State state){
	if (_logger.isDebugEnabled ())
	    _logger.debug (_connection+" : "+this+" : "+_state+" -> "+state);
	_state = state;
    }
    
    // EndStream storage
    protected boolean _recvES;
    protected boolean recvEndStream (){ return _recvES;}
    protected void recvEndStream (boolean es){ _recvES = es;}
    
    protected void reset (StreamError se){
	// do not close automatically (may be called from CLOSED state)
	reset (se.code ());
    }
    protected void reset (Http2Error.Code code){
	// do not close automatically (may be called from CLOSED state)
	ResetFrame rf = Frame.newFrame (ResetFrame.TYPE);
	rf.set (0x00, _id);
	rf.set (code.value ());
	reset (rf);
    }
    protected void reset (ResetFrame rf){
	_connection.writeExecutor ().execute (() -> {
		_connection.sendNow (null, rf); // do not trigger a SendR event !
	    });
    }
    protected void error (ConnectionError ce){
	_connection.error (ce);
    }

    // close event storage
    protected StreamStateMachine.Event _closedEvent;
    protected StreamStateMachine.Event closedEvent (){ return _closedEvent;}
    protected void closedEvent (StreamStateMachine.Event closedEvent){
	_closedEvent = closedEvent;
    }
    // called in _readExec
    protected void closed (){
	_connection.closed (this);
	_writeExec.execute (this::closedInWriteExec);
    }
    // called in _writeExec for cleaning
    // can be overridden
    protected void closedInWriteExec (){
	_closed = true;
	_onAvailable.closed ();
    }
    // must be called in _writeExec
    public boolean isClosed (){ return _closed;}
}
