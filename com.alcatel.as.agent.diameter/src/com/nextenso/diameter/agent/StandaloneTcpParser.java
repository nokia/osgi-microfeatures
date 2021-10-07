package com.nextenso.diameter.agent;

import java.nio.ByteBuffer;

import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.socket.TcpMessageParser;

/**
 * Class used to parse sip message in standalone containers.
 * This class must be recorded in the SipAgent MuxHandler configuration.
 * @see MuxHandler#CONF_TCP_PARSER
 */
public class StandaloneTcpParser implements TcpMessageParser
{
    private final static StandaloneTcpParser _instance = new StandaloneTcpParser();

    public static StandaloneTcpParser getInstance()
    {
        return _instance;
    }

    /**
     * @see com.nextenso.mux.socket.TcpMessageParser#parse(java.nio.ByteBuffer)
     */
    @Override
    public int parse(ByteBuffer buffer) {
	buffer.mark();
	try {
	    if (buffer.remaining() < 4)
		return buffer.remaining() - 4;
	    buffer.get();
	    int size = buffer.get() & 0xFF;
	    size <<= 8;
	    size |= buffer.get() & 0xFF;
	    size <<= 8;
	    size |= buffer.get() & 0xFF;
	    if (size < 20){
		// this is not a valid diameter message
		// see #DCTPD00764438
		throw new RuntimeException ("Invalid diameter message : length="+size);
	    }
	    int needed = size - 4;
	    if (buffer.remaining() >= needed) {
		return size;
	    }
	    return buffer.remaining() - needed;
	}
	finally {
	    buffer.reset();
	}
    }
}
