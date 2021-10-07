package com.alcatel.as.http2;

import java.nio.ByteBuffer;
import com.alcatel.as.http2.frames.*;

public class SendReqStateMachine extends StreamStateMachine<SendReqStream> {

    public static final SendReqStateMachine INSTANCE = new SendReqStateMachine ();
    
    protected State getIdleState (){ return STATE_OPEN;} // STATE_IDLE is not needed : we sendH immediately

    protected State STATE_OPEN = new State (){
	    public String toString (){ return "OPEN";}
	    @Override
	    public void enter (SendReqStream stream){
		super.enter (stream);
	    }
	    @Override
	    protected void event (SendReqStream stream, Event event, Frame frame){
		switch (event){
		case SendH:
		case SendC:
		    // register in _readExec -- TODO change it the day when we support trailers to avoid 2 register
		    stream.connection ().register (stream, stream.initialSendFCWindow ());
		case SendD:
		    // no need to go to HALF_CLOSED_LOCAL : identical to OPEN for incoming events
		    return;
		case RecvH:
		    HeadersFrame hf = (HeadersFrame) frame;
		    if (stream.hasRecvHeaders () &&
			!hf.endStreamFlag ()){
		    	stream.reset (Http2Error.Code.PROTOCOL_ERROR);
		    	stream.closedEvent (Event.StreamErr);
		    	STATE_CLOSED.enter (stream);
		    	return;
		    }
		    stream.recvHeaders (hf.payload ());
		    stream.recvEndStream (hf.endStreamFlag ());
		    if (hf.endHeadersFlag ()){
			stream.recvHeaders ();
			if (hf.endStreamFlag ()){
			    STATE_HALF_CLOSED_REMOTE.enter (stream);
			}
		    } else {
			stream.connection ().setNextFrame (stream.id (), ContinuationFrame.TYPE);
		    }
		    return;
		case RecvC:
		    ContinuationFrame cf = (ContinuationFrame) frame;
		    stream.recvHeaders (cf.payload ());
		    if (cf.endHeadersFlag ()){
			stream.connection ().setNextFrame (-1, -1);
			stream.recvHeaders ();
			if (stream.recvEndStream ()){
			    STATE_HALF_CLOSED_REMOTE.enter (stream);
			}
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
		case ConnR:
		    // CSFAR-1739: if the whole response data based on contentLen has arrived --> be cool and pretend it is ok
		    if (stream.respDataPending () == 0L){
			STATE_HALF_CLOSED_REMOTE.enter (stream);
			stream.event (event, frame); // propagate the ConnR
			return;
		    }
		case RecvR:
		    stream.closedEvent (event);
		    STATE_CLOSED.enter (stream);
		    return;
		case SendR:
		    stream.closedEvent (event);
		    STATE_CLOSED.enter (stream);
		    return;
		default :
		    stream.error (new ConnectionError (Http2Error.Code.PROTOCOL_ERROR, "Unexpected Event : "+event+" in state="+stream.state (), frame));
		    // no real need to move to CLOSED state since the connection will be closed
		}
	    }
	};

    protected State STATE_HALF_CLOSED_REMOTE = new State (){
	    public String toString (){ return "HALF_CLOSED_REMOTE";}
	    @Override
	    public void enter (SendReqStream stream){
		super.enter (stream);
		stream.recvResponse ();
		stream.closedEvent (Event.RecvES);
		STATE_CLOSED.enter (stream);
	    }
	    @Override
	    protected void event (SendReqStream stream, Event event, Frame frame){
		// N/A
	    }
	};
}
