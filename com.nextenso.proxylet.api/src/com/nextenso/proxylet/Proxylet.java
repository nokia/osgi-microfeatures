// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet;

/**
 * This interface is implemented by all proxylets.
 * <p/>
 * It contains the integration methods into the proxylet engine (mainly
 * initialization and removal).
 */
public interface Proxylet {

	/**
	 * Called by the Engine to indicate to a proxylet that it is being placed into
	 * service. <br/>
	 * The Engine calls the init method exactly once after instantiating the
	 * proxylet. The init method must complete successfully before the proxylet is
	 * asked to do any work.
	 * 
	 * @param configuration The object to read the configuration from.
	 * @throws ProxyletException if the initialization failed.
	 */
	public void init(ProxyletConfig configuration)
		throws ProxyletException;

	/**
	 * Called by the Engine to indicate to a proxylet that it is being taken out
	 * of service. <br/>
	 * Once the Engine has called this method, it will not ask the proxylet do any
	 * more work.
	 */
	public void destroy();

	/**
	 * Returns information about the proxylet, such as author, copyright, version.
	 * 
	 * @return the proxylet description.
	 */
	public String getProxyletInfo();

}
