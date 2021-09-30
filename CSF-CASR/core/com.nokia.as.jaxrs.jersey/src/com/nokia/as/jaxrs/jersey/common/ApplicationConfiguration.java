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
