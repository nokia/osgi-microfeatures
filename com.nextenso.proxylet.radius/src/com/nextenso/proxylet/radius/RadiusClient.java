// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.radius;

import java.util.Enumeration;

/**
 * This interface is used to perform radius access and accounting requests.
 * <p/>
 * An instance can be retrieved via
 * <code>RadiusClientFactory.newRadiusClient(String server, byte[] secret)</code>
 * . <br/>
 * Note that the client is not thread safe.
 */
public interface RadiusClient {

	/**
	 * Performs an accounting request. <br/>
	 * Puts all the specified attributes in the request, performs it and returns
	 * the server response code (5 for accounting response) or -1 (request
	 * failed). <br/>
	 * The response attributes can be obtained via
	 * <code>getResponseAttributes()</code>.
	 * 
	 * @param attributes The request attributes.
	 * @return The server response code (5) or -1 if the request failed.
	 */
	public int doAccounting(RadiusAttribute[] attributes);

	/**
	 * Performs an accounting request, specifying a request code. <br/>
	 * The response attributes can be obtained via
	 * <code>getResponseAttributes()</code>.
	 * 
	 * @param attributes The request attributes.
	 * @param code The request code
	 * @return The server response code or -1 if the request failed.
	 */
	public int doAccounting(RadiusAttribute[] attributes, int code);
	
	/**
	 * Performs an accounting request asynchronously, specifying a request code. <br/>
	 * 
	 * @param attributes The request attributes.
	 * @param code The request code
	 * @param listener The listener to be notified when the response is known.
	 */
	public void doAccounting(RadiusAttribute[] attributes, int code, RadiusClientListener listener);

	/**
	 * Performs an accounting request asynchronously.
	 * 
	 * @param attributes The request attributes.
	 * @param listener The listener to be notified when the response is known.
	 */
	public void doAccounting(RadiusAttribute[] attributes, RadiusClientListener listener);

	/**
	 * Performs an access request. <br/>
	 * Puts all the specified attributes in the request, performs it and returns
	 * the server response code (2 for accept, 3 for reject or 11 for challenge)
	 * or -1 (request failed). <br/>
	 * The response attributes can be obtained via
	 * <code>getResponseAttributes()</code>.
	 * 
	 * @param password The password.
	 * @param attributes The request attributes.
	 * @return The server response code (2, 3 or 11) or -1 if the request failed.
	 */
	public int doAccess(byte[] password, RadiusAttribute[] attributes);

	/**
	 * Performs an access request asynchronously.
	 * 
	 * @param password The password.
	 * @param attributes The request attributes.
	 * @param listener The listener to be notified when the response is known.
	 */
	public void doAccess(byte[] password, RadiusAttribute[] attributes, RadiusClientListener listener);

	/**
	 * Gets the response attributes.
	 * 
	 * @return The response attributes, or <code>null</code> if the request
	 *         failed.
	 */
	public Enumeration getResponseAttributes();

}
