// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.controller.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.util.MeteringRegistry;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.MuxHeaderV0;
import com.nextenso.mux.mesh.MuxMesh;
import com.nextenso.mux.mesh.MuxMeshListener;
import com.nokia.as.gpto.common.msg.api.ExecutionStateMessage;
import com.nokia.as.gpto.common.msg.api.GPTOMessage;
import com.nokia.as.gpto.common.msg.api.MetricRegistryMessage;
import com.nokia.as.gpto.common.msg.api.Pair;
import com.nokia.as.gpto.common.msg.api.ScenarioListMessage;
import com.nokia.as.gpto.common.msg.api.StrategyListMessage;
import com.nokia.as.gpto.controller.metering.MetricAggregator;

public class GPTOAgentManager implements MuxMeshListener {

	private Map<MuxConnection, GPTOAgent> _agents;

	protected Map<Integer, CountDownLatch> executionToReadiness = new ConcurrentHashMap<>();

	protected Map<Integer, Pair<String, String>> runningExecutions = new ConcurrentHashMap<>();
	
	private Map<Pair<Integer, MuxConnection>, Boolean> hasCountDown = new ConcurrentHashMap<>();

	protected AtomicInteger executionID = new AtomicInteger(1);

	protected boolean isStandaloneMode = false;
	
	protected MetricAggregator metricAggregator;
	
	private static Logger LOG = Logger.getLogger(GPTOAgentManager.class);

	private Map<Class<? extends GPTOMessage>, BiConsumer<GPTOMessage, MuxConnection>> messageHandler;

	public GPTOAgentManager(BundleContext bc, MeteringService meteringService, MeteringRegistry meteringRegistry) {
		_agents = new ConcurrentHashMap<>();
		
		metricAggregator = new MetricAggregator(bc, meteringService, meteringRegistry);
		
		Map<Class<? extends GPTOMessage>, BiConsumer<GPTOMessage, MuxConnection>> temporaryMap = new HashMap<>();
		temporaryMap.put(ScenarioListMessage.class, (msg, cnx) -> handleListScenario((ScenarioListMessage) msg, cnx));
		temporaryMap.put(StrategyListMessage.class, (msg, cnx) -> handleStrategyList((StrategyListMessage) msg, cnx));
		temporaryMap.put(ExecutionStateMessage.class, (msg, cnx) -> handleExecutionState((ExecutionStateMessage) msg, cnx));
		temporaryMap.put(MetricRegistryMessage.class, (msg, cnx) -> handleMetricMessage((MetricRegistryMessage) msg, cnx));
		
		// Creates an immutable map
		messageHandler = Collections.unmodifiableMap(new LinkedHashMap<>(temporaryMap));
	}

	public Collection<GPTOAgent> getAgents() {
		return _agents.values();
	}

	public void setStandaloneMode(boolean isStandalone) {
		isStandaloneMode = isStandalone;
	}
	
	public void setExecutionCountDown(int executionID, int countdown) {
		if (executionToReadiness.containsKey(executionToReadiness)) {
			LOG.debug("/!\\ Map already contains a countdown for executionID=" + executionID);
		}
		executionToReadiness.put(executionID, new CountDownLatch(countdown));
	}

	Optional<CountDownLatch> getCountDownLatch(int executionID) {
		return Optional.ofNullable(executionToReadiness.get(executionID));
	}

	public void sendCommand(GPTOMessage cmd) throws IOException {
		LOG.debug("Sending message " + cmd);
		for (MuxConnection cnx : _agents.keySet()) {
			cnx.sendMuxData(new MuxHeaderV0(), false, cmd.toBytes());
		}
	}

	public void disconnectAgents() {
		Iterator<MuxConnection> it = _agents.keySet().iterator();

		while (it.hasNext()) {
			MuxConnection agent = it.next();
			agent.close();
			it.remove();
		}
	}

	public void saveExecution(int execution, String scenario, String strategy) {
		runningExecutions.put(execution, new Pair<>(scenario, strategy));
	}
	
	public Map<GPTOAgent, Set<String>> getAvailableScenarii() {
		return _agents.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, e -> e.getValue().getScenarii()));
	}

	public Map<GPTOAgent, Set<String>> getAvailableStrategies() {
		return _agents.entrySet()
					  .stream()
					  .collect(Collectors.toMap(Map.Entry::getValue, e -> e.getValue().getAvailableStrategies()));
	}
	
	public Map<Integer, Pair<String, String>> getRunningExecutions(){
		return runningExecutions;
	}
	
	@Override
	public void muxOpened(MuxMesh mesh, MuxConnection connection) {
		connectAgent(connection);
	}

	@Override
	public void muxClosed(MuxMesh mesh, MuxConnection connection) {
		disconnectAgent(connection);
	}

	@Override
	public void muxData(MuxMesh mesh, MuxConnection connection, MuxHeader header, ByteBuffer buffer) {
		try {
			GPTOMessage msg = GPTOMessage.fromBytes(buffer);
			if(LOG.isDebugEnabled()) {
				if(msg instanceof MetricRegistryMessage) {
//					LOG.debug("MetricMsg received from " + connection + " message " + msg);
				} else {
					LOG.debug("received from " + connection + " message " + msg);
				}
			}
			messageHandler.get(msg.getClass()).accept(msg, connection);
		} catch (Exception e) {
			LOG.debug(e);
			e.printStackTrace();
		}
	}

	private void connectAgent(MuxConnection cnx) {
		if(LOG.isDebugEnabled()) LOG.debug("agent connected " + cnx);
		// TODO: add remote address
		_agents.put(cnx, new GPTOAgent(cnx.toString(), 
									   "TODO"));
	}

	private void disconnectAgent(MuxConnection cnx) {
		if(LOG.isDebugEnabled()) LOG.debug("agent disconnected " + cnx.toString());
		cnx.close();
		_agents.remove(cnx);
	}

	private void handleListScenario(ScenarioListMessage scenarii, MuxConnection connection) {
		GPTOAgent agent = _agents.get(connection);
		if (agent == null) {
			LOG.warn("Unknown connection " + connection);
		}
		synchronized (agent) {
			agent.setScenarii(scenarii.getScenarii());
		}
	}

	private void handleStrategyList(StrategyListMessage msg, MuxConnection cnx) {
		GPTOAgent agent = _agents.get(cnx);
		if (agent == null) {
			LOG.warn("Unknown connection " + cnx);
		}

		synchronized (agent) {
			agent.setAvailableStrategies(msg.getStrategies());
		}
	}
	private void handleExecutionStopped(ExecutionStateMessage msg) {
		if (runningExecutions.remove(msg.getExecutionID()) == null){
			LOG.warn("[handleExecutionStopped] no such execution (ID="+msg.getExecutionID()+") running");
		}
	}
	
	private void handleExecutionStarted(ExecutionStateMessage msg) {
		saveExecution(msg.getExecutionID(), msg.getScenarioName() , msg.getStrategyName());
	}
	
	private void handleExecutionReady(ExecutionStateMessage msg, MuxConnection connection) {
		Pair<Integer, MuxConnection> entry = new Pair<>(msg.getExecutionID(), connection);
		Boolean mustCountDown = (hasCountDown.get(entry) == null || !hasCountDown.get(entry));
		LOG.debug("mustCountDown?");
		if (mustCountDown) {
			LOG.debug("Countdown OK");
			getCountDownLatch(msg.getExecutionID()).get().countDown();
			hasCountDown.put(entry, true);
		}
	}
	
	private void handleExecutionState(ExecutionStateMessage msg, MuxConnection connection) {
		GPTOAgent agent = _agents.get(connection);
		if (agent == null) {
			LOG.warn("unknown connection " + connection.toString() + " id " + connection.getId());
			LOG.warn("stack instance " + connection.getStackHost());
			LOG.warn("impl " + connection.getClass().getName());
			LOG.warn("MuxIndentification " + connection.getRemoteAddress());
			return;
		}

		try {
			switch(msg.getState()) {
			case READY:
				handleExecutionReady(msg, connection);
				break;
			case STOPPED:
				handleExecutionStopped(msg);
				break;
			case STARTED:
				handleExecutionStarted(msg);
				break;
			default: break;
			}
		} catch (NoSuchElementException e) {
			LOG.error("No CountDownLatch found", e);
		}
	}
	
	private void handleMetricMessage(MetricRegistryMessage msg, MuxConnection connection) {
		if (!isStandaloneMode) {
			GPTOAgent agent = _agents.get(connection);
			if (agent == null) {
				LOG.warn("unknown connection " + connection.toString() + " id " + connection.getId());
				LOG.warn("stack instance " + connection.getStackHost());
				LOG.warn("impl " + connection.getClass().getName());
				LOG.warn("MuxIndentification " + connection.getRemoteAddress());
				return;
			}
			metricAggregator.updateRegistry(connection.hashCode(), agent, msg.getRegistry());
		}
	}
}
