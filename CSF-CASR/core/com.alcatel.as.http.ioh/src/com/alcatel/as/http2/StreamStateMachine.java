package com.alcatel.as.http2;

import java.nio.ByteBuffer;
import com.alcatel.as.http2.frames.*;
import com.alcatel.as.http2.hpack.*;
import com.alcatel.as.http2.headers.*;

public abstract class StreamStateMachine<T extends Stream> {

    public enum Event {
	SendPP (true, false),
	RecvPP (false, true),
	SendH (true, false),
	RecvH (false, true),
	SendES (true, false),
	RecvES (false, true),
	SendR (true, false),
	RecvR (false, true),
	SendD (true, false),
	RecvD (false, true),
	SendC (true, false),
	RecvC (false, true),
	RecvP (false, true),
	ConnR (false, false), // connection reset
	StreamErr (false, false), // stream error
	StreamReject (false, false); // stream rejected - only at init

	private boolean _isSend, _isRecv;
	private Event (boolean isSend, boolean isRecv){
	    _isSend = isSend;
	    _isRecv = isRecv;
	}
	public boolean isSend (){ return _isSend;}
	public boolean isRecv (){ return _isRecv;}
    };

    protected abstract State getIdleState ();

    protected class State {
	public void enter (T stream){
	    stream.state (this);
	}
	protected void event (T stream, Event event, Frame frame){}
	protected void error (T stream, StreamError error){
	    reset (stream, error);
	}

	// internal API
	protected boolean causedByReset (StreamError error){ // dont reset a reset
	    Frame frame = error.frame ();
	    return frame != null && frame.type () == ResetFrame.TYPE;
	}
	protected void reset (T stream, StreamError error){
	    if (causedByReset (error)){ // dont reset a reset
		stream.closedEvent (Event.RecvR);
		STATE_CLOSED.enter (stream);
	    } else {
		stream.reset (error);
		stream.closedEvent (Event.StreamErr);
		STATE_CLOSED.enter (stream);
	    }
	}
    }

    protected State STATE_CLOSED = new State (){
	    public String toString (){ return "CLOSED";}
	    public void enter (T stream){
		super.enter (stream);
		stream.closed ();
	    }
	    @Override
	    protected void event (T stream, Event event, Frame frame){
		if (event.isSend ()) return; // in particular, avoid recursive reset loop
		if (event.isRecv ()){
		    switch (event){
		    case RecvR: // avoid reset loop
			return;
		    case RecvP: // allowed
			return;
		    case RecvH:
		    case RecvC:
			// maintain headers state
			consumeHeaders (stream.connection (), frame);
			break;
		    }
		    stream.reset (Http2Error.Code.STREAM_CLOSED);
		}
	    }
	    @Override
	    protected void error (T stream, StreamError error){
		if (!causedByReset (error)) stream.reset (error); // dont re-enter STATE_CLOSED
	    }
	};

    protected static class EmptyDecoder implements HPACKDecoder.DecoderSouth {
	protected Connection _conn;
	protected EmptyDecoder (Connection conn){
	    _conn = conn;
	}
	public void add(Header h){}
	public void add(Header h, String value){}
	public void add(String name, String value){}
	public void last_or_end_or_finish_or_stop_or_complete(){}
	public void error(){
	    _conn.error (new ConnectionError (Http2Error.Code.COMPRESSION_ERROR, "Error in HPACKDecoder"));
	}
    };

    protected static void consumeHeaders (Connection c, Frame frame){
	EmptyDecoder decoder = new EmptyDecoder (c);
	c.hpackDecoder ().decode (frame.payload (), decoder);
	c.hpackDecoder ().last_or_end_or_finish_or_stop_or_complete (decoder);
    }
}
