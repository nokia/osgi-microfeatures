// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux;

/**
 * This is the Version-3 of the MuxHeader.
 * <p/>
 * <b>Used for FastCache ONLY</b> - may be removed in future release. <br/>
 * It sends the same information as MuxHeaderV0, but does not break the data
 * into small chunks.
 */
public class MuxHeaderV3 extends MuxHeaderV0
{

    /**
     * Constructor for this class.
     */
    public MuxHeaderV3()
    {
    }

    /**
     * @see com.nextenso.mux.MuxHeaderV0#getVersion()
     */
    @Override
    public int getVersion()
    {
        return 3;
    }

}
