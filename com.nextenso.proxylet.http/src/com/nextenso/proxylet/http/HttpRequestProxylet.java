// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

import com.nextenso.proxylet.Proxylet;

/**
 * The superclass of proxylets that handle Requests.
 */
public interface HttpRequestProxylet extends Proxylet {
  
    /**
     * Possible return code of method 'accept'.
     * <br/>The proxylet accepts the message and will not block while processing it
     */
    public static final int ACCEPT = 0x03;
    /**
     * Possible return code of method 'accept'.
     * <br/>The proxylet accepts the message but might block while processing it
     */
    public static final int ACCEPT_MAY_BLOCK = 0x07 ;
    /**
     * Possible return code of method 'accept'. 
     * <br/>The proxylet is not interested in the message
     */
    public static final int IGNORE = 0;
  
    /**
     * Called by the Engine to know how the proxylet will handle the Request.
     * <br/>The possible return codes are: ACCEPT, ACCEPT_MAY_BLOCK, IGNORE.
     * <b>NOTE: This method can be called by the Engine several times in a row for the same request.</b>
     * Therefore it should behave accordingly.
     * @param prolog the request prolog
     * @param headers the request headers
     * @return ACCEPT, ACCEPT_MAY_BLOCK or IGNORE
     */
    public int accept(HttpRequestProlog prolog, HttpHeaders headers);
    
}
