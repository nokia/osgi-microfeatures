// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.common;

import java.nio.ByteBuffer;

public interface ClientContext {

    public ServerContext getServerContext ();
	
    public void send (ByteBuffer data, boolean copy);
    
    public void close();

    public void setSuspendTimeout (long timeOut);
    
}
