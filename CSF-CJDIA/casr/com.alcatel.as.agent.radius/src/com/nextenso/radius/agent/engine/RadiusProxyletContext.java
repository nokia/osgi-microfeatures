package com.nextenso.radius.agent.engine;

import static com.nextenso.proxylet.admin.radius.RadiusBearer.ACCT_REQUEST_CHAIN;
import static com.nextenso.proxylet.admin.radius.RadiusBearer.ACCT_REQUEST_LISTENER;
import static com.nextenso.proxylet.admin.radius.RadiusBearer.ACCT_RESPONSE_CHAIN;
import static com.nextenso.proxylet.admin.radius.RadiusBearer.ACCT_RESPONSE_LISTENER;
import static com.nextenso.proxylet.admin.radius.RadiusBearer.AUTH_REQUEST_CHAIN;
import static com.nextenso.proxylet.admin.radius.RadiusBearer.AUTH_REQUEST_LISTENER;
import static com.nextenso.proxylet.admin.radius.RadiusBearer.AUTH_RESPONSE_CHAIN;
import static com.nextenso.proxylet.admin.radius.RadiusBearer.AUTH_RESPONSE_LISTENER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.engine.Context;
import com.nextenso.proxylet.engine.ProxyletApplication;
import com.nextenso.proxylet.engine.ProxyletChain;
import com.nextenso.proxylet.engine.ProxyletEnv;
import com.nextenso.proxylet.engine.xml.XMLConfigException;
import com.nextenso.proxylet.event.ProxyletEventListener;

public class RadiusProxyletContext
		extends Context {

	private static final int ACCOUNTING_REQUEST_CHAIN = 0;
	private static final int ACCOUNTING_RESPONSE_CHAIN = 1;
	private static final int ACCESS_REQUEST_CHAIN = 2;
	private static final int ACCESS_RESPONSE_CHAIN = 3;

	private static final Logger LOGGER = Logger.getLogger("agent.radius.pxlet.context");

	private List<ProxyletEventListener> _accountingRequestListeners = new ArrayList<ProxyletEventListener>();
	private List<ProxyletEventListener> _accountingResponseListeners = new ArrayList<ProxyletEventListener>();
	private List<ProxyletEventListener> _accessRequestListeners = new ArrayList<ProxyletEventListener>();
	private List<ProxyletEventListener> _accessResponseListeners = new ArrayList<ProxyletEventListener>();
	private List<RadiusProxyletChain> _chains = new ArrayList<RadiusProxyletChain>();

	public RadiusProxyletContext(ProxyletApplication app)
			throws XMLConfigException {
		super(LOGGER);
		_chains.add(new RadiusProxyletChain(this, ACCOUNTING_REQUEST_CHAIN));
		_chains.add(new RadiusProxyletChain(this, ACCOUNTING_RESPONSE_CHAIN));
		_chains.add(new RadiusProxyletChain(this, ACCESS_REQUEST_CHAIN));
		_chains.add(new RadiusProxyletChain(this, ACCESS_RESPONSE_CHAIN));
		load(app);
	}

	/**
	 * @see com.nextenso.proxylet.engine.Context#load(com.nextenso.proxylet.engine.ProxyletApplication)
	 */
	@Override
	public void load(ProxyletApplication app)
		throws XMLConfigException {
		setName("Radius Proxylet Context");
		setDescription("List of radius proxylets");

		loadChains(app);
		loadListeners(app);
	}

	/**
	 * @see com.nextenso.proxylet.engine.Context#init(com.nextenso.proxylet.engine.ProxyletApplication)
	 */
	@Override
	public void init(ProxyletApplication app)
		throws Exception {
		super.init(app);
		// we initialize the proxylets
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Initializing chains");
		}

		for (ProxyletChain chain : _chains) {
			chain.init();
		}
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Initialization done");
		}
	}

	private ProxyletEventListener[] getListeners(List<ProxyletEventListener> list) {
		ProxyletEventListener[] res = list.toArray(new ProxyletEventListener[list.size()]);
		return res;
	}

	public RadiusProxyletChain getAccountingRequestChain() {
		return _chains.get(ACCOUNTING_REQUEST_CHAIN);
	}

	public RadiusProxyletChain getAccountingResponseChain() {
		return _chains.get(ACCOUNTING_RESPONSE_CHAIN);
	}

	public RadiusProxyletChain getAccessRequestChain() {
		return _chains.get(ACCESS_REQUEST_CHAIN);
	}

	/**
	 * Gets the Access Response chain.
	 * 
	 * @return The Access Response chain.
	 */
	public RadiusProxyletChain getAccessResponseChain() {
		return _chains.get(ACCESS_RESPONSE_CHAIN);
	}

	private void addListener(ProxyletEventListener listener, List<ProxyletEventListener> listeners) {
		// no need to synchronize - called at initialization
		// we avoid duplicates
		int index = listeners.indexOf(listener);
		if (index == -1) {
			listeners.add(listener);
		}
	}

	public ProxyletEventListener[] getAccountingRequestListeners() {
		return getListeners(_accountingRequestListeners);
	}

	public ProxyletEventListener[] getAccountingResponseListeners() {
		return getListeners(_accountingResponseListeners);
	}

	public ProxyletEventListener[] getAccessRequestListeners() {
		return getListeners(_accessRequestListeners);
	}

	public ProxyletEventListener[] getAccessResponseListeners() {
		return getListeners(_accessResponseListeners);
	}

	/**
	 * @see com.nextenso.proxylet.engine.Context#loadListeners(com.nextenso.proxylet.engine.ProxyletApplication)
	 */
	@Override
	protected void loadListeners(ProxyletApplication app)
		throws XMLConfigException {
		super.loadListeners(app);
		for (String name : app.getListeners(ACCT_REQUEST_LISTENER)) {
			ProxyletEventListener listener = (ProxyletEventListener) app.getListener(ACCT_REQUEST_LISTENER, name);
			addListener(listener, _accessRequestListeners);
		}
		for (String name : app.getListeners(ACCT_RESPONSE_LISTENER)) {
			ProxyletEventListener listener = (ProxyletEventListener) app.getListener(ACCT_RESPONSE_LISTENER, name);
			addListener(listener, _accessResponseListeners);
		}
		for (String name : app.getListeners(AUTH_REQUEST_LISTENER)) {
			ProxyletEventListener listener = (ProxyletEventListener) app.getListener(AUTH_REQUEST_LISTENER, name);
			addListener(listener, _accessRequestListeners);
		}
		for (String name : app.getListeners(AUTH_RESPONSE_LISTENER)) {
			ProxyletEventListener listener = (ProxyletEventListener) app.getListener(AUTH_RESPONSE_LISTENER, name);
			addListener(listener, _accessResponseListeners);
		}
	}

	/**
	 * Loads the different proxylet chains into this context.
	 */
	private void loadChains(ProxyletApplication app) {
		Map<String, List<ProxyletEnv>> chains = new HashMap<String, List<ProxyletEnv>>();
		chains.put(ACCT_REQUEST_CHAIN, new ArrayList<ProxyletEnv>());
		chains.put(ACCT_RESPONSE_CHAIN, new ArrayList<ProxyletEnv>());
		chains.put(AUTH_REQUEST_CHAIN, new ArrayList<ProxyletEnv>());
		chains.put(AUTH_RESPONSE_CHAIN, new ArrayList<ProxyletEnv>());

		super.loadChains(chains, app);

		if (chains.get(ACCT_REQUEST_CHAIN).size() > 0) {
			getAccountingRequestChain().setValue(chains.get(ACCT_REQUEST_CHAIN));
		}
		if (chains.get(ACCT_RESPONSE_CHAIN).size() > 0) {
			getAccountingResponseChain().setValue(chains.get(ACCT_RESPONSE_CHAIN));
		}
		if (chains.get(AUTH_REQUEST_CHAIN).size() > 0) {
			getAccessRequestChain().setValue(chains.get(AUTH_REQUEST_CHAIN));
		}
		if (chains.get(AUTH_RESPONSE_CHAIN).size() > 0) {
			getAccessResponseChain().setValue(chains.get(AUTH_RESPONSE_CHAIN));
		}
	}

	/**
	 * @see com.nextenso.proxylet.engine.Context#getProxyletChains()
	 */
	@Override
	public ProxyletChain[] getProxyletChains() {
		return _chains.toArray(new ProxyletChain[_chains.size()]);
	}
}
