// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent;

import java.nio.ByteBuffer;

import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.socket.TcpMessageParser;

/**
 * Class used to parse HTTP message in stand-alone containers.
 * This class must be recorded in the HttpAgent MuxHandler configuration.
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
      return buffer.remaining(); // No parsing
    }
}
