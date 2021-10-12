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

public interface SctpServerProcessor {

    public void serverCreated (SctpServer server);

    public void serverDestroyed (SctpServer server);

    public void serverOpened (SctpServer server);

    public void serverFailed (SctpServer server, Object cause);

    public void serverUpdated (SctpServer server);

    public void serverClosed (SctpServer server);

    public void connectionAccepted (SctpServer server, SctpChannel client, Map<String, Object> props);

    public SctpChannelListener getChannelListener (SctpChannel channel);

    public default void closeConnection (SctpChannel client){ client.close (); }
}
