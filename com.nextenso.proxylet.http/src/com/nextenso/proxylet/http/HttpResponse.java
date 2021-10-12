// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

/**
 * This Class encapsulates a response.
 */
public interface HttpResponse extends HttpMessage {
	
    /**
     * Proxylet Attribute key for the exception that caused this error response to be generated 
     * internally by the proxylet engine. 
     */
    public final Object ERROR_REASON_ATTR = new Object();
  
    /**
     * Returns the response prolog.
     * @return the prolog.
     */
    public HttpResponseProlog getProlog ();
  
}
