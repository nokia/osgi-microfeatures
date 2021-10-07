package com.alcatel.as.http2.frames;

import com.alcatel.as.http2.*;
import java.nio.ByteBuffer;

public class GoAwayFrame extends Frame {

    public static final int TYPE = 0x07;
    
    protected int _streamId;
    protected long _errorCode;

    protected GoAwayFrame (){
	super (TYPE);
    }
    @Override
    public String toString (){ return "GoAwayFrame["+_streamId+"/errCode="+_errorCode+"/data="+debugData ()+"]";}
    public boolean isControlFrame (){ return true;}
    public int lastStreamId (){ return _streamId;}
    public long errorCode (){ return _errorCode;}
    public String debugData (){
	StringBuilder sb = new StringBuilder ();
	for (int i=_payload.position (); i<_payload.limit (); i++)
	    sb.append ((char) _payload.get (i));
	return sb.toString ();
    }

    public GoAwayFrame set (int streamId, long errCode, String data){
	_streamId = streamId;
	_errorCode = errCode;
	if (data == null) data = "";
	int size = data.length ();
	_payload = ByteBuffer.allocate (8 + size);
	_isCopy = true;
	_payload.putInt (_streamId);
	_payload.putInt ((int) _errorCode);
	for (int i=0; i<size; i++) _payload.put ((byte) data.charAt (i));
	_payload.flip ();
	return this;
    }
    
    @Override
    public Frame parse () throws ConnectionError, StreamError {
	_streamId = _payload.getInt () & 0x7FFFFFFF;
	_errorCode = _payload.getInt () & 0xFFFFFFFFL;
	return check ();
    }
}
