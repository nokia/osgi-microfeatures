// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux;

import java.nio.ByteBuffer;

/**
 * Class used by client/server jvms, when connecting/accepting simple mux
 * connections (mux data only)
 */
public abstract class SimpleMuxHandler
{

    /**
     * Called when a connection is connected or not
     * 
     * @param connection The connection.
     * @param error The error if the connection failed.
     */
    public void muxConnected(MuxConnection connection, Throwable error)
    {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * 
     * @param connection The connection.
     * @param error The error.
     */
    public void muxAccepted(MuxConnection connection, Throwable error)
    {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * 
     * @param connection The connection.
     * @param header The header.
     * @param message The message.
     */
    public void muxData(MuxConnection connection, MuxHeader header, ByteBuffer message)
    {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * 
     * @param connection The connection.
     * @param header The header.
     * @param data The data.
     * @param off The offset.
     * @param len The length.
     */
    public void muxData(MuxConnection connection, MuxHeader header, byte[] data, int off, int len)
    {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * Called when a connection is closed.
     * 
     * @param connection The disconnected connection.
     */
    public abstract void muxClosed(MuxConnection connection);
}
