package com.nokia.as.gpto.agent.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.BundleDependency;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.MuxHeaderV0;
import com.nextenso.mux.mesh.MuxMesh;
import com.nextenso.mux.mesh.MuxMeshFactory;
import com.nextenso.mux.mesh.MuxMeshListener;
import com.nokia.as.gpto.agent.api.Scenario.Factory;
import com.nokia.as.gpto.agent.api.ScenarioRunner;
import com.nokia.as.gpto.agent.api.Strategy;
import com.nokia.as.gpto.common.msg.api.AgentRegistration;
import com.nokia.as.gpto.common.msg.api.ControlScenarioRunMessage;
import com.nokia.as.gpto.common.msg.api.ExecutionState;
import com.nokia.as.gpto.common.msg.api.ExecutionStateMessage;
import com.nokia.as.gpto.common.msg.api.GPTOMessage;
import com.nokia.as.gpto.common.msg.api.MetricRegistryMessage;
import com.nokia.as.gpto.common.msg.api.PrepareExecutionMessage;
import com.nokia.as.gpto.common.msg.api.ScenarioListMessage;
import com.nokia.as.gpto.common.msg.api.SetInjectionPropertiesMessage;
import com.nokia.as.gpto.common.msg.api.StrategyListMessage;

@Component(provides = Object.class)
public class GPTOAgent implements MuxMeshListener, ScenarioRunner.ScenarioListener {

	@ServiceDependency(required = true)
	private volatile MuxMeshFactory _meshFactory;
	@ServiceDependency(required = true)
	private volatile ScenarioRunner scenarioRunner;
	@Inject
	private volatile BundleContext bundleCtx;

	private MuxMesh _mesh;
	private MuxConnection _orchestratorCnx;
	private Queue<GPTOMessage> messageQueue = new ConcurrentLinkedQueue<>();
	private Map<Class<? extends GPTOMessage>, Consumer<GPTOMessage>> messageHandler;
	private static Logger LOG = Logger.getLogger(GPTOAgent.class);

	@BundleDependency(required = false, filter="(Bundle-SymbolicName=com.nokia.as.gpto.controller)", 
			  stateMask = Bundle.ACTIVE)
	public void localAgentAdded() {
		LOG.debug("GPTO AGENT STANDALONE MODE STARTED");
		scenarioRunner.setStandaloneMode(true);
	}
	
	@Start
	public void start() {
		LOG.warn("Started agent");
		Map<String, String> meshOpts = new HashMap<>();
		meshOpts.put("client", "true");
		_mesh = _meshFactory.newMuxMesh("gpto-mesh", this, meshOpts);
		_mesh.start();

		Map<Class<? extends GPTOMessage>, Consumer<GPTOMessage>> map = new HashMap<>();
		map.put(PrepareExecutionMessage.class, (msg) -> onPrepareExecutionMessage((PrepareExecutionMessage) msg));
		map.put(SetInjectionPropertiesMessage.class, (msg) -> onSetPropertyMessage((SetInjectionPropertiesMessage) msg));
		map.put(ControlScenarioRunMessage.class, (msg) -> {
			switch (((ControlScenarioRunMessage) msg).getOp()) {
			case START:
				onStartScenarioMessage((ControlScenarioRunMessage) msg);
				break;
			case STOP:
				onStopScenarioMessage((ControlScenarioRunMessage) msg);
				break;
			}
		});

		scenarioRunner.addFactoryListener(this);
		messageHandler = Collections.unmodifiableMap(map);
	}

	@Stop
	public void stopBundle() {
		_orchestratorCnx.close();
	}

	@Override
	public void muxData(MuxMesh mesh, MuxConnection connection, MuxHeader header, ByteBuffer buffer) {
		try {
			GPTOMessage msg = GPTOMessage.fromBytes(buffer);
			if(LOG.isDebugEnabled()) LOG.debug("received message " + msg);
			messageHandler.get(msg.getClass()).accept(msg);
		} catch (Exception e) {
			LOG.debug("An error occured when handling the message");
			e.printStackTrace();
		}
	}

	@Override
	public void muxOpened(MuxMesh mesh, MuxConnection connection) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("mux cnx opened " + connection);
			LOG.debug("impl " + connection.getClass().getName());
		}
		_orchestratorCnx = connection;
		sendScenarioListMessage(scenarioRunner.getAvailableScenarii());
		sendStrategyListMessage(scenarioRunner.getAvailableStrategies());
	}

	@Override
	public void muxClosed(MuxMesh mesh, MuxConnection connection) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("mux cnx closed " + connection);
		}
	}

	private void sendScenarioListMessage(Set<String> scenarii) {
		GPTOMessage msg = new ScenarioListMessage(scenarii);
		try {
			sendGPTOMessage(msg);
		} catch (IOException e) {
			LOG.error("Failed to send message");
			messageQueue.add(msg);
		}
	}

	private void sendStrategyListMessage(Set<String> strategies) {
		GPTOMessage msg = new StrategyListMessage(strategies);
		try {
			sendGPTOMessage(msg);
		} catch (IOException e) {
			LOG.error("failed to send message");
			messageQueue.add(msg);
		}
	}
	
	private void sendMetricRegistryMessage(Map<String, AgentRegistration> registry) {
		GPTOMessage msg = new MetricRegistryMessage(registry);
		try {
			sendGPTOMessage(msg);
		} catch (IOException e) {
			LOG.error("failed to send message");
			messageQueue.add(msg);
		}
	}

	private void onStartScenarioMessage(ControlScenarioRunMessage msg) {
		scenarioRunner.startExecution(msg.getExecutionID());
	}

	private void onStopScenarioMessage(ControlScenarioRunMessage msg) {
		scenarioRunner.stopScenarioExecution(msg.getExecutionID());
	}

	private void onPrepareExecutionMessage(PrepareExecutionMessage msg) {
		if(LOG.isDebugEnabled()) LOG.debug("prepare scenario ");
		
	  JSONObject strategyConf;
	  JSONObject scenarioConf;
	  try {
			strategyConf = new JSONObject(msg.getInjectionStrategyJson());
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
	  
	  try {
	  	scenarioConf = new JSONObject(msg.getScenarioJson());
	  } catch (JSONException e) {
	  	e.printStackTrace();
	  	return;
	  } 
		scenarioRunner.prepareScenarioExecution(msg.getScenarioName(),
				msg.getStrategyName(), 
				scenarioConf, 
				strategyConf,
				msg.getExecutionID());
	}

	private void onSetPropertyMessage(SetInjectionPropertiesMessage msg) {
		JSONObject props;
		
		try {
			props = new JSONObject(msg.getMap());
		} catch (Exception e) {
			LOG.warn("Pilot sent invalid JSON: " + e.getMessage());
			return;
		}
		scenarioRunner.setExecutionStrategyOption(msg.getId(), props);
	}

	private void sendExecutionStateChange(ExecutionState isPrepared, int executionID, String strategyName, String scenarioName) {
		ExecutionStateMessage execMsg = new ExecutionStateMessage(isPrepared, executionID);
		execMsg.setScenarioName(scenarioName);
		execMsg.setStrategyName(strategyName);
	
		try {
			if(LOG.isDebugEnabled()) LOG.debug("sendExecutionReady msg: " + execMsg.toString());
			sendGPTOMessage(execMsg);
		} catch (IOException e) {
			LOG.error("Failed to send message");
			messageQueue.add(execMsg);
		}
	}

	private void sendGPTOMessage(GPTOMessage msg) throws IOException {
		if (_orchestratorCnx == null) {
			messageQueue.add(msg);
		} else {
			while (!messageQueue.isEmpty()) {
				_orchestratorCnx.sendMuxData(new MuxHeaderV0(), false, messageQueue.poll().toBytes());
			}
			_orchestratorCnx.sendMuxData(new MuxHeaderV0(), false, msg.toBytes());
		}
	}

	@Override
	public void onScenarioFactoryChange(Map<String, Factory> factories) {
		sendScenarioListMessage(factories.keySet().stream().collect(Collectors.toSet()));
	}

	@Override
	public void onStrategyFactoryChange(Map<String, Strategy.Factory> factories) {
		sendStrategyListMessage(factories.keySet().stream().collect(Collectors.toSet()));
	}

	@Override
	public void onExecutionStateChange(int executionId, String scenarioName, String strategyName, ExecutionState state) {
		sendExecutionStateChange(state, executionId, strategyName, scenarioName);
	}

	@Override
	public void onMetricTick(Map<String, AgentRegistration> registry) {
		sendMetricRegistryMessage(registry);
	}

}
