// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.server;

import java.util.*;
import java.net.*;
import java.nio.*;

import com.alcatel.as.ioh.*;

import alcatel.tess.hometop.gateways.reactor.*;

public interface TcpServerProcessor {

    public void serverCreated (TcpServer server);

    public void serverDestroyed (TcpServer server);
    
    public void serverOpened (TcpServer server);

    public void serverFailed (TcpServer server, Object cause);

    public void serverUpdated (TcpServer server);

    public void serverClosed (TcpServer server);

    public void connectionAccepted (TcpServer server, TcpChannel client, Map<String, Object> props);

    public TcpChannelListener getChannelListener (TcpChannel channel);

    public default void closeConnection (TcpChannel client){ client.close (); }
}
