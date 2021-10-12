// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.dns;

import java.lang.reflect.Constructor;

/**
 * This class is used to create a new instance of DNSClient.
 * <p/>
 * The name of the class used for the DNSClient is taken from the System
 * property <i>com.nextenso.proxylet.DNSClient.class</i>
 */
public class DNSClientFactory {

	/**
	 * The System property name indicating the DNSClient implementation.
	 */
	public static final String DNS_CLIENT_CLASS = "com.nextenso.proxylet.DNSClient.class";

	volatile private static Constructor _constructor;
	private final static Object[] EMPTY_ARGS = new Object[0];

	private DNSClientFactory() {}

	/**
	 * Gets a new instance of the current DNSClient implementation.
	 * 
	 * @return a new instance of DNSClient or <code>null</code> if an
	 *         Instantiation problem occurred.
	 */
	public static DNSClient newDNSClient() {
		// we load the constructor only once - when the method is called the first time
		if (_constructor == null) {
			String className = System.getProperty(DNS_CLIENT_CLASS);
			if (className == null) {
				return null;
			}
			try {
				Class dnsClientClass = Class.forName(className);
				_constructor = dnsClientClass.getConstructor(new Class[0]);
			}
			catch (Exception e) {
				_constructor = null;
				return null;
			}
		}
		try {
			return (DNSClient) _constructor.newInstance(EMPTY_ARGS);
		}
		catch (Throwable t) {
			return null;
		}
	}
}
