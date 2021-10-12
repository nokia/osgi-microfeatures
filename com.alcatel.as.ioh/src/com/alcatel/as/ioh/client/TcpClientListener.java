// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.client;

import java.util.*;
import java.net.*;
import java.nio.*;
import com.alcatel.as.ioh.*;

import alcatel.tess.hometop.gateways.reactor.TcpChannelListener;

public interface TcpClientListener {

    public TcpChannelListener connectionEstablished (TcpClient client, TcpClient.Destination destination);

    public void connectionFailed (TcpClient client, TcpClient.Destination destination);
}