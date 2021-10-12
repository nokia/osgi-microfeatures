// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.socket;

import java.nio.ByteBuffer;

/**
 * Interface for TCP message parsers.
 */
public interface TcpMessageParser
{

    /**
     * Parses a chunk of TCP message.
     * 
     * @param buffer The message to parse
     * @return The size of the message which has been fully parsed, or the missing
     *         bytes (negative number) if the message could not be fully parsed.
     */
    int parse(ByteBuffer buffer);
}
