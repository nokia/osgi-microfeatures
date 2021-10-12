// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.jetty.webconnector;

import java.io.OutputStream;

public interface WebEndPoint {

    /**
     * Return codes
     */
    final static int CHANNEL_OK = 0;
    final static int CHANNEL_CLOSED = 1;
    final static int CHANNEL_OVERLOADED = 2;

    /**
     * Get the end point output stream
     * @return the stream to write requests
     */
    public OutputStream geOutputStream();
    
    /**
     * Get the state of the end point (OK, closed, overlaoded)
     * @return the state
     */
    public int getState(); 
    
    /**
     * Close the end point
     */
    public void terminate();
    
}
