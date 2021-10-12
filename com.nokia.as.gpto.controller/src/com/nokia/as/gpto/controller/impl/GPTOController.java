// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.controller.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.BundleDependency;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.alcatel.as.ioh.server.ServerFactory;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.util.MeteringRegistry;
import com.nextenso.mux.mesh.MuxMesh;
import com.nextenso.mux.mesh.MuxMeshFactory;
import com.nokia.as.gpto.common.msg.api.ControlScenarioRunMessage;
import com.nokia.as.gpto.common.msg.api.ControlScenarioRunMessage.Operation;
import com.nokia.as.gpto.common.msg.api.GPTOMessage;
import com.nokia.as.gpto.common.msg.api.Pair;
import com.nokia.as.gpto.common.msg.api.PrepareExecutionMessage;
import com.nokia.as.gpto.common.msg.api.SetInjectionPropertiesMessage;

/**
 * Identify Gogo commands to communicate with agents
 *
 */
@Component(provides = Object.class)
@Property(name = CommandProcessor.COMMAND_SCOPE, value = "asr.gpto")
@Property(name = CommandProcessor.COMMAND_FUNCTION, value = {"stop", "start", "listScenario", "listAgents", "setOpt", "listStrategies", "listExecution" })
public class GPTOController {

	@ServiceDependency(required = true)
	private volatile MuxMeshFactory _meshFactory;
	
	@ServiceDependency(required = true)
	private volatile ServerFactory _serverFactory;
	
	@ServiceDependency(required = true)
	MeteringService meteringService;
	
	@ServiceDependency(required = true)
	MeteringRegistry meteringRegistry;
	
	@Inject
	private volatile BundleContext bundleContext;
	
	/**
	 * If the bundle gpto.agent is present on the same jvm then it must run in a standalone mode
	 */
	@BundleDependency(required = false, filter="(Bundle-SymbolicName=com.nokia.as.gpto.agent)", 
					  stateMask = Bundle.ACTIVE)
	public void localAgentAdded() {
		LOG.debug("GPTO CONTROLLER STANDALONE MODE STARTED");
		_manager.setStandaloneMode(true);
	}
	
	private MuxMesh _myMesh;
	private GPTOConfiguration config;
	private GPTOAgentManager _manager;
	private static Logger LOG = Logger.getLogger(GPTOController.class);
	
	public GPTOController() {
//		_manager = new GPTOAgentManager(bundleContext, meteringService, meteringRegistry);
	}
	
	@ConfigurationDependency
	void updated(GPTOConfiguration serversConf) {
		LOG.info("ServersConfiguration : " + serversConf);
		config = serversConf;
	}

	@Start
	void startBundle() {
		LOG.debug("started GPTO orchestrator");
		_manager = new GPTOAgentManager(bundleContext, meteringService, meteringRegistry);
		_serverFactory.newTcpServerConfig("gpto-mesh", config.getMuxMeshServerConfig());

		Map<String, String> meshOpts = new HashMap<>();
		meshOpts.put("server", "true");
		_myMesh = _meshFactory.newMuxMesh("gpto-mesh", _manager, meshOpts);
		_myMesh.start();
	}

	@Stop
	void stopBundle() {
		_manager.disconnectAgents();
	}

	@Descriptor("Start a scenario")
	public void start(@Descriptor("The scenario to run") 
					  @Parameter(names = { "-scenario", "-sc" }, absentValue = "") 
					  String scenario,
					  @Descriptor("The strategy to use") 
					  @Parameter(names = { "-strategy", "-st" }, absentValue = "") 
					  String strategy,
					  @Descriptor("JSON config file for strategy")
  					@Parameter(names = { "-strategy-config", "-scc" }, absentValue = "")
  					String strategyConfig,
					  @Descriptor("JSON config file for strategy")
  					@Parameter(names = { "-scenario-config", "-stc" }, absentValue = "")
  					String scenarioConfig
	  ) {
		LOG.debug("Scenario name " + scenario);
		
		Path p = Paths.get(strategyConfig);
		String injectionConf;
		
		try(BufferedReader br = Files.newBufferedReader(p)){
			injectionConf = br.lines().collect(Collectors.joining());
		} catch(Exception e) {
			System.out.println("couldn't read JSON config for injection strategy: " + e.getMessage());
			return;
		}
		
		String scenarioConf;
		p = Paths.get(scenarioConfig);
		
		try(BufferedReader br = Files.newBufferedReader(p)) {
			scenarioConf = br.lines().collect(Collectors.joining());
		} catch (Exception e) {
			System.out.println("Couldn't read JSON config for scenario: " + e.getMessage());
			return;
		}
		
		int executionID = _manager.executionID.getAndIncrement();
		
		PrepareExecutionMessage testStateMsg = new PrepareExecutionMessage(executionID,
				scenario, strategy);
		testStateMsg.setInjectionStrategyJson(injectionConf);
		testStateMsg.setScenarioJson(scenarioConf);
		
		LOG.debug(testStateMsg + " will be sent to agents");
		try {
			_manager.setExecutionCountDown(executionID, _manager.getAgents().size());
			_manager.sendCommand(testStateMsg);
			Optional<CountDownLatch> countdown = _manager.getCountDownLatch(executionID);
			boolean hasPassed = false;
			if(countdown.isPresent())
				hasPassed = countdown.get().await(10, TimeUnit.SECONDS);
			else {
				System.out.println("Error with execution ID");
				return;
			}
			if(hasPassed) {
				GPTOMessage startMsg = new ControlScenarioRunMessage(Operation.START, executionID);
				_manager.sendCommand(startMsg);
				System.out.println("Execution id " + executionID + " started");
			} else {
				System.out.println("Timeout error: please retry later...");
			}
		} catch (IOException e) {
			LOG.warn("failed to send start command :" + e.getMessage());
		} catch (InterruptedException e) {
			LOG.error(e);
		}
	}

	private Optional<Map<String, Object>> parseOpts(List<String> opts) {
		if (opts.isEmpty()) return Optional.empty();
		Map<String, Object> optList = new HashMap<>();
		
		for(String opt:opts) {
			String[] keyValue = opt.split("=");
			if (keyValue.length != 2) return Optional.empty();
			String key = keyValue[0];
			Object value = getOptValue(keyValue[1]);
			optList.put(key, value);
		}
		return Optional.ofNullable(optList);
	}

	private Object getOptValue(String value) {
		try{
			return Integer.parseInt(value);
		} catch(NumberFormatException e) {
			try {
				return Float.parseFloat(value);
			} catch (NumberFormatException e2) {
				if(isBoolean(value)) {
					return Boolean.parseBoolean(value);
				} else {
					return value;
				}
			}
		}
	}

	private boolean isBoolean(String s) {
		return "true".equals(s.toLowerCase()) || "false".equals(s.toLowerCase());
	}
	
	@Descriptor("Stop a currently running execution")
	public void stop(
			@Descriptor("The execution ID") 
			@Parameter(names = { "-id" }, absentValue = "") 
			int id) {
		try {
			GPTOMessage startMsg = new ControlScenarioRunMessage(Operation.STOP, id);
			_manager.sendCommand(startMsg);
		} catch (IOException e) {
			LOG.warn("failed to send start command :" + e.getMessage());
		}
	}

	public void listAgents() {
		StringBuilder sb = new StringBuilder();
		Collection<GPTOAgent> agents = _manager.getAgents();
		sb.append(agents.size()).append(" Agent(s) connected to the Orchestrator\n");
 

		System.out.println(sb.toString());
	}

	public void listScenario() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<GPTOAgent, Set<String>> e : _manager.getAvailableScenarii().entrySet()) {
			sb.append("Agent ").append(e.getKey().getName());
			sb.append("\n---------\n");
			for (String s : e.getValue()) {
				sb.append(s).append('\n');
			}
		}

		System.out.println(sb.toString());
	}
	
	public void listStrategies() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<GPTOAgent, Set<String>> e : _manager.getAvailableStrategies().entrySet()) {
			sb.append("Strategies ").append(e.getKey().getName());
			sb.append("\n---------\n");
			for (String s : e.getValue()) {
				sb.append(s).append('\n');
			}
		}
		System.out.println(sb.toString());
	}
	
	public void listExecution() {
		StringBuilder sb = new StringBuilder();
		Map<Integer, Pair<String, String>> runningExecutions = _manager.getRunningExecutions();
		for (int execution : runningExecutions.keySet()) {
			sb.append("Execution ID=").append(execution);
			sb.append(", Scenario=").append(runningExecutions.get(execution).getLeft());
			sb.append(", Strategy=").append(runningExecutions.get(execution).getRight());
			sb.append("\n");
		}
		if(runningExecutions.isEmpty())
			sb.append("No executions running...");
		System.out.println(sb.toString());
	}
	
	@Descriptor("Set a property of a running scenario execution, such as its injection rate")
	public void setOpt(		
				@Descriptor("The execution ID") 
				@Parameter(names = { "-id" }, absentValue = "")
				int executionId,
			  @Descriptor("The list of arguments (ex: -args key=value key2=value2...) /!\\ flag to use at the end") 
			  @Parameter(names = { "-opts", "-args", "-conf" }, absentValue = "") 
			  String... opts) {
		try {
			List<String> optList = Arrays.asList(opts);
			Optional<Map<String, Object>> optMap = parseOpts(optList);
			if(!optMap.isPresent()) {
				return;
			}
			
			JSONObject obj;
			obj = new JSONObject(optMap.get());
			GPTOMessage optMessage = new SetInjectionPropertiesMessage(executionId, obj.toString());
			
			_manager.sendCommand(optMessage);
		} catch(Exception e) {
			LOG.error("An error occured when sending the message " + e);
		}

	}
}
