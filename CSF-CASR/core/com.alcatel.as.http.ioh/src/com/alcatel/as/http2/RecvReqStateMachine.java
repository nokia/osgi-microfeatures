package com.alcatel.as.http2;

import java.nio.ByteBuffer;
import com.alcatel.as.http2.frames.*;

public class RecvReqStateMachine extends StreamStateMachine<RecvReqStream> {

    public static final RecvReqStateMachine INSTANCE = new RecvReqStateMachine ();
    
    protected State getIdleState (){ return STATE_IDLE;}

    protected State STATE_IDLE = new State (){
	    public String toString (){ return "IDLE";}
	    @Override
	    protected void event (RecvReqStream stream, Event event, Frame frame){
		switch (event){
		case RecvH:
		    HeadersFrame hf = (HeadersFrame) frame;
		    stream.recvHeaders (hf.payload ());
		    stream.recvEndStream (hf.endStreamFlag ());
		    if (hf.endHeadersFlag ()){
			STATE_OPEN.enter (stream);
		    } else {
			stream.connection ().setNextFrame (stream.id (), ContinuationFrame.TYPE);
		    }
		    return;
		case RecvC:
		    ContinuationFrame cf = (ContinuationFrame) frame;
		    stream.recvHeaders (cf.payload ());
		    if (cf.endHeadersFlag ()){
			stream.connection ().setNextFrame (-1, -1);
			STATE_OPEN.enter (stream);
		    }
		    return;
		case RecvP:
		    // TODO manage priority
		    return;
		case ConnR:
		    stream.closedEvent (Event.ConnR);
		    STATE_CLOSED.enter (stream);
		    return;
		case StreamReject:
		    stream.reset (Http2Error.Code.REFUSED_STREAM);
		    stream.closedEvent (Event.SendR);
		    STATE_CLOSED.enter (stream);
		    return;
		case SendR:
		    stream.closedEvent (Event.SendR);
		    STATE_CLOSED.enter (stream);
		    return;
		default :
		    stream.error (new ConnectionError (Http2Error.Code.PROTOCOL_ERROR, "Unexpected Event : "+event+" in state="+stream.state (), frame));
		    // no real need to move to CLOSED state since the connection will be closed
		}
	    }
	};
    
    protected State STATE_OPEN = new State (){
	    public String toString (){ return "OPEN";}
	    @Override
	    public void enter (RecvReqStream stream){
		super.enter (stream);
		stream.recvHeaders ();
		if (stream.recvEndStream ())
		    STATE_HALF_CLOSED_REMOTE.enter (stream);
	    }
	    @Override
	    protected void event (RecvReqStream stream, Event event, Frame frame){
		switch (event){
		case RecvH: // trailer
		    HeadersFrame hf = (HeadersFrame) frame;
		    if (!hf.endStreamFlag ()){
			stream.reset (Http2Error.Code.PROTOCOL_ERROR);
			stream.closedEvent (Event.StreamErr);
			STATE_CLOSED.enter (stream);
			return;
		    }
		    stream.recvTrailers (hf.payload ());
		    stream.recvEndStream (hf.endStreamFlag ());
		    if (hf.endHeadersFlag ()){
			stream.recvTrailers ();
			STATE_HALF_CLOSED_REMOTE.enter (stream);
		    } else {
			stream.connection ().setNextFrame (stream.id (), ContinuationFrame.TYPE);
		    }
		    return;
		case RecvC:
		    ContinuationFrame cf = (ContinuationFrame) frame;
		    stream.recvTrailers (cf.payload ());
		    if (cf.endHeadersFlag ()){
			stream.connection ().setNextFrame (-1, -1);
			stream.recvTrailers ();
			STATE_HALF_CLOSED_REMOTE.enter (stream);
		    }
		    return;
		case RecvD:
		    DataFrame df = (DataFrame) frame;
		    stream.recvEndStream (df.endStreamFlag ());
		    stream.recvData (df.payload ());
		    if (df.endStreamFlag ()){
			STATE_HALF_CLOSED_REMOTE.enter (stream);
		    }
		    return;
		case RecvR:
		case ConnR:
		    stream.closedEvent (event);
		    STATE_CLOSED.enter (stream);
		    return;
		case SendH:
		case SendC:
		case SendD:
		    if (frame.endsStream ()){
			STATE_HALF_CLOSED_LOCAL.enter (stream);
			return;
		    }
		    return;
		case SendR:
		    stream.closedEvent (Event.SendR);
		    STATE_CLOSED.enter (stream);
		    return;
		default :
		    // TODO all events are allowed (incl. H for trailing headers)
		}
	    }
	};
    
    protected State STATE_HALF_CLOSED_REMOTE = new State (){
	    public String toString (){ return "HALF_CLOSED_REMOTE";}
	    @Override
	    public void enter (RecvReqStream stream){
		super.enter (stream);
		stream.recvRequest ();
	    }
	    @Override
	    protected void event (RecvReqStream stream, Event event, Frame frame){
		switch (event){
		case RecvP:// TODO manage priority
		    return;
		case RecvR:
		case ConnR:
		    stream.closedEvent (event);
		    STATE_CLOSED.enter (stream);
		    return;
		case SendH:
		case SendC:
		case SendD:
		    if (frame.endsStream ()){
			stream.closedEvent (Event.SendES);
			STATE_CLOSED.enter (stream);
		    }
		    return;
		case SendR:
		    stream.closedEvent (Event.SendR);
		    STATE_CLOSED.enter (stream);
		    return;
		default :
		    stream.reset (Http2Error.Code.PROTOCOL_ERROR);
		    stream.closedEvent (Event.StreamErr);
		    STATE_CLOSED.enter (stream);
		    return;
		}
	    }
	};
    
    protected State STATE_HALF_CLOSED_LOCAL = new State (){
	    public String toString (){ return "HALF_CLOSED_LOCAL";}
	    @Override
	    public void enter (RecvReqStream stream){
		super.enter (stream);
		// send a reset : rfc section 8.1
		stream.reset (Http2Error.Code.NO_ERROR);
		stream.closedEvent (Event.SendES);
		STATE_CLOSED.enter (stream);
	    }
	    @Override
	    protected void event (RecvReqStream stream, Event event, Frame frame){
		// N/A
	    }
	};
    
}
