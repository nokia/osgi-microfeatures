// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.impl;

import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.APPLICATION_HANDLER;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.COMMON_LOG_FORMAT;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.HTTP;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.HTTPS;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_ALIAS;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_IP;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_PORT;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_SCHEME;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_AUTH_HEADER;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_SCHEME_HEADER;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_SCHEME_PSEUDO_HEADER;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_AUTH_PSEUDO_HEADER;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_OVERLOAD_LOW_WM;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_OVERLOAD_HIGH_WM;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_OVERLOAD_INJECT;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.PROP_JAXRS_SERVER_OVERLOAD_INJECT_PATH;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ApplicationHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.alcatel.as.http.parser.CommonLogFormat;
import com.alcatel.as.http.parser.HttpMeters;
import com.alcatel.as.http2.ConnectionFactory;
import com.alcatel.as.ioh.server.Server;
import com.alcatel.as.ioh.server.ServerFactory;
import com.alcatel.as.ioh.server.TcpServer;
import com.alcatel.as.ioh.server.TcpServerProcessor;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.nokia.as.jaxrs.jersey.ServersConf;
import com.nokia.as.jaxrs.jersey.common.JaxRsResourceRegistry;
import com.nokia.as.jaxrs.jersey.common.ServerContext;
import com.nokia.as.jaxrs.jersey.common.impl.Helper;
import com.nokia.as.jaxrs.jersey.common.impl.JerseyTracker;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpChannelListener;

@Component(provides = { JerseyProcessor.class, TcpServerProcessor.class })
@Property(name = "processor.id", value = "jaxrs.jersey")
public class JerseyProcessor implements TcpServerProcessor {

	private static final String PROP_SERVER_CONTEXT = "server.context";
	public static final String PROP_HTTP2_PRIOR_KNOWLEDGE = "http2.prior-knowledge";

	static final Logger log = Logger.getLogger("as.ioh.jaxrs");

	@Inject
	BundleContext _bc;

	private ServersConf _serversConf;
	private boolean _started;

	@ServiceDependency
	private volatile JaxRsResourceRegistry registration;
	@ServiceDependency
	private volatile MeteringService _metering;
	@ServiceDependency
	private volatile ServerFactory _serverFactory;
	@ServiceDependency
	private volatile ConnectionFactory _connF;
	@ServiceDependency
	private volatile PlatformExecutors _pfExecs;

	/**
	 * Ensure all jersey bundles are fully started.
	 */
	@ServiceDependency
	JerseyTracker _jerseyTracker;

	@ConfigurationDependency
	void updated(ServersConf serversConf) {
		log.info("ServersConfiguration : " + serversConf);
		_serversConf = serversConf;
		if (_started) applyConf (); 
	}

	@Start
	void start() {
		_started = true;
		applyConf ();
	}

	private void applyConf (){
		_serverFactory.newTcpServerConfig("jaxrs.jersey", _serversConf.getServersConf());
	}

	/********************************
	 * @see TcpServerProcessor impl *
	 *******************************/
	
	@Override
	public void serverCreated(TcpServer server) {
		log.info("serverCreated " + server);
		Map<String, Object> properties = server.getProperties();
		try {
			// When using jaxrs in embedded mode, the jaxrs api is in the class path. 
			// Now, the point is: the jaxrs api is doing something very unmodular (as it is often the case): it uses some nasty factory finder that is looking up
			// some classes from ... the classpath. work around is to temporarily set the thread context class loader 			
			Thread.currentThread().setContextClassLoader(ApplicationHandler.class.getClassLoader());
			properties.put(APPLICATION_HANDLER, new ApplicationHandler());
		} finally {
			Thread.currentThread().setContextClassLoader(null);
		}
		properties.put(TcpServer.PROP_READ_ENABLED, true);
		Object o = server.getProperties().get(PROP_JAXRS_SERVER_PORT);
		Integer serverPort = null;
		if (o != null) {
			serverPort = Integer.parseInt(o.toString());
			server.getProperties().put(PROP_JAXRS_SERVER_PORT, serverPort);
		} else {
			serverPort = server.getAddress().getPort();
			server.getProperties().put(PROP_JAXRS_SERVER_PORT, serverPort);
		}
		
		o = server.getProperties().get(PROP_JAXRS_SERVER_SCHEME);
		if (o != null) {
		    String s = o.toString ().toLowerCase ().replace ("://", "");
		    server.getProperties().put(PROP_JAXRS_SERVER_SCHEME, s);
		} else {
		    boolean serverIsSecure = (boolean) server.getProperties().get(Server.PROP_SERVER_SECURE);
		    server.getProperties().put(PROP_JAXRS_SERVER_SCHEME, serverIsSecure ? "https" : "http");
		}
		
		String prop = PROP_JAXRS_SERVER_SCHEME_HEADER+".http1";
		o = server.getProperties().get(prop);
		if (o == null) o = server.getProperties().get(PROP_JAXRS_SERVER_SCHEME_HEADER);
		if (o == null){
		    server.getProperties ().put (prop, "x-forwarded-proto");
		}else{
		    String s = o.toString ().trim ().toLowerCase ();
		    if ("none".equals (s))
			server.getProperties ().remove (prop);
		    else
			server.getProperties ().put (prop, s);
		}
		prop = PROP_JAXRS_SERVER_SCHEME_HEADER+".http2";
		o = server.getProperties().get(prop);
		if (o == null) o = server.getProperties().get(PROP_JAXRS_SERVER_SCHEME_HEADER);
		if (o == null){
		    server.getProperties ().put (prop, "x-forwarded-proto");
		}else{
		    String s = o.toString ().trim ().toLowerCase ();
		    if ("none".equals (s))
			server.getProperties ().remove (prop);
		    else
			server.getProperties ().put (prop, s);
		}
		
		prop = PROP_JAXRS_SERVER_AUTH_HEADER+".http1";
		o = server.getProperties().get(prop);
		if (o == null) o = server.getProperties().get(PROP_JAXRS_SERVER_AUTH_HEADER);
		if (o == null){
		    server.getProperties ().put (prop, "host");
		}else{
		    String s = o.toString ().trim ().toLowerCase ();
		    if ("none".equals (s))
			server.getProperties ().remove (prop);
		    else
			server.getProperties ().put (prop, s);
		}
		prop = PROP_JAXRS_SERVER_AUTH_HEADER+".http2";
		o = server.getProperties().get(prop);
		if (o == null) o = server.getProperties().get(PROP_JAXRS_SERVER_AUTH_HEADER);
		if (o == null){
		    server.getProperties ().put (prop, "host");
		}else{
		    String s = o.toString ().trim ().toLowerCase ();
		    if ("none".equals (s))
			server.getProperties ().remove (prop);
		    else
			server.getProperties ().put (prop, s);
		}
		
		
		String global = addLastSlash(_serversConf.getGlobalAlias()); // should look like /services/
		o = properties.get(PROP_JAXRS_SERVER_ALIAS);
		if (o != null) {
			String path = o.toString();
			if (path.startsWith("/"))
				properties.put(PROP_JAXRS_SERVER_ALIAS, addLastSlash(path)); // should look like /custom/
			else
				properties.put(PROP_JAXRS_SERVER_ALIAS, addLastSlash(global + path)); // should look like

		} else  // /services/custom/
			properties.put(PROP_JAXRS_SERVER_ALIAS, global);

		String alias = (String) properties.get(PROP_JAXRS_SERVER_ALIAS);
		o = server.getProperties().get(PROP_JAXRS_SERVER_OVERLOAD_INJECT_PATH);
		if (o == null) server.getProperties().put(PROP_JAXRS_SERVER_OVERLOAD_INJECT_PATH, alias+"overload/");
		else server.getProperties().put(PROP_JAXRS_SERVER_OVERLOAD_INJECT_PATH, addLastSlash (alias+o));

		CommonLogFormat clf = new CommonLogFormat();
		clf.configure(properties);
		properties.put(COMMON_LOG_FORMAT, clf);

		String serverName = (String) properties.get(Server.PROP_SERVER_NAME);
		HttpMeters meters = new HttpMeters("as.ioh." + serverName, "JaxRs connector for server : " + serverName, _metering);
		properties.put(SimpleMonitorable.class.toString(), meters);
		meters.init(null);
		meters.start(FrameworkUtil.getBundle(JerseyProcessor.class).getBundleContext());

		String tlsExportLabel = (String) properties.get(TcpServer.PROP_TCP_SECURE_KEYEXPORT_LABEL);
		String tlsExportLen = (String) properties.get (TcpServer.PROP_TCP_SECURE_KEYEXPORT_LENGTH);
		Object secure = properties.get ("server.secure");
		boolean tlsExportEnabled = (secure != null && (Boolean) secure) && tlsExportLabel != null && tlsExportLen != null;

		ChannelWriter.SendBufferMonitor monitor = new ChannelWriter.ProgressiveSendBufferMonitor (getIntProperty (PROP_JAXRS_SERVER_OVERLOAD_LOW_WM, properties, 10000),
													  getIntProperty (PROP_JAXRS_SERVER_OVERLOAD_HIGH_WM, properties, 50000)
													  );
		
		ServerContext serverCtx = new ServerContext()
		    .setApplicationHandler((ApplicationHandler) properties.get(APPLICATION_HANDLER)).setMeters(meters)
		    .setAddress(server.getAddress())
		    .setAlias(alias)
		    .setPfExecutors(_pfExecs)
		    .setCommonLogFormat(clf)
		    .setHttp2ConnectionFactory(_connF)
		    .setProperties(properties)
		    .setTlsExportKeyPolicy(tlsExportEnabled, tlsExportLabel, tlsExportEnabled ? Integer.parseInt (tlsExportLen) : 0)
		    .setSchemeHeaderH1((String) server.getProperties().get(PROP_JAXRS_SERVER_SCHEME_HEADER+".http1"))
		    .setSchemeHeaderH2((String) server.getProperties().get(PROP_JAXRS_SERVER_SCHEME_HEADER+".http2"))
		    .setAuthHeaderH1((String) server.getProperties().get(PROP_JAXRS_SERVER_AUTH_HEADER+".http1"))
		    .setAuthHeaderH2((String) server.getProperties().get(PROP_JAXRS_SERVER_AUTH_HEADER+".http2"))
		    .usePseudoSchemeHeader (getBooleanProperty (PROP_JAXRS_SERVER_SCHEME_PSEUDO_HEADER, server.getProperties(), true))
		    .usePseudoAuthHeader (getBooleanProperty (PROP_JAXRS_SERVER_AUTH_PSEUDO_HEADER, server.getProperties(), true))
		    .setSendBufferMonitor (monitor)
		    .injectOverload (getBooleanProperty (PROP_JAXRS_SERVER_OVERLOAD_INJECT, server.getProperties(), false));
		    
		properties.put(PROP_SERVER_CONTEXT, serverCtx);
		registration.add(serverCtx);
	}

	@Override
	public void connectionAccepted(TcpServer server, TcpChannel client, Map<String, Object> props) {
		ServerContext serverCtx = (ServerContext) server.getProperties().get(PROP_SERVER_CONTEXT);
		
		client.setWriteBlockedPolicy(AsyncChannel.WriteBlockedPolicy.IGNORE);

		String serverIP = null;
		Object o = server.getProperties().get(PROP_JAXRS_SERVER_IP);
		if (o != null)
			serverIP = o.toString();
		else
			serverIP = client.getLocalAddress().getAddress().getHostAddress();
		int serverPort = (Integer) server.getProperties().get(PROP_JAXRS_SERVER_PORT);
		String scheme = (String) server.getProperties ().get (PROP_JAXRS_SERVER_SCHEME);
		URI uri = null;
		try{
		    uri = new URI (scheme, new StringBuilder ().append (serverIP).append (':').append (serverPort).toString (), serverCtx.getAlias (), null, null);
		}catch(Exception e){
		    // not expected
		    log.warn (server+" : failed to create URI, closing client connection", e);
		    client.close ();
		    return;
		}
		JerseyProcessorClientContext cc = new JerseyProcessorClientContext(serverCtx, client, uri, getResourceExecutor());
		if (serverCtx.getLogger().isDebugEnabled())
			serverCtx.getLogger().debug(server + " : created : " + cc);
		
		client.attach(cc);
		client.enableReading();
	}

	private Executor getResourceExecutor() {
		return Helper.getResourceExecutor(_pfExecs, _serversConf.getExecutorType());
	}

	@Override
	public TcpChannelListener getChannelListener(TcpChannel channel) {
		return (TcpChannelListener) channel.attachment();
	}

	@Override
	public void serverClosed(TcpServer server) {
		log.info("serverClosed" + server);
	}

	@Override
	public void serverDestroyed(TcpServer server) {
		log.info("serverDestroyed" + server);
		((HttpMeters) server.getProperties().get(SimpleMonitorable.class.toString())).stop();
	}

	@Override
	public void serverFailed(TcpServer server, Object cause) {
		log.trace("serverFailed" + server + "; cause:" + cause);
	}

	@Override
	public void serverOpened(TcpServer server) {
		// the logger was not yet set in serverCreated
		ServerContext serverCtx = (ServerContext) server.getProperties().get(PROP_SERVER_CONTEXT);
		serverCtx.setLogger((Logger) server.getProperties().get("server.logger"))
		    .setHttp2Config (server.getProperties ()); // need the logger set
	}

	@Override
	public void serverUpdated(TcpServer server) {
		log.info("serverUpdated");
	}

	/********************************
	 * Utils *
	 *******************************/

	private static final String addLastSlash(String alias) {
		if (alias.endsWith("/"))
			return alias;
		return alias + "/";
	}

	private static boolean getBooleanProperty (String name, Map<String, Object> props, boolean def){
	    Object o = props.get (name);
	    if (o != null){
		if (o instanceof String) return Boolean.parseBoolean (((String)o).trim ());
		return ((Boolean) o).booleanValue ();
	    }
	    return def;
	}
	public static int getIntProperty (String name, Map<String, Object> props, int def){
	    Object o = props.get (name);
	    if (o != null){
		if (o instanceof String){
		    return Integer.parseInt (((String)o).trim ());
		}
		return ((Number) o).intValue ();
	    }
	    return def;
	}
}
