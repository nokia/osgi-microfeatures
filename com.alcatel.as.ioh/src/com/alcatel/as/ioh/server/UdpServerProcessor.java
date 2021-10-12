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

public interface UdpServerProcessor {
    
    public void serverCreated (UdpServer server);

    public void serverDestroyed (UdpServer server);
    
    public void serverOpened (UdpServer server);

    public void serverFailed (UdpServer server, Object cause);
    
    public void serverUpdated (UdpServer server);
    
    public void serverClosed (UdpServer server);

    public UdpChannelListener getChannelListener (UdpChannel cnx);

}
