// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux;

/**
 * This is the interface implemented by all types of MuxHeaders.
 * <p/>The minimum information to provide is the mux version.
 */
public interface MuxHeader
{

    public int getVersion();

    public long getSessionId();

    public int getFlags();

    public int getChannelId();

}
