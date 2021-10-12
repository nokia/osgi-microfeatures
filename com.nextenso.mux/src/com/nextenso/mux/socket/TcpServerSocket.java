// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.socket;

public interface TcpServerSocket extends Socket
{

    public long getListenId();

    public boolean isSecure();
}
