// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.agent;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.osgi.framework.BundleContext;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.util.config.ConfigHelper;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.utils.Log;

/**
 * Manager for all reactors created for all mux handlers.
 * 
 * This class if part of the DM CalloutServer components composition, and participates in all dependency and lifecycle callbacks. 
 */
public class ReactorsManager {
    private final ReactorProvider _reactorProvider; 
    private final PlatformExecutors _pfExecs; 
    private final BundleContext _bctx;

    private final static Log _logger = Log.getLogger("callout.ReactorsManager");
    private Dictionary<String, String> _agentConfig;
    private final Map<String, Reactor> _protocolReactors = new HashMap<String, Reactor>();
    private final Map<String, PlatformExecutor> _protocolQueues = new HashMap<String, PlatformExecutor>();
    private Reactor _mainReactor;
    private final MuxHandlerDescriptors _descriptors;

    public ReactorsManager(Dictionary<String, String> agentConfig, MuxHandlerDescriptors descriptors, BundleContext bctx, PlatformExecutors execs, ReactorProvider reactorProvider) {
        _agentConfig = agentConfig;
        _descriptors = descriptors;
        _bctx = bctx;
        _pfExecs = execs;
        _reactorProvider = reactorProvider;
    }

    /**
     * Creates and registers
     */
    void registerExecutors() {
        try {
            _mainReactor = _reactorProvider.getDefaultReactor();
        } catch (IOException e) {
            _logger.error("Could not get default reactor", e);
        }

        // Initialize the muxHandler descriptors
        String protocols = ConfigHelper.getString(_agentConfig, AgentProperties.MUX_HANDLERS, "");
        String convergentProtocols = ConfigHelper.getString(_agentConfig, AgentProperties.ASYNC_HANDLERS,
            "").toLowerCase();

        int elasticity = CalloutServer.getElasticity(_agentConfig);
        
        _logger.debug(
            "Setting up protocol reactors ... enabled protocols=%s, convergent protocols=%s, elasticity=%d, agent reactor=%s",
            protocols, convergentProtocols, elasticity, _mainReactor.getName());

        StringTokenizer tok = new StringTokenizer(protocols, ", ");
        while (tok.hasMoreElements()) {
            String protocol = tok.nextToken().toLowerCase(Locale.getDefault());
            MuxHandlerDesc desc = _descriptors.getFromProtocol(protocol);

            if (convergentProtocols.indexOf(protocol) == -1) {
                // Not a convergent protocol, possibly elastic
                if (!desc.isElastic()) {
                    setupNonElastic(protocol.toLowerCase());
                } else if (elasticity == 1) {
                    protocol = protocol.toLowerCase();
                    setupElastic1(protocol, protocol);
                } else {
                    setupElasticN(protocol.toLowerCase(), elasticity);
                }
            } else {
                if (!desc.isElastic()) {
                    setupNonElasticConvergent(protocol.toLowerCase());
                } else {
                    setupElasticConvergentN(protocol.toLowerCase(), elasticity);
                }
            }
        }

        register(_pfExecs.getExecutor("main"), 1, "main");
    }

    public Reactor getProtocolReactor(String protocol) {
        Reactor r = _protocolReactors.get(protocol.toLowerCase(Locale.getDefault()));
        if (r != null) {
            _logger.debug("found reactor %s for protocol %s", r, protocol);
            return r;
        }

        _logger.debug("using default reactor %s for protocol %s", _mainReactor, protocol);
        return _mainReactor;
    }

    public PlatformExecutor getProtocolQueue(String protocol, String scope) {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol.toLowerCase());
        sb.append(scope == null ? "1" : scope);
        return _protocolQueues.get(sb.toString());
    }

    private void setupNonElastic(String protocol) {
        setupElastic1(protocol, null /* no composite name */);
    }

    private void setupElastic1(String protocol, String compositeName) {
        // Create one dedicated reactor, as before.
        _logger.debug("Creating reactor for protocol %s (composite=%s)", protocol, compositeName);
        Reactor protocolReactor = _reactorProvider.create(protocol);
        protocolReactor.start();
        _protocolReactors.put(protocol, protocolReactor);
        PlatformExecutor protocolExecutor = _pfExecs.getExecutor(protocol);
        register(protocolExecutor, 1, protocol);
    }

    private void setupElasticN(String protocol, int elasticity) {
        // use default main reactor, and create one queue per protocol instance
        _logger.debug("Registering elastic(%d) protocol %s", elasticity, protocol);
        _reactorProvider.aliasReactor(protocol, _mainReactor);
        for (int scopeIndex = 1; scopeIndex <= elasticity; scopeIndex++) {
            String name = protocol + scopeIndex;
            PlatformExecutor queue = _pfExecs.createQueueExecutor(_pfExecs.getProcessingThreadPoolExecutor(), name);
            _protocolQueues.put(name, queue);
            register(queue, scopeIndex, protocol);
        }
    }

    private void setupNonElasticConvergent(String protocol) {
        _logger.debug("Registering convergent/non elastic protocol %s in main reactor", protocol);
        _reactorProvider.aliasReactor(protocol, _mainReactor);
        PlatformExecutor protocolExecutor = _pfExecs.getExecutor("main");
        register(protocolExecutor, 1, protocol);
    }

    private void setupElasticConvergentN(String protocol, int elasticity) {
        if (elasticity > 1) {
            throw new UnsupportedOperationException("elasticity must equal 1 for convergent protocol " + protocol);
        }
        _logger.debug("Registering convergent/elastic protocol %s in main reactor", protocol);
        _reactorProvider.aliasReactor(protocol, _mainReactor);
        PlatformExecutor protocolExecutor = _pfExecs.getExecutor("main");
        register(protocolExecutor, 1, protocol);
    }

    private void register(PlatformExecutor protocolExecutor, int containerIndex, String id) {
        protocolExecutor.attach(containerIndex);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("id", id);
        props.put("containerIndex", String.valueOf(containerIndex));
        _logger.debug("Registering executor %s with props %s", protocolExecutor, props);
        _bctx.registerService(PlatformExecutor.class.getName(), protocolExecutor, props);
    }
}
