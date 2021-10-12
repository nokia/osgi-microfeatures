// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http.event;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.event.ProxyletEvent;

/**
 * This class encapsulates an abort event which means that the request is interrupted.
 * <p/>Only the Agent should fire an AbortEvent.
 * <br/>The Agent fires the event on both the request and the response, so a maximum number of listeners can catch it.
 */
public class AbortEvent extends ProxyletEvent {
    
    /**
     * Constructs a new AbortEvent.
     * @param source the source of the event, the ProxyletContext of the request if fired by the Agent.
     * @param data the ProxyletData involved, may be an HttpRequest or an HttpResponse.
     */
    public AbortEvent (Object source, ProxyletData data){
	super(source, data);
    }
    
}
