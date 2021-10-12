// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.common;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.server.model.Resource;

public interface JaxRsResourceRegistry {

	void bindJaxRsResource(Object resource, Map<String, String> properties);

	void unbindJaxRsResource(Object resource, Map<String, String> properties);

	Map<InetSocketAddress, Set<Class<?>>> getLoadedClasses();

	Map<InetSocketAddress, Set<Class<?>>> getLoadedClasses(Integer valueOf);

	Map<InetSocketAddress, Set<Object>> getLoadedSingletons();

	Map<InetSocketAddress, Set<Object>> getLoadedSingletons(Integer valueOf);

	Map<InetSocketAddress, Set<Resource>> getLoadedResources();

	Map<InetSocketAddress, Set<Resource>> getLoadedResources(Integer valueOf);

	Map<InetSocketAddress, Map<String, Object>> getLoadedProperties();

	Map<InetSocketAddress, Map<String, Object>> getLoadedProperties(Integer valueOf);

	void add(ServerContext serverCtx);

}
