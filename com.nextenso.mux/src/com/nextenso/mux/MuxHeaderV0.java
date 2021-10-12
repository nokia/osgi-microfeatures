// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux;

/**
 * This is the Version-0 of the MuxHeader.
 * <p/>
 * The fields are in the order:<br/>
 * <ul>
 * <li>the flags (1 byte)
 * <li>the length (2 bytes)
 * <li>the sessionId (8 bytes)
 * <li>the channelId (4 bytes)
 * </ul>
 * <br/>
 * The length is not part of the Header. The length is directly determined by
 * the size of the data sent along with the Header.
 */
public class MuxHeaderV0 implements MuxHeader
{

    private long _sessionId;
    private int _channelId, _flags;

    /**
     * Constructor for this class. 
     */
    public MuxHeaderV0()
    {
    }

    public void set(long sessionId, int channelId, int flags)
    {
        _sessionId = sessionId;
        _channelId = channelId;
        _flags = flags;
    }

    /**
     * @see com.nextenso.mux.MuxHeader#getVersion()
     */
    public int getVersion()
    {
        return 0;
    }

    /**
     * @see com.nextenso.mux.MuxHeader#getSessionId()
     */
    public long getSessionId()
    {
        return _sessionId;
    }

    /**
     * @see com.nextenso.mux.MuxHeader#getChannelId()
     */
    public int getChannelId()
    {
        return _channelId;
    }

    /**
     * @see com.nextenso.mux.MuxHeader#getFlags()
     */
    public int getFlags()
    {
        return _flags;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Version=").append(getVersion());
        sb.append(",SessionId=").append(getSessionId());
        sb.append(",ChannelId=").append(getChannelId());
        sb.append(",Flags=").append(getFlags());
        return sb.toString();
    }
}
