// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.ioh;

import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

/**
 * An IOH Endpoint interface, which can be used to interact with an IOH engine.
 * Each time an io handler is connected, then one instance of a IohEndpoint osgi service
 * is registered in the osgi service registry.
 * An overload controller can then track all registered MuxWire services in order to 
 * send muxStart/muxStop messages to the IOH.
 * 
 * An IOhEndpoint service is registered with some service properties which keys are declared in the IohEndpoint interface
 * (check IohEndpoint.PROTOCOL, IohEndpoint.GROUP, etc ...)
 */
@ProviderType
public interface IohEndpoint {
	
	/**
	 * protocol service property (value is a string, and corresponds to the ioh protocol name in lowercase)
	 */
	String PROTOCOL = "protocol";
	
	/**
	 * ioh group name service property (value is a string)
	 */
	String GROUP = "group";

	/**
	 * ioh instance name service property (value is a string)
	 */
	String INSTANCE = "instance";
	
	/**
	 * Ioh remote address (value is a string)
	 */
	String ADDRESS = "address";
	
	/**
	 * Ioh remote port (value is an Integer)
	 */
	String PORT = "port";

	/**
	 * Indicates if this endpoint is currently registered. A Registered endpoint means incoming traffic is accepted.
	 * A Deregistered endpoint means the endpoint remains connected but no incoming traffic is accepted.
	 */
	boolean isRegistered();
	
	/**
	 * Asynchronously registers to the IO handler to get incoming traffic. Once this method returns, the 
	 * {@link #isRegistered()} will return true.
	 */
	void register();
	
	/**
	 * Asynchronously deregisters this agent from the IO handler. Once the returned future is completed,
	 * the {@link #isRegistered()} will return false (unless the operation has failed).
	 * 
	 * @return a CompletableFuture used to be notified when the  message has been received by the IO Handler.
	 * The future will complete with "true" once the agent has been deregistered, "false" in case the 
	 * deregistration could not complete.
	 */
	CompletableFuture<Boolean> deregister();
	
}
