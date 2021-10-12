// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.k8s.controller.registry;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.nokia.as.k8s.controller.CustomResource;

import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Role;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1DaemonSet;

public class Registrar {
	
	static class ResourceKey {
		public final String namespace;
		public final String name;
		public final Object obj;
		
		public ResourceKey(String namespace, String name, Object obj) {
			this.namespace = namespace;
			this.name = name;
			this.obj = obj;
		}

		@Override
		public int hashCode() {
			return Objects.hash(obj, name, namespace);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ResourceKey other = (ResourceKey) obj;
			return Objects.equals(obj, other.obj) && Objects.equals(name, other.name)
					&& Objects.equals(namespace, other.namespace);
		}
	}
	
	private static Logger LOG = Logger.getLogger(Registrar.class);
	private Map<ResourceKey, ServiceRegistration<?>> register = new ConcurrentHashMap<>();
	private Map<Class<?>, Function<Object, Dictionary<String, Object>>> flatten;
	private Map<Class<?>, String> kinds;
	private BundleContext bc;
	
	public Registrar(BundleContext bc) {
		this.bc = bc;
		flatten = new HashMap<>();
		flatten.put(CustomResource.class, o -> flattenMap(((CustomResource) o)));
		flatten.put(V1Pod.class, o -> flattenMap(((V1Pod) o).getMetadata()));
		flatten.put(V1ConfigMap.class, o -> flattenMap(((V1ConfigMap) o).getMetadata()));
		flatten.put(V1Secret.class, o -> flattenMap(((V1Secret) o).getMetadata()));
		flatten.put(V1Service.class, o -> flattenMap(((V1Service) o).getMetadata()));
		flatten.put(ExtensionsV1beta1Ingress.class, o -> flattenMap(((ExtensionsV1beta1Ingress) o).getMetadata()));
		flatten.put(V1Deployment.class, o -> flattenMap(((V1Deployment) o).getMetadata()));
		flatten.put(V1DaemonSet.class, o -> flattenMap(((V1DaemonSet) o).getMetadata()));
		flatten.put(V1ServiceAccount.class, o -> flattenMap(((V1ServiceAccount) o).getMetadata()));
		flatten.put(V1ClusterRole.class, o -> flattenMap(((V1ClusterRole) o).getMetadata()));
		flatten.put(V1ClusterRoleBinding.class, o -> flattenMap(((V1ClusterRoleBinding) o).getMetadata()));
		flatten.put(V1Role.class, o -> flattenMap(((V1Role) o).getMetadata()));
		flatten.put(V1RoleBinding.class, o -> flattenMap(((V1RoleBinding) o).getMetadata()));
		
		kinds.put(V1Pod.class, "Pod");
		kinds.put(V1ConfigMap.class, "ConfigMap");
		kinds.put(V1Secret.class, "Secret");
		kinds.put(V1Service.class, "Service");
		kinds.put(ExtensionsV1beta1Ingress.class, "Ingress");
		kinds.put(V1Deployment.class, "Deployment");
		kinds.put(V1DaemonSet.class, "DaemonSet");
		kinds.put(V1ServiceAccount.class, "ServiceAccount");
		kinds.put(V1ClusterRole.class, "ClusterRole");
		kinds.put(V1ClusterRoleBinding.class, "ClusterRoleBinding");
		kinds.put(V1Role.class, "Role");
		kinds.put(V1RoleBinding.class, "RoleBinding");
	}
	
	public synchronized <T> void register(Class<T> clazz, T obj) {
		Dictionary<String, Object> props = flatten.get(clazz).apply(obj);
		if(kinds.containsKey(clazz)) props.put("kind", kinds.get(clazz));
		ResourceKey key = getKey((String) props.get("namespace"), (String) props.get("name"), obj);
		if(register.containsKey(key)) {
			return;
			//unregister(resource);
		}
		
		LOG.info("Registering resource " + obj);
		register.put(key, bc.registerService(clazz, obj, props));
	}
	
	public synchronized <T> void unregister(Class<T> clazz, Object obj) {
		Dictionary<String, Object> props = flatten.get(clazz).apply(obj);
		if(kinds.containsKey(clazz)) props.put("kind", kinds.get(clazz));
		ResourceKey key = getKey((String) props.get("namespace"), (String) props.get("name"), obj);
		if(register.containsKey(key)) {
			LOG.info("Unregistering resource " + obj);
			register.remove(key).unregister();
		} else {
			LOG.debug("Resource does not exist, ignoring " + key.name);
		}
	}
	
	private ResourceKey getKey(String namespace, String name, Object obj) {
		if(obj == null) throw new IllegalArgumentException("Resource is null");
		return new ResourceKey(namespace, name, obj);
	}
	
	private static class Element {
		public final String path;
		public final Object obj;
		public Element(String path, Object obj) {
			this.path = path;
			this.obj = obj;
		}
		
		public boolean isMap() {
			return obj instanceof Map<?, ?>;
		}
		
		public boolean isList() {
			return obj instanceof List<?>;
		}
		
		@SuppressWarnings("unchecked")
		public <T> T value() {
			return (T) obj;
		}
	}
	
	public static Dictionary<String, Object> flattenMap(V1ObjectMeta metadata) {
		Dictionary<String, Object> result = new Hashtable<String, Object>();
		result.put("name", metadata.getName());
		result.put("namespace", metadata.getNamespace());
		result.put("labels", flattenMap(metadata.getLabels()));
		result.put("annotations", flattenMap(metadata.getAnnotations()));
		return result;
	}
	
	public static Dictionary<String, Object> flattenMap(CustomResource res) {
		Dictionary<String, Object> result = flattenMap(res.metadata());
		result.put("kind", res.kind());
		return result;
	}
	
	public static Dictionary<String, Object> flattenMap(Map<?, ?> metadata) {
		Dictionary<String, Object> result = new Hashtable<String, Object>();
		result.put("name", metadata.get("name"));
		result.put("namespace", metadata.get("namespace"));

		if(metadata == null || metadata.isEmpty()) {
			return result;
		}
		
		Deque<Element> dfsStack = new ArrayDeque<Element>();
		dfsStack.push(new Element("", metadata));
		
		while(!dfsStack.isEmpty()) {
			Element el = dfsStack.pop();
			if(el.isMap()) {
				Map<String, Object> subMap = el.value();
				for(Map.Entry<String, Object> kv : subMap.entrySet()) {
					String key = el.path.isEmpty() ? kv.getKey() : el.path + "." + kv.getKey();
					dfsStack.add(new Element(key, kv.getValue()));
				}
			} else if(el.isList()) {
				List<Object> subList = el.value();
				result.put(el.path, subList);
			} else {
				result.put(el.path, el.value());
			}
		}
		
		return result;
	}
}