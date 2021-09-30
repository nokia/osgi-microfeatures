package com.alcatel.as.http2;

import java.nio.ByteBuffer;
import java.util.*;
import com.alcatel.as.http2.client.SendReqBuffer;

public class SendBuffer implements SendReqBuffer {

    protected int _maxSize, _size;
    protected boolean _done, _closed;
    protected List<ByteBuffer> _buffers;
    protected Stream _stream;
    
    protected SendBuffer (Stream stream, int maxSize){
	_stream = stream;
	_maxSize = maxSize;
    }
    
    public int size (){ return _size;}
    public int maxSize (){ return _maxSize;}

    // throws an IndexOutOfBoundsException if the maxSize is exceeded
    // returns true if all was sent
    public boolean send (ByteBuffer data, boolean copy, boolean done){
	if (_closed) return false;
	_done = done;
	if (data == null || data.remaining () == 0){
	    if (done && _size == 0) _stream.sendData (null, false, true);
	    return _size == 0;
	}	
	if (_size > 0){
	    // already tracking	    
	    store (data, copy);
	    return false;
	}
	int available = _stream.sendFCWindow ();
	if (available > 0){
	    if (available >= data.remaining ()){
		// we can send all now
		_stream.sendData (data, copy, done);
		return true;
	    }
	    ByteBuffer partial = data.duplicate ();
	    partial.limit (partial.position () + available);
	    data.position (data.position () + available);
	    store (data, true); // must copy since the partial is given to the TCP layer
	    _stream.sendData (partial, copy, false); // send AFTER store - else the buffer was given
	} else {
	    store (data, copy);
	}
	track ();
	return false;
    }

    // help the GC when aborting
    public void clear (){
	_buffers.clear ();
	_size = 0;
    }

    private void sendNow (){
	while (_buffers.size () > 0){
	    ByteBuffer data = _buffers.remove (0);
	    boolean last = _buffers.size () == 0;
	    int available = _stream.sendFCWindow ();
	    if (available >= data.remaining ()){
		// we can send all now
		_size -= data.remaining ();
		_stream.sendData (data, false, last && _done);
		continue;
	    }
	    ByteBuffer partial = data.duplicate ();
	    partial.limit (partial.position () + available);
	    data.position (data.position () + available);
	    _size -= partial.remaining ();
	    store (data, true, 0, false);
	    _stream.sendData (partial, false, false);
	    track ();
	    return;
	}
    }
    private void closed (){
	_closed = true;
	clear ();
    }

    private void track (){
	_stream.onWriteAvailable (this::sendNow, this::closed, Long.MAX_VALUE);
    }
    private void store (ByteBuffer data, boolean copy){
	if (_buffers == null) _buffers = new ArrayList<> ();
	store (data, copy, _buffers.size (), true);
    }
    private void store (ByteBuffer data, boolean copy, int pos, boolean incSize){
	if (incSize) _size += data.remaining ();
	if (_size > _maxSize){
	    clear ();
	    throw new java.lang.IndexOutOfBoundsException ("SendRespBuffer max size exceeded");
	}
	_buffers.add (pos, clone (data, copy));
    }

    private static ByteBuffer clone (ByteBuffer buff, boolean copy){
	if (copy){
	    ByteBuffer tmp = ByteBuffer.allocate (buff.remaining ());
	    tmp.put (buff);
	    tmp.flip ();
	    return tmp;
	}
	return buff;
    }
}
