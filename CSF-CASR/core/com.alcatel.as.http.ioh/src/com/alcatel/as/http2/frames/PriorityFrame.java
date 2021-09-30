package com.alcatel.as.http2.frames;

import com.alcatel.as.http2.*;
import java.nio.ByteBuffer;

public class PriorityFrame extends Frame {

    public static final int TYPE = 0x02;
    
    protected int _dependency, _weight;
    protected boolean _exclusiveDep;

    protected PriorityFrame (){
	super (TYPE);
    }
    @Override
    public String toString (){ return
	    "PriorityFrame["+_streamId+
	    "/streamDependency="+_dependency+
	    "/exclusiveDep="+_exclusiveDep+
	    "/weight="+_weight
	    +"]"
	    ;}
    public boolean isControlFrame (){ return false;}
    public boolean exclusiveDependency (){ return _exclusiveDep;}
    public int dependency (){ return _dependency;}
    public int weight (){ return _weight;}
    
    @Override
    public Frame parse () throws ConnectionError, StreamError {
	check (); // throw ConnectionError first
	if (_payload.remaining () != 5)
	    throw new StreamError (Http2Error.Code.FRAME_SIZE_ERROR,
				   "Priority Frame with invalid size : "+_payload.remaining (),
				   this);
	_dependency = _payload.getInt ();
	_exclusiveDep = (_dependency & 0x80) == 0x80;
	_dependency &= 0x7F;
	_weight = (_payload.get () & 0xFF) +1;
	return this;
    }

    public PriorityFrame set (int streamId, boolean exclusive, int _dependency, int weight){
	if (streamId < 0) throw new IllegalArgumentException ("Cannot setPriority : Invalid streamId : "+streamId);
	if (weight < 1 || weight > 256) throw new IllegalArgumentException ("Cannot setPriority : Invalid weight : "+weight);
	set (0, streamId);
	_exclusiveDep = exclusive;
	_dependency = _dependency;
	_weight = weight;
	byte[] data = new byte[5];
	if (exclusive)
	    data[0] = (byte) ((_dependency >> 24) | 0x80);
	else
	    data[0] = (byte) ((_dependency >> 24) & 0x7F);
	data[1] = (byte) (_dependency >> 16);
	data[2] = (byte) (_dependency >> 8);
	data[3] = (byte) _dependency;
	data[4] = (byte) (weight - 1);
	_payload = ByteBuffer.wrap (data);
	_isCopy = true;
	return this;
    }
}
