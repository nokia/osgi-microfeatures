// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client;

import org.osgi.annotation.versioning.ProviderType;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import java.util.concurrent.Executor;


@ProviderType
public interface Http2Request {
    
    public int id ();

    public boolean isClosed ();

    public Http2Connection getConnection ();

    public <T> T attachment ();

    public void attach (Object o);

    // corresponds to the connection writeExecutor
    public Executor requestExecutor ();
    
    // corresponds to the connection readExecutor
    public Executor responseExecutor ();

    // weight between 1 and 256 (inclusive)
    public Http2Request setPriority (boolean exclusive, int streamDepId, int weight);

    public Http2Request setReqMethod (String method);

    public Http2Request setReqPath (String path);

    public Http2Request setReqAuth (String auth);

    public Http2Request setReqScheme (String scheme);

    public Http2Request setReqHeader (String name, String value);

    public Http2Request sendReqHeaders (boolean done);
	
    public Http2Request sendReqData (ByteBuffer data, boolean copy, boolean done);

    public void abort (int code);

    public int sendWindow ();

    public void onWriteAvailable (Runnable success, Runnable failure, long delay);

    public SendReqBuffer newSendReqBuffer (int maxBufferSize);
    
}
