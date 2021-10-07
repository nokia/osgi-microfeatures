package com.nokia.as.k8s.controller.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.CustomResourceDefinition;
import com.nokia.as.k8s.controller.ResourceService;
import com.nokia.as.k8s.controller.WatchHandle;
import com.nokia.as.k8s.controller.impl.ResourceServiceImpl.ExceptionalBiFunction;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.Watch;

public class Watcher {
	
	private List<String> allNamespaces;
	private ConcurrentMap<Key, WatchTask<?>> tasks = new ConcurrentHashMap<>();
	private ThreadGroup grp;
	
	public Watcher(List<String> allNamespaces, BundleContext bc) {
		this.allNamespaces = allNamespaces;
		grp = new ThreadGroup("K8S-Watcher");
	}
	
	public <T> WatchHandleImpl<T> createHandle(String namespace, Object obj, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
		return new WatchHandleImpl<>(namespace, obj, added, modified, deleted);
	}
	
	private class Key {
		public final String namespace;
		public final Object obj;
		
		public Key(String namespace, Object obj) {
			this.namespace = namespace;
			this.obj = obj;
		}

		@Override
		public int hashCode() {
			return Objects.hash(obj, namespace);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			return Objects.equals(obj, other.obj) && Objects.equals(namespace, other.namespace);
		}

		@Override
		public String toString() {
			return "WatchKey [namespace=" + namespace + ", crd=" + obj + "]";
		}
	}
	
	public class WatchHandleImpl<T> implements WatchHandle<T> {
		private volatile boolean valid;
		private List<Key> keys = new ArrayList<>();
		private Consumer<T> added;
		private Consumer<T> modified;
		private Consumer<T> deleted;

		public WatchHandleImpl(String namespace, Object obj, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
			this.valid = false;
			if(ResourceService.ALL_NAMESPACES.equals(namespace)) this.keys.addAll(allNamespaces.stream().map(n -> new Key(namespace, obj)).collect(Collectors.toList()));
			else keys.add(new Key(namespace, obj));
			
			this.added = added;
			this.modified = modified;
			this.deleted = deleted;
		}
		
		@Override
		public void close() throws IOException {
			if(valid) {
				keys.forEach(ks -> tasks.computeIfPresent(ks, (k, t) -> ((WatchTask<T>) t).removeConsumers(added, modified, deleted)));
			}
		}
		
		public void valid(boolean valid) {
			this.valid = valid;
		}
		
		public boolean isValid() {
			return valid;
		}
	}
	
	public <T> WatchTask<T> createTask(String namespace, Class<T> clazz, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted, ExceptionalBiFunction watch) {
		return new WatchTaskKubernetes<>(namespace, clazz, added, modified, deleted, watch);
	}
	
	public WatchTask<CustomResource> createTask(String namespace, CustomResourceDefinition def, Consumer<CustomResource> added, Consumer<CustomResource> modified, Consumer<CustomResource> deleted, ExceptionalBiFunction watch) {
		return new WatchTaskCustom(namespace, def, added, modified, deleted, watch);
	}
	
	public void prepareWatchTask(String namespace, CustomResourceDefinition def, Consumer<CustomResource> added, Consumer<CustomResource> modified, Consumer<CustomResource> deleted, ExceptionalBiFunction watch) {
		if(ResourceService.ALL_NAMESPACES.equals(namespace)) {
			for(String ns : allNamespaces) {
				Key key = new Key(ns, def);
				tasks.compute(key, (k, t) -> {
					if(t == null) {
						t = createTask(namespace, def, added, modified, deleted, watch);
						t.start(grp);
					} else {
						((WatchTask<CustomResource>) t).addConsumers(added, modified, deleted);
					}
					return t;
				});
			}
		} else {
			Key key = new Key(namespace, def);
			tasks.compute(key, (k, t) -> {
				if(t == null) {
					t = createTask(namespace, def, added, modified, deleted, watch);
					t.start(grp);
				} else {
					((WatchTask<CustomResource>) t).addConsumers(added, modified, deleted);
				}
				return t;
			});
		}			
	}
	
	public <T> void prepareWatchTask(String namespace, Class<T> clazz, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted, ExceptionalBiFunction watch) {
		if(ResourceService.ALL_NAMESPACES.equals(namespace)) {
			for(String ns : allNamespaces) {
				Key key = new Key(ns, clazz);
				tasks.compute(key, (k, t) -> {
					if(t == null) {
						t = createTask(namespace, clazz, added, modified, deleted, watch);
						t.start(grp);
					} else {
						((WatchTask<T>) t).addConsumers(added, modified, deleted);
					}
					return t;
				});
			}
		} else {
			Key key = new Key(namespace, clazz);
			tasks.compute(key, (k, t) -> {
				if(t == null) {
					t = createTask(namespace, clazz, added, modified, deleted, watch);
					t.start(grp);
				} else {
					((WatchTask<T>) t).addConsumers(added, modified, deleted);
				}
				return t;
			});
		}
	}
	
	private abstract class WatchTask<T> implements Runnable {
		protected Logger LOG = Logger.getLogger(WatchTask.class);
		protected Thread t;
		private List<Consumer<T>> added = new ArrayList<>();
		private List<Consumer<T>> modified = new ArrayList<>();
		private List<Consumer<T>> deleted = new ArrayList<>();
		protected String namespace;
		protected int errorCount = 0;
		protected static final int MAX_RETRY = 10;
		protected ExceptionalBiFunction watch;
		
		public void start(ThreadGroup group) {
			t = new Thread(group, this);
			t.start();
		}
		
		public void stop() {
			t.interrupt();
		}
		
		public WatchTask<T> addConsumers(Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
			this.added.add(added);
			this.modified.add(modified);
			this.deleted.add(deleted);
			return this;
		}
		
		public WatchTask<T> removeConsumers(Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
			this.added.remove(added);
			this.modified.remove(modified);
			this.deleted.remove(deleted);
			return this;
		}
		
		public Consumer<T> added() {
			return added.stream().reduce(t -> { }, Consumer::andThen);
		}
		
		public Consumer<T> modified() {
			return modified.stream().reduce(t -> { }, Consumer::andThen);
		}
		
		public Consumer<T> deleted() {
			return deleted.stream().reduce(t -> { }, Consumer::andThen);
		}
		
		public abstract void run();
	}
	
	public class WatchTaskCustom extends WatchTask<CustomResource> {
		private CustomResourceDefinition def;
		
		public WatchTaskCustom(String namespace, CustomResourceDefinition def, Consumer<CustomResource> added, Consumer<CustomResource> modified, Consumer<CustomResource> deleted, ExceptionalBiFunction watch) {
			this.namespace = namespace;
			this.def = def;
			this.addConsumers(added, modified, deleted);
			this.watch = watch;
		}
		
		@Override
		public void run() {
			LOG.debug("Watch Task starting " + namespace + " " + def);
			while(!Thread.currentThread().isInterrupted()) {
				try {
					Iterable<?> result = watch.apply(namespace, def);
					for(Object response : result) {
						Watch.Response<Object> r = (Watch.Response<Object>) response;
						String type = r.type.toUpperCase();
						Map<String, Object> customObject = (Map<String, Object>) r.object;
						
						CustomResource resource = new CustomResource(def)
								.apiVersion(String.valueOf(customObject.get("apiVersion")))
								.kind(String.valueOf(customObject.get("kind")));
						resource.metadata().putAll((Map<String, Object>) customObject.get("metadata"));
						resource.spec().putAll((Map<String, Object>) customObject.get("spec"));
						
						LOG.debug("Event of type " + type);
						LOG.trace("Content " + customObject);
						
						switch(type) {
							case "ADDED": added().accept(resource); break;
							case "MODIFIED": modified().accept(resource); break;
							case "DELETED": deleted().accept(resource); break;
						}
					}
					errorCount = 0;
				} catch(ApiException e) {
					LOG.warn("Error while watching CustomResource " + def);
					LOG.debug("Exception is", e);
					errorCount++;
					if(errorCount > MAX_RETRY) {
						LOG.warn("Too many errors, stopping task");
						stop();
					}
				}
			}
			LOG.debug("Watch Task exiting for " + namespace + " " + def);
		}
	}
	
	public class WatchTaskKubernetes<T> extends WatchTask<T> {
		private Class<T> clazz;
		
		public WatchTaskKubernetes(String namespace, Class<T> clazz, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted, ExceptionalBiFunction watch) {
			this.namespace = namespace;
			this.clazz = clazz;
			this.addConsumers(added, modified, deleted);
			this.watch = watch;
		}
		
		@Override
		public void run() {
			LOG.debug("Watch Task starting " + namespace + " " + clazz.getName());
			while(!Thread.currentThread().isInterrupted()) {
				try {
					Iterable<?> result = watch.apply(namespace, clazz);
					for(Object response : result) {
						Watch.Response<T> r = (Watch.Response<T>) response;
						String type = r.type.toUpperCase();
						T obj = (T) r.object;
						
						LOG.debug("Event of type " + type);
						LOG.trace("Content " + obj);
						
						switch(type) {
							case "ADDED": added().accept(obj); break;
							case "MODIFIED": modified().accept(obj); break;
							case "DELETED": deleted().accept(obj); break;
						}
					}
					errorCount = 0;
				} catch(ApiException e) {
					LOG.warn("Error while watching KubernetesResource " + clazz.getName() + ": code=" + e.getCode());
					LOG.debug("Exception is:" +
							", msg=" + e.getMessage() + 
							", responseHeaders=" + e.getResponseHeaders() + 
							", responseBody=" + e.getResponseBody(), e);
					errorCount++;
					if(errorCount > MAX_RETRY) {
						LOG.warn("Too many errors, stopping task");
						stop();
					}
				}
			}
			LOG.debug("Watch Task exiting for " + namespace + " " + clazz.getName());
		}
	}

}
