package com.alcatel.as.http2.frames;

import com.alcatel.as.http2.*;
import java.nio.ByteBuffer;

public class WindowUpdateFrame extends Frame {

    public static final int TYPE = 0x08;

    protected int _increment;

    protected WindowUpdateFrame (){
	super (TYPE);
    }
    @Override
    public String toString (){ return "WindowUpdateFrame["+_streamId+"/increment="+increment ()+"]";}
    public boolean isControlFrame (){ return _streamId == 0;}
    public int increment (){ return _increment;}
    
    @Override
    public Frame parse () throws ConnectionError, StreamError {
	if (_payload.remaining () != 4)
	    throw new ConnectionError (Http2Error.Code.FRAME_SIZE_ERROR,
				       "Invalid WindowUpdate Frame length : "+_payload.remaining ());
	_increment = _payload.getInt () & 0x7FFFFFFF;
	return check ();
    }

    @Override
    protected Frame check () throws ConnectionError, StreamError {
	// super.check () is N/A
	if (_increment == 0)
	    {
		if (isControlFrame ())
		    throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR,
					       this+" : WindowUpdate Frame with increment = 0"
					       );
		else
		    throw new StreamError (Http2Error.Code.PROTOCOL_ERROR,
					   this+" : WindowUpdate Frame with increment = 0",
					   this);
	    }
	return this;
    }

    @Override
    public void received (Stream stream){
	stream.updateSendFCWindow (this);
    }
    @Override
    public void sent (Stream stream) {
	//TODO
    }

    public static WindowUpdateFrame newWindowUpdateFrame  (int streamId, int increment){
	WindowUpdateFrame f = new WindowUpdateFrame ();
	f.set (0x00, streamId);
	f._increment = increment;
	ByteBuffer buffer = ByteBuffer.allocate (4);
	buffer.putInt (increment);
	buffer.flip ();
	f.payload (buffer, true);
	return f;
    }
    
}
