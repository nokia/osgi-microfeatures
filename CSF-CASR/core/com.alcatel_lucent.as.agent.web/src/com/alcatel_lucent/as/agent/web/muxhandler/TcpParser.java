package com.alcatel_lucent.as.agent.web.muxhandler;

import java.nio.ByteBuffer;

import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.socket.TcpMessageParser;

/**
 * Class used to parse HTTP message in stand-alone containers.
 * This class must be recorded in the HttpAgent MuxHandler configuration.
 * @see MuxHandler#CONF_TCP_PARSER
 */
public class TcpParser implements TcpMessageParser
{
    private final static TcpParser _instance = new TcpParser();

    public static TcpParser getInstance()
    {
        return _instance;
    }

    /**
     * @see com.nextenso.mux.socket.TcpMessageParser#parse(java.nio.ByteBuffer)
     */
    @Override
    public int parse(ByteBuffer buffer) {
      return buffer.remaining(); // No parsing
    }
}
