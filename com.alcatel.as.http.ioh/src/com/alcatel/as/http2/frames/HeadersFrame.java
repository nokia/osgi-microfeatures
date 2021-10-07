package com.alcatel.as.http2.frames;

import com.alcatel.as.http2.*;
import java.nio.ByteBuffer;

public class HeadersFrame extends Frame {

    public static final int TYPE = 0x01;
    
    protected int _padding, _dependency, _weight;
    protected boolean _exclusiveDep;

    protected HeadersFrame (){
	super (TYPE);
    }
    @Override
    public String toString (){ return "HeadersFrame["+_streamId+"/endHeaders="+endHeadersFlag ()+"/endStream="+endStreamFlag()+"]";}
    public boolean isControlFrame (){ return false;}
    public boolean endStreamFlag (){ return (_flags & 0x01) == 0x01;}
    public boolean endHeadersFlag (){ return (_flags & 0x04) == 0x04;}
    public boolean paddedFlag (){ return (_flags & 0x08) == 0x08;}
    public boolean prioritySetFlag (){ return (_flags & 0x20) == 0x20;}
    public boolean exclusiveDependency (){ return _exclusiveDep;}
    public int dependency (){ return _dependency;}
    public int weight (){ return _weight;}
    @Override
    public boolean endsStream (){ return (_flags & 0x05) == 0x05;} // endStream && endHeader
    
    @Override
    public Frame parse () throws ConnectionError, StreamError {
	int padding = 0;
	if (paddedFlag ())
	    padding = _payload.get () & 0xFF;
	if (padding >= _payload.remaining ())
	    throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR,
				       "Headers Frame with excessive padding length",
				       this);
	_payload.limit (_payload.limit () - padding);
	if (prioritySetFlag ()){
	    _dependency = _payload.getInt ();
	    _exclusiveDep = (_dependency & 0x80) == 0x80;
	    _dependency &= 0x7F;
	    _weight = (_payload.get () & 0xFF) +1;
	}
	return check ();
    }

    @Override
    public void received (Stream stream) {
	stream.event (StreamStateMachine.Event.RecvH, this);
    }
    @Override
    public void sent (Stream stream) {
	stream.event (StreamStateMachine.Event.SendH, this);
    }

    public static int flags (boolean endHeaders, boolean endStream){
	return endHeaders ? (endStream ? 0x05 : 0x04) : 0x00;
    }
}
