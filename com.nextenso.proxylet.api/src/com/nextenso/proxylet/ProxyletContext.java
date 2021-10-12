// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import com.nextenso.proxylet.event.ProxyletContextEvent;
import com.nextenso.proxylet.event.ProxyletContextListener;
import com.nextenso.proxylet.mgmt.Monitor;

/**
 * This interface encapsulates the context in which a proxylet is deployed.
 * <p/>
 * A proxylet container may contain several contexts.
 */
public interface ProxyletContext {

	/**
	 * 
	 * Resumes a suspended proxylet
	 * 
	 * @param msg The message
	 * @param status The status
	 * @deprecated use {@link ProxyletData#resume(int)}
	 */
	@Deprecated
	public void resume(ProxyletData msg, int status);

	/**
	 * Gets the value of the specified initialization parameter. <br/>
	 * The ProxyletContext initialization parameters are included in the
	 * ProxyletConfig objects passed to the Proxylets at initialization.
	 * 
	 * @param name the parameter name.
	 * @return the parameter value, or <code>null</code> if not set.
	 */
	public String getInitParameter(String name);

	/**
	 * Gets the names of the initialization parameters.
	 * 
	 * @return an Enumeration of the parameter names.
	 */
	public Enumeration getInitParameterNames();

	/**
	 * Gets the engine major version.
	 * 
	 * @return The engine major version.
	 */
	public int getMajorVersion();

	/**
	 * Gets the engine minor version.
	 * 
	 * @return The engine minor version.
	 */
	public int getMinorVersion();

	/**
	 * Gets the engine description.
	 * 
	 * @return The engine description.
	 */
	public String getServerInfo();

	/**
	 * Fires an event that all registered listeners will get. <br/>
	 * This feature enables asynchronous and synchronous context-wide event
	 * notifications. <br/>
	 * A ProxyletContextEvent object will be automatically constructed using the
	 * specified source and the current ProxyletContext.
	 * 
	 * @param source The source of the event.
	 * @param asynchronous true if asynchronous; false if synchronous
	 * @see ProxyletContext#registerProxyletContextListener(ProxyletContextListener)
	 */
	public void fireProxyletContextEvent(Object source, boolean asynchronous);

	/**
	 * Fires an event that all registered listeners will get.
	 * 
	 * @param event The event
	 * @param asynchronous true if asynchronous; false if synchronous
	 * @see com.nextenso.proxylet.ProxyletContext#fireProxyletContextEvent(java.lang.Object,
	 *      boolean)
	 * @see ProxyletContext#registerProxyletContextListener(ProxyletContextListener)
	 */
	public void fireProxyletContextEvent(ProxyletContextEvent event, boolean asynchronous);

	/**
	 * Registers a listener.
	 * 
	 * @param listener The listener to add to the list of listeners.
	 */
	public void registerProxyletContextListener(ProxyletContextListener listener);

	/**
	 * Unregisters a listener.
	 * 
	 * @param listener the listener to remove from the list of
	 *          listeners.
	 */
	public void deregisterProxyletContextListener(ProxyletContextListener listener);

	/**
	 * Specifies if the debug Log is enabled.
	 * 
	 * @return true if enabled; false if disabled.
	 */
	public boolean isDebugEnabled();

	/**
	 * Specifies if the info Log is enabled.
	 * 
	 * @return true if enabled; false if disabled.
	 */
	public boolean isLogEnabled();

	/**
	 * Specifies if the warn Log is enabled.
	 * 
	 * @return true if enabled; false if disabled.
	 */
	public boolean isWarnEnabled();

	/**
	 * Logs a message into the debug Log. <br/>
	 * <code>isDebugEnabled()</code> should be called prior to instantiating the
	 * message.
	 * 
	 * @param message The message to log.
	 */
	public void debug(String message);

	/**
	 * Logs a message and a Throwable into the debug Log. <br/>
	 * <code>isDebugEnabled()</code> should be called prior to instantiating the
	 * message.
	 * 
	 * @param message the message to log.
	 * @param t the Throwable to log.
	 */
	public void debug(String message, Throwable t);

	/**
	 * Logs a message into the warn Log. <br/>
	 * <code>isWarnEnabled()</code> should be called prior to instantiating the
	 * message.
	 * 
	 * @param message the message to log.
	 */
	public void warn(String message);

	/**
	 * Logs a message and a Throwable into the warn Log. <br/>
	 * <code>isWarnEnabled()</code> should be called prior to instantiating the
	 * message.
	 * 
	 * @param message the message to log.
	 * @param t the Throwable to log.
	 */
	public void warn(String message, Throwable t);

	/**
	 * Logs a message into the info Log. <br/>
	 * <code>isLogEnabled()</code> should be called prior to instantiating the
	 * message.
	 * 
	 * @param message the message to log.
	 */
	public void log(String message);

	/**
	 * Logs a message and a Throwable into the info Log. <br/>
	 * <code>isLogEnabled()</code> should be called prior to instantiating the
	 * message.
	 * 
	 * @param message the message to log.
	 * @param t the Throwable to log.
	 */
	public void log(String message, Throwable t);

	/**
	 * Gets the context name as specified in the deployment.
	 * 
	 * @return The name.
	 */
	public String getProxyletContextName();

	/**
	 * Gets the specified attribute.
	 * 
	 * @param name The attribute name.
	 * @return The attribute value, or <code>null</code> if the attribute is not
	 *         set.
	 */
	public Object getAttribute(String name);

	/**
	 * Gets all  the attribute names.
	 * 
	 * @return An enumeration of the attribute names.
	 */
	public Enumeration getAttributeNames();

	/**
	 * Sets an attribute.
	 * 
	 * @param name The attribute name.
	 * @param value The attribute value.
	 */
	public void setAttribute(String name, Object value);

	/**
	 * Removes an attribute.
	 * 
	 * @param name The attribute name.
	 * @return The attribute value that was removed, or <code>null</code> if the
	 *         attribute was not set.
	 */
	public Object removeAttribute(String name);

	/**
	 * Gets a resource as a stream.
	 * 
	 * @param name The resource name.
	 * @return The resource as an InputStream.
	 * @throws IOException if an I/O error occurs.
	 */
	public InputStream getResourceAsStream(String name)
		throws IOException;

	/**
	 * Gets the monitor associated to this context.
	 * 
	 * @return the Monitor.
	 * @internal
	 */
	public Monitor getMonitor();
}
