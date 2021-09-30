package com.nextenso.diameter.agent.engine;

import java.util.Dictionary;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.engine.ProxyletApplication;
import com.nextenso.proxylet.engine.ProxyletContainer;

/**
 * The DiameterProxyletContainer manages all the deployed Contexts. It is at the
 * top of the whole architecture.
 */

public class DiameterProxyletContainer
		extends ProxyletContainer {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.pxlet");
	private Dictionary _systemconf;

	/**
	 * Constructor
	 */
	public DiameterProxyletContainer(Dictionary sc) {
		super(LOGGER);
		_systemconf = sc;
	}

	/**
	 * Gets the chain for the requests.
	 * 
	 * @return The chain or null if not found.
	 */
	public DiameterProxyletChain getRequestChain() {
		DiameterProxyletContext dContext = getDiameterProxyletContext();
		if (dContext == null) {
			return null;
		}
		return dContext.getRequestChain();
	}

	/**
	 * Gets the chain for the reponses.
	 * 
	 * @return The chain or null if not found.
	 */
	public DiameterProxyletChain getResponseChain() {
		DiameterProxyletContext dContext = getDiameterProxyletContext();
		if (dContext == null) {
			return null;
		}
		return dContext.getResponseChain();
	}

	/**
	 * Returns the DiameterProxyletContext.
	 */
	public DiameterProxyletContext getDiameterProxyletContext() {
		return (DiameterProxyletContext) getContext();
	}

	/**
	 * Returns the diameter Engine
	 */
	public DiameterProxyletEngine getDiameterProxyletEngine() {
		return (DiameterProxyletEngine) getProxyletEngine();
	}

	/**
	 * @see com.nextenso.proxylet.engine.ProxyletContainer#init(ProxyletApplication)
	 */
	@Override
	public void init(ProxyletApplication app)
		throws Exception {

		DiameterProxyletContext ctx = null;
		if (app != null) { //container may run without a proxyapp
			ctx = new DiameterProxyletContext(app);
			ctx.setSystemProperties(_systemconf);
			super.setContext(ctx);
			super.init(app);
		}
		super.setProxyletEngine(new DiameterProxyletEngine(ctx));

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("init: Checking license for diameter proxylets");
		}
		getProxyletEngine().checkLicense();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("init: Diameter proxylets license check ok");
		}
	}

}
