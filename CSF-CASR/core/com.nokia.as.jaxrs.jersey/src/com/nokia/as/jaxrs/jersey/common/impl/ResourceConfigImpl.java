package com.nokia.as.jaxrs.jersey.common.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ResourceFinder;
import org.glassfish.jersey.server.ServerConfig;
import org.glassfish.jersey.server.model.Resource;

/**
 * Singleton class registered in the osgi service registry.
 * it defines the components of a JAX-RS application and supplies additional meta-data.
 */
public class ResourceConfigImpl extends Application {
	
	private volatile ResourceConfig _resourceConfig;	
	
	public ResourceConfigImpl(ResourceConfig rs) {
		setResourceConfig(rs);
	}
	
	public void setResourceConfig(ResourceConfig rs) {
		_resourceConfig = rs;
	}
	
	// ResourceConfig delegate methods

	public final ResourceConfig addProperties(Map<String, Object> properties) {
		return _resourceConfig.addProperties(properties);
	}

	public boolean equals(Object obj) {
		return _resourceConfig.equals(obj);
	}

	public final ResourceConfig files(boolean recursive, String... files) {
		return _resourceConfig.files(recursive, files);
	}

	public final ResourceConfig files(String... files) {
		return _resourceConfig.files(files);
	}

	public final Application getApplication() {
		return _resourceConfig.getApplication();
	}

	public String getApplicationName() {
		return _resourceConfig.getApplicationName();
	}

	public final ClassLoader getClassLoader() {
		return _resourceConfig.getClassLoader();
	}

	public final Set<Class<?>> getClasses() {
		return _resourceConfig.getClasses();
	}

	public final ServerConfig getConfiguration() {
		return _resourceConfig.getConfiguration();
	}

	public Map<Class<?>, Integer> getContracts(Class<?> componentClass) {
		return _resourceConfig.getContracts(componentClass);
	}

	public final Set<Object> getInstances() {
		return _resourceConfig.getInstances();
	}

	public final Map<String, Object> getProperties() {
		return _resourceConfig.getProperties();
	}

	public final Object getProperty(String name) {
		return _resourceConfig.getProperty(name);
	}

	public Collection<String> getPropertyNames() {
		return _resourceConfig.getPropertyNames();
	}

	public final Set<Resource> getResources() {
		return _resourceConfig.getResources();
	}

	public RuntimeType getRuntimeType() {
		return _resourceConfig.getRuntimeType();
	}

	public final Set<Object> getSingletons() {
		return _resourceConfig.getSingletons();
	}

	public int hashCode() {
		return _resourceConfig.hashCode();
	}

	public boolean isEnabled(Class<? extends Feature> featureClass) {
		return _resourceConfig.isEnabled(featureClass);
	}

	public boolean isEnabled(Feature feature) {
		return _resourceConfig.isEnabled(feature);
	}

	public final boolean isProperty(String name) {
		return _resourceConfig.isProperty(name);
	}

	public boolean isRegistered(Class<?> componentClass) {
		return _resourceConfig.isRegistered(componentClass);
	}

	public boolean isRegistered(Object component) {
		return _resourceConfig.isRegistered(component);
	}

	public final ResourceConfig packages(boolean recursive, String... packages) {
		return _resourceConfig.packages(recursive, packages);
	}

	public final ResourceConfig packages(String... packages) {
		return _resourceConfig.packages(packages);
	}

	public ResourceConfig property(String name, Object value) {
		return _resourceConfig.property(name, value);
	}

	public ResourceConfig register(Class<?> componentClass, Class<?>... contracts) {
		return _resourceConfig.register(componentClass, contracts);
	}

	public ResourceConfig register(Class<?> componentClass, int bindingPriority) {
		return _resourceConfig.register(componentClass, bindingPriority);
	}

	public ResourceConfig register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
		return _resourceConfig.register(componentClass, contracts);
	}

	public ResourceConfig register(Class<?> componentClass) {
		return _resourceConfig.register(componentClass);
	}

	public ResourceConfig register(Object component, Class<?>... contracts) {
		return _resourceConfig.register(component, contracts);
	}

	public ResourceConfig register(Object component, int bindingPriority) {
		return _resourceConfig.register(component, bindingPriority);
	}

	public ResourceConfig register(Object component, Map<Class<?>, Integer> contracts) {
		return _resourceConfig.register(component, contracts);
	}

	public ResourceConfig register(Object component) {
		return _resourceConfig.register(component);
	}

	public final ResourceConfig registerClasses(Class<?>... classes) {
		return _resourceConfig.registerClasses(classes);
	}

	public final ResourceConfig registerClasses(Set<Class<?>> arg0) {
		return _resourceConfig.registerClasses(arg0);
	}

	public final ResourceConfig registerFinder(ResourceFinder resourceFinder) {
		return _resourceConfig.registerFinder(resourceFinder);
	}

	public final ResourceConfig registerInstances(Object... instances) {
		return _resourceConfig.registerInstances(instances);
	}

	public final ResourceConfig registerInstances(Set<Object> arg0) {
		return _resourceConfig.registerInstances(arg0);
	}

	public final ResourceConfig registerResources(Resource... resources) {
		return _resourceConfig.registerResources(resources);
	}

	public final ResourceConfig registerResources(Set<Resource> resources) {
		return _resourceConfig.registerResources(resources);
	}

	public final ResourceConfig setApplicationName(String applicationName) {
		return _resourceConfig.setApplicationName(applicationName);
	}

	public final ResourceConfig setClassLoader(ClassLoader classLoader) {
		return _resourceConfig.setClassLoader(classLoader);
	}

	public ResourceConfig setProperties(Map<String, ?> properties) {
		return _resourceConfig.setProperties(properties);
	}

	public String toString() {
		return _resourceConfig.toString();
	}

}
