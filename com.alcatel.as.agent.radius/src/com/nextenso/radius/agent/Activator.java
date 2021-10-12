// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent;

import static com.nextenso.proxylet.admin.radius.RadiusBearer.ACCT_REQUEST_CHAIN;
import static com.nextenso.proxylet.admin.radius.RadiusBearer.ACCT_RESPONSE_CHAIN;
import static com.nextenso.proxylet.admin.radius.RadiusBearer.AUTH_REQUEST_CHAIN;
import static com.nextenso.proxylet.admin.radius.RadiusBearer.AUTH_RESPONSE_CHAIN;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.ServiceDependency;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.management.platform.ConfigManager;
import com.alcatel.as.management.platform.CreateInstancePlugin;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.util.osgi.DependencyActivatorHelper;
import com.nextenso.mux.MuxHandler;
import com.nextenso.proxylet.engine.DeployerDescriptor;
import com.nextenso.proxylet.engine.ProxyletApplication;
import com.nextenso.proxylet.engine.criterion.CriterionParser;
import com.nextenso.proxylet.radius.AuthenticationManager;
import com.nextenso.proxylet.radius.RadiusClientFactory;
import com.nextenso.proxylet.radius.acct.AccountingRequestProxylet;
import com.nextenso.proxylet.radius.acct.AccountingResponseProxylet;
import com.nextenso.proxylet.radius.auth.AccessRequestProxylet;
import com.nextenso.proxylet.radius.auth.AccessResponseProxylet;
import com.nextenso.proxylet.admin.Bearer;
import com.nextenso.proxylet.admin.radius.RadiusBearer;
import com.nextenso.proxylet.admin.radius.agg.RadiusBearerAggregator;
import com.nextenso.radius.agent.client.RadiusClientFactoryImpl;
import com.nextenso.radius.agent.engine.criterion.RadiusCriterionParser;
import com.nextenso.radius.agent.impl.AuthenticationManagerImpl;

public class Activator extends DependencyActivatorBase {

	private static class Descriptor implements DeployerDescriptor {
        Dictionary _config; //injected

		private Map<String, Class[]> _chains = new Hashtable<String, Class[]>();

		public Descriptor() {
			_chains.put(ACCT_REQUEST_CHAIN, new Class[] { AccountingRequestProxylet.class });
			_chains.put(ACCT_RESPONSE_CHAIN, new Class[] { AccountingResponseProxylet.class });
			_chains.put(AUTH_REQUEST_CHAIN, new Class[] { AccessRequestProxylet.class });
			_chains.put(AUTH_RESPONSE_CHAIN, new Class[] { AccessResponseProxylet.class });
		}

		public String getProtocol() {
			return "radius";
		}

		public CriterionParser getParser() {
			return new RadiusCriterionParser();
		}

		public Map<String, Class[]> getBindings() {
			return _chains;
		}

		public String getProxyletsConfiguration() { 
			return (String)_config.get("radiusagent.proxylets"); 
		}
                
		public Bearer.Factory getBearerFactory() { 
			return new RadiusBearer(); 
		}
	}

	/**
	 * @see com.alcatel.as.util.osgi.DependencyActivatorHelper#init(org.osgi.framework.BundleContext,
	 */
	@Override
	public void init(BundleContext ctx, DependencyManager mgr)
		throws Exception {

		//DEPLOYER
		Dictionary protocols = new Hashtable();
		protocols.put("protocol", "radius");
		Component deployerService = createComponent();
		deployerService.setInterface(DeployerDescriptor.class.getName(), protocols);
		deployerService.setImplementation(new Descriptor());
                deployerService.add(createServiceDependency()
                    .setService(Dictionary.class, "(service.pid=radiusagent)")
                    .setRequired(true).setAutoConfig("_config"));
		mgr.add(deployerService);

		// Route table
		AuthenticationManagerImpl authManager = new AuthenticationManagerImpl();
		Utils.setAuthenticationManager(authManager);		
		mgr.add(createComponent()
			.setInterface(AuthenticationManager.class.getName(), null).setImplementation(authManager));

		// AGENT
		protocols = new Hashtable();
		protocols.put("protocol", "Radius");
		Component agentService = createComponent();
		agentService.setInterface(MuxHandler.class.getName(), protocols);
		agentService.setImplementation(Agent.class);
		agentService.setCallbacks(null, null, null, null);

		ServiceDependency systemConfig = createServiceDependency();
		systemConfig.setService(Dictionary.class, "(service.pid=system)");
		systemConfig.setAutoConfig(false);
		systemConfig.setRequired(true);
		systemConfig.setCallbacks("setSystemConfig", null, null);
		agentService.add(systemConfig);

		//		ServiceDependency secretManager = createServiceDependency();
		//		secretManager.setService(AuthenticationManager.class);
		//		secretManager.setCallbacks("setAuthenticationManager", null);
		//		secretManager.setAutoConfig(false);
		//		secretManager.setRequired(true);
		//		agentService.add(secretManager);

		ServiceDependency agentConfig = createServiceDependency();
		agentConfig.setService(Dictionary.class, "(service.pid=radiusagent)");
		agentConfig.setCallbacks("updateAgentConfig", null /* removed */);
		agentConfig.setAutoConfig(false);
		agentConfig.setRequired(true);
		agentService.add(agentConfig);

		ServiceDependency application = createServiceDependency();
		application.setService(ProxyletApplication.class, "(protocol=radius)");
		application.setAutoConfig(false);
		application.setRequired(true);
		application.setCallbacks("bindProxyletApplication", "unbindProxyletApplication");
		agentService.add(application);

		ServiceDependency executors = createServiceDependency();
		executors.setService(PlatformExecutors.class);
		executors.setAutoConfig(false);
		executors.setRequired(true);
		executors.setCallbacks("bindPlatformExecutors", null);
		agentService.add(executors);

		mgr.add(agentService);

		// CLIENT FACTORY
		Component factoryService = createComponent();
		factoryService.setInterface(RadiusClientFactory.class.getName(), null);
		RadiusClientFactoryImpl clientImpl = new RadiusClientFactoryImpl();
		factoryService.setImplementation(clientImpl);
		RadiusClientFactory.setRadiusClientFactory(clientImpl);
		mgr.add(factoryService);
	}
}
