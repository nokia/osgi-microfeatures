// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.agent.api;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.util.MeteringRegistry;
import com.nokia.as.gpto.agent.impl.Execution;
import com.nokia.as.gpto.agent.impl.ExecutionContextImpl;
import com.nokia.as.gpto.agent.metering.AgentMetricExporter;
import com.nokia.as.gpto.common.msg.api.AgentRegistration;
import com.nokia.as.gpto.common.msg.api.ExecutionState;
import com.nokia.as.gpto.common.msg.api.GPTOMonitorable;

@Component(provides = ScenarioRunner.class)
public class ScenarioRunner {

	public interface ScenarioListener {
		public void onScenarioFactoryChange(Map<String, Scenario.Factory> factories);
		public void onStrategyFactoryChange(Map<String, Strategy.Factory> factories);
		public void onExecutionStateChange(int executionId, String scenarioName, String strategyName, ExecutionState state);
		public void onMetricTick(Map<String, AgentRegistration> registry);
	}
	
	@ServiceDependency(required = true)
	private volatile PlatformExecutors pfexecs;

	@ServiceDependency
	MeteringRegistry meteringRegistry;
	
	@Inject
	BundleContext bundleContext;

	private static final String KEY_BASE = "as.gpto";
	private Map<Integer, Execution> executions;
	private Map<String, Scenario.Factory> scenarioFactories;
	private Map<String, Strategy.Factory> strategyFactories;
	private AgentMetricExporter exporter;
	private boolean isStandaloneMode = false;
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private static Logger LOG = Logger.getLogger(ScenarioRunner.class);
	
	
	private List<ScenarioListener> listeners;
	
	@Start
	public void start() {
		LOG.debug("Started ScenarioRunner");

		executions = new ConcurrentHashMap<>();
		scenarioFactories = new ConcurrentHashMap<>();
		strategyFactories = new ConcurrentHashMap<>();
		listeners = new CopyOnWriteArrayList<>();
		exporter = new AgentMetricExporter(meteringRegistry);
		exporter.start();
		executor.scheduleAtFixedRate(() -> pushMetrics(), 0, 1, TimeUnit.SECONDS);
	}
	
	@Stop
	public void stop() {
		executor.shutdown();
		exporter.stop();
	}

	@ServiceDependency(required = false, removed = "onScenarioFactoryUnpublish")
	public void onScenarioFactoryPublish(Scenario.Factory scenario, Dictionary<String, Object> serviceProperties) {
		Object value = serviceProperties.get(Scenario.Factory.PROP_NAME);
		if(LOG.isDebugEnabled()) LOG.debug(" got factory " + value);
		if (value == null) {
			LOG.warn("Was notified of a ScenarioFactory but it has no name");
			value = scenario.getClass().getName();
		}

		scenarioFactories.put((String) value, scenario);
		listeners.forEach((listener) -> listener.onScenarioFactoryChange(scenarioFactories));
	}

	public void onScenarioFactoryUnpublish(Scenario.Factory scenario, Dictionary<String, Object> serviceProperties) {
		Object value = serviceProperties.get(Scenario.Factory.PROP_NAME);
		if (value == null) {
			LOG.warn("Was notified of a ScenarioFactory but it has no name");
			value = scenario.getClass().getName();
		}
		scenarioFactories.remove((String) value, scenario);
		listeners.forEach((listener) -> listener.onScenarioFactoryChange(scenarioFactories));
	}

	@ServiceDependency(required = false, removed = "onStrategyFactoryUnpublish")
	public void onStrategyFactoryPublish(Strategy.Factory strategy, Dictionary<String, Object> serviceProperties) {
		Object value = serviceProperties.get(Strategy.Factory.PROP_NAME);
		if(LOG.isDebugEnabled()) LOG.debug(" got strategy " + value);

		if (value == null) {
			LOG.warn("Was notified of a Strategy Factory but it has no name");
			value = strategy.getClass().getName();
		}
		strategyFactories.put((String) value, strategy);
		listeners.forEach((listener) -> listener.onStrategyFactoryChange(strategyFactories));
	}

	public void onStrategyFactoryUnpublish(Strategy.Factory strategy, Dictionary<String, Object> serviceProperties) {
		Object value = serviceProperties.get(Strategy.Factory.PROP_NAME);
		if (value == null) {
			LOG.warn("Was notified of a Strategy Factory but it has no name");
			value = strategy.getClass().getName();
		}
		strategyFactories.put((String) value, strategy);
		listeners.forEach((listener) -> listener.onStrategyFactoryChange(strategyFactories));
	}

	public CompletableFuture<Boolean> prepareScenarioExecution(String scenarioName,
			String strategyName, JSONObject scenarioConf, JSONObject strategyConf,
			int executionId) {
		if(LOG.isDebugEnabled()) LOG.debug("About to prepare scenario " + scenarioName);
		String key = getScenarioKey(scenarioName, executionId);
		Logger logger = Logger.getLogger(key);
		String monitorableDesc = new StringBuffer("Execution of scenario ").append(scenarioName).append(" ExecutionID: ")
				.append(executionId).toString();
		GPTOMonitorable monitorable = new GPTOMonitorable(key, monitorableDesc);
		ExecutionContextImpl ctx = new ExecutionContextImpl(executionId, scenarioConf, monitorable, logger);
		
		Optional<Scenario> scenario = instanciateScenario(scenarioName, ctx);
		if (!scenario.isPresent()) {
			LOG.warn("failed to instanciate scenario");
			broadcastExecutionState(executionId, ExecutionState.NO_SUCH_SCENARIO);
			return CompletableFuture.completedFuture(false);
		}

		Optional<Strategy> maybeStrategy = instanciateStrategy(strategyName, scenario.get(), ctx, strategyConf);
		if (!maybeStrategy.isPresent()) {
			LOG.warn("failed to instanciate strategy");
			broadcastExecutionState(executionId, ExecutionState.NOT_READY);
			return CompletableFuture.completedFuture(false);
		}
		try {
			Strategy strategy = maybeStrategy.get();
			
			Execution exec = new Execution(scenarioName, strategyName, strategy);
			executions.put(executionId, exec);

			return scenario.get().init().whenCompleteAsync((isOk, err) -> {
				//start monitorable
				monitorable.start(bundleContext);
				
				if(isOk && err == null) {
					broadcastExecutionState(executionId, ExecutionState.READY);
				} else {
					LOG.warn("Couldn't init execution, throwable = ", err);
				}
			});
		} catch (IllegalArgumentException e) {
			LOG.error("Got Illegal arguments " + e);
			broadcastExecutionState(executionId, ExecutionState.NOT_READY);
			return CompletableFuture.completedFuture(false);
		}
	}

	public void startExecution(int executionId) {
		if(LOG.isDebugEnabled()) LOG.debug("Starting scenario execution " + executionId);
		Strategy strategy = executions.get(executionId).getStrategy();
		broadcastExecutionState(executionId, ExecutionState.STARTED);

		strategy.startScheduling(() -> {
			if(LOG.isDebugEnabled()) LOG.debug("Execution finished");
			broadcastExecutionState(executionId, ExecutionState.STOPPED);
		});
	}

	public CompletableFuture<Void> stopScenarioExecution(int executionId) {
		Strategy strategy = executions.get(executionId).getStrategy();
		
		return strategy.stopScheduling().thenRunAsync(() -> {
			broadcastExecutionState(executionId, ExecutionState.STOPPED);
		});
	}

	public void setStandaloneMode(boolean isStandalone) {
		isStandaloneMode = isStandalone;
	}
	
	public void setExecutionStrategyOption(int executionId, JSONObject value) {
		Strategy strategy = executions.get(executionId).getStrategy();
		if (strategy == null) {
			if(LOG.isDebugEnabled()) LOG.debug("No strategy for given id " + executionId);
			return;
		}

		strategy.updateProperties(value);
	}
	
	public void addFactoryListener(ScenarioListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeFactoryListener(ScenarioListener listener) {
		this.listeners.remove(listener);
	}

	public Optional<Scenario> instanciateScenario(String scenarioName, ExecutionContext ectx) {
		Scenario.Factory scenario = scenarioFactories.get(scenarioName);
		if(scenario == null) {
			LOG.warn("No such scenario " + scenarioName);
			return Optional.empty();
		} else {
			return Optional.of(scenario.instanciate(ectx));
		}
	}

	public Optional<Strategy> instanciateStrategy(String strategyName, Scenario scenario, ExecutionContextImpl ectx,
			JSONObject opts) {
		Strategy.Factory strategy = strategyFactories.get(strategyName);
		if(strategy == null) {
			LOG.warn("No such strategy " + strategyName);
			return Optional.empty();
		} else {
			return Optional.of(strategy.instanciate(scenario, ectx, opts));
		}	
	}
	
	public Set<String> getAvailableScenarii() {
		return scenarioFactories.keySet().stream().collect(Collectors.toSet());
	}
	
	public Set<String> getAvailableStrategies() {
		return strategyFactories.keySet().stream().collect(Collectors.toSet());
	}

	public void broadcastExecutionState(int executionId, ExecutionState state) {
		String strategyName = executions.containsKey(executionId) ?
				executions.get(executionId).getStrategyName() : null;
		String scenarioName = executions.containsKey(executionId) ?
				executions.get(executionId).getScenarioName() : null;
		
		listeners.forEach((listener) 
				-> listener.onExecutionStateChange(executionId, scenarioName, strategyName, state));
	}

	public void pushMetrics() {
		if (!isStandaloneMode) {
			Map<String, AgentRegistration> metrics =  exporter.getMetricRegistry();
			if(!metrics.isEmpty()) {
				listeners.forEach((listener) -> listener.onMetricTick(metrics));
			}
		}
	}
	
	private String getScenarioKey(String scenarioName, int executionId) {
		return new StringBuffer(KEY_BASE).append(".").append(scenarioName).append(".").append(executionId).toString();
	}

	
}
