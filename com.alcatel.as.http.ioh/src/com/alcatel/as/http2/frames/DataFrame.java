package com.alcatel.as.http2.frames;

import com.alcatel.as.http2.*;
import java.nio.ByteBuffer;

public class DataFrame extends Frame {

    public static final int TYPE = 0x00;

    private int _totalSize;
    private boolean _needsWU = true;

    protected DataFrame (){
	super (TYPE);
    }
    @Override
    public String toString (){ return "DataFrame["+_streamId+"/len="+_payload.remaining ()+"/endStream="+endStreamFlag ()+"]";}
    public boolean isControlFrame (){ return false;}
    public boolean endStreamFlag (){ return (_flags & 0x01) == 0x01;}
    public boolean paddedFlag (){ return (_flags & 0x08) == 0x08;}
    @Override
    public boolean endsStream (){ return endStreamFlag ();}

    // if the parser chunks the DataFrame : do not necessarily send a WU for an endStrem frame that was chunked
    public boolean needsWindowUpate (){ return _needsWU && !endStreamFlag ();}
    public DataFrame needsWindowUpate (boolean needs){ _needsWU = needs; return this;}

    @Override
    public Frame parse () throws ConnectionError, StreamError {
	_totalSize = _payload.remaining ();
	if (!paddedFlag ()) return this;
	int padding = _payload.get () & 0xFF;
	if (padding >= _totalSize)
	    throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR,
				       "Data Frame with excessive padding length",
				       this);
	_payload.limit (_payload.limit () - padding);
	return check ();
    }

    // used for flow ctl window
    public int totalPayloadSize (){ return _totalSize;}
    
    @Override
    public void received (Stream stream){
	stream.event (StreamStateMachine.Event.RecvD, this);
    }
    @Override
    public void sent (Stream stream) {
	stream.event (StreamStateMachine.Event.SendD, this);
    }
}
