// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.osgi.launcher;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface OsgiLauncher {
	
	public OsgiLauncher withFrameworkConfig(Map<String, String> config);
	
	public OsgiLauncher withFrameworkConfig(String propertyFile);
		
	public OsgiLauncher useDirectory(String directory);
		
	public OsgiLauncher withBundles(String... bundles);
	
	public OsgiLauncher filter(String filter);
	
	public OsgiLauncher blacklist(String blacklist);

	public OsgiLauncher start();
	
	public void stop(int timeout);
	
	public Framework getFramework();
		
	public OsgiLauncher useExceptionHandler(Consumer<Throwable> handler);
	
	public <T> ServiceRegistration<T> registerService(Class<T> service, T implementation);

	public <T> ServiceRegistration<T> registerService(Class<T> service, T implementation, Dictionary<String, ?> properties);
	
	public <T> CompletableFuture<T> getService(Class<T> service);
	
	public <T> CompletableFuture<T> getService(String service);
	
	public <T> CompletableFuture<T> getService(Class<T> service, String filter);

	public <T> CompletableFuture<T> getService(String service, String filter);
	
	public <T> ServiceTracker<?, ?> listenService(Class<T> service, Consumer<T> onAdded, Consumer<T> onModified, Consumer<T> onRemoved);
	
	public void configureService(String pid, Map<String, Object> configuration);
}
