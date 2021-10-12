// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter;

/**
 * The Diameter Route interface.
 */
public interface DiameterRoute {

	/**
	 * Gets the routing peer.
	 * 
	 * @return The routing peer.
	 */
	public DiameterPeer getRoutingPeer();

	/**
	 * Gets the application identifier.
	 * 
	 * @return The application identifier.
	 */
	public long getApplicationId();

	/**
	 * Gets the application type.
	 * 
	 * @return The application type.
	 */
	public int getApplicationType();

	/**
	 * Gets the destination realm.
	 * 
	 * @return The destination realm.
	 */
	public String getDestinationRealm();

	/**
	 * Gets the metrics.
	 * 
	 * The metrics are used to get the priority of the route for equivalent routes
	 * (same application identifier, same realm). A route with lower metrics is
	 * priority.
	 * 
	 * @return The metrics
	 */
	public int getMetrics();

	/**
	 * Indicates whether the route matches the arguments.
	 * 
	 * @param destinationRealm The destination realm.
	 * @param applicationId The application identifier.
	 * @param applicationType The application type.
	 * @return true if the route matches the arguments.
	 */
	public boolean match(String destinationRealm, long applicationId, int applicationType);

}