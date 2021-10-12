// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb;

import com.alcatel.as.ioh.client.TcpClient;


public interface UnicastRouter {

    public void init (Client client);

    // indicates the number of bytes needed for routing
    // return -1 : all message
    // return 0 : no byte needed
    // return N : at least N bytes
    public int neededBuffer ();

    // route TCP unicast
    public TcpClient.Destination route (Client client, Chunk initChunk);
}
