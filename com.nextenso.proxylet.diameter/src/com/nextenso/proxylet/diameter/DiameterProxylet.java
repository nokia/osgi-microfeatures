// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter;

import com.nextenso.proxylet.Proxylet;

/**
 * This interface contains the features common to DiameterRequestProxylet and
 * DiameterResponseProxylet.
 */
public interface DiameterProxylet
		extends Proxylet {

	/**
	 * The configuration parameter indicating the lifetime of a session in
	 * seconds. <br/>
	 * A value of '0' stands for 'stateless' (no session maintained).
	 */
	public static final String CONFIG_SESSION_LIFETIME = "config.session.lifetime";

	/**
	 * The configuration parameter indicating the diameter application Id. <br/>
	 * A value of '0-1' stands for 'any'.
	 */
	public static final String CONFIG_APPLICATION_ID = "config.application.id";

	/**
	 * The configuration parameter indicating the local OriginHost.
	 */
	public static final String CONFIG_ORIGIN_HOST = "config.origin.host";

	/**
	 * The configuration parameter indicating the local OriginRealm.
	 */
	public static final String CONFIG_ORIGIN_REALM = "config.origin.realm";

	/**
	 * Possible return code of method <code>accept</code>: the proxylet accepts
	 * the message and will not block while processing it.
	 */
	public static final int ACCEPT = 1;

	/**
	 * Possible return code of method <code>accept</code>: the proxylet accepts
	 * the message but may block while processing it.
	 */
	public static final int ACCEPT_MAY_BLOCK = 2;

	/**
	 * Possible return code of method <code>accept</code>: the proxylet does not
	 * want to process the message.
	 */
	public static final int IGNORE = 3;

}
