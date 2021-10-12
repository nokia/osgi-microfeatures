// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.engine;

import static com.nextenso.proxylet.engine.ProxyletApplication.REQUEST_CHAIN;
import static com.nextenso.proxylet.engine.ProxyletApplication.REQUEST_LISTENER;
import static com.nextenso.proxylet.engine.ProxyletApplication.RESPONSE_CHAIN;
import static com.nextenso.proxylet.engine.ProxyletApplication.RESPONSE_LISTENER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.diameter.agent.impl.DiameterResponseFacade;
import com.nextenso.proxylet.engine.Context;
import com.nextenso.proxylet.engine.ProxyletApplication;
import com.nextenso.proxylet.engine.ProxyletChain;
import com.nextenso.proxylet.engine.ProxyletEnv;
import com.nextenso.proxylet.engine.xml.XMLConfigException;
import com.nextenso.proxylet.event.ProxyletEventListener;

/**
 * The Diameter proxylet context.
 */
public class DiameterProxyletContext
		extends Context {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.pxlet.context");

	private List<ProxyletEventListener> _requestListeners = new ArrayList<ProxyletEventListener>();
	private List<ProxyletEventListener> _responseListeners = new ArrayList<ProxyletEventListener>();
	private DiameterProxyletChain _responseChain, _requestChain;

	/**
	 * Constructor for this class.
	 * 
	 * @param app The application.
	 * @throws XMLConfigException
	 */
	public DiameterProxyletContext(ProxyletApplication app)
			throws XMLConfigException {
		super(LOGGER);
		_requestChain = new DiameterProxyletChain(this);
		_responseChain = new DiameterProxyletChain(this);

		load(app);
	}

	/**
	 * @see com.nextenso.proxylet.engine.Context#load(com.nextenso.proxylet.engine.ProxyletApplication)
	 */
	@Override
	public void load(ProxyletApplication app)
		throws XMLConfigException {
		setName("Diameter Proxylet Context");
		setDescription("List of diameter proxylets");

		loadChains(app);
		loadListeners(app);
	}

	/**
	 * @see com.nextenso.proxylet.engine.Context#init(ProxyletApplication)
	 */
	@Override
	public void init(ProxyletApplication app)
		throws Exception {
		super.init(app);

		// we initialize the proxylets
		getRequestChain().init();
		getResponseChain().init();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Initialization done");
		}
	}

	/**
	 * Gets the request chain.
	 * 
	 * @return The request chain.
	 */
	public DiameterProxyletChain getRequestChain() {
		return _requestChain;
	}

	/**
	 * Gets the response chain.
	 * 
	 * @return The response chain.
	 */
	public DiameterProxyletChain getResponseChain() {
		return _responseChain;
	}

	/**
	 * Adds a listener for requests.
	 * 
	 * @param listener The listener to be added.
	 */
	public void addRequestListener(ProxyletEventListener listener) {
		// no need to synchronize - called at initialization
		// we avoid duplicates
		int index = _requestListeners.indexOf(listener);
		if (index == -1) {
			_requestListeners.add(listener);
		}
	}

	/**
	 * Adds a listener for responses.
	 * 
	 * @param listener The listener to be added.
	 */
	public void addResponseListener(ProxyletEventListener listener) {
		// no need to synchronize - called at initialization
		// we avoid duplicates
		int index = _responseListeners.indexOf(listener);
		if (index == -1) {
			_responseListeners.add(listener);
		}
	}

	public List<ProxyletEventListener> getListeners(@SuppressWarnings("unused") DiameterRequestFacade msg) {
		return _requestListeners;
	}

	public List<ProxyletEventListener> getListeners(@SuppressWarnings("unused") DiameterResponseFacade msg) {
		return _responseListeners;
	}

	/**
	 * @see com.nextenso.proxylet.engine.Context#loadListener(com.nextenso.proxylet.admin.Listener,
	 *      com.nextenso.proxylet.engine.ProxyAppEnv)
	 */
	@Override
	protected void loadListeners(ProxyletApplication app)
		throws XMLConfigException {
		super.loadListeners(app);

		for (String name : app.getListeners(REQUEST_LISTENER)) {
			addRequestListener((ProxyletEventListener) app.getListener(REQUEST_LISTENER, name));
		}
		for (String name : app.getListeners(RESPONSE_LISTENER)) {
			addResponseListener((ProxyletEventListener) app.getListener(RESPONSE_LISTENER, name));
		}
	}

	/**
	 * Loads the different proxylet chains into this context.
	 * 
	 * @param bearer The bearer.
	 * @throws XMLConfigException
	 */
	private void loadChains(ProxyletApplication app) {
		Map<String, List<ProxyletEnv>> chains = new HashMap<String, List<ProxyletEnv>>();
		chains.put(REQUEST_CHAIN, new ArrayList<ProxyletEnv>());
		chains.put(RESPONSE_CHAIN, new ArrayList<ProxyletEnv>());

		super.loadChains(chains, app);

		if (chains.get(REQUEST_CHAIN).size() > 0) {
			getRequestChain().setValue(chains.get(REQUEST_CHAIN));
		}
		if (chains.get(RESPONSE_CHAIN).size() > 0) {
			getResponseChain().setValue(chains.get(RESPONSE_CHAIN));
		}
	}

	@Override
	public ProxyletChain[] getProxyletChains() {
		return new DiameterProxyletChain[] { _requestChain, _responseChain };
	}

}
