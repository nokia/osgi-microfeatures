package com.nokia.as.k8s.controller.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.CustomResourceDefinition;
import com.nokia.as.k8s.controller.ResourceService;
import com.nokia.as.k8s.controller.WatchHandle;
import com.nokia.as.k8s.controller.mock.Watcher.WatchHandleImpl;
import com.nokia.as.k8s.controller.serialization.SerializationUtils;

import io.kubernetes.client.openapi.ApiException;

@Component
public class ResourceServiceImpl implements ResourceService {

	@ServiceDependency
	private LogServiceFactory logFactory;
	
	private Configuration configuration;
	
	@ConfigurationDependency
	public void loadConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	@ServiceDependency
	PlatformExecutors pfes;

	@Inject
	private BundleContext bc;

	private LogService logger;
	private Watcher watch;

	@Start
	public void start() throws Exception {
		logger = logFactory.getLogger(getClass());
		watch = new Watcher(configuration.getPath(), logger);
	}

	public <T> WatchHandle<T> watch(Class<T> clazz, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
		return watch("", clazz, added, modified, deleted);
	}
	
	public <T> WatchHandle<T> watch(String namespace, Class<T> clazz, Consumer<T> added, Consumer<T> modified, Consumer<T> deleted) {
		WatchHandleImpl<T> handle = watch.createHandle(clazz, added, modified, deleted);
				
		recoverWatch(namespace, clazz, added).whenComplete((nothing, ex) -> {
			if(ex != null) {
				logger.warn("Exception raised when trying to recover existing resources for watch - " + ex);
				ex.printStackTrace();
			} else {
				watch.prepareWatchTask(clazz, added, modified, deleted);
				handle.valid(true);
			}
		}).exceptionally((e -> {
				logger.warn("An exception occured %s ", e.toString());
				return null;
		}));
		
		return handle;
	}
	
	public WatchHandle<CustomResource> watch(CustomResourceDefinition def, Consumer<CustomResource> added, Consumer<CustomResource> modified, Consumer<CustomResource> deleted) {
		return watch("", def, added, modified, deleted);
	}
	
	public WatchHandle<CustomResource> watch(String namespace, CustomResourceDefinition def, Consumer<CustomResource> added, Consumer<CustomResource> modified, Consumer<CustomResource> deleted) {
		WatchHandleImpl<CustomResource> handle = watch.createHandle(def, added, modified, deleted);
		
		recoverWatch(namespace, def, added).whenComplete((nothing, ex) -> {
			if(ex != null) {
				logger.warn("Exception raised when trying to recover existing resources for watch - " + ex);
				ex.printStackTrace();
			} else {
				watch.prepareWatchTask(def, added, modified, deleted);
				handle.valid(true);
			}
		}).exceptionally((e -> {
				logger.warn("An exception occured %s ", e.toString());
				return null;
		}));
		
		return handle;
	}
	
	private CompletableFuture<Void> recoverWatch(String namespace, CustomResourceDefinition crd, Consumer<CustomResource> added) {
		return getAll(namespace, crd).thenCompose(res -> {
			for(CustomResource r : res) {
				added.accept(r);
			}
			return CompletableFuture.completedFuture(null);
		});
	}
	
	private <T> CompletableFuture<Void> recoverWatch(String namespace, Class<T> clazz, Consumer<T> added) {
		return getAll(namespace, clazz).thenCompose(res -> {
			for(T r : res) {
				added.accept(r);
			}
			return CompletableFuture.completedFuture(null);
		});
	}

	@Override
	public <T> CompletableFuture<Boolean> create(Class<T> clazz, T obj) {
		throw new UnsupportedOperationException("Create not implemented");
	}
	
	@Override
	public <T> CompletableFuture<Boolean> create(String namespace, Class<T> clazz, T obj) {
		throw new UnsupportedOperationException("Create not implemented");
	}
	
	@Override
	public CompletableFuture<Boolean> create(CustomResourceDefinition crd, CustomResource cr) {
		throw new UnsupportedOperationException("Create not implemented");
	}
	
	@Override
	public CompletableFuture<Boolean> create(String namespace, CustomResourceDefinition crd, CustomResource cr) {
		throw new UnsupportedOperationException("Create not implemented");
	}
	
	@Override
	public <T> CompletableFuture<Boolean> delete(Class<T> clazz, String resourceName) {
		throw new UnsupportedOperationException("Delete not implemented");
	}

	@Override
	public <T> CompletableFuture<Boolean> delete(String namespace, Class<T> clazz, String resourceName) {
		throw new UnsupportedOperationException("Delete not implemented");
	}
	
	@Override
	public CompletableFuture<Boolean> delete(CustomResourceDefinition def, String resourceName) {
		throw new UnsupportedOperationException("Delete not implemented");
	}
	
	@Override
	public CompletableFuture<Boolean> delete(String namespace, CustomResourceDefinition def, String resourceName) {
		throw new UnsupportedOperationException("Delete not implemented");
	}

	@Override
	public <T> CompletableFuture<Optional<T>> get(Class<T> clazz, String resourceName) {
		throw new UnsupportedOperationException("Get not implemented");
	}

	@Override
	public <T> CompletableFuture<Optional<T>> get(String namespace, Class<T> clazz, String resourceName) {
		throw new UnsupportedOperationException("Get not implemented");
	}
	
	@Override
	public CompletableFuture<Optional<CustomResource>> get(CustomResourceDefinition def, String resourceName) {
		throw new UnsupportedOperationException("Get not implemented");
	}
	
	@Override
	public CompletableFuture<Optional<CustomResource>> get(String namespace, CustomResourceDefinition def, String resourceName) {
		throw new UnsupportedOperationException("Get not implemented");
	}

	@Override
	public <T> CompletableFuture<List<T>> getAll(Class<T> clazz) {
		return getAll("", clazz);
	}

	@Override
	public <T> CompletableFuture<List<T>> getAll(String namespace, Class<T> clazz) {
		CompletableFuture<List<T>> future = new CompletableFuture<>();
		List<T> objs = new ArrayList<>();
		try {
			objs = 
			Files.walk(Paths.get(configuration.getPath()))
			    .filter(Files::isRegularFile)
				.map(c -> {
					try {
						return SerializationUtils.fromYAML(new String(Files.readAllBytes(c)), clazz);
					} catch(Exception e) {
						logger.debug("Event for file %s not compatible for watch of type %s, ignoring...", c, clazz.getName());
						logger.debug("Error is", e);
						return null;
					}
				})
				.filter(o -> o != null)
				.collect(Collectors.toList());
			future.complete(objs);
		} catch(IOException e) {
			future.completeExceptionally(e);
		}
		return future;
	}
	
	@Override
	public CompletableFuture<List<CustomResource>> getAll(CustomResourceDefinition def) {
		return getAll("", def);
	}

	@Override
	public CompletableFuture<List<CustomResource>> getAll(String namespace, CustomResourceDefinition def) {
		CompletableFuture<List<CustomResource>> future = new CompletableFuture<>();
		List<CustomResource> objs = new ArrayList<>();
		try {
			objs = 
			Files.walk(Paths.get(configuration.getPath()))
				.map(c -> {
					try {
						return SerializationUtils.fromYAML(new String(Files.readAllBytes(c)), def);
					} catch(Exception e) {
						logger.debug("Event for file %s not compatible for watch of type %s, ignoring...", c, def.names().kind());
						logger.debug("Error is", e);
						return null;
					}
				})
				.filter(o -> o != null)
				.collect(Collectors.toList());
			future.complete(objs);
		} catch(IOException e) {
			future.completeExceptionally(e);
		}
		return future;
	}

    public CompletableFuture<List<String>> listNamespaces() {
    	throw new UnsupportedOperationException("List namespaces not implemented");
	}

	public CompletableFuture<String> currentNamespace() {
		throw new UnsupportedOperationException("Get current namespace not implemented");
	}
	
	@Override
	public <T> CharSequence toJSON(T r) throws Exception {
		return SerializationUtils.toJSON(r);
	}

	@Override
	public <T> CharSequence toYAML(T r) throws Exception {
		return SerializationUtils.toYAML(r);
	}

	@Override
	public CustomResource fromJSON(CharSequence cs, CustomResourceDefinition crd) throws Exception {
		return SerializationUtils.fromJSON(cs.toString(), crd);
	}

	@Override
	public CustomResource fromYAML(CharSequence cs, CustomResourceDefinition crd) throws Exception {
		return SerializationUtils.fromYAML(cs.toString(), crd);
	}
	
	@Override
	public <T> T fromJSON(CharSequence cs, Class<T> clazz) throws Exception {
		return SerializationUtils.fromJSON(cs.toString(), clazz);
	}

	@Override
	public <T> T fromYAML(CharSequence cs, Class<T> clazz) throws Exception {
		return SerializationUtils.fromYAML(cs.toString(), clazz);
	}
	
	@FunctionalInterface
	interface ExceptionalBiFunction {
		public Iterable<?> apply(String namespace, Object payload) throws ApiException;
	}

	@Override
	public <T> CompletableFuture<Boolean> update(Class<T> clazz, String resourceName, T obj) {
		throw new UnsupportedOperationException("Update not implemented");
	}

	@Override
	public <T> CompletableFuture<Boolean> update(String namespace, Class<T> clazz, String resourceName, T obj) {
		throw new UnsupportedOperationException("Update not implemented");
	}

	@Override
	public CompletableFuture<Boolean> update(CustomResourceDefinition crd, String resourceName, CustomResource res) {
		throw new UnsupportedOperationException("Update not implemented");
	}

	@Override
	public CompletableFuture<Boolean> update(String namespace, CustomResourceDefinition crd, String resourceName, CustomResource res) {
		throw new UnsupportedOperationException("Update not implemented");
	}

	@Override
	public <T, U> CompletableFuture<Boolean> addDependent(Class<T> pClazz, T parent, Class<U> cClazz, U child) {
		throw new UnsupportedOperationException("addDependent not implemented");
	}

	@Override
	public <T, U> CompletableFuture<Boolean> addDependent(String namespace, Class<T> pClazz, T parent, Class<U> cClazz,
			U child) {
		throw new UnsupportedOperationException("addDependent not implemented");
	}
}
