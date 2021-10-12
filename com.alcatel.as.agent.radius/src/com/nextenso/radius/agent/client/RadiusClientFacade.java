// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent.client;

import java.util.Enumeration;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.mux.util.MuxConnectionManager;
import com.nextenso.proxylet.engine.ProxyletUtils;
import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.RadiusClient;
import com.nextenso.proxylet.radius.RadiusClientListener;
import com.nextenso.proxylet.radius.acct.AcctUtils;
import com.nextenso.proxylet.radius.auth.AuthUtils;
import com.nextenso.radius.agent.RadiusProperties;
import com.nextenso.radius.agent.Utils;
import com.nextenso.radius.agent.impl.RadiusServer;

/**
 * The Radius Client implementation..
 */
public class RadiusClientFacade
		implements RadiusClient {

	private static final Logger LOGGER = Logger.getLogger("agent.radius.client");
	private static MuxConnectionManager CONNECTION_MANAGER;
	public static final int IP_LOCAL = MuxUtils.getIPAsInt("127.0.0.1");

	private boolean _isDone = false;
	private RadiusRequest _request;
	private RadiusServer _acctServer, _authServer;
	private final Object lock = new Object();
    
	private String _componentId = null;

	public static void setMuxConnectionManager(MuxConnectionManager manager) {
		CONNECTION_MANAGER = manager;
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param server The server.
	 * @param secret The secret.
	 * @param componentName The component name.
	 * @throws Exception if an error occurs
	 */
	public RadiusClientFacade(String server, byte[] secret, String componentName)
			throws Exception {
		this(server, secret);
		if (componentName == null) {
			throw new Exception("Error, componentId cannot be null in RadiusClient constructor");
		}
		_componentId = componentName;
		ProxyletUtils.maySendRequest(_componentId);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param server The server.
	 * @param secret The secret.
	 */
	public RadiusClientFacade(String server, byte[] secret) {
		String host = null;
		int index = server.indexOf(':');
		int port = -1;
		if (index != -1) {
			host = server.substring(0, index);
			try {
				port = Integer.parseInt(server.substring(index + 1));
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("Invalid radius client definition: invalid host : " + server + " (cannot parse port number)");
			}
		} else {
			host = server;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("RadiusClientFacade: server=" + server + ", host=" + host + ", port=" + port);
		}

		try {
			_acctServer = new RadiusServer(host, (port != -1) ? port : AcctUtils.ACCT_PORT, secret, IP_LOCAL, true);
			host = _acctServer.getHostAddress();
		}
		catch (Exception e1) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("RadiusClient: Failed to instanciate Accounting Server : " + e1.getMessage());
			}
		}

		try {
			_authServer = new RadiusServer(host, (port != -1) ? port : AuthUtils.AUTH_PORT, secret, IP_LOCAL, false);
		}
		catch (Exception e2) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("RadiusClient: Failed to instanciate Access Server : " + e2.getMessage());
			}
		}

		if (_acctServer == null && _authServer == null)
			throw new IllegalArgumentException("Invalid Radius Server definition");
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusClient#doAccounting(com.nextenso.proxylet.radius.RadiusAttribute[])
	 */
	public int doAccounting(RadiusAttribute[] attributes) {
		return doAccounting(attributes, AcctUtils.CODE_ACCOUNTING_REQUEST);
	}

	public int doAccounting(RadiusAttribute[] attributes, int code) {
		if (_acctServer == null) {
			LOGGER.warn("RadiusClient: Cannot perform Accounting Requests");
			return -1;
		}
		_request = new RadiusAccountRequest(attributes, _acctServer, code);
		int res = sendRequest(_request, _acctServer);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("doAccounting: res=" + res);
		}
		return res;
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusClient#doAccess(byte[],
	 *      com.nextenso.proxylet.radius.RadiusAttribute[])
	 */
	public int doAccess(byte[] password, RadiusAttribute[] attributes) {
		if (_authServer == null) {
			LOGGER.warn("RadiusClient: Cannot perform Access Requests");
			return -1;
		}
		_request = new RadiusAccessRequest(password, attributes, _authServer);
		int res = sendRequest(_request, _authServer);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("doAccess: res=" + res);
		}
		return res;
	}

	private void checkSync() {
		// Avoid locking if synchronous request is performed within main thread.
		if (Utils.getPlatformExecutors().getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase("radius")) {
			throw new IllegalStateException("Cannot use synchronous clients in the container main thread (your proxylet's accept method did not return ACCEPT_MAY_BLOCK)");
		}
	}

	private int sendRequest(RadiusRequest request, RadiusServer server) {

		checkSync();

		// Notify license manager that client sends a request
		ProxyletUtils.sendRequest(_componentId);

		RadiusManager.handleRequestProxyState(request);
		_isDone = false;
		int maxTries = RadiusProperties.getRequestMaxTry(); // may be modified on the fly
		MuxConnection connection = CONNECTION_MANAGER.getRandomMuxConnection ();
		if (connection == null){
			if (LOGGER.isEnabledFor(Level.WARN)) {
				LOGGER.warn("RadiusClient: No MuxConnection available for request #" + request.getIdentifier());
			}
			return -1;
		}
		int sockId = Utils.getSockId (connection);
		synchronized (lock) {
			for (int i = 0; i < maxTries; i++) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("sendRequest: Attempt number #" + i + " to send request #" + request.getIdentifier());
				}
				RadiusManager.sendRequest(this, request, server, connection, sockId);
				try {
					lock.wait(RadiusProperties.getRequestTimeout());
				}
				catch (InterruptedException e) {}
				if (_isDone) {
					break;
				}
			}
			if (!_isDone) {
				if (LOGGER.isEnabledFor(Level.WARN)) {
					LOGGER.warn("RadiusClient: Received no response for request #" + request.getIdentifier());
				}
				RadiusManager.abort(request.getIdentifier());
				return -1;
			} else if (!request.isValid()) {
				if (LOGGER.isEnabledFor(Level.WARN)) {
					LOGGER.warn("RadiusClient: Received invalid response for request #" + request.getIdentifier());
				}
				return -1;
			}
		}

		return request.getResponseCode();
	}

	/**
	 * @see com.nextenso.proxylet.radius.RadiusClient#getResponseAttributes()
	 */
	public Enumeration getResponseAttributes() {
		if (_request == null) {
			return null;
		}
		return _request.getResponseAttributes();
	}

	protected void handleRadiusResponse(byte[] data, int off, int len) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("handleRadiusResponse: received response for request #" + _request.getIdentifier());
		}
		synchronized (lock) {
			try {
				if (data == null) {
					// the attempt failed
					LOGGER.debug("handleRadiusResponse: no data -> do nothing (_isDone not set)");
					return;
				}
				_isDone = true;
				_request.handleRadiusResponse(data, off, len);
			}
			finally {
				lock.notifyAll();
			}

		}
	}

	@Override
	public void doAccounting(RadiusAttribute[] attributes, RadiusClientListener listener) {
		doAccounting(attributes, AcctUtils.CODE_ACCOUNTING_REQUEST, listener);
	}

	@Override
	public void doAccounting(final RadiusAttribute[] attributes, final int code, final RadiusClientListener listener) {
		PlatformExecutor executor = Utils.getPlatformExecutors().getCurrentThreadContext().getCurrentExecutor();
		final PlatformExecutor callbackExecutor = Utils.getPlatformExecutors().getCurrentThreadContext().getCallbackExecutor();
		Runnable task = new Runnable() {

			public void run() {
				final int res = doAccounting(attributes, code);
				Runnable callbackTask = new Runnable() {

					public void run() {
						listener.handleResponse(RadiusClientFacade.this, res);
					}
				};
				callbackExecutor.execute(callbackTask, ExecutorPolicy.SCHEDULE);
			}
		};

		executor.execute(task, ExecutorPolicy.SCHEDULE);
	}

	@Override
	public void doAccess(final byte[] password, final RadiusAttribute[] attributes, final RadiusClientListener listener) {
		PlatformExecutor executor = Utils.getPlatformExecutors().getCurrentThreadContext().getCurrentExecutor();
		final PlatformExecutor callbackExecutor = Utils.getPlatformExecutors().getCurrentThreadContext().getCallbackExecutor();
		Runnable task = new Runnable() {

			public void run() {
				final int res = doAccess(password, attributes);

				Runnable callbackTask = new Runnable() {

					public void run() {
						listener.handleResponse(RadiusClientFacade.this, res);
					}
				};
				callbackExecutor.execute(callbackTask, ExecutorPolicy.SCHEDULE);
			}
		};

		executor.execute(task, ExecutorPolicy.SCHEDULE);
	}

}
