// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

/**
 * 
 */
package com.alcatel_lucent.ha.services;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.alcatel_lucent.servlet.sip.services.Statable;

/**
 * 
 * The flattable framework Generics interface seen as a RecoveryService.
 * <br> This hight level service allows to :
 * <ul>
 * <li>create a HAContext bucket with an id
 * <li>define the factory method to build  root Flattable objects are created (<T>)
 * <li>provide passivate method : write a context in a safe place (another jvm in a cluster
 * typically)
 * <li>provide activate method : restore a context from a safe place into the local jvm
 * <li>provide unpassivate method : remove the contex from the sage place (another jvm of the cluster)
 * </ul>
 */
public interface RecoveryService<T extends Flattable>  extends Statable{
	final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("agent.sip.ha");
	/**
	An asynchronous api to be callback when asynchronous passivation occurs
	@param boolean true if passivation is ok, false else
	 */
	interface PassivationCallback {
		void passivated(boolean ok);
	}
	
	/**
	An asynchronous api to be callback when asynchronous activation occurs
	@param boolean true if passivation is ok, false else
	 */
	interface ActivationCallback {
		void activated(boolean ok);
	}
	
    HAContext context(String id);

    T createRoot(HAContext context);

    boolean unpassivate(final HAContext context);
    
    boolean passivate(HAContext context, PassivationCallback cb) throws IllegalArgumentException,
            IllegalAccessException, SecurityException, NoSuchFieldException;

    HAContext context(Map<String, Object> context, String id)
            throws IllegalArgumentException, IllegalAccessException,
            SecurityException, NoSuchFieldException,
            InstantiationException, InvocationTargetException,
            NoSuchMethodException,ClassNotFoundException;

    boolean activate(HAContext context, ActivationCallback cb);
}
