// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.socket;

public interface TcpSocket extends Socket
{

    public long getConnectionId();

    public int getRemoteIP();

    public int getRemotePort();

    public int getVirtualIP();

    public String getVirtualIPString();

    public int getVirtualPort();

    public boolean isSecure();

    public boolean isClientSocket();

}
