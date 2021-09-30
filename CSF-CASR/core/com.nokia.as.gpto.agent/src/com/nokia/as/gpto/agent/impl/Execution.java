package com.nokia.as.gpto.agent.impl;

import com.nokia.as.gpto.agent.api.Strategy;

public class Execution {

	private final String scenarioName;
	private final String strategyName;
	private final Strategy strategy;
	
	public Execution(String scenarioName, String strategyName, Strategy strategy) {
		super();
		this.scenarioName = scenarioName;
		this.strategyName = strategyName;
		this.strategy = strategy;
	}

	public String getScenarioName() {
		return scenarioName;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public Strategy getStrategy() {
		return strategy;
	}
}
