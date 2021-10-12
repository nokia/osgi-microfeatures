// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.socket;

public interface Socket
{
    public static final int TYPE_UDP = 0x1;
    public static final int TYPE_TCP = 0x2;
    public static final int TYPE_TCP_SERVER = 0x12;
    public static final int TYPE_SCTP = 0x3;
    public static final int TYPE_SCTP_SERVER = 0x13;

    public int getSockId();

    public int getType();

    public int getLocalIP();

    public String getLocalIPString();

    public int getLocalPort();

    public boolean close();
}
