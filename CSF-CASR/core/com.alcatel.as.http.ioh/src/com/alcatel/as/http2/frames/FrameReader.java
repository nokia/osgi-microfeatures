package com.alcatel.as.http2.frames;

import java.nio.ByteBuffer;
import java.io.IOException;

import com.alcatel.as.http2.*;

public class FrameReader {

    protected Settings _settings;
    protected ByteBuffer _cache;

    private Frame _frameInFlight;
    private int _neededLen, _padding;
    private BufferedByteBuffer _bbuffer = new BufferedByteBuffer (9);
    private State _state = STATE_INIT;
    
    public FrameReader (ConnectionConfig config){
	_settings = config.settings ();
    }
    
    public Frame read (ByteBuffer buffer) throws ConnectionError {
	if (buffer.remaining () == 0) return null;
	return _state.read (this, buffer);
    }

    private void initCache (ByteBuffer buffer, int available){
	if (_cache != null && _cache.capacity () >= _neededLen)
	    _cache.clear ();
	else
	    _cache = ByteBuffer.allocate (_neededLen); // TODO be smarter ?
	for (int i=0; i<available; i++) _cache.put (buffer.get ());
    }

    private Frame chunkDataFrame_init (Frame frame, ByteBuffer buffer, int available) throws ConnectionError {
	if (frame.type () != DataFrame.TYPE) return null;
	DataFrame df = (DataFrame) frame;
	if (df.paddedFlag ()) return null;
	_state = STATE_CHUNKING_DATA;
	if (available == 0){
	    _frameInFlight = frame;
	    return null;
	} else {
	    return chunkDataFrame_next (df, buffer, available);
	}
    }
    private Frame chunkDataFrame_next (Frame frame, ByteBuffer buffer, int available) throws ConnectionError {
	_frameInFlight = Frame.newFrame (DataFrame.TYPE);
	_frameInFlight.set (frame.flags (), frame.streamId ());
	_neededLen -= available;
	frame.flags (0x0); // unset endStreamFlag
	((DataFrame)frame).needsWindowUpate (!((DataFrame)_frameInFlight).endStreamFlag ());
	return setPayload (frame, buffer, available);
    }

    private static Frame setPayload (Frame frame, ByteBuffer buffer, int len){
	ByteBuffer payload = buffer.duplicate ();
	payload.limit (payload.position () + len);
	buffer.position (buffer.position () + len);
	return frame.payload (payload, false);
    }
    

    private abstract static class State {
	public abstract Frame read (FrameReader reader, ByteBuffer buffer) throws ConnectionError;
    }

    private static State STATE_INIT = new State (){
	    public Frame read (FrameReader reader, ByteBuffer buffer) throws ConnectionError{
		BufferedByteBuffer bbuffer = reader._bbuffer;
		if (bbuffer.set (buffer) == false) return null;
		// else we now know that we have 9 bytes ready
		
		int len = (bbuffer.get () & 0xFF) << 16;
		len |= (bbuffer.get () & 0xFF) << 8;
		len |= (bbuffer.get () & 0xFF);
	    
		if (len > reader._settings.MAX_FRAME_SIZE)
		    // could send a StreamError for stream frames, but this is not worth the complexity
		    throw new ConnectionError (Http2Error.Code.FRAME_SIZE_ERROR,
					       "Frame too large : "+len+" > "+reader._settings.MAX_FRAME_SIZE);
	    
		Frame frame = Frame.newFrame (bbuffer.get () & 0xFF) //type
		    .set (bbuffer.get () & 0xFF, //flags
			  bbuffer.getInt () & 0x7F_FF_FF_FF  //streamId
			  );

		// we are now done with the intial 9 bytes
		bbuffer.reset ();
	    
		int available = Math.min (len, buffer.remaining ());
		
		if (len == available){
		    // all the payload is available
		    return reader.setPayload (frame, buffer, len);
		}

		reader._neededLen = len;
		
		Frame dataChunk = reader.chunkDataFrame_init (frame, buffer, available);
		if (reader._frameInFlight != null) return dataChunk; // else we did not activate chunking

		reader._frameInFlight = frame;
		if (available > 0){
		    reader.initCache (buffer, available); // _neededLen is well set
		    reader._neededLen -= available;
		    reader._state = STATE_BUFFERING_WITH_CACHE;
		} else {
		    reader._state = STATE_BUFFERING_NO_CACHE;
		}
		return null;
	    }
	};
    private static State STATE_BUFFERING_NO_CACHE = new State (){
	    public Frame read (FrameReader reader, ByteBuffer buffer) throws ConnectionError{
		int available = Math.min (reader._neededLen, buffer.remaining ());
		if (reader._neededLen == available){ // frame is ready
		    try{
			return setPayload (reader._frameInFlight, buffer, available);
		    } finally {
			reader._frameInFlight = null;
			reader._state = STATE_INIT;
		    }
		} else {
		    reader.initCache (buffer, available); // _neededLen is well set at this stage
		    reader._neededLen -= available;
		    reader._state = STATE_BUFFERING_WITH_CACHE;
		}
		return null;
	    }
	 };
    private static State STATE_BUFFERING_WITH_CACHE = new State (){
	    public Frame read (FrameReader reader, ByteBuffer buffer) throws ConnectionError{
		int available = Math.min (reader._neededLen, buffer.remaining ());
		ByteBuffer cache = reader._cache;
		for (int i=0; i<available; i++) cache.put (buffer.get ());
		reader._neededLen -= available;
		if (reader._neededLen == 0){ // frame is ready
		    try{
			cache.flip ();
			return reader._frameInFlight.payload (cache, false);
		    } finally {
			reader._frameInFlight = null;
			reader._state = STATE_INIT;
		    }
		}
		return null;
	    }
	 };
    private static State STATE_CHUNKING_DATA = new State (){
	    public Frame read (FrameReader reader, ByteBuffer buffer) throws ConnectionError{
		int available = Math.min (reader._neededLen, buffer.remaining ());
		if (reader._neededLen == available){
		    try{
			return setPayload (reader._frameInFlight, buffer, available);
		    } finally {
			reader._frameInFlight = null;
			reader._state = STATE_INIT;
		    }
		} else {
		    return reader.chunkDataFrame_next (reader._frameInFlight, buffer, available);
		}
	    }
	 };
    
    private static class BufferedByteBuffer { // this is a utility class to buffer the first 9 bytes if needed
	private byte[] _store;
	private ByteBuffer _buffer;
	private int _windex, _rindex;
	private int _size;
	private State _state = STATE_INIT;
	private BufferedByteBuffer (int size){
	    _size = size;
	}
	private void init (){
	    if (_store == null)
		_store = new byte[_size];
	}
	private void reset (){ // ready for a new frame
	    _state = STATE_INIT;
	    _windex = _rindex = 0;
	}
	private boolean set (ByteBuffer buffer){
	    return _state.set (this, buffer);
	}
	private byte get (){
	    return _state.get (this);
	}
	private int getInt (){
	    return _state.getInt (this);
	}
	
	protected abstract static class State {
	    protected abstract boolean set (BufferedByteBuffer bbb, ByteBuffer buffer);	    
	    protected byte get (BufferedByteBuffer bbb){throw new IllegalStateException ();}
	    protected int getInt (BufferedByteBuffer bbb){throw new IllegalStateException ();}
	}
	protected static State STATE_INIT = new State (){
		protected boolean set (BufferedByteBuffer bbb, ByteBuffer buffer){
		    bbb._buffer = buffer;
		    int remaining = buffer.remaining ();
		    if (remaining >= bbb._size){
			bbb._state = STATE_READ_BYTE_BUFFER;
			return true;
		    }
		    if (remaining == 0) return false;
		    bbb.init ();
		    buffer.get (bbb._store, 0, remaining);
		    bbb._windex += remaining;
		    bbb._state = STATE_FILLING_STORE;
		    return false;
		}
	    };
	protected static State STATE_READ_BYTE_BUFFER = new State (){
		protected boolean set (BufferedByteBuffer bbb, ByteBuffer buffer){
		    bbb._buffer = buffer;
		    return true;
		}
		protected byte get (BufferedByteBuffer bbb){
		    return bbb._buffer.get ();
		}
		protected int getInt (BufferedByteBuffer bbb){
		    return bbb._buffer.getInt ();
		}
	    };
	protected static State STATE_FILLING_STORE = new State (){
		protected boolean set (BufferedByteBuffer bbb, ByteBuffer buffer){
		    bbb._buffer = buffer;
		    int remaining = buffer.remaining ();
		    if (remaining == 0) return false;
		    int needed = bbb._size - bbb._windex;
		    int read = Math.min (needed, remaining);
		    buffer.get (bbb._store, bbb._windex, read);
		    if (needed == read){
			bbb._state = STATE_READ_STORE;
			return true;
		    } else {
			bbb._windex += read;
			return false;
		    }
		}
	    };
	protected static State STATE_READ_STORE = new State (){
		protected boolean set (BufferedByteBuffer bbb, ByteBuffer buffer){
		    bbb._buffer = buffer;
		    return true;
		}
		protected byte get (BufferedByteBuffer bbb){
		    byte b = bbb._store[bbb._rindex++];
		    if (bbb._rindex == bbb._size)
			bbb._state = STATE_READ_BYTE_BUFFER;
		    return b;
		}
		protected int getInt (BufferedByteBuffer bbb){
		    int i = bbb.get () & 0xFF;
		    i = (i << 8) | (bbb.get () & 0xFF);
		    i = (i << 8) | (bbb.get () & 0xFF);
		    i = (i << 8) | (bbb.get () & 0xFF);
		    return i;
		}
	    };
    }
    
    
}
