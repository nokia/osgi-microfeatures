package com.nextenso.radius.agent;

import java.lang.reflect.Constructor;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;
import alcatel.tess.hometop.gateways.utils.ConfigListener;

import com.nextenso.agent.Launcher;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.socket.TcpMessageParser;
import com.nextenso.proxylet.admin.Bearer;
import com.nextenso.proxylet.admin.Protocol;
import com.nextenso.proxylet.admin.xml.GenericParser;
import com.nextenso.proxylet.engine.ProxyletApplication;
import com.nextenso.proxylet.engine.criterion.CriterionParser;
import com.nextenso.radius.agent.engine.criterion.RadiusCriterionParser;

public class RadiusLauncher
		extends Launcher
		implements ConfigListener {

	public static final String PXLET_CONTEXT = "radiusagent.pxletContext";
	private static final Logger LOGGER = Logger.getLogger("agent.radius.launcher");
	public static final String BEST_EFFORT_PROPERTY =  "radiusagent.proxystate.besteffort";
	private Agent _agent = null;

	private RadiusLauncher(Object arg, Config conf)
			throws Exception {
		this(arg, conf, getNewAgent(conf));
	}

	private static Agent getNewAgent(Config conf) {
		Agent agent = new Agent();
		boolean bestEffort = conf.getBoolean(BEST_EFFORT_PROPERTY, false);
		agent.setUseBestEffortForMissingProxyState(bestEffort);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getNewAgent: bestEffort=" + agent.isUsingBestEffortForMissingProxyState());
		}
		conf.remove(BEST_EFFORT_PROPERTY);
		return agent;
	}

	public static RadiusLauncher start(Object arg)
		throws Exception {
		return start(arg, "radius.properties");
	}

	public static RadiusLauncher start(Object arg, String configFile)
		throws Exception {
		Config conf = loadConfig(configFile);

		conf.setProperty("radiusagent.stackInstance", "*");
		RadiusLauncher res = new RadiusLauncher(arg, conf);
		res.startListening();
		return res;
	}

	/**
	 * @see com.nextenso.agent.Launcher#stop()
	 */
	@Override
	public void stop() {
		super.stop();
	}

	protected RadiusLauncher(Object arg, Config config, Agent muxHandler)
			throws Exception {
		super(arg, config, muxHandler);
	}

	@Override
	protected String getDescFile() {
		return "calloutAgent/radius.desc";
	}

	@Override
	protected int getStackAppId() {
		return Utils.APP_RADIUS_STACK;
	}

	@Override
	protected String getStackAppName() {
		return "RadiusStack";
	}

	/**
	 * @see com.nextenso.agent.Launcher#getTcpMessageParser()
	 */
	@Override
	protected TcpMessageParser getTcpMessageParser() {
		return null;
	}

	/**
	 * @see com.nextenso.agent.Launcher#init(alcatel.tess.hometop.gateways.utils.Config,
	 *      com.nextenso.mux.MuxHandler)
	 */
	@Override
	protected void init(Config config, MuxHandler handler) {
		_agent = (Agent) handler;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("init: config=" + config);
		}
		long exitDelay = config.getLong("radiusagent.exitDelay", 5L);
		config.put("radiusagent.exitDelay", Long.valueOf(exitDelay));

		_agent.setSystemConfig(config);

		try {
			Constructor constr = Class.forName(LAUNCHER_APPLICATION_IMPL).getConstructor(Bearer.class, CriterionParser.class);
			ProxyletApplication app = (ProxyletApplication) constr.newInstance(getBearer("radius", config.getString(PXLET_CONTEXT)), new RadiusCriterionParser());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("init: application=" + app);
			}
			_agent.bindProxyletApplication(app);
			_agent.bindPlatformExecutors(getPlatformExecutors());
		}
		catch (Throwable t) {
			LOGGER.warn("Error while loading DiameterLauncher ProxyletApplication", t);
		}

		try {
			_agent.updateAgentConfig(config);
		}
		catch (Exception e) {
			LOGGER.error("cannot initialize the Radius agent with the configuration", e);
		}
	}

	public static Bearer getBearer(String protocolName, String xml) {
		Protocol protocol = Protocol.getProtocolInstance(protocolName.toUpperCase(Locale.getDefault()));
		GenericParser myparser = protocol.getParserInstance();
		Bearer bearer = null;
		try {
			Document document = myparser.parseString(xml);
			bearer = protocol.getBearerInstance();
			bearer.setNode(document.getDocumentElement());
		}
		catch (Throwable t) {
			throw new IllegalArgumentException("Failed loading launcher mode bearer for " + protocolName, t);
		}
		return bearer;
	}

	/**
	 * @see alcatel.tess.hometop.gateways.utils.ConfigListener#propertyChanged(alcatel.tess.hometop.gateways.utils.Config,
	 *      java.lang.String[])
	 */
	public void propertyChanged(Config cnf, String[] propertyNames)
		throws ConfigException {
		try {
			_agent.updateAgentConfig(cnf);
		}
		catch (Exception e) {
			LOGGER.error("Cannot take into account the changed properties", e);
		}

	}

	public static void main(String args[])
		throws Exception {
		String arg = null;
		if (args != null && args.length > 0) {
			arg = args[0];
		}
		start(arg);
		Thread.sleep(Integer.MAX_VALUE);
	}

}
