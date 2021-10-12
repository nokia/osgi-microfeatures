// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb;

import com.alcatel.as.ioh.client.TcpClient.Destination;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface DestinationManager {

    public Destination getAny ();

    public Destination get (int hash);
}