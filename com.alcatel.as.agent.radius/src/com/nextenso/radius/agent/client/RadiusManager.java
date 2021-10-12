// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent.client;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.ByteBuffer;

import com.nextenso.mux.MuxConnection;
import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.RadiusUtils;
import com.nextenso.radius.agent.Utils;
import com.nextenso.radius.agent.impl.RadiusMessageFacade;
import com.nextenso.radius.agent.impl.RadiusServer;

public class RadiusManager {

	private static final Logger LOGGER = Logger.getLogger("agent.radius.client.manager");

	private static final String PROXY_STATE = "NxClient";
	private static final byte[] PROXY_STATE_B = RadiusAttribute.convertTextToValue(PROXY_STATE);
	public static final int PROXY_STATE_LENGTH = PROXY_STATE_B.length + 8; // MUST NOT BE 8 !!!!!!
	public static final int PROXY_STATE_LENGTH_FULL = PROXY_STATE_LENGTH + 2;

    
	private static final Map<Long, PendingReq> REQUESTS = new ConcurrentHashMap<Long, PendingReq>();

	private static class PendingReq {

		private RadiusClientFacade _client;
		private RadiusRequest _request;
		private RadiusServer _server;

		public PendingReq(RadiusClientFacade client, RadiusRequest request, RadiusServer server) {
			_client = client;
			_server = server;
			setRequest(request);
		}

		public final RadiusRequest getRequest() {
			return _request;
		}

		public final void setRequest(RadiusRequest request) {
			_request = request;
		}

		public final RadiusClientFacade getClient() {
			return _client;
		}

		public final RadiusServer getServer() {
			return _server;
		}

		public void send(long identifier, MuxConnection connection, int sockId) {

			if (getRequest() == null) {
				// already sent !
				return;
			}
			ByteBuffer buff = new ByteBuffer(128);
			OutputStream out = buff.getOutputStream();
			try {
				if (LOGGER.isDebugEnabled()) {
				    LOGGER.debug("sendRequest: identifier=" + identifier + ", sockId="+sockId+", client=" + _client + ", request=" + _request);
				}
				if (isUsingBestEffortForMissingProxyState()) {
				    identifier |=  ((long)sockId) << 32;
				}
				REQUESTS.put(identifier, this);
				try {
					getRequest().getRequest().writeTo(out);
					boolean isSent = connection.sendUdpSocketData(sockId, getServer().getIp(), getServer().getPort(), 0, 0, buff.toByteArray(false), 0, buff.size(), false);
					if (isSent) {
						setRequest(null); //clear sent request so that it's not sent again in case of mux down/up
						return;
					}
					LOGGER.warn("RadiusManager : Failed to send radius request (identifier=" + identifier + ") to the proxy");
				}
				catch (IOException e) {
					LOGGER.warn("RadiusManager : Failed to send radius request (identifier=" + identifier + ") to the proxy", e);
				}
				abort(identifier);
				getClient().handleRadiusResponse(null, 0, 0);
			}
			finally {
				buff.close();
			}
		}

		public void handleRadiusResponse(byte[] data, int off, int len) {
			getClient().handleRadiusResponse(data, off, len);
		}
	}

	private static boolean BEST_EFFORT = false;

	private static boolean isUsingBestEffortForMissingProxyState() {
		return BEST_EFFORT;
	}

	public static void setIsUsingBestEffortForMissingProxyState(boolean value) {
		BEST_EFFORT = value;
	}

	/**
	 * Performs an asynchronous Radius request. Notifies the RadiusClientFacade
	 * when the request is done.
	 */
	protected static void sendRequest(RadiusClientFacade client, RadiusRequest request, RadiusServer server, MuxConnection connection, int sockId) {
		long identifier = request.getIdentifier();
		if (isUsingBestEffortForMissingProxyState()) {
			identifier = request.getRadiusIdentifier();
		}
		PendingReq pending = new PendingReq(client, request, server);
		pending.send(identifier, connection, sockId);
	}

	protected static void handleRequestProxyState(RadiusRequest request) {
		RadiusMessageFacade message = request.getRequest();
		RadiusAttribute ps = message.getRadiusAttribute(RadiusUtils.PROXY_STATE);
		if (ps == null) {
			ps = new RadiusAttribute(RadiusUtils.PROXY_STATE);
			message.addRadiusAttribute(ps);
		}

		long identifier = request.getIdentifier();
		if (isUsingBestEffortForMissingProxyState()) {
			identifier = request.getRadiusIdentifier();
		}
		byte[] value = new byte[PROXY_STATE_LENGTH];
		System.arraycopy(PROXY_STATE_B, 0, value, 0, PROXY_STATE_B.length);
		Utils.setRequestId(identifier, value, PROXY_STATE_B.length);
		ps.addValue(value, false);
	}

	protected static void abort(long identifier) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("abort : remove identifier=" + identifier);
		}

		REQUESTS.remove(identifier);
	}

	/*********************
	 * Callback from Agent
	 *********************/

	private static final int PROXY_STATE_LENGTH_HEAD = PROXY_STATE_B.length + 2;

    public static void handleProxyData(byte[] buff, int off, int len, int index) {
		long identifier = Utils.getRequestId(buff, index + PROXY_STATE_LENGTH_HEAD, 8);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("handleProxyData : Received radius response (proxy-state identifier=" + identifier + ")");
		}
		PendingReq pending = REQUESTS.remove(identifier);
		if (pending != null) {
			pending.handleRadiusResponse(buff, off, len);
		} else {
			LOGGER.warn("RadiusManager : Received radius response (identifier=" + identifier + ") with no matching request");
		}
	}

    public static void handleProxyData(int sockId, long identifier, byte[] buff, int off, int len) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("handleProxyData : Received radius response without proxy state (identifier=" + identifier + ", sockId="+sockId+")");
		}
		long identifierOrig = identifier;
		identifier |= ((long)sockId) << 32;
		PendingReq pending = REQUESTS.remove(identifier);
		if (pending != null) {
			pending.handleRadiusResponse(buff, off, len);
		} else {
			LOGGER.warn("RadiusManager : Received radius response (identifier=" + identifierOrig + ", sockId="+sockId+") with no matching request");
		}
	}
}
