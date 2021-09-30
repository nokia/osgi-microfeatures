package com.alcatel.as.ioh.lb;

import java.nio.ByteBuffer;

public class StreamingParserBuffer implements Parser {

    protected Parser _wrapped;
    protected int _size;
    protected ByteBuffer _buffer;
    protected Chunk _cache;

    public StreamingParserBuffer (Parser wrapped, int size){
	_wrapped = wrapped;
	_size = size;
    }

    public Chunk parse (java.nio.ByteBuffer buffer){
	if (_cache != null){
	    try{
		return _cache;
	    }finally{
		_cache = null;
	    }
	}
	
	Chunk chunk = _wrapped.parse (buffer);
	if (chunk == null) return null;

	if (chunk.newMessage ()){
	    if (_buffer != null)
		throw new RuntimeException ("Message too short : cannot bufferize "+_size+" bytes");
	    int size = chunk.getData ().remaining ();
	    if (size >= _size) return chunk; // enough in this first message
	    _buffer = ByteBuffer.allocate (_size);
	    _buffer.put (chunk.getData ()); // not enough in this first message
	    // note : if !copy chunk.getData().compact() should make sense but is not needed with LengthParserFactory which uses duplicates
	    return null;
	}
	if (_buffer == null) // subsequent chunk : first was already given away
	    return chunk;
	
	ByteBuffer src = chunk.getData ();
	int size = _buffer.limit () - _buffer.position ();
	int read = Math.min (size, src.remaining ());
	for (int i=0; i<read; i++) _buffer.put (src.get ());
	if (read == size){
	    // we read enough
	    _buffer.flip ();
	    Chunk first = new Chunk (true).setData (_buffer, true);
	    _buffer = null;
	    if (src.remaining () > 0)
		_cache = chunk; // dont need to copy (thanks to the parse loop)
	    // else if !copy src.compact() // makes sense but not needed
	    return first;
	} // else if !copy src.compact() // makes sense but not needed
	return null; // first is not yet ready - not read enough
    }
}
