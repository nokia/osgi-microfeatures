// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter;

import java.util.Enumeration;

import com.nextenso.proxylet.ProxyletContext;
import com.nextenso.proxylet.diameter.event.DiameterSessionListener;

/**
 * This class encapsulates a Diameter session.
 * <p/>
 * A session stores a context across multiple requests and is dismissed upon
 * timeout or explicit destruction.
 */
public interface DiameterSession {

	/**
	 * Destroys the session explicitly.
	 */
	public void destroy();

	/**
	 * Specifies if the session is alive or dead (timed out or destroyed).
	 * 
	 * @return true if alive, false if dead.
	 */
	public boolean isAlive();

	/**
	 * Gets the application identifier.
	 * 
	 * @return The application identifier.
	 */
	public long getDiameterApplication();

	/**
	 * Gets the vendor identifier.
	 * 
	 * @return The vendor identifier.
	 */
	public long getDiameterApplicationVendorId();

	/**
	 * Gets the peer which shares this session.
	 * 
	 * @return The peer.
	 */
	public DiameterPeer getDiameterPeer();

	/**
	 * Gets the session identifier (Session-Id).
	 * 
	 * @return The session identifier.
	 */
	public String getSessionId();

	/**
	 * Gets the sender identifier (Sender-Id).
	 * 
	 * @return The sender identifier.
	 */
	public String getSenderId();

	/**
	 * Gets the proxylet context associated to this session.
	 * 
	 * @return The proxylet context.
	 */
	public ProxyletContext getProxyletContext();

	/**
	 * Gets the session lifetime in milliseconds.
	 * 
	 * @return The lifetime in milliseconds.
	 */
	public long getSessionLifetime();

	//public void setSessionLifetime (long value);

	/**
	 * Gets the session state.
	 * 
	 * @return The state.
	 * @see #setState(int) setState
	 */
	public int getState();

	/**
	 * Sets the session state. <br/>
	 * The state is an application-specific value that can be used to track the
	 * state of the session (some Diameter applications define several states for
	 * the sessions). <br/>
	 * The state is ignored by the platform.
	 * 
	 * @param state The state.
	 */
	public void setState(int state);

	/**
	 * Gets the time when this session was created, measured in milliseconds since
	 * midnight January 1, 1970 GMT.
	 * 
	 * @return A <code>long</code> specifying when this session was created,
	 *         expressed in milliseconds since 1/1/1970 GMT
	 */
	public long getCreationTime();

	/**
	 * 
	 * Gets the last time the client sent a request associated with this session,
	 * as the number of milliseconds since midnight January 1, 1970 GMT, and
	 * marked by the time the container received the request.
	 * 
	 * <p>
	 * Actions that your application takes, such as getting or setting a value
	 * associated with the session, do not affect the access time.
	 * 
	 * @return a <code>long</code> representing the last time the client sent a
	 *         request associated with this session, expressed in milliseconds
	 *         since 1/1/1970 GMT
	 */
	public long getLastAccessedTime();

	/**
	 * 
	 * Gets the object bound with the specified name in this session, or
	 * <code>null</code> if no object is bound under the name.
	 * 
	 * @param name A string specifying the name of the object
	 * 
	 * @return The object with the specified name.
	 * 
	 */
	public Object getAttribute(Object name);

	/**
	 * 
	 * Returns an <code>Enumeration</code> of <code>String</code> objects
	 * containing the names of all the objects bound to this session.
	 * 
	 * @return An <code>Enumeration</code> of <code>String</code> objects
	 *         specifying the names of all the objects bound to this session
	 * 
	 */
	public Enumeration getAttributeNames();

	/**
	 * Binds an object to this session, using the name specified. If an object of
	 * the same name is already bound to the session, the object is replaced.
	 * 
	 * <p>
	 * If the value parameter is null, this method has the same effect as calling
	 * <code>removeAttribute()<code>.
	 * 
	 * @param name The name to which the object is bound. It cannot be null.
	 * @param value The object to be bound.
	 */
	public void setAttribute(Object name, Object value);

	/**
	 * Removes and returns the object bound with the specified name from this
	 * session. If the session does not have an object bound with the specified
	 * name, this method does nothing.
	 * 
	 * 
	 * @param name The name of the object to be removed.
	 * @return The removed object, or <code>null</code> if no object was removed.
	 */
	public Object removeAttribute(Object name);

	/**
	 * Removes all the objects bound to this session.
	 */
	public void removeAttributes();

	/**
	 * Adds a session listener.
	 * 
	 * @param listener The listener to be added.
	 */
	public void addSessionListener(DiameterSessionListener listener);
}
