// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gpto.agent.api;

import java.util.concurrent.CompletableFuture;

/**
 * Implement this interface to specify a scenario for GPTO. The life cycle is
 * the following:
 * <li>init is called when a new scenario execution starts
 * <li>beginSession is called when a new thread (i.e a session) is a bout to
 * start running the scenario
 * <li>run is called at every "iteration" of GPTO as specified by the injection
 * strategy
 * <li>endSession is called when the thread of execution is stopping
 * <li>dispose is called when all sessions are finished <br/>
 * The Scenario instance receives as an argument an ExecutionContext that provides
 * facilities for tracking metrics and logging information. <br/>
 * Note that a Scenario will be called by multiple threads at the same time
 * within a single execution, so non thread-safe objects should be isolated
 * per-thread in the SessionContext attachment
 * 
 */
public interface Scenario {
	
	/**
	 * The OSGI component in charge of providing instances of the Strategy should
	 * implement this interface
	 */
	public interface Factory {
		/**
		 * Name of the OSGI property for the scenario this factory is capable of
		 * instancing
		 */
		String PROP_NAME = "scenario.name";

		/**
		 * Called when a new Scenario instance is required.
		 * The passed ExecutionContext contains facilities for logging and metering.
		 */
		Scenario instanciate(ExecutionContext ectx);
		
		/**
		 * Return an help string made available to the operator. Can be used to details
		 * the configurable properties for this scenario
		 * 
		 */
		
		String help();
	}

	/**
	 * Called when an execution begins, use this method for preparing connection to
	 * the system under test for example.
	 * 
	 * @return a {@link CompletableFuture CompletableFuture} of boolean. Return true
	 *         to signal the initialization went successfully, otherwise false. The
	 *         CompletableFuture let you perform asynchronous operation.
	 */
	CompletableFuture<Boolean> init();

	/**
	 * Called by a single thread of execution begin executing the scenario
	 * 
	 * @param sctx
	 *          the SessionContext give information for a single thread of execution
	 *          of a scenario and let you attach a context Object to a single thread
	 * @return a {@link CompletableFuture CompletableFuture} of boolean. Return true
	 *         to signal the initialization went successfully, otherwise false. The
	 *         CompletableFuture let you perform asynchronous operation.
	 */
	CompletableFuture<Boolean> beginSession(SessionContext sctx);

	/**
	 * Called at each iteration of GPTO, this is where the main logic of the
	 * scenario is done. You must keep your thread-local state in the attachment
	 * provided by the the SessionContext
	 * 
	 * @param sctx
	 *          the SessionContext give information for a single thread of execution
	 *          of a scenario
	 * @return a {@link CompletableFuture CompletableFuture} of boolean. Return true
	 *         to signal the used strategy should continue iterating. 
	 *         The CompletableFuture let you perform asynchronous operation.
	 */
	CompletableFuture<Boolean> run(SessionContext sctx);

	/**
	 * Called by a single thread of execution when it is done executing this
	 * scenario
	 * 
	 * @param sctx
	 *          the SessionContext give information for a single thread of execution
	 *          of a scenario
	 * @return a {@link CompletableFuture CompletableFuture} of boolean. The
	 *         CompletableFuture let you perform asynchronous operation.
	 */
	CompletableFuture<Void> endSession(SessionContext sctx);

	/**
	 * Called when all sessions have been terminated.
	 * Can be used for cleanup, such as the connection you have established.
	 * 
	 * @return a {@link CompletableFuture CompletableFuture} with no value. The
	 *         CompletableFuture let you perform the cleanup asynchronously.
	 */
	CompletableFuture<Void> dispose();
}
