// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.server;

import java.util.*;

import alcatel.tess.hometop.gateways.reactor.*;

public interface ServerFactory {
    
    TcpServer newTcpServer (TcpServerProcessor proc, Map<String, Object> props);

    UdpServer newUdpServer (UdpServerProcessor proc, Map<String, Object> props);
    
    SctpServer newSctpServer (SctpServerProcessor proc, Map<String, Object> props);

    void newTcpServerConfig (String namespace, String xml);

    void newUdpServerConfig (String namespace, String xml);
    
    void newSctpServerConfig (String namespace, String xml);

    List<Server> getServers (String namespace);
    
}
