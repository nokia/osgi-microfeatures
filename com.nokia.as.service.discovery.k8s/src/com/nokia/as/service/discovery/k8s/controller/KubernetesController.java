// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.discovery.k8s.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.k8s.controller.CasrResource;
import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.ResourceService;
import com.nokia.as.k8s.controller.WatchHandle;
import com.nokia.as.service.discovery.k8s.controller.objs.ConfigMap;
import com.nokia.as.service.discovery.k8s.controller.objs.DaemonSet;
import com.nokia.as.service.discovery.k8s.controller.objs.Deployment;
import com.nokia.as.service.discovery.k8s.controller.objs.Ingress;
import com.nokia.as.service.discovery.k8s.controller.objs.Pod;
import com.nokia.as.service.discovery.k8s.controller.objs.Rbac;
import com.nokia.as.service.discovery.k8s.controller.objs.Service;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Role;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1DaemonSet;

@Component
@Property(name = "asr.component.parallel", value = "true")
public class KubernetesController {
	
	private LogService logger;
	private ControllerConfiguration configuration;
	private Map<String, Map<String, Object>> registers = new ConcurrentHashMap<>();
	private Map<String, WatchHandle<CustomResource>> handles = new HashMap<>();
	
	@ServiceDependency	
	private LogServiceFactory logFactory;
	
	@ServiceDependency
	private ResourceService resourceService;
	
	@Inject
	private BundleContext bc;
	
	@ConfigurationDependency
	public void loadConfiguration(ControllerConfiguration configuration) {
		this.configuration = configuration;
	}
	
	@Start
	public void start() throws IOException {
		logger = logFactory.getLogger(KubernetesController.class);
		logger.info("Starting kubernetes controller...");
		
		logger.info("Configuration is:");
		logger.info("Namespaces: %s", configuration.getNamespaces());
		logger.info("RBAC enabled?: %s", configuration.getRbacEnabled());
		logger.info("CASR registry: %s", configuration.getCasrRegistry());
		logger.info("CASR image tag: %s", configuration.getCasrImageTag());
		logger.info("CASR version: %s", configuration.getCasrVersion());
		
	    List<String> namespaces = configuration.getNamespaces();
		logger.debug("Namespaces defined in configuration are %s", namespaces);
		namespaces.replaceAll(s -> "$CURRENT$".equals(s) ? System.getProperty("k8s.namespace") : s);
		if("*".equals(namespaces.get(0))) namespaces = getAllNamespaces();
			            
		logger.debug("Namespaces = %s", namespaces);
		if(!namespaces.isEmpty()) {
	    	for(String namespace: namespaces) {
				logger.debug("Getting current runtimes and starting watch in namespace %s", namespace);
	    	    registers.put(namespace, new ConcurrentHashMap<>());
				registers.get(namespace).put("runtimes", new ArrayList<String>());
	    	    getRuntimes(namespace);
	    	}
		}
	}
	
	@Stop
	public void stop() throws Exception {
		logger.info("Stopping...");
		handles.values().stream()
						.forEach(h -> {
							try {
								h.close();
							} catch (IOException e) { }
						});
		handles.clear();
	    registers.values().clear();
		registers.clear();
		logger.info("Stopped");
	}
	
	//Gets all namespaces from cluster
	private List<String> getAllNamespaces() {
		try {
			return resourceService.listNamespaces().get(5, TimeUnit.SECONDS);
		} catch(Exception e) {
			logger.error("Could not list namespaces", e);
			return Collections.emptyList();
		}
	}
	
	private void getRuntimes(String namespace) {
		Map<String, Object> register = registers.get(namespace);
		logger.debug("Getting runtimes in %s", namespace);
		resourceService.getAll(namespace, CasrResource.CRD)
					   .thenAccept(casrs -> {
						   casrs.forEach(casr -> {
							   logger.debug("Parsing custom object");
							   Map<String, Object> parsed = parseRuntime(casr.attributes());
							   logger.trace("Parsed: %s", parsed);
							   logger.debug("Creating kubernetes object");
							   consumeRuntime(casr, parsed, namespace);
						   });
						   watchRuntimes(namespace);
					   });
		logger.trace("Current register on namespace %s", namespace);
		logger.trace("%s", register);
		logger.debug("Finished getting runtimes");
	}
	
	@SuppressWarnings("unchecked")
	private void watchRuntimes(String namespace) {
		Map<String, Object> register = registers.get(namespace);
		
		WatchHandle<CustomResource> handle = resourceService.watch(namespace, CasrResource.CRD, 
				r -> {   //ADDED
					logger.debug("Received event of type ADDED");
					logger.debug("Parsing custom object");
					Map<String, Object> parsed = parseRuntime(r.attributes());
					logger.trace("Parsed: %s", parsed);

					String runtimeName = parsed.get("name").toString();
					List<String> runtimeList = (List<String>) registers.get(namespace).get("runtimes");
					if(!runtimeList.contains(runtimeName)) {
						logger.debug("Creating kubernetes object");
						consumeRuntime(r, parsed, namespace);
					} else {
						logger.debug("Ignoring event, runtime %s already exists", runtimeName);
					}
					logger.trace("Current register for namespace %s", namespace);
					logger.trace("%s", register);
				},
				r -> {}, //MODIFIED 
				r -> {   //DELETED
					logger.debug("Received event of type DELETED");
					logger.debug("Parsing custom object");
					Map<String, Object> parsed = parseRuntime(r.attributes());
					logger.trace("Parsed: %s", parsed);

					String runtimeName = parsed.get("name").toString();
					List<String> runtimeList = (List<String>) registers.get(namespace).get("runtimes");
					if(runtimeList.contains(runtimeName)) {
						logger.debug("Querying registry for runtime of name %s", runtimeName);
						unregisterRuntime(runtimeName, namespace);
					} else {
						logger.debug("Ignoring event, runtime %s does not exist", runtimeName);
					}
					logger.trace("Current register for namespace %s", namespace);
					logger.trace("%s", register);
				});
		handles.put(namespace, handle);		
	}
	
	@SuppressWarnings({ "unchecked" })
	private Map<String, Object> parseRuntime(Map<String, Object> runtime) {
		Map<String, Object> properties = new HashMap<>();
		String name = ((Map<String, Object>) runtime.get("metadata")).get("name").toString();
		properties.put("name", name);
		Map<String, Object> spec = (Map<String, Object>) runtime.get("spec");
		
		//runtime section
		Map<String, Object> specRuntime = (Map<String, Object>) spec.get("runtime");
		properties.put("runtime.replicas", new Double(specRuntime.getOrDefault("replicas", 1l).toString()).intValue());
		if(specRuntime.containsKey("docker")) {
			Map<String, Object> docker = (Map<String, Object>) specRuntime.get("docker");
			properties.put("runtime.docker.registry", docker.get("registry").toString());
			properties.put("runtime.docker.imageRepo", docker.get("imageRepo").toString());
			properties.put("runtime.docker.imageTag", docker.get("imageTag").toString());
			properties.put("docker", true);
		} else {
			Map<String, Object> features = (Map<String, Object>) specRuntime.get("build");
			properties.put("runtime.features", features.get("features"));
			properties.put("runtime.features.registry", configuration.getCasrRegistry());
			properties.put("runtime.features.imageTag", configuration.getCasrImageTag());
			properties.put("runtime.features.casrVersion", features.getOrDefault("version", configuration.getCasrVersion()));
			properties.put("runtime.features.casrRepo", features.getOrDefault("repository", configuration.getCasrRepo()));
			
			if(!"none".equals(configuration.getCasrObrUrl())) {
				properties.put("runtime.features.obrUrl", features.getOrDefault("obrUrl", configuration.getCasrObrUrl()));
			}
			properties.put("docker", false);
		}
		
		//ports section
		properties.put("ports", spec.getOrDefault("ports", new ArrayList<>()));
		
		//configuration section
		Map<String, Object> configuration = (Map<String, Object>) spec.getOrDefault("configuration", new HashMap<>());
		properties.put("configuration.labels", configuration.getOrDefault("labels", new ArrayList<>()));
		properties.put("configuration.configMapName", configuration.getOrDefault("configMapName", ""));
		properties.put("configuration.tlsSecret", configuration.getOrDefault("tlsSecret", ""));
		properties.put("configuration.env", configuration.getOrDefault("env", new ArrayList<>()));
		properties.put("configuration.override", configuration.getOrDefault("override", new ArrayList<>()));
		properties.put("configuration.files", configuration.getOrDefault("files", new ArrayList<>()));
		
		Map<String, Object> prometheus = (Map<String, Object>) configuration.get("prometheus");
		if(prometheus != null) {
			properties.put("configuration.prometheus.port", prometheus.get("port"));
			properties.put("configuration.prometheus.path", prometheus.getOrDefault("path", "/metrics"));
		}
		
		return properties;
	}
	
	private void consumeRuntime(CustomResource r, Map<String, Object> runtime, String namespace) {
		String name = runtime.get("name").toString();

		V1ConfigMap cm = ConfigMap.create(runtime);
		V1ServiceAccount sa = Rbac.createSA(name, namespace);
		V1Role role = Rbac.createRole(name, namespace);
		V1RoleBinding rb = Rbac.createRB(name, namespace);
		V1PodSpec pod = Pod.create(runtime, sa);
		V1Service svc = Service.create(runtime);
		
		boolean deployment = (boolean) runtime.get("deployment");
		List<String> runtimeList = (List<String>) registers.get(namespace).get("runtimes");
		runtimeList.add(name);
		if(configuration.getRbacEnabled()) {
			logger.debug("RBAC enabled, installing ServiceAcount, Role and RoleBinding");
			createAndAddDependent(namespace, name + "-sa", V1ServiceAccount.class, sa, r);
			createAndAddDependent(namespace, name + "-role", V1Role.class, role, r);
			createAndAddDependent(namespace, name + "-rb", V1RoleBinding.class, rb, r);
		}
			
		createAndAddDependent(namespace, name + "-cm", V1ConfigMap.class, cm, r);
		createAndAddDependent(namespace, name + "-svc", V1Service.class, svc, r);
			
		if(deployment) {
			V1Deployment deploy = Deployment.create(runtime, pod);
			createAndAddDependent(namespace, name + "-deploy", V1Deployment.class, deploy, r);
		} else {
			V1DaemonSet ds = DaemonSet.create(runtime, pod);
			createAndAddDependent(namespace, name + "-ds", V1DaemonSet.class, ds, r);
		}
			
		boolean ingress = (boolean) runtime.get("ports.ingress");
		if(ingress) {
			ExtensionsV1beta1Ingress ing = Ingress.create(runtime);
			createAndAddDependent(namespace, name + "-ing", ExtensionsV1beta1Ingress.class, ing, r);
		}
	}
	
	private void unregisterRuntime(String name, String namespace) {
		Map<String, Object> register = registers.get(namespace);
		List<String> runtimeList = (List<String>) register.get("runtimes");
		runtimeList.remove(name);
	}
	
	private <T> void createAndAddDependent(String namespace, String name, Class<T> clazz, T obj, CustomResource r) {
		try {
			resourceService.create(namespace, clazz, obj)
				.thenAccept(b -> { 
					if(b) {
						int retries = 0;
						while(retries < 5) {
							try {
								T res = resourceService.get(clazz, name).get().get();
								if(resourceService.addDependent(namespace, CustomResource.class, r, clazz, res).get()) break;
							} catch (InterruptedException | ExecutionException e) {
								logger.warn("Unable to link %s %s", clazz, name);
								logger.debug("Exception is", e);
							}
							retries++;
						}
					} else {
						logger.warn("Resource not created %s %s", clazz, name);
					}
				}).get();
		} catch (InterruptedException | ExecutionException e) {
			logger.warn("Unable to create %s %s", clazz, name);
			logger.debug("Exception is", e);
		}
	}
}
