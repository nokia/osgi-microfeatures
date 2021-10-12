// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl.ioh;

import java.nio.ByteBuffer;

import com.nextenso.mux.*;

public abstract class ExtendedMuxHandler extends MuxHandler {

    // the following does not exist in MuxHandler but may be useful in ioh
    
    public void tcpSocketData(MuxConnection connection,
			      int sockId,
			      long sessionId,
			      java.nio.ByteBuffer[] data){
    }

    public void udpSocketData(MuxConnection connection,
			      int sockId,
			      long sessionId,
			      String remoteIP,
			      int remotePort,
			      String virtualIP,
			      int virtualPort,
			      ByteBuffer[] buff){
    }

    public void sctpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer[] data, String addr,
                               boolean isUnordered, boolean isComplete, int ploadPID, int streamNumber){
	
    }

    public void internalMuxData (MuxConnection connection,
				 MuxHeader header,
				 ByteBuffer data){}
}
