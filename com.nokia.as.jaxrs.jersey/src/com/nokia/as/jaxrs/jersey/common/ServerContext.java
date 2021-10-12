// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.common;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.ws.rs.core.Application;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ApplicationHandler;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.ioh.tools.ChannelWriter.SendBufferMonitor;
import com.alcatel.as.http.parser.CommonLogFormat;
import com.alcatel.as.http.parser.HttpMeters;
import com.alcatel.as.http2.ConnectionConfig;
import com.alcatel.as.http2.ConnectionFactory;
import com.alcatel.as.http2.Settings;
import com.alcatel.as.service.concurrent.PlatformExecutors;

public class ServerContext {

	protected ApplicationHandler _appHandler;
	protected Logger _logger;
	protected HttpMeters _meters;
	protected String _alias;
	private Pattern _aliasPattern;
	protected CommonLogFormat _clf;
	protected int _serverPort;
	private ServiceRegistration<Application> _registration;
	private InetSocketAddress _address;
	private Map<Integer, ClientContext> _clientContexts;
	private ConnectionFactory _connF;
	private ConnectionConfig _http2Config;
	private Object _attachment;
	private PlatformExecutors _pfExecs;
	private String _tlsExportLabel;
	private boolean _tlsExportEnabled;
	private int _tlsExportLength;
	private Map<String, Object> _properties;
	private String _schemeHeaderH1, _authHeaderH1;
	private String _schemeHeaderH2, _authHeaderH2;
	private boolean _pseudoSchemeHeader = true, _pseudoAuthHeader = true;
	private SendBufferMonitor _sendMonitor;
	private boolean _injectOverload;

	public ServerContext() {
	}
	
	public <T> T attachment (){ return (T) _attachment;}
	
	public ServerContext attach (Object o){ _attachment = o; return this;}
	
	public ServerContext setApplicationHandler(ApplicationHandler handler) {
		_appHandler = handler;
		return this;
	}

	public ApplicationHandler getApplicationHandler() {
		return _appHandler;
	}

	public ServerContext setLogger(Logger logger) {
		_logger = logger;
		return this;
	}

	public Logger getLogger() {
		return _logger;
	}

	public ServerContext setMeters(HttpMeters meters) {
		_meters = meters;
		return this;
	}

	public HttpMeters getMeters() {
		return _meters;
	}

	public ServerContext setAlias(String alias) {
		_alias = alias;
		_aliasPattern = Pattern.compile(_alias);
		return this;
	}

	public String getAlias() {
		return _alias;
	}

	public ServerContext setSchemeHeaderH1 (String s){ _schemeHeaderH1 = s; return this;}
	public String getSchemeHeaderH1 (){ return _schemeHeaderH1;}
	public ServerContext setAuthHeaderH1 (String s){ _authHeaderH1 = s; return this;}
	public String getAuthHeaderH1 (){ return _authHeaderH1;}
	public ServerContext setSchemeHeaderH2 (String s){ _schemeHeaderH2 = s; return this;}
	public String getSchemeHeaderH2 (){ return _schemeHeaderH2;}
	public ServerContext setAuthHeaderH2 (String s){ _authHeaderH2 = s; return this;}
	public String getAuthHeaderH2 (){ return _authHeaderH2;}
	public ServerContext usePseudoSchemeHeader (boolean b){ _pseudoSchemeHeader = b; return this;}
	public boolean usePseudoSchemeHeader () { return _pseudoSchemeHeader;}
	public ServerContext usePseudoAuthHeader (boolean b){ _pseudoAuthHeader = b; return this;}
	public boolean usePseudoAuthHeader () { return _pseudoAuthHeader;}

	public ServerContext setSendBufferMonitor (SendBufferMonitor mon){ _sendMonitor = mon; return this;}
	public SendBufferMonitor getSendBufferMonitor (){ return _sendMonitor;}
	public ServerContext injectOverload (boolean b){ _injectOverload = b; return this;}
	public boolean injectOverload () { return _injectOverload;}

	public Pattern getAliasPattern () {
		return _aliasPattern;
	}

	public ServerContext setCommonLogFormat(CommonLogFormat clf) {
		_clf = clf;
		return this;
	}

	public CommonLogFormat getCommonLogFormat() {
		return _clf;
	}

	public int getServerPort() {
		return _address.getPort();
	}

	public ServiceRegistration<Application> getRegistration() {
		return _registration;
	}

	public void setRegistration(ServiceRegistration<Application> _registration) {
		this._registration = _registration;
	}

	public InetSocketAddress getAddress() {
		return _address;
	}

	public ServerContext setAddress(InetSocketAddress _address) {
		this._address = _address;
		return this;
	}

	public Map<Integer, ClientContext> getClientContexts() {
		return _clientContexts;
	}

	public ServerContext setClientContexts(ConcurrentHashMap<Integer, ClientContext> concurrentHashMap) {
		_clientContexts = concurrentHashMap;
		return this;
	}

	public ServerContext setHttp2Config (Map<String, Object> props){
		Settings settings = new Settings ().load (props);
		_http2Config = new ConnectionConfig (settings, getLogger ())
			.priorKnowledge (false)
			.load (true, props);
		return this;
	}

	public ConnectionConfig getHttp2Config (){ return _http2Config;}

	public ServerContext setHttp2ConnectionFactory (ConnectionFactory cf){ _connF = cf; return this;}
	public ConnectionFactory getHttp2ConnectionFactory (){ return _connF;}
    public PlatformExecutors getPfExecutors() { return _pfExecs; }
    public ServerContext setPfExecutors(PlatformExecutors execs) { _pfExecs = execs; return this;}


    public ServerContext setTlsExportKeyPolicy (boolean enabled, String label, int len){
	_tlsExportEnabled = enabled;
	_tlsExportLabel = label;
	_tlsExportLength = len;
	return this;
    }
    public boolean tlsExportEnabled (){ return _tlsExportEnabled;}
    public String tlsExportLabel (){ return _tlsExportLabel;}
    public int tlsExportLen (){ return _tlsExportLength;}

	public ServerContext setProperties(Map<String, Object> properties) {
		_properties = properties;
		return this;
	}
	
	public Map<String, Object> getProperties() {
		return _properties;
	}
}
