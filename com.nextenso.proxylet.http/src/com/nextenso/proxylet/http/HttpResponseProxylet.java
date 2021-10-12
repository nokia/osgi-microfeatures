// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

import com.nextenso.proxylet.Proxylet;

/**
 * The superclass of proxylets that handle Responses.
 */
public interface HttpResponseProxylet extends Proxylet {
  
    /**
     * Possible return code of method 'accept'.
     * <br/>The proxylet accepts the message and will not block while processing it
     */
    public static final int ACCEPT = 0x03;
    /**
     * Possible return code of method 'accept'.
     * the proxylet accepts the message but might block while processing it
     */
    public static final int ACCEPT_MAY_BLOCK = 0x7;
    /**
     * Possible return code of method 'accept'.
     * <br/>The proxylet is not interested in the message
     */
    public static final int IGNORE = 0;
  
    /**
     * Called by the Engine to know how the proxylet will handle the Response.
     * <br/>The possible return codes are: ACCEPT, ACCEPT_MAY_BLOCK, IGNORE.
     * <b>NOTE: This method can be called by the Engine several times in a row for the same request.</b>
     * Therefore it should behave accordingly.
     * @param prolog the response prolog
     * @param headers the response headers
     * @return ACCEPT, ACCEPT_MAY_BLOCK, or IGNORE
     */
    public int accept(HttpResponseProlog prolog, HttpHeaders headers);
  
}
