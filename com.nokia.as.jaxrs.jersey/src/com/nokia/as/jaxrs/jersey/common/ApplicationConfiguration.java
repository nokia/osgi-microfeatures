// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.common;

import java.util.Map;

import javax.ws.rs.core.Application;

/**
 * Service that allows configuration of the JAX-RS {@link Application}. Multiple
 * registrations will be tracked.
 *
 */
public interface ApplicationConfiguration {
	Map<String, Object> getProperties();
}
