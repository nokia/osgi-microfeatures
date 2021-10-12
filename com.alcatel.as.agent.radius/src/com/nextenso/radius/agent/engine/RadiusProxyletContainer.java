// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent.engine;

import java.util.Dictionary;

import org.apache.log4j.Logger;

import com.nextenso.proxylet.engine.ProxyletApplication;
import com.nextenso.proxylet.engine.ProxyletContainer;
import com.nextenso.radius.agent.impl.AccessRequestFacade;
import com.nextenso.radius.agent.impl.AccessResponseFacade;
import com.nextenso.radius.agent.impl.AccountingRequestFacade;
import com.nextenso.radius.agent.impl.AccountingResponseFacade;

/**
 * The RadiusProxyletContainer manages all the deployed Contexts. It is at the
 * top of the whole architecture.
 */

public class RadiusProxyletContainer
		extends ProxyletContainer {

	private static Logger LOGGER = Logger.getLogger("agent.radius.pxlet.container");

	private RadiusProxyletEngine _radiusEngine;
	private RadiusProxyletContext _radiusContext;
	private Dictionary _systemconf;

	/**
	 * Constructor
	 */
	public RadiusProxyletContainer(Dictionary conf) {
		super(LOGGER);
		_radiusEngine = new RadiusProxyletEngine(this);
		setProxyletEngine(_radiusEngine);
		_systemconf = conf;
	}

	/**
	 * Returns the app Name
	 */
	public String getAppName() {
		return "Radius proxylets";
	}

	/**
	 * Returns the ProxyletChain for a given acct request
	 */
	public RadiusProxyletChain getChain(@SuppressWarnings("unused") AccountingRequestFacade req) {
		return _radiusContext.getAccountingRequestChain();
	}

	/**
	 * Returns the ProxyletChain for a given acct response
	 */
	public RadiusProxyletChain getChain(@SuppressWarnings("unused") AccountingResponseFacade resp) {
		return _radiusContext.getAccountingResponseChain();
	}

	/**
	 * Returns the ProxyletChain for a given auth request
	 */
	public RadiusProxyletChain getChain(@SuppressWarnings("unused") AccessRequestFacade req) {
		return _radiusContext.getAccessRequestChain();
	}

	/**
	 * Returns the ProxyletChain for a given auth response
	 */
	public RadiusProxyletChain getChain(@SuppressWarnings("unused") AccessResponseFacade resp) {
		return _radiusContext.getAccessResponseChain();
	}

	/**
	 * Returns the RadiusProxyletContext.
	 */
	public RadiusProxyletContext getRadiusProxyletContext() {
		return _radiusContext;
	}

	/**
	 * Returns the radius Engine
	 */
	public RadiusProxyletEngine getRadiusProxyletEngine() {
		return _radiusEngine;
	}

	/*******************************************************
	 * External calls
	 *******************************************************/

	@Override
	public void init(ProxyletApplication app)
		throws Exception {
		_radiusEngine.checkLicense();

		_radiusContext = new RadiusProxyletContext(app);
		_radiusContext.setSystemProperties(_systemconf);
		super.setContext(_radiusContext);
		super.init(app);
	}

	/**
	 * Specifies the number of accounting request proxylets deployed
	 */
	public int accountingRequestProxyletsSize() {
		return _radiusContext.getAccountingRequestChain().getSize();
	}

	/**
	 * Specifies the number of accounting response proxylets deployed
	 */
	public int accountingResponseProxyletsSize() {
		return _radiusContext.getAccountingResponseChain().getSize();
	}

	/**
	 * Specifies the number of access request proxylets deployed
	 */
	public int accessRequestProxyletsSize() {
		return _radiusContext.getAccessRequestChain().getSize();
	}

	/**
	 * Specifies the number of access response proxylets deployed
	 */
	public int accessResponseProxyletsSize() {
		return _radiusContext.getAccessResponseChain().getSize();
	}

}
