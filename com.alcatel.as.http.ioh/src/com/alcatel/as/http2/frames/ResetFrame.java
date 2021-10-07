package com.alcatel.as.http2.frames;

import com.alcatel.as.http2.*;
import java.nio.ByteBuffer;

public class ResetFrame extends Frame {

    public static final int TYPE = 0x03;
    
    protected long _errorCode;

    protected ResetFrame (){
	super (TYPE);
    }
    @Override
    public String toString (){ return "ResetFrame["+_streamId+"]";}
    public boolean isControlFrame (){ return false;}
    public long errorCode (){ return _errorCode;}
    
    @Override
    public Frame parse () throws ConnectionError, StreamError {
	check (); // throw ConnectionError first
	if (_payload.remaining () != 4)
	    throw new StreamError (Http2Error.Code.FRAME_SIZE_ERROR,
				   "Reset Frame with invalid size : "+_payload.remaining (),
				   this);
	_errorCode = _payload.getInt () & 0xFFFFFFFFL;
	return this;
    }

    @Override
    public void received (Stream stream) {
	stream.event (StreamStateMachine.Event.RecvR, this);
    }
    @Override
    public void sent (Stream stream) {
	stream.event (StreamStateMachine.Event.SendR, this);
    }
    
    public ResetFrame set (long errCode){
	_payload = ByteBuffer.allocate (4);
	_payload.putInt ((int) errCode);
	_payload.flip ();
	_isCopy = true;
	return this;
    }
}
