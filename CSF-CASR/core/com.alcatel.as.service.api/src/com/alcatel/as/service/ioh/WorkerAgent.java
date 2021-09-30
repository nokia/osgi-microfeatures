package com.alcatel.as.service.ioh;

import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A worker agent interface.
 * A worker agent service is registered with some service properties whose keys are declared in the WorkerAgent interface
 * (check WorkerAgent.PROTOCOL, WorkerAgent.GROUP, etc ...)
 */
@ProviderType
public interface WorkerAgent {
	
	/**
	 * protocol service property (value is a string, and corresponds to the agent protocol name in lowercase)
	 */
	String PROTOCOL = "protocol";
	
	/**
	 * agent group name service property (value is a string)
	 */
	String GROUP = "group";

	/**
	 * agent instance name service property (value is a string)
	 */
	String INSTANCE = "instance";
	
	/**
	 * Indicates if this endpoint is currently active. An active endpoint means incoming traffic is accepted.
	 * An inactive endpoint means the endpoint remains connected but no incoming traffic is accepted.
	 */
	boolean isActive();
	
	/**
	 * Asynchronously activates to the IO handler to get incoming traffic.
	 *
	 */
	void activate();
	
	/**
	 * Asynchronously deactivates this agent from the IO handler.
	 * 
	 */
	void deactivate();
	
}
