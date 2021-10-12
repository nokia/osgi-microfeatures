// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.agent.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.json.JSONObject;

import com.nokia.as.gpto.agent.impl.ExecutionContextImpl;

/**
 * A Strategy defines how a Scenario will be executed. For example, it is the responsibility
 * of the Strategy to define the parallelism model or how frequently the Scenario will be run.
 */
public interface Strategy {

	/**
	 * The OSGI component in charge of providing instances of the Strategy should
	 * implement this interface
	 */
	public interface Factory {
		String PROP_NAME = "strategy.name";

		/*
		 * Instanciate a new Strategy for the given Scenario, using the passed
		 * executionContext and the opts as parameters. Implementors can use this map to
		 * pass injected services or other options.
		 */
		Strategy instanciate(Scenario scenario, ExecutionContextImpl ectx, JSONObject opts);
	}
	
	/**
	 * Start scheduling the Scenario, using the given function as a SessionContext
	 * factory. A simple implementation could use an ExecutorService.
	 * 
	 * shutdownCallback will be called when the Strategy will shut down
	 */
	void startScheduling(Runnable shutdownCallBack);

	/**
	 * Update properties during a run
	 * 
	 * @throws IllegalArgumentException
	 *           thrown if incorrect arguments are given or mandatory arguments are
	 *           missing
	 */
	public abstract void updateProperties(JSONObject value) throws IllegalArgumentException;

	/**
	 * Stop scheduling the scenario. The returned CompletableFuture should be
	 * completed once all scenario session are stopped.
	 */
	public abstract CompletableFuture<Void> stopScheduling();
}
