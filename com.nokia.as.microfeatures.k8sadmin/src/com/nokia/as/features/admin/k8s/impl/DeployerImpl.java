// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.features.admin.k8s.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.LifecycleController;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.log4j.Logger;

import com.nokia.as.features.admin.k8s.DeployStatus;
import com.nokia.as.features.admin.k8s.Deployer;
import com.nokia.as.features.admin.k8s.Function;
import com.nokia.as.features.admin.k8s.Pod;
import com.nokia.as.features.admin.k8s.Route;
import com.nokia.as.features.admin.k8s.Runtime;
import com.nokia.as.k8s.controller.CasrResource;
import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.ResourceService;
import com.nokia.as.k8s.controller.WatchHandle;
import com.nokia.as.k8s.sless.fwk.FunctionResource;
import com.nokia.as.k8s.sless.fwk.RouteResource;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1DaemonSet;

@Component
public class DeployerImpl implements Deployer {
	private static Logger LOG = Logger.getLogger(DeployerImpl.class);

	@LifecycleController
	Runnable _startme;

	@ServiceDependency
	ResourceService resourceService;

	Map<String, StatusWatcher> watchers = new HashMap<>();
	
	private WatchHandle<CustomResource> functions;
	private WatchHandle<CustomResource> routes;
	private WatchHandle<CustomResource> runtimes;
	
	private Map<String, Function> functionRepo = new HashMap<>();
	private Map<String, Route> routeRepo = new HashMap<>();
	private Map<String, Runtime> runtimeRepo = new HashMap<>();
	
	private List<String> deployedFunctions = new ArrayList<>();
	private List<String> deployedRoutes = new ArrayList<>();
	private List<String> deployedRuntimes = new ArrayList<>();
	
	
	@Init
	void init() {
		if (!Boolean.getBoolean("standalone")) {
			_startme.run();
		}
	}

	@Start
	public void start() {
		functions = resourceService.watch(FunctionResource.CRD, 
				f -> deployedFunctions.add(f.name() + "@" + f.namespace()), 
				f -> {}, 
				f -> deployedFunctions.remove(f.name() + "@" + f.namespace()));
		routes = resourceService.watch(RouteResource.CRD, 
				r -> deployedRoutes.add(r.name() + "@" + r.namespace()), 
				r -> {}, 
			    r -> deployedRoutes.remove(r.name() + "@" + r.namespace()));
		runtimes = resourceService.watch(CasrResource.CRD, 
				r -> deployedRuntimes.add(r.name() + "@" + r.namespace()), 
				r -> {}, 
			    r -> deployedRuntimes.remove(r.name() + "@" + r.namespace()));
	}
	
	@Stop
	public void stop() throws Exception {
		functions.close();
		routes.close();
		runtimes.close();
	}
	
	public void deployRuntime(Runtime runtime) throws Exception {
		if(watchers.containsKey(rID(runtime))) throw new Exception("Runtime already deployed");
		CasrResource casr = runtime.parsedRuntime();
		CompletableFuture<Boolean> future = resourceService.create(CasrResource.CRD, casr);
		try {
			boolean created = future.get(5, TimeUnit.SECONDS); //TODO is this timeout correct?
			if(created) {
				StatusWatcher watcher = new StatusWatcher(runtime);
				watchers.put(rID(runtime), watcher);
			} else {
				Exception e = new Exception("Creation was unsuccessful.");
				runtime.status(DeployStatus.ERROR(e));
				throw e;
			}
		} catch(Exception e) {
			runtime.status(DeployStatus.ERROR(e));
			throw e;
		}
		runtimeRepo.put(rID(runtime), runtime);
	}
	
	public void undeployRuntime(String name) throws Exception {
		Runtime runtime = runtimeRepo.remove(name);
		if(!watchers.containsKey(rID(runtime))) throw new Exception("Runtime not deployed");
		CasrResource casr = runtime.parsedRuntime();
		CompletableFuture<Boolean> future = resourceService.delete(casr.definition(), casr.name);
		try {
			boolean deleted = future.get(5, TimeUnit.SECONDS); //TODO is this timeout correct?
			if(deleted) {
				watchers.remove(rID(runtime)).stopWatch();
			} else {
				Exception e = new Exception("Deletion was unsuccessful.");
				runtime.status(DeployStatus.ERROR(e));
				throw e;
			}
		} catch(Exception e) {
			runtime.status(DeployStatus.ERROR(e));
			throw e;
		}
	}
	
	private String rID(com.nokia.as.features.admin.k8s.Runtime runtime) {
		return runtime.name + "@" + runtime.namespace;
	}
	
	private class StatusWatcher {
		private Runtime runtime;
		private int totalReplicas;
		private String appSelector;
		private WatchHandle<V1Pod> handle;
		private WatchHandle<V1Deployment> deployHandle;
		private WatchHandle<V1DaemonSet> daemonHandle;
		
		private boolean watch = true;
		
		StatusWatcher(com.nokia.as.features.admin.k8s.Runtime runtime) throws Exception {
			this.runtime = runtime;
			try {
				if(runtime.parsedRuntime().isDeployment()) {
					Consumer<V1Deployment> lambda = d -> {
						if(d.getMetadata().getName().equals(runtime.name)) {
							totalReplicas = d.getSpec().getReplicas();
							appSelector = d.getSpec().getSelector().getMatchLabels().get("app");
							startWatch();
						}
					};
					deployHandle = 
					resourceService.watch(runtime.namespace, V1Deployment.class, lambda, lambda, d -> {});
				} else { //daemonSet
					Consumer<V1DaemonSet> lambda = d -> {
						if(d.getMetadata().getName().equals(runtime.name)) {
							totalReplicas = d.getStatus().getDesiredNumberScheduled();
							appSelector = d.getMetadata().getLabels().get("app");
							startWatch();
						}
					};
					daemonHandle = 
					resourceService.watch(runtime.namespace, V1DaemonSet.class, lambda, lambda, d -> {});
				}
			} catch(Exception e) {
				e.printStackTrace();
				runtime.status(DeployStatus.ERRORK8S(e));
			}
		}
		
		private void startWatch() {
			try {
				if(deployHandle != null) deployHandle.close();
				if(daemonHandle != null) daemonHandle.close();
			} catch (IOException e) { }
			Consumer<V1Pod> calculateStatus = p -> {
				if(!appSelector.equals(p.getMetadata().getLabels().get("app"))) return;
				
				List<V1ContainerStatus> cs = p.getStatus().getContainerStatuses();
				if(cs == null) return;
				
				boolean ready = cs.stream().map(V1ContainerStatus::getReady).reduce(true, Boolean::logicalAnd);
				String name = p.getMetadata().getName();
				Optional<Pod> podOpt = runtime.pod(name);
				Pod pod;
				if(podOpt.isPresent()) {
					pod = podOpt.get();
					pod.setIp(p.getStatus().getPodIP());
				} else {
					pod = new Pod(name, p.getStatus().getPodIP(), 17001); //gogows
					runtime.addPod(pod);
				}
				pod.ready(ready);
				int deployed = Math.toIntExact(runtime.pods().stream().filter(Pod::isReady).count());
				if(this.watch) {
					if(deployed == totalReplicas) runtime.status(DeployStatus.DEPLOYED(totalReplicas));
					else runtime.status(DeployStatus.PENDING(deployed, totalReplicas));
				} else {
					if(deployed == 0) {
						runtime.status(DeployStatus.UNDEPLOYED);
						try {
							handle.close();
						} catch (IOException e) {
							runtime.status(DeployStatus.ERROR(e));
						}
					}
					else runtime.status(DeployStatus.PENDING(deployed, totalReplicas));
				}
			};
			handle = resourceService.watch(runtime.namespace, V1Pod.class, calculateStatus, calculateStatus, calculateStatus);
		}
		
		void stopWatch() {
			this.watch = false;
		}
	}

	public void createFunction(Function function) throws Exception {
		FunctionResource func = function.parsedFunction();
		CompletableFuture<Boolean> future = resourceService.create(FunctionResource.CRD, func);
		boolean created = future.get(5, TimeUnit.SECONDS); //TODO is this timeout correct?
		if(!created) {
			throw new Exception("Creation was unsuccessful.");
		}
		functionRepo.put(function.name + "@" + function.namespace, function);
	}
	
	public void deleteFunction(String name) throws Exception {
		FunctionResource func = functionRepo.get(name).parsedFunction();
		CompletableFuture<Boolean> future = resourceService.delete(func.definition(), func.name);
		boolean deleted = future.get(5, TimeUnit.SECONDS); //TODO is this timeout correct?
		if(!deleted) {
			throw new Exception("Deletion was unsuccessful.");
		}
	}
	
	public void createRoute(Route route) throws Exception {
		RouteResource rte = route.parsedRoute();
		CompletableFuture<Boolean> future = resourceService.create(RouteResource.CRD, rte);
		boolean created = future.get(5, TimeUnit.SECONDS); //TODO is this timeout correct?
		if(!created) {
			throw new Exception("Creation was unsuccessful.");
		}
		routeRepo.put(route.name + "@" + route.namespace, route);
	}
	
	public void deleteRoute(String name) throws Exception {
		RouteResource rte = routeRepo.get(name).parsedRoute();
		CompletableFuture<Boolean> future = resourceService.delete(rte.definition(), rte.name);
		boolean deleted = future.get(5, TimeUnit.SECONDS); //TODO is this timeout correct?
		if(!deleted) {
			throw new Exception("Deletion was unsuccessful.");
		}
	}
	
	public List<Function> deployedFunctions() {
		return functionRepo.entrySet().stream()
						   .filter(e -> deployedFunctions.contains(e.getKey()))
						   .map(Entry::getValue)
						   .collect(Collectors.toList());
	}
	
	public List<Runtime> deployedRuntimes() {
		return new ArrayList<>(runtimeRepo.values());
	}
	
	public List<Route> deployedRoutes() {
		return routeRepo.entrySet().stream()
					     .filter(e -> deployedRoutes.contains(e.getKey()))
					     .map(Entry::getValue)
					     .collect(Collectors.toList());
	}
}
