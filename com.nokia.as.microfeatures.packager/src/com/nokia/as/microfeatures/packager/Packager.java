// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.packager;

import java.net.URL;
import java.nio.file.Path;
import java.util.List ;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

// import org.osgi.annotation.versioning.ProviderType;

/**
 * Osgi service allowing to generate a microfeature runtime.
 */
@ProviderType
public interface Packager {
	
	enum Params {
		TARGET,    // string value, target runtime directory name (mandatory)
		LEGACY,    // boolean value, false by default
		PLATFORM,  // string value, "csf" by default
		GROUP,     // string value, "group" by default
		COMPONENT, // string value, "component" by default
		INSTANCE   // string value, "instance" by default
	}
	
	/**
	 * Asynchronously creates a runtime zip.
	 * @param urls the urls of the artifacts (bundles) that are part of the runtime.
	 * @param params parameters.
	 * @return the zip file represented as a CompletableFuture.
	 */
	CompletableFuture<Path> packageRuntime (List<URL> urls, Map<Params, Object> params);
}
