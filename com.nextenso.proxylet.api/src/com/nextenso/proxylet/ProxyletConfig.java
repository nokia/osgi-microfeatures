// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet;

import java.util.Enumeration;

import com.nextenso.proxylet.event.ProxyletConfigListener;

/**
 * This interface encapsulates the configuration passed to the proxylet at
 * initialization.
 */
public interface ProxyletConfig {

	/**
	 * Gets the String value of an initialization parameter.
	 * 
	 * @param name The name of the parameter to look up.
	 * @return The parameter value.
	 * @throws ProxyletException if the parameter is not found.
	 */
	public String getStringParameter(String name)
		throws ProxyletException;

	/**
	 * Gets the String value of an initialization parameter with a default value. <br/>
	 * The default value is returned if the parameter was not found.
	 * 
	 * @param name The name of the parameter to look up.
	 * @param def The default value.
	 * @return the parameter value.
	 */
	public String getStringParameter(String name, String def);

	/**
	 * Gets the int value of an initialization parameter.
	 * 
	 * @param name The name of the parameter to look up.
	 * @return The parameter value.
	 * @throws ProxyletException if the parameter is not found.
	 */
	public int getIntParameter(String name)
		throws ProxyletException;

	/**
	 * Gets the int value of an initialization parameter with a default value. <br/>
	 * The default value is returned if the parameter was not found.
	 * 
	 * @param name The name of the parameter to look up.
	 * @param def The default value.
	 * @return The parameter value.
	 */
	public int getIntParameter(String name, int def);

	/**
	 * Gets the boolean value of an initialization parameter.
	 * 
	 * @param name The name of the parameter to look up.
	 * @return The parameter value.
	 * @throws ProxyletException if the parameter is not found.
	 */
	public boolean getBooleanParameter(String name)
		throws ProxyletException;

	/**
	 * Gets the boolean value of an initialization parameter with a default value. <br/>
	 * The default value is returned if the parameter was not found.
	 * 
	 * @param name The name of the parameter to look up.
	 * @param def The default value.
	 * @return the parameter value.
	 */
	public boolean getBooleanParameter(String name, boolean def);

	/**
	 * Gets the names of the existing parameters.
	 * 
	 * @return an Enumeration of the parameter names.
	 */
	public Enumeration getParameterNames();

	/**
	 * Gets the context in which the Proxylet is deployed.
	 * 
	 * @return The context.
	 */
	public ProxyletContext getProxyletContext();

	/**
	 * Registers a listener. <br/>
	 * This feature enables an Application to be notified of dynamic configuration
	 * changes.
	 * 
	 * @param listener the ProxyletConfigListener to add to the list of listeners.
	 * @see com.nextenso.proxylet.ProxyletConfig#deregisterProxyletConfigListener(com.nextenso.proxylet.event.ProxyletConfigListener)
	 */
	public void registerProxyletConfigListener(ProxyletConfigListener listener);

	/**
	 * Unregisters a listener.
	 * 
	 * @param listener The listener to remove from the list of listeners.
	 * @see com.nextenso.proxylet.ProxyletConfig#registerProxyletConfigListener(com.nextenso.proxylet.event.ProxyletConfigListener)
	 */
	public void deregisterProxyletConfigListener(ProxyletConfigListener listener);

	/**
	 * Gets the proxylet name as specified in the deployment.
	 * 
	 * @return The proxylet name.
	 */
	public String getProxyletName();

}
