package com.nokia.as.k8s.controller.mock;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.alcatel.as.service.log.LogService;
import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.CustomResourceDefinition;
import com.nokia.as.k8s.controller.WatchHandle;
import com.nokia.as.k8s.controller.serialization.SerializationUtils;

public class Watcher {
	
	private LogService logger;
	private ConcurrentMap<Object, WatchTask<?>> tasks = new ConcurrentHashMap<>();
	private ThreadGroup grp;
	private WatchService watcher;
	private WatchKey theKey;
	private Path watchDir;
	private Future<?> watchFuture;
	
	List<Consumer<Event<Path>>> consumers = Collections.synchronizedList(new ArrayList<>());
	
	private Runnable watcherRunnable = () -> {
		while(!Thread.currentThread().isInterrupted()) {
			try {
				theKey = watcher.take();
			} catch(Exception e) {
				logger.warn("Oops!", e);
				break;
			}
			
			for(WatchEvent<?> event : theKey.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
		        WatchEvent<Path> ev = (WatchEvent<Path>)event;
		        Path filename = ev.context();
				logger.debug("Watch event kind " + kind + " path " + filename);

		        Path child = watchDir.resolve(filename);
		        try {
		        	if(kind == StandardWatchEventKinds.ENTRY_DELETE) consumers.forEach(c -> c.accept(new Event<>("DELETED", child)));
		        	else {
			            if (!Files.probeContentType(child).equals("application/x-yaml")) {
			                logger.debug("file " + child + " is not application/x-yaml");
			                continue;
			            }
			            
			            if(kind == StandardWatchEventKinds.ENTRY_CREATE) consumers.forEach(c -> c.accept(new Event<>("ADDED", child)));
			            if(kind == StandardWatchEventKinds.ENTRY_MODIFY) consumers.forEach(c -> c.accept(new Event<>("MODIFIED", child)));
		        	}
		        } catch (Exception e) {
		            logger.warn("Exception raised when reading file", e);
		            continue;
		        }
			}
			if (!theKey.reset()) break;
		}
	};
	
	public Watcher(String path, LogService logger) {
		this.logger = logger;
		grp = new ThreadGroup("K8S-Watcher");
		try {
			watcher = FileSystems.getDefault().newWatchService();
			watchDir = Paths.get(path);
			theKey = watchDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, 
											    StandardWatchEventKinds.ENTRY_DELETE, 
												StandardWatchEventKinds.ENTRY_MODIFY);
			watchFuture = Executors.newSingleThreadExecutor().submit(watcherRunnable);
		} catch (IOException e) {
			logger.warn("Error while initializing watch", e);
		}
	}
	
	public void stop() {
		watchFuture.cancel(true);
	}
	
	public <T> WatchHandleImpl<T> createHandle(Object obj, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
		return new WatchHandleImpl<>(obj, added, modified, deleted);
	}
	
	public class WatchHandleImpl<T> implements WatchHandle<T> {
		private volatile boolean valid;
		private List<Object> keys = new ArrayList<>();
		private Consumer<T> added;
		private Consumer<T> modified;
		private Consumer<T> deleted;

		public WatchHandleImpl(Object obj, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
			this.valid = false;
			keys.add(obj);
			
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
	
	public <T> WatchTask<T> createTask(Class<T> clazz, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
		return new WatchTaskKubernetes<>(clazz, added, modified, deleted);
	}
	
	public WatchTask<CustomResource> createTask(CustomResourceDefinition def, Consumer<CustomResource> added, Consumer<CustomResource> modified, Consumer<CustomResource> deleted) {
		return new WatchTaskCustom(def, added, modified, deleted);
	}
	
	public void prepareWatchTask(CustomResourceDefinition def, Consumer<CustomResource> added, Consumer<CustomResource> modified, Consumer<CustomResource> deleted) {
		tasks.compute(def, (k, t) -> {
			if(t == null) {
				t = createTask(def, added, modified, deleted);
				t.start(grp);
			} else {
				((WatchTask<CustomResource>) t).addConsumers(added, modified, deleted);
			}
			return t;
		});			
	}
	
	public <T> void prepareWatchTask(Class<T> clazz, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
		tasks.compute(clazz, (k, t) -> {
			if(t == null) {
				t = createTask(clazz, added, modified, deleted);
				t.start(grp);
			} else {
				((WatchTask<T>) t).addConsumers(added, modified, deleted);
			}
			return t;
		});
	}
	
	private abstract class WatchTask<T> implements Runnable {
		protected Logger LOG = Logger.getLogger(WatchTask.class);
		protected Thread t;
		private List<Consumer<T>> added = new ArrayList<>();
		private List<Consumer<T>> modified = new ArrayList<>();
		private List<Consumer<T>> deleted = new ArrayList<>();
		
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
	
	private class Event<T> {
		public final String type;
		public final T obj;
		public Event(String type, T obj) {
			this.type = type;
			this.obj = obj;
		}
	}
	
	public class WatchTaskCustom extends WatchTask<CustomResource> {
		private CustomResourceDefinition def;
		
		public WatchTaskCustom(CustomResourceDefinition def, Consumer<CustomResource> added, Consumer<CustomResource> modified, Consumer<CustomResource> deleted) {
			this.def = def;
			this.addConsumers(added, modified, deleted);
		}
		
		@Override
		public void run() {
			LOG.debug("Watch Task starting " + def);
			while(!Thread.currentThread().isInterrupted()) {
				Iterable<Event<CustomResource>> result = new Iterable<Event<CustomResource>>() {
					public Iterator<Event<CustomResource>> iterator() {
						return new IteratorCustom(def);
					}
				};
				for(Event<CustomResource> r : result) {
					String type = r.type.toUpperCase();

					LOG.debug("Event of type " + type);
					LOG.trace("Content " + r.obj);
						
					switch(type) {
						case "ADDED": added().accept(r.obj); break;
						case "MODIFIED": modified().accept(r.obj); break;
						case "DELETED": deleted().accept(r.obj); break;
					}
				}
			}
			LOG.debug("Watch Task exiting for " + def);
		}
	}
	
	public class WatchTaskKubernetes<T> extends WatchTask<T> {
		private Class<T> clazz;
		
		public WatchTaskKubernetes(Class<T> clazz, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
			this.clazz = clazz;
			this.addConsumers(added, modified, deleted);
		}
		
		@Override
		public void run() {
			LOG.debug("Watch Task starting " + clazz.getName());
			while(!Thread.currentThread().isInterrupted()) {
					Iterable<Event<T>> result = new Iterable<Event<T>>() {
						public Iterator<Event<T>> iterator() {
							return new IteratorKubernetes<>(clazz);
						}
					};
					for(Event<T> r : result) {
						String type = r.type.toUpperCase();
						T obj = (T) r.obj;
						
						LOG.debug("Event of type " + type);
						LOG.trace("Content " + obj);
						
						switch(type) {
							case "ADDED": added().accept(obj); break;
							case "MODIFIED": modified().accept(obj); break;
							case "DELETED": deleted().accept(obj); break;
						}
					}
				LOG.debug("Watch Task exiting for " + clazz.getName());
			}
		}
	}
	
	private class IteratorCustom implements Iterator<Event<CustomResource>> {
		private BlockingQueue<Event<CustomResource>> queue = new LinkedBlockingQueue<>();
		private Consumer<Event<Path>> consumer;

		public IteratorCustom(CustomResourceDefinition crd) {
			this.consumer = e -> {
				Path child = e.obj;
				try {
					CustomResource loaded = SerializationUtils.fromYAML(new String(Files.readAllBytes(child)), crd);
					queue.put(new Event<>(e.type, loaded));
				} catch(Exception ex) {
					logger.debug("Event for file %s not compatible for watch of type %s, ignoring...", child, crd.names().kind());
					logger.debug("Error is ", ex);
				}
			};
			consumers.add(consumer);
		}
		
		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public Event<CustomResource> next() {
			try {
				return queue.take();
			} catch (InterruptedException e) {
				logger.warn("Error while taking element", e);
				return null;
			}
		}
	}
	
	private class IteratorKubernetes<T> implements Iterator<Event<T>> {
		private Map<Path, T> resources = new HashMap<>();
		private BlockingQueue<Event<T>> queue = new LinkedBlockingQueue<>();
		private Consumer<Event<Path>> consumer;

		public IteratorKubernetes(Class<T> clazz) {
			this.consumer = e -> {
				Path child = e.obj;
				try {
					if(e.type.equals("DELETED")) {
						T loaded = resources.remove(child);
						queue.put(new Event<T>(e.type, loaded));
					} else {
						T loaded = SerializationUtils.fromYAML(new String(Files.readAllBytes(child)), clazz);
						resources.put(child, loaded);
						queue.put(new Event<T>(e.type, loaded));
					}
				} catch(Exception ex) {
					logger.debug("Event for file %s not compatible for watch of type %s, ignoring...", child, clazz.getName());
					logger.debug("Error is ", ex);
				}
			};
			consumers.add(consumer);
		}
		
		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public Event<T> next() {
			try {
				return queue.take();
			} catch (InterruptedException e) {
				logger.warn("Error while taking element", e);
				return null;
			}
		}
	}

}
