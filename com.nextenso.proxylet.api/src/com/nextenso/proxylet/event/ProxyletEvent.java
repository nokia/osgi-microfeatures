// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.event;

import java.util.EventObject;
import com.nextenso.proxylet.ProxyletData;

/**
 * A ProxyletEvent encapsulates a ProxyletData-wide event.
 * <p/>
 * It may be overridden to fire specific events to selected listeners.
 */
public class ProxyletEvent
		extends EventObject {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private ProxyletData _data;

	/**
	 * Constructs a new ProxyletEvent given the source and the ProxyletData.
	 * 
	 * @param src The source.
	 * @param data The involved data.
	 */
	public ProxyletEvent(Object src, ProxyletData data) {
		super(src);
		_data = data;
	}

	/**
	 * Returns the involved data.
	 * 
	 * @return the involved data.
	 */
	public ProxyletData getProxyletData() {
		return _data;
	}

}
