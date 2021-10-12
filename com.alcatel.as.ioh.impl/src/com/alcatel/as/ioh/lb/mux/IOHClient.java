// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb.mux;

import org.apache.log4j.Logger;
import java.util.List;
import java.nio.ByteBuffer;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.lb.Chunk;

public interface IOHClient {

    public long getId ();

    public Logger getLogger ();

    public PlatformExecutor getExecutor ();
    
    public SimpleMonitorable getMeters ();
    
    public IOHChannel getIOHChannel ();

    public void attach (Object[] attachment);
    
    public Object[] attachment ();

    public <T> T attachment (int index);

    public void sendToClient (boolean copy, java.nio.ByteBuffer data);
    public void sendToClient (MuxClient fromServer, Chunk chunk);

    public void sendToDestination (MuxClient agent, Chunk chunk, boolean checkBuffer);
    public void sendToDestination (MuxClient agent, boolean copy, ByteBuffer data);
    
}
