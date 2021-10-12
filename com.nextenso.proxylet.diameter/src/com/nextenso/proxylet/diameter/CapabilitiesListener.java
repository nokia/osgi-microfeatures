// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter;

/**
 * The Capabilities Listener.
 * 
 * An application can modify a Capabilities-Exchange message before the message
 * is sent in order to add some AVPs.
 * 
 * All the listeners in the list (given by the
 * DiameterPeerTable.getCapabilitiesListeners() method) are called sequentially
 * in the same order as the list. The message is sent after all listeners have
 * finished to handle capabilities.
 */
public interface CapabilitiesListener {

	/**
	 * Called before a Capabilities-Exchange message is sent to remote node.
	 * 
	 * @param message The Capabilities-Exchange message.
	 */
	public void handleCapabilities(DiameterMessage message);
}
