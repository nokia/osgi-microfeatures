// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.event;

import java.util.EventObject;
import com.nextenso.proxylet.ProxyletContext;

/**
 * A ProxyletContextEvent encapsulates a ProxyletContext-wide event.
 * <p/>
 * It may be overridden to fire specific events to selected listeners.
 */
public class ProxyletContextEvent
		extends EventObject {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	private ProxyletContext _context;

	/**
	 * Constructs a new ProxyletContextEvent.
	 * 
	 * @param src The source.
	 * @param context The involved context.
	 */
	public ProxyletContextEvent(Object src, ProxyletContext context) {
		super(src);
		_context = context;
	}

	/**
	 * Gets the involved context.
	 * 
	 * @return the involved context.
	 */
	public ProxyletContext getProxyletContext() {
		return _context;
	}

}
