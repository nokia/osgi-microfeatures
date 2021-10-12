// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.management;

/**
 * An interface to any contributor to the graceful shutdown mechanism
 * 
 */
public interface Shutdownable {

	/**
	 * invoked during shutdown procedure. implement condition checking. optionally
	 * fork treatment in appropriate reactor. returning a non zero value trigger a
	 * retry of the shutdown procedure maximum equal to the value of the return
	 * 
	 * @return The retry delay in milliseconds, 0 if no retry needed
	 */
	int shutdown();
}
