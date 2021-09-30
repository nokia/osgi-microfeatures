package com.nextenso.proxylet.http.event;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.event.ProxyletEvent;

/**
 * This class encapsulates a timeout event.
* <p/>A timeout event occurs when a request times out.
* <br/>The Agent may or may not use it instead of AbortEvent.
 */
public class ServiceTimeoutEvent extends AbortEvent {
    
    /**
     * Constructs a new ServiceTimeoutEvent.
     * @param source the source of the event, the ProxyletContext of the request if fired by the Agent.
     * @param data the ProxyletData involved, may be an HttpRequest or an HttpResponse.
     */
    public ServiceTimeoutEvent (Object source, ProxyletData data){
	super(source, data);
    }
        
        
}
