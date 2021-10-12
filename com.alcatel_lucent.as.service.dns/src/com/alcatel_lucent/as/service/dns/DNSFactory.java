// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns;

import org.apache.log4j.Logger;

import com.alcatel.as.util.serviceloader.ServiceLoader;

/**
 * This class is used to create a new instance of a DNSClient or a
 * TelURLResolver.
 */
public abstract class DNSFactory {
	public final static Logger LOGGER = Logger
			.getLogger("dns.factory");

	protected DNSFactory() {
	}

	/**
	 * Returns an instance of current DNSFactory implementation.
	 * 
	 * 
	 * @return an instance of DNSFactory or <code>null</code> if an
	 *         instanciation problem occurred.
	 * 
	 */
	public static DNSFactory getInstance() {
		DNSFactory res = (DNSFactory) ServiceLoader
				.getService(DNSFactory.class.getName());
		if (res == null) {
			LOGGER.warn("getInstance: cannot get an available service for the API com.alcatel_lucent.as.service.dns.DNSFactory");
		}
		return res;
	}

	/**
	 * Returns a new instance of DNS Client.
	 * 
	 * @return A new instance of DNSClient or <code>null</code> if an
	 *         instanciation problem occurred.
	 */
	public abstract DNSClient newDNSClient();

}
