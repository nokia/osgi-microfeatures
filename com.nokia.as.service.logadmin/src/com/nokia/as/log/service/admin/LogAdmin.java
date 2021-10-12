// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.log.service.admin;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Service used to perform administractive tasks on a given logger
 */
@ProviderType
public interface LogAdmin {
		
	/**
	 * Sets the level of a given logger. 
	 * @param loggerName the logger name (can be "rootLogger")
	 * @param level the level to set
	 */
	void setLevel(String loggerName, String level);
	
	/**
	 * Gets the current level of a given logger
	 * @param loggerName the logger name
	 * @return the level of the specified logger
	 */
	String getLevel(String loggerName);
	
}
