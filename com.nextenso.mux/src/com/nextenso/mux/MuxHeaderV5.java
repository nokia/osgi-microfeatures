// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux;

/**
 * This is the Version-5 of the MuxHeader.
 * <p/>
 * The fields are in the order:<br/>
 * <ul>
 * <li>the flags (1 byte)
 * <li>the length (4 bytes)
 * </ul>
 * <br/>
 * The length is not part of the Header. The length is directly determined by
 * the size of the data sent along with the Header.
 */
public class MuxHeaderV5 implements MuxHeader
{

    protected int _flags;

    /**
     * Constructor for this class. 
     */
    public MuxHeaderV5()
    {
    }

    /**
     * Sets the flags.
     * 
     * @param flags The flags.
     */
    public void set(int flags)
    {
        _flags = flags;
    }

    /**
     * @see com.nextenso.mux.MuxHeader#getVersion()
     */
    public int getVersion()
    {
        return 5;
    }

    /**
     * @see com.nextenso.mux.MuxHeader#getFlags()
     */
    public int getFlags()
    {
        return _flags;
    }

    /**
     * @see com.nextenso.mux.MuxHeader#getSessionId()
     */
    public long getSessionId()
    {
        return 0L;
    }

    /**
     * @see com.nextenso.mux.MuxHeader#getChannelId()
     */
    public int getChannelId()
    {
        return 0;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Version=").append(getVersion());
        sb.append(",Flags=").append(getFlags());
        return (sb.toString());
    }
}
