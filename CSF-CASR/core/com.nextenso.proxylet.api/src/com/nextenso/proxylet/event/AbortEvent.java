package com.nextenso.proxylet.event;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.event.ProxyletEvent;

/**
 * This class encapsulates an abort event which means that the request is dropped.
 */
public class AbortEvent extends ProxyletEvent{
    /**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

		/**
     * Constructs a new AbortEvent.
     * @param src the event source
     * @param data the Proxylet data
     */
	public AbortEvent(Object src, ProxyletData data){
	super(src, data);
    }
}
