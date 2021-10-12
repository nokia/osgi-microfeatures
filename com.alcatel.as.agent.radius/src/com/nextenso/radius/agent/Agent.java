// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.reporter.api.CommandScopes;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel_lucent.as.management.annotation.command.Commands;
import com.alcatel_lucent.as.management.annotation.stat.Counter;
import com.alcatel_lucent.as.management.annotation.stat.Stat;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxContext;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.util.MuxConnectionManager;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.proxylet.engine.ProxyletApplication;
import com.nextenso.proxylet.radius.RadiusUtils;
import com.nextenso.proxylet.radius.acct.AcctUtils;
import com.nextenso.proxylet.radius.acct.CoAUtils;
import com.nextenso.proxylet.radius.acct.DisconnectUtils;
import com.nextenso.proxylet.radius.auth.AuthUtils;
import com.nextenso.radius.agent.client.RadiusManager;
import com.nextenso.radius.agent.client.RadiusClientFacade;
import com.nextenso.radius.agent.engine.RadiusProxyletContainer;
import com.nextenso.radius.agent.impl.RadiusInputStream;
import com.nextenso.radius.agent.impl.RadiusServer;

@Commands(rootSnmpName = "alcatel.srd.a5350.RadiusAgent", rootOid = { 637, 71, 6, 1030 })
@Stat(rootSnmpName = "alcatel.srd.a5350.RadiusAgent", rootOid = { 637, 71, 6, 1030 })
public class Agent
		extends MuxHandler {

	private static final Logger LOGGER = Logger.getLogger("agent.radius");

	private final AtomicInteger _accountCounter = new AtomicInteger(0);
	private final AtomicInteger _accessCounter = new AtomicInteger(0);
	private final MuxConnectionManager _connectionManager = new MuxConnectionManager();

	private ProxyletApplication _application;
	private volatile BundleContext _serviceRegistry;

	private boolean _isUsingBestEffortForMissingProxyState = false;

	public Agent() {}

	/**
	 * Called by Activator or Launcher.
	 * 
	 * @param d The configuration.
	 */
	protected void setSystemConfig(Dictionary d) {
		RadiusProxyletContainer proxyletContainer = new RadiusProxyletContainer(d);
		Utils.setContainer(proxyletContainer);
	}

	/**
	 * Called by Activator or Launcher.
	 * 
	 * @param app The application.
	 */
	protected void bindProxyletApplication(ProxyletApplication app) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("bindProxyletApplication: application= " + app);
		}
		_application = app;
	}

	/**
	 * Called by Activator.
	 * 
	 * @param app The application.
	 */
	protected void unbindProxyletApplication(ProxyletApplication app) {}

	/**
	 * @see com.nextenso.mux.MuxHandler#init(alcatel.tess.hometop.gateways.utils.Config)
	 */
	@Override
	public void init(Config conf)
		throws ConfigException {
		super.init(conf);

		Long delay = (Long) conf.get("radiusagent.exitDelay");
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("init (conf): delay=" + delay);
		}
		if (delay != null) {
			getMuxConfiguration().put(CONF_EXIT_DELAY, delay);
		}

	}

	/**
	 * @see com.nextenso.mux.MuxHandler#init(int, java.lang.String,
	 *      java.lang.String, com.nextenso.mux.MuxContext)
	 */
	@Override
	public void init(int appId, String appName, String appInstance, MuxContext context) {
		super.init(appId, appName, appInstance, context);
		if (_serviceRegistry != null) {
			// register our counters and commands
			Hashtable<String, String> properties = new Hashtable<String, String>();
			properties.put(CommandScopes.COMMAND_SCOPE, CommandScopes.APP_COUNTER_SCOPE);
			properties.put(ConfigConstants.MODULE_NAME, Utils.MODULE_RADIUS_AGENT);
			properties.put(ConfigConstants.MODULE_ID, Integer.toString(Utils.APP_RADIUS_AGENT));
			_serviceRegistry.registerService(Object.class.getName(), this, properties);

			properties = new Hashtable<String, String>();
			properties.put(CommandScopes.COMMAND_SCOPE, CommandScopes.APP_COMMAND_SCOPE);
			properties.put(ConfigConstants.MODULE_NAME, Utils.MODULE_RADIUS_AGENT);
			properties.put(ConfigConstants.MODULE_ID, Integer.toString(Utils.APP_RADIUS_AGENT));
			_serviceRegistry.registerService(Object.class.getName(), this, properties);
		}

		int[] stackIds = new int[2];
		stackIds[0] = Utils.APP_RADIUS_STACK;
		stackIds[1] = Utils.APP_RADIUS_IOH;
		getMuxConfiguration().put(CONF_STACK_ID, stackIds);

		getMuxConfiguration().put(CONF_MUX_START, true);
		getMuxConfiguration().put(CONF_THREAD_SAFE, true);
		getMuxConfiguration().put(CONF_L4_PROTOCOLS, new String[] {"udp"});
		
		RadiusClientFacade.setMuxConnectionManager(_connectionManager);
		RadiusServer.setMuxConnectionManager(_connectionManager);

		try {
			Utils.getContainer().init(_application);
			_application.initDone();
		}
		catch (Exception e) {
			LOGGER.warn("Failed to initialize radius applications", e);
		}
	}

	/**
	 * Takes into account the modification of properties.
	 * 
	 * @param cnf The configuration.
	 * @throws Exception
	 */
	public synchronized void updateAgentConfig(Dictionary cnf)
		throws Exception {
		RadiusProperties.updateProperties(cnf);
		setUseBestEffortForMissingProxyState (ConfigHelper.getBoolean (cnf, PropertiesDeclaration.PROXY_STATE_BEST_EFFORT, false));
		if (LOGGER.isDebugEnabled ()) LOGGER.debug ("isUsingBestEffortForMissingProxyState : "+isUsingBestEffortForMissingProxyState());

		// check if the callout should serve any stack or a specific one
		String stacks = ConfigHelper.getString(cnf, PropertiesDeclaration.STACK, "*").trim();
		if (stacks.equals("*")) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Available for any radius stack");
			}
			getMuxConfiguration().put(CONF_STACK_INSTANCE, new String[0]);
		} else {
			StringTokenizer st = new StringTokenizer(stacks);
			String[] stackInstance = new String[st.countTokens()];
			int k = 0;
			while (st.hasMoreTokens()) {
				stackInstance[k] = st.nextToken();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Serving radius stack: " + stackInstance[k]);
				}
				++k;
			}
			getMuxConfiguration().put(CONF_STACK_INSTANCE, stackInstance);
		}
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#destroy()
	 */
	@Override
	public void destroy() {
		if (Utils.getContainer() != null) {
			Utils.getContainer().destroy();
		}
	}

	@com.alcatel_lucent.as.management.annotation.command.Command(code = 1, snmpName = "Reset", oid = 50, desc = "Clear all pending requests")
	public void clearPendingRequests() {
		synchronized (_connectionManager) {
			Enumeration e = _connectionManager.getMuxConnections();
			while (e.hasMoreElements()) {
				MuxConnection connection = (MuxConnection) e.nextElement();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("commandEvent: Reseting requests for connection #" + connection.getId());
				}
				Utils.removeCommands(connection);
			}
		}
	}

	/**
	 * @see com.nextenso.mux.event.MuxMonitorable#getCounters()
	 */
	public int[] getCounters() {
		int[] res = new int[] { getAccountingRequestNb(), getAccessRequestNb() };
		return res;
	}

	@Counter(index = 0, snmpName = "NumAccountingReq", oid = 100, desc = "Accounting Requests processed")
	public int getAccountingRequestNb() {
		return _accountCounter.get();
	}

	@Counter(index = 1, snmpName = "NumAccessReq", oid = 101, desc = "Access Requests processed")
	public int getAccessRequestNb() {
		return _accessCounter.get();
	}

	/**
	 * @see com.nextenso.mux.event.MuxMonitorable#getMajorVersion()
	 */
	public int getMajorVersion() {
		return 1;
	}

	/**
	 * @see com.nextenso.mux.event.MuxMonitorable#getMinorVersion()
	 */
	public int getMinorVersion() {
		return 0;
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#muxOpened(com.nextenso.mux.MuxConnection)
	 */
	@Override
	public void muxOpened(MuxConnection connection) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("muxOpened: connection=" + connection);
			LOGGER.debug("muxOpened: opening client on : "+RadiusProperties.getClientSrcIP ()+"/"+RadiusProperties.getClientSrcPort ());
		}

		// first, we open client UDP socket(s)
		int portOrig = RadiusProperties.getClientSrcPort ();
		for (int i=0; i<RadiusProperties.getClientSrcPortRange (); i++){
		    int port = (portOrig != 0) ? portOrig+i : 0;
		    connection.sendUdpSocketBind(100L+(long)i, RadiusProperties.getClientSrcIP (), port, false);
		}
		
		int nb = 0;
		synchronized (_connectionManager) {
			_connectionManager.addMuxConnection(connection);
			nb = _connectionManager.size();
		}
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#muxClosed(com.nextenso.mux.MuxConnection)
	 */
	@Override
	public void muxClosed(MuxConnection connection) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("muxClosed: connection=" + connection);
		}
		synchronized (_connectionManager) {
			_connectionManager.removeMuxConnection(connection);
		}
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#udpSocketBound(com.nextenso.mux.MuxConnection,
	 *      int, int, int, boolean, long, int)
	 */
	@Override
	public void udpSocketBound(MuxConnection connection, int sockId, int localIP, int localPort, boolean shared, long bindId, int errno) {
		if (LOGGER.isDebugEnabled()) {
		    LOGGER.debug("udpSocketBound: bindId="+bindId+", sockId=" + sockId + ", shared=" + shared + ", localIP=" + MuxUtils.getIPAsString (localIP) + ", localPort="+localPort+", errno=" + errno);
		}
		if (shared) {
			return;
		}

		if (errno == 0) {
		    if (bindId >= 100L){
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("udpSocketBound: Radius Client socket opened : sockId=" + sockId);
			}
			if (Utils.setSockId(connection, sockId)){ // returns true when all bind are ok
			    LOGGER.warn ("All client sockets bound : sendMuxStart");
			    connection.sendMuxStart();
			}
		    }
		    // else this is the port open by the stack and advertized by it.
		} else {
			LOGGER.warn("Could not open Radius Client socket : " + MuxUtils.getErrorMessage(errno));
			// exit ?
		}
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#udpSocketClosed(com.nextenso.mux.MuxConnection,
	 *      int)
	 */
	@Override
	public void udpSocketClosed(MuxConnection connection, int sockId) {
		// nothing to do
		if (LOGGER.isInfoEnabled()) {
		    LOGGER.info("Radius Client socket closed : sockId=" + sockId);
		}
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#udpSocketData(com.nextenso.mux.MuxConnection,
	 *      int, long, int, int, int, int, byte[], int, int)
	 */
	@Override
	public void udpSocketData(MuxConnection connection, int sockId, long sessionId, int remoteIP, int remotePort, int virtualIP, int virtualPort,
			byte[] data, int offset, int len) {
		// Only Proxy-State is useful for responses
		int code = data[offset] & 0xFF;
		int identifier = (len > 1) ? data[offset + 1] & 0xFF : -1;
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("udpSocketData: received proxy data:" + getDataDescription(remoteIP, remotePort, len, code, identifier));
		}
		if (len == 1) {
			discard(sessionId, connection, getDataDescription(remoteIP, remotePort, len, code, identifier), "Invalid Radius Message length=1");
			return;
		}
		boolean isAccounting = true;
		if (code == AuthUtils.CODE_ACCESS_REQUEST || code == AuthUtils.CODE_ACCESS_ACCEPT || code == AuthUtils.CODE_ACCESS_REJECT
				|| code == AuthUtils.CODE_ACCESS_CHALLENGE) {
			isAccounting = false;
		}

		boolean isRequest = false;
		if (code == AuthUtils.CODE_ACCESS_REQUEST || code == CoAUtils.CODE_COA_REQUEST || code == DisconnectUtils.CODE_DISCONNECT_REQUEST
				|| code == AcctUtils.CODE_ACCOUNTING_REQUEST) {
			isRequest = true;
		}

		try {
			Command cmd = null;
			if (isRequest) {
				int rightSideInt = sockId & 0xFF | ((identifier & 0xFF) << 8) | ((remotePort & 0xFFFF) << 16);
				long rightSide = ((long)rightSideInt) & 0xFFFFFFFFL;
				long leftSide = ((long) remoteIP) << 32;
				long requestId =  leftSide | rightSide;
				Utils.Key key = new Utils.Key (requestId, data, offset+4);

				cmd = Utils.getCommand(connection, key);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("udpSocketData: access request code, message Id=" + requestId + ", command=" + cmd);
				}
				if (cmd != null) {
					boolean sentAgain = cmd.handleRetransmission();

					if (LOGGER.isDebugEnabled()) {
						if (sentAgain) {
							LOGGER.debug("udpSocketData: response retransmitted : " + getDataDescription(remoteIP, remotePort, len, code, identifier));
						} else {
							LOGGER.debug("udpSocketData: retransmission ignored : " + getDataDescription(remoteIP, remotePort, len, code, identifier));
						}
					}

					return;
				}

				if (isAccounting) {
					cmd = new AcctCommand(connection, sockId, requestId, identifier, remoteIP, remotePort);
					_accountCounter.incrementAndGet();
				} else {
					cmd = new AuthCommand(connection, sockId, requestId, identifier, remoteIP, remotePort);
					_accessCounter.incrementAndGet();
				}
				cmd.setPlatformExecutor (Utils.getCurrentExecutor ());
				cmd.setKey (key);
				Utils.putCommand(connection, cmd);
				cmd.handleRequest(data, offset, len, code);
			} else {
				// response
				int index = RadiusInputStream.lastIndexOfAttribute(RadiusUtils.PROXY_STATE, data, offset + 20, len - 20);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("udpSocketData: response received, index=" + index);
				}

				long reqid = 0;
				if (index >= 0) {
					int length = data[index + 1] & 0xFF;
					if (length == RadiusManager.PROXY_STATE_LENGTH_FULL) {
						// response to an own radius request
						if (isUsingBestEffortForMissingProxyState ())
							RadiusManager.handleProxyData(sockId, identifier, data, offset, len);
						else
							RadiusManager.handleProxyData(data, offset, len, index);
						return;
					}
					// response to a proxied request
					reqid = Utils.getRequestId(data, index + 2, length - 2);
				} else {
					if (isUsingBestEffortForMissingProxyState()) {
						RadiusManager.handleProxyData(sockId, identifier, data, offset, len);
					} else {
						// error case
						discard(sessionId, null, getDataDescription(remoteIP, remotePort, len, code, identifier), "Proxy-State corrupted");
					}
					return;
				}

				cmd = Utils.getCommand(connection, reqid);
				if (cmd == null) {
					discard(sessionId, null, getDataDescription(remoteIP, remotePort, len, code, identifier), "No Matching Request (reqid=" + reqid + ")");
					return;
				}
				cmd.handleResponse(data, offset, len, code);
			}
		}
		catch (Throwable t) {
			LOGGER.warn("Unidentified exception in Radius Agent while handling data: " + getDataDescription(remoteIP, remotePort, len, code, identifier), t);
		}
	}

	/**
	 * discard
	 */
	private void discard(long sessionId, MuxConnection connection, String description, String reason) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Discarding data: " + description + " : " + reason);
		}
	}

	/**
	 * getDataDescription
	 */
	private String getDataDescription(int remoteIP, int remotePort, int len, int code, int identifier) {
		StringBuilder buff = new StringBuilder();
		buff.append("[remoteIP=");
		buff.append(MuxUtils.getIPAsString(remoteIP));
		buff.append(", remotePort=");
		buff.append(String.valueOf(remotePort));
		buff.append(", identifier=");
		buff.append(String.valueOf(identifier));
		buff.append(", length=");
		buff.append(String.valueOf(len));
		buff.append(", code=").append(code);
		buff.append("]");
		return buff.toString();
	}

	public void setUseBestEffortForMissingProxyState(boolean useBestEffortForMissingProxyState) {
		_isUsingBestEffortForMissingProxyState = useBestEffortForMissingProxyState;
		RadiusManager.setIsUsingBestEffortForMissingProxyState(useBestEffortForMissingProxyState);
	}

	public boolean isUsingBestEffortForMissingProxyState() {
		return _isUsingBestEffortForMissingProxyState;
	}

	public void bindPlatformExecutors(PlatformExecutors pfe) {
		Utils.setPlatformExecutors(pfe);
	}

}
