// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.discovery.k8s.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.discovery.Advertisement;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.osgi.MapToDictionary;
import com.nokia.as.k8s.controller.ResourceService;
import com.nokia.as.k8s.controller.WatchHandle;

import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodStatus;

@Component
@Property(name = "asr.component.parallel", value = "true")
public class KubernetesClientDiscovery {
	
	private final String CONTAINER_PORT_NAME = "container.port.name";
	private final String CONTAINER_NAME = "container.name";
	private final String CONTAINER_PORT_PROTOCOL = "container.port.protocol";	
	private final String NAMESPACE = "namespace";
	private final String POD_NAME = "pod.name";
	
	private LogService logger;
	private ClientConfiguration configuration;
	private Map<String, Map<String, ServiceRegistration<Advertisement>>> registers = new ConcurrentHashMap<>();
	private Map<String, WatchHandle<V1Pod>> handles = new HashMap<>();
	
	@ServiceDependency	
	private LogServiceFactory logFactory;
	
	@ServiceDependency
	private ResourceService resourceService;
	
	@Inject
	private BundleContext bc;
	
	//This collector get the first and only element of a list
	private static <T> Collector<T, List<T>, T> singletonCollector(String msg) {
	    return Collector.of(
	            ArrayList::new,
	            List::add,
	            (left, right) -> { left.addAll(right); return left; },
	            list -> {
	                if (list.size() != 1) {
	                    throw new IllegalStateException(msg);
	                }
	                return list.get(0);
	            }
	    );
	}
	
	@ConfigurationDependency
	public void loadConfiguration(ClientConfiguration configuration) {
		this.configuration = configuration;
	}
	
	@Start
	public void start() throws IOException {
		logger = logFactory.getLogger(KubernetesClientDiscovery.class);
		logger.info("Starting kubernetes discovery...");
		
	    List<String> namespaces = configuration.getNamespaces();
		logger.debug("Namespaces defined in configuration are %s", namespaces);
		namespaces.replaceAll(s -> "$CURRENT$".equals(s) ? System.getProperty("k8s.namespace") : s);
		if("*".equals(namespaces.get(0))) namespaces = getAllNamespaces();
			            
		logger.debug("Namespaces = %s", namespaces);
		if(!namespaces.isEmpty()) {
	    	for(String namespace: namespaces) {
				logger.debug("Getting current pods and starting watch in namespace %s", namespace);
	    	    registers.put(namespace, new ConcurrentHashMap<>());
	    	    getPods(namespace);
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
	    registers.values().stream()
	                      .flatMap(regs -> regs.values().stream())
	                      .forEach(ServiceRegistration::unregister);
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

	private void watchPods(String namespace) {
		logger.debug("Watching namespace: %s", namespace);
		WatchHandle<V1Pod> handle = resourceService.watch(namespace, V1Pod.class, 
				p -> {},  //ADDED,
				p -> {    //MODIFIED
					logger.debug("Received event of type MODIFIED");
					logger.debug("Parsing pod...");
					List<Map<String, Object>> props = parsePod(p, namespace);
					Map<String, ServiceRegistration<Advertisement>> register = registers.get(namespace);
					
					props.forEach(prop -> {
						String id = getContainerId(prop);
						
						logger.debug("container id = %s", id);
						logger.debug("container isReady? %s", isContainerReady(prop));
						if(isContainerReady(prop)) { //pod went ready
							if(!register.containsKey(id)) register.put(id, registerContainer(prop));
						} else { //pod stopped being ready 
							ServiceRegistration<Advertisement> reg = register.remove(id);
							if(reg != null) {
								logger.debug("Unregistering %s", id);
								reg.unregister();
							}
						}
					});
					logger.trace("Current register %s", register);
				},
				p -> {}); //DELETED
		handles.put(namespace, handle);
	}
	
	//Consume initial pods in a namespace
	//This method will ask the Kubernetes API server the pods in a given namespace
	//Then for each pod, it will parse the information necessary to create the advert
	//Finally, it will save the pod in a map for later use (unregistration)
	private void getPods(String namespace) {
		logger.debug("Getting pods in %s", namespace);
		resourceService.getAll(namespace, V1Pod.class)
					   .thenAccept(pods -> {
						   List<Map<String, Object>> props = 
									pods.stream()
									    .map(p -> parsePod(p, namespace))
									    .flatMap(List::stream)
									    .collect(Collectors.toList());
							   
						   Map<String, ServiceRegistration<Advertisement>> register = registers.get(namespace);
						   props.forEach(p -> {
								if(isContainerReady(p)) register.put(getContainerId(p), registerContainer(p));
								else logger.debug("Pod is not yet ready %s", getContainerId(p));
						   });
						   logger.debug("Current register %s", register);
						   logger.debug("Finished list pods for namespace %s, starting watch...", namespace);
						   watchPods(namespace);
					   });
	}
	
	private String getContainerId(Map<String, Object> containerProperties) {
		return containerProperties.get(NAMESPACE) + "/" +
			   containerProperties.get(POD_NAME) + ":" +
			   containerProperties.get(CONTAINER_PORT_NAME) + "/" +
			   containerProperties.get(CONTAINER_PORT_PROTOCOL);
	}

	private boolean isContainerReady(Map<String, Object> containerProperties) {
		return getContainerId(containerProperties).equals("no id") 
					? false
					: (boolean) containerProperties.getOrDefault("container.isReady", "false");
	}
	
	private List<Map<String, Object>> parsePod(V1Pod pod, String namespace) {
		List<Map<String, Object>> parsedPod = new ArrayList<>();
		
		logger.trace("Parsing pod %s", pod);
		try {
		pod.getSpec().getContainers()
					 .forEach(c -> {
						 logger.trace("Parsing container %s", c);
						 if(c.getPorts() != null) {
							 c.getPorts().forEach(p -> {
								logger.trace("Parsing port: %s", p);
								try {
									Map<String, Object> properties = new HashMap<>();
									V1PodStatus pStatus = pod.getStatus();
									logger.trace("Pod status: %s", pStatus);
									if(pStatus != null && pStatus.getContainerStatuses() != null) {
										V1ContainerStatus containerStatus = pStatus.getContainerStatuses().stream()
																			.filter(cs -> cs.getName().equals(c.getName()))
																			.collect(singletonCollector(c.getName()));
										logger.trace("Container status: %s", containerStatus);
										
										properties.put(ConfigConstants.SERVICE_IP, pStatus.getPodIP());
										properties.put("container.isReady", containerStatus.getReady());
										properties.put("containerID", containerStatus.getContainerID());		
									} else {
										properties.put("container.isReady", false);
									}
									properties.put(NAMESPACE, namespace);
									properties.put(POD_NAME, pod.getMetadata().getName());
									properties.put(CONTAINER_NAME, c.getName());
									properties.put(CONTAINER_PORT_NAME, p.getName());
									properties.put(CONTAINER_PORT_PROTOCOL, p.getProtocol());
									properties.put(ConfigConstants.SERVICE_PORT, p.getContainerPort());
									properties.put("k8s.labels", pod.getMetadata().getLabels());
									addCASRProperties(pod, properties);
									logger.trace("Parsed properties: %s", properties);
									parsedPod.add(properties);
								} catch(Exception e) {
									logger.warn("Error while parsing properties status for %s", c.getName());
									logger.debug("Exception is", e);
								}
							 });
						} else {
							logger.trace("ports is null");
						}
					 });
		} catch(Exception e) {
			logger.debug("Got exception when parsing pod", e);
		}
		
		logger.trace("Parsed pod %s", parsedPod);
		return parsedPod;
	}
	
	private void addCASRProperties(V1Pod pod, Map<String, Object> properties) {
		Map<String, String> labels = pod.getMetadata().getLabels();
		String loadBalancer = labels == null ? "" : labels.getOrDefault("com.nokia.casr.loadbalancer", "");
	    String portName = properties.get(CONTAINER_PORT_NAME).toString();
		String portNameLower = portName.toLowerCase();
	    
		if(!loadBalancer.isEmpty()) {
	        properties.put("pod.label.com.nokia.casr.loadbalancer", loadBalancer);
	    }

		//port name should be ioh-mux-PROTOCOL-INSTANCENAME
		List<String> iohs = Arrays.asList("d-", "r-", "h-", "m-", "s-");
		String prefix = "mux-";
		if(portNameLower.startsWith(prefix)) { //it's an ioh
			portNameLower = portNameLower.replace(prefix, ""); //now name should be PROTOCOL-INSTANCENAME
			int idx = portNameLower.indexOf("-");
			String protocol = portNameLower.substring(0, idx + 1); //include the -
			String instanceName = portName.replace(prefix, "").replace(protocol, ""); //keep the case for instance name
			
			if(!instanceName.isEmpty()) {
				String suffix = labels == null ? null : labels.get("casr.ioh.instance.name.suffix");
				if(suffix != null) {
					instanceName += ("-" + suffix);
				}
				
				//we need to add the replica number to the instance name
				try {
					String[] podNameSplit = pod.getMetadata().getName().split("-");
					int replica = Integer.parseInt(podNameSplit[podNameSplit.length - 1]);
					instanceName += ("-" + (replica + 1)); //if instance 0, then instanceName is name-1
				} catch(Exception e) {
					logger.debug("%s is not part of a replica set", pod.getMetadata().getName());
				}
			} else {
				instanceName = pod.getMetadata().getName();
			}

			if(iohs.contains(protocol)) {
				properties.put("mux.factory.remote", "ioh");
				properties.put("group.target", "*");
				properties.put("group.name", properties.get(NAMESPACE));
				properties.put("instance.name", instanceName);
				properties.put("platform.name", "csf");
				
				int moduleId = -1;
				switch(protocol) {
					case "h-": properties.put("component.name", "HttpIOH"); moduleId = 286; break;
					case "d-": properties.put("component.name", "DiameterIOH"); moduleId = 289; break;
					case "r-": properties.put("component.name", "RadiusIOH"); moduleId = 296; break;
					case "m-": properties.put("component.name", "MetersIOH"); moduleId = 324; break;
					case "s-": properties.put("component.name", "SlessIOH"); moduleId = 499; break;
					default:
				}
				properties.put("module.id", moduleId);
			}
	    }
	}
	
	private ServiceRegistration<Advertisement> registerContainer(Map<String, Object> properties) {
		properties.put("provider", "kubernetes");
		MapToDictionary<String, Object> dict = new MapToDictionary<>(properties);
		Advertisement advert = new Advertisement(properties.get(ConfigConstants.SERVICE_IP).toString(),
												 properties.get(ConfigConstants.SERVICE_PORT).toString());
		ServiceRegistration<Advertisement> registration = bc.registerService(Advertisement.class, advert, dict);
		logger.debug("Registered %s to OSGi registry", getContainerId(properties));
		return registration;
	}
}
