// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.agent.impl;

import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.*;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import javax.ws.rs.core.UriBuilder;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ApplicationHandler;
import org.osgi.framework.FrameworkUtil;

import com.alcatel.as.http.parser.CommonLogFormat;
import com.alcatel.as.http.parser.HttpMeters;
import com.alcatel.as.ioh.server.ServerFactory;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.metering2.MeteringService;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxContext;
import com.nextenso.mux.MuxHandler;
import com.nokia.as.jaxrs.jersey.agent.AgentServersConf;
import com.nokia.as.jaxrs.jersey.common.ClientContext;
import com.nokia.as.jaxrs.jersey.common.JaxRsResourceRegistry;
import com.nokia.as.jaxrs.jersey.common.ServerContext;
import com.nokia.as.jaxrs.jersey.common.impl.Helper;
import com.nokia.as.jaxrs.jersey.common.impl.JerseyTracker;

@Property(name = "protocol", value = "jax-rs")
@Property(name = "autoreporting", value = "false")
@Component(provides = MuxHandler.class)
public class JerseyAgent extends MuxHandler {

	static final Logger _log = Logger.getLogger("as.ioh.jaxrs");
	static final int[] JERSEY_IOH_ID = new int[] { 286 };

	@ServiceDependency
	private volatile JaxRsResourceRegistry registration;
	@ServiceDependency
	private volatile MeteringService _metering;
	@ServiceDependency
	private volatile ServerFactory _serverFactory;
	@ServiceDependency
	private PlatformExecutors _execs;
	
	private AgentServersConf _agentServersConf;
	private volatile int metersId = 0;
	
	private final Properties _properties = new Properties();
	private final CommonLogFormat _clf = new CommonLogFormat();
	
	/**
	 * Ensure all jersey bundles are fully started.
	 */
	@ServiceDependency
	JerseyTracker _jerseyTracker;

	@ConfigurationDependency(pid="com.nokia.as.jaxrs.jersey.AgentServersConf")
	void updated(AgentServersConf conf) {
		_agentServersConf = conf;
	}

	@Start
	public void start() {
		try {
			_properties.load(new StringReader(_agentServersConf.getServersConf()));
		} catch (IOException e) {
			_log.error(this + ":" + e.getMessage(), e);
		}
		// createCommonLogFormat
		_clf.configure(_properties);
		_properties.put(COMMON_LOG_FORMAT, _clf);
	}

	// ---------------- MuxHandler interface

	/** Called by the CalloutAgent when it has seen our MuxHandler */
	@SuppressWarnings("unchecked")
	@Override
	public void init(int appId, String appName, String appInstance, MuxContext muxContext) {
		// Don't forget to call the super.init method !
		super.init(appId, appName, appInstance, muxContext);

		// Configure our MUX handler for the Web protocol
		getMuxConfiguration().put(CONF_STACK_ID, JERSEY_IOH_ID);
		getMuxConfiguration().put(CONF_USE_NIO, true);
		getMuxConfiguration().put(CONF_THREAD_SAFE, true);
		getMuxConfiguration().put(CONF_IPV6_SUPPORT, true);
	}

	@Override
	public void muxOpened(final MuxConnection connection) {
		// Q = q1
		_log.info("muxOpened: " + connection);
	}

	@Override
	public void muxClosed(MuxConnection connection) {
		// Q = q1
		_log.info("muxClosed: " + connection);
	}

	@Override
	public void tcpSocketListening(MuxConnection connection, int sockId, String localIP, int localPort, boolean secure,
			long listenId, int errno) {
		// Q = q1
		_log.info("tcpSocketListening");

		// createMeters
		HttpMeters meters = new HttpMeters("as.ioh.agent.jaxrs." + localPort + "." + metersId++, "Jax-rs agent", _metering);
		meters.init(null);
		meters.start(FrameworkUtil.getBundle(JerseyAgent.class).getBundleContext());

		// createAlias
		String global = addLastSlash(_agentServersConf.getGlobalAlias()); // should look like /services/

		// createServerContext
		ApplicationHandler appHandler;
		try {
		    Thread.currentThread().setContextClassLoader(ApplicationHandler.class.getClassLoader());
		    appHandler = new ApplicationHandler ();
		} finally {
		    Thread.currentThread().setContextClassLoader(null);
		}
		ServerContext serverCtx = new ServerContext().setApplicationHandler(appHandler)
		    .setMeters(meters)
		    .setAlias(global).setCommonLogFormat(_clf).setLogger(_log)
		    .setAddress(new InetSocketAddress(localIP, localPort))
		    .setClientContexts(new ConcurrentHashMap<Integer, ClientContext>())
		    .attach(sockId)
		    .setProperties((Map) _properties);

		connection.attach(serverCtx);
		registration.add(serverCtx);
	}

	@Override
	public void tcpSocketConnected(MuxConnection connection, int sockId, String remoteIP, int remotePort,
			String localIP, int localPort, String virtualIP, int virtualPort, boolean secure, boolean clientSocket,
			long connectionId, int errno) {
		// Q = qSocket/sockId
		ServerContext serverCtx = (ServerContext) connection.attachment();
		String protocol = secure ? HTTPS : HTTP;
		StringBuilder authority = new StringBuilder().append(protocol).append(localIP).append('/');
		URI uri = UriBuilder.fromUri(authority.toString()).port(localPort).build();
		JerseyAgentClientContext cc = new JerseyAgentClientContext(serverCtx, connection, remoteIP, remotePort, sockId, uri, getResourceExecutor());
		serverCtx.getClientContexts().put(sockId, cc);
		if (serverCtx.getLogger().isDebugEnabled())
			serverCtx.getLogger().debug(connection+" : created : "+cc);
	}

	private Executor getResourceExecutor() {
		return Helper.getResourceExecutor(_execs, _agentServersConf.getExecutorType());
	}

	@Override
	public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer buf) {
		// Q = qSocket/sockId
		ServerContext serverCtx = (ServerContext) connection.attachment();
		JerseyAgentClientContext clientCtx = (JerseyAgentClientContext) serverCtx.getClientContexts().get(sockId);
		if (clientCtx != null)
		    clientCtx.messageReceived(buf);
		else
		    serverCtx.getLogger ().warn ("tcpSocketData : sockId="+sockId+" : no matching client context");
	}

	@Override
	public void tcpSocketClosed(MuxConnection connection, int sockId) {
		// Q = qSocket/sockId
		if (_log.isDebugEnabled()) {
			_log.debug(this + " : tcpSocketClosed");
		}
		ServerContext serverCtx = (ServerContext) connection.attachment();
		Integer serverSockId = serverCtx.attachment ();
		if (sockId == serverSockId){
		    // close listening port
		    serverCtx.getMeters ().stop ();
		    return;
		}

		JerseyAgentClientContext clientCtx = (JerseyAgentClientContext) serverCtx.getClientContexts().remove(sockId);
		if (clientCtx != null)
		    clientCtx.closed ();
		else
		    serverCtx.getLogger ().warn ("tcpSocketClosed : sockId="+sockId+" : no matching client context");
	}

	public void destroy() {
		// The Callout is gone and asks us to destroy ourself ...
		_log.info("Destroying JerseyAgent");
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

	/********************************
	 * Utils *
	 *******************************/

	private static final String addLastSlash(String alias) {
		if (alias.endsWith("/"))
			return alias;
		return alias + "/";
	}
}
