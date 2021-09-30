package com.nextenso.proxylet.event;

import com.nextenso.proxylet.ProxyletData;

/**
 * An event sent when a message is flushed out.
 */
public class FlushEvent extends ProxyletEvent {

    public FlushEvent (Object src, ProxyletData data){
	super (src, data);
    }

}
