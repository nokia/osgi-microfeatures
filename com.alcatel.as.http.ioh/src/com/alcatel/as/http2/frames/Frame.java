package com.alcatel.as.http2.frames;

import com.alcatel.as.http2.*;
import java.nio.ByteBuffer;

public abstract class Frame {

    protected int _type, _flags, _streamId;
    protected boolean _isCopy;
    protected ByteBuffer _payload;

    public static <T extends Frame> T newFrame (int type) {
	switch (type){
	case DataFrame.TYPE: return (T) new DataFrame ();
	case HeadersFrame.TYPE: return (T) new HeadersFrame ();
	case ContinuationFrame.TYPE : return (T) new ContinuationFrame ();
	case GoAwayFrame.TYPE : return (T) new GoAwayFrame ();
	case PingFrame.TYPE : return (T) new PingFrame ();
	case PriorityFrame.TYPE : return (T) new PriorityFrame ();
	case ResetFrame.TYPE : return (T) new ResetFrame ();
	case SettingsFrame.TYPE : return (T) new SettingsFrame ();
	case WindowUpdateFrame.TYPE : return (T) new WindowUpdateFrame ();
	}
	return (T) new UnknownFrame (type);
    }

    protected Frame (int type){
	_type = type;
    }

    public int streamId (){ return _streamId;}
    public int flags (){ return _flags;}
    public int type (){ return _type;}
    
    public <T extends Frame> T set (int flags, int streamId){
	_flags = flags;
	_streamId = streamId;
	return (T) this;
    }
    public <T extends Frame> T flags (int flags){ _flags = flags; return (T) this;}
    public <T extends Frame> T streamId (int streamId){ _streamId = streamId; return (T) this;}
    
    protected <T extends Frame> T check () throws ConnectionError, StreamError {
	if (isControlFrame ()){
	    if (_streamId != 0)
		throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR,
					   this+" : Control Frame with non-0 streamId : "+_streamId,
					   this);
	} else {
	    if (_streamId == 0)
		throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR,
					   this+" : Non-Control Frame with streamId=0",
					   this);

	}
	return (T) this;
    }

    public boolean isCopy (){ return _isCopy;}
    public ByteBuffer copyPayload (){
	if (_isCopy) return _payload;
	ByteBuffer copy = ByteBuffer.allocate (_payload.remaining ());
	copy.put (_payload);
	copy.flip ();
	_payload = copy;
	_isCopy = true;
	return _payload;
    }
    public ByteBuffer payload (){ return _payload;}
    
    public Frame payload (ByteBuffer payload, boolean isCopy){
	_payload = payload;
	_isCopy = isCopy;
	return this;
    }

    public Frame parse () throws ConnectionError, StreamError { return check ();}
    public abstract boolean isControlFrame ();
    public boolean endsStream (){ throw new IllegalStateException ();} // to be overridden
    public void received (Stream stream) {}
    public void sent (Stream stream){}

    public int sendSize (){return 9 + _payload.remaining ();}

    public ByteBuffer getHeading (){
	ByteBuffer h = ByteBuffer.allocate (9);
	int len = _payload.remaining ();
	h.put ((byte)(len >> 16));
	h.put ((byte)(len >> 8));
	h.put ((byte)(len));
	h.put ((byte) _type);
	h.put ((byte) _flags);
	h.putInt (_streamId);
	h.flip ();
	return h;
    }
}
