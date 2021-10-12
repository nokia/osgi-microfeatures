// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.mux.reactor;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxContext;
import com.nextenso.mux.MuxHandler;

import alcatel.tess.hometop.gateways.reactor.ReactorProvider;

@Component(service = {
		MuxHandler.class }, immediate = true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property = {
				"protocol=ip", "autoreporting=false", "hidden=true" })
public class Agent extends MuxHandler {

	static final Logger LOGGER = Logger.getLogger("as.service.reactor.mux.agent");

	private static Map<Integer, String> IOHs = new HashMap<>();
	static {
		IOHs.put(287, "generic");
		IOHs.put(286, "http");
	}

	private String _toString;
	private PlatformExecutors _execs;
	private BundleContext _bc;
	
	@Reference(target = "(type=nio)")
	ReactorProvider _nioProvider;

	public Agent() {
		_toString = "ReactorProviderAgent";
	}

	public String toString() {
		return _toString;
	}

	@Reference
	public void setExecs(PlatformExecutors execs) {
		_execs = execs;
	}

	@Activate
	public void activate(BundleContext bc) {
		_bc = bc;
	}

	/////////////////// private methods

	private MuxReactorProviderImpl provider(MuxConnection mux) {
		return (MuxReactorProviderImpl) mux.attachment();
	}

	// ---------------- MuxHandler interface
	// -----------------------------------------------------------

	/** Called by the CalloutAgent when it has seen our MuxHandler */
	@SuppressWarnings("unchecked")
	@Override
	public void init(int appId, String appName, String appInstance, MuxContext muxContext) {
		// Don't forget to call the super.init method !
		super.init(appId, toString(), appInstance, muxContext);

		// Configure our MUX handler for the Web protocol
		LOGGER.warn("ReactorProvider Mux Agent started!");
		getMuxConfiguration().put(CONF_STACK_ID, new int[] { /*286,*/ 287 });
		getMuxConfiguration().put(CONF_USE_NIO, true);
		getMuxConfiguration().put(CONF_THREAD_SAFE, false); // false ! mthread support done elsewhere
		getMuxConfiguration().put(CONF_IPV6_SUPPORT, true);
		getMuxConfiguration().put(CONF_L4_PROTOCOLS, new String[] { "tcp" }); // TODO add sctp/udp later
		
	}

	@Override
	public boolean accept(int stackAppId, String stackName, String stackHost, String stackInstance) {
		return IOHs.get (stackAppId) != null;
	}

	@Override
	public void muxOpened(final MuxConnection connection) {
		LOGGER.warn(this + " : muxOpened : " + connection);
		MuxReactorProviderImpl mrp = new MuxReactorProviderImpl(IOHs.get(connection.getStackAppId()), connection, _execs);
		connection.attach(mrp.start(_bc));
	}

	@Override
	public void muxClosed(MuxConnection connection) {
		LOGGER.warn(this + " : muxClosed : " + connection);
		provider(connection).stop();
	}

	@Override
	public void tcpSocketConnected(MuxConnection connection, int sockId, String remoteIP, int remotePort,
			String localIP, int localPort, String virtualIP, int virtualPort, boolean secure, boolean clientSocket,
			long connectionId, int errno) {
		if (clientSocket)
			return;
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(this + " : tcpSocketConnected : " + connectionId + "/" + sockId + "/" + remoteIP + "/"
					+ remotePort + "/" + errno);
		provider(connection).tcpSocketConnected(connectionId, sockId, errno, remoteIP, remotePort, localIP, localPort);
	}

	@Override
	public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer buf) {
		provider(connection).tcpSocketData(sockId, sessionId, buf);
	}

	@Override
	public void tcpSocketClosed(MuxConnection connection, int sockId) {
		provider(connection).tcpSocketClosed(sockId);
	}
	
	@Override
	public void tcpSocketAborted(MuxConnection connection, int sockId) {
		provider(connection).tcpSocketAborted(sockId);
	}

	public void destroy() {
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public int getMajorVersion() {
		return 1;
	}

	@Override
	public int[] getCounters() {
		throw new RuntimeException("deprecated method, should not be used anymore");
	}

	@Override
	public void commandEvent(int command, int[] intParams, String[] strParams) {
	}
}
