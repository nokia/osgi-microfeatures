// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.socket;

public interface SctpSocket extends Socket
{
    long getConnectionId();

    String[] getLocalIPs();

    String[] getRemoteIPs();

    int getRemotePort();

    boolean isSecure();

    boolean isClientSocket();

    int maxOutStreams();

    int maxInStreams();
}
