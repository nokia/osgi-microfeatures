package com.nokia.as.jaxrs.jersey.common.impl;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency.Any;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.validation.ValidationFeature;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.concurrent.SerialExecutor;
import com.nokia.as.jaxrs.jersey.common.ApplicationConfiguration;
import com.nokia.as.jaxrs.jersey.common.JaxRsResourceRegistry;
import com.nokia.as.jaxrs.jersey.common.ServerContext;

@Component
public class JaxRsResourceRegistration implements JaxRsResourceRegistry {
	private static final String HTTP_PORT = "http.port";
	public static final Logger log = Logger.getLogger("as.ioh.jaxrs");
	public static final String HTTP = "http://";
	public static final String HTTPS = "https://";
	public static final String PROP_JAXRS_SERVER_ALIAS = "jaxrs.server.alias";
	public static final String PROP_JAXRS_SERVER_IP = "jaxrs.server.ip";
	public static final String PROP_JAXRS_SERVER_PORT = "jaxrs.server.port";
	public static final String PROP_JAXRS_SERVER_SCHEME = "jaxrs.server.scheme";
	public static final String PROP_JAXRS_SERVER_AUTH_HEADER = "jaxrs.server.auth.header";
	public static final String PROP_JAXRS_SERVER_SCHEME_HEADER = "jaxrs.server.scheme.header";
	public static final String PROP_JAXRS_SERVER_AUTH_PSEUDO_HEADER = "jaxrs.server.auth.pseudo.header";
	public static final String PROP_JAXRS_SERVER_SCHEME_PSEUDO_HEADER = "jaxrs.server.scheme.pseudo.header";
	public static final String PROP_JAXRS_SERVER_OVERLOAD_LOW_WM = "jaxrs.server.overload.lowWM";
	public static final String PROP_JAXRS_SERVER_OVERLOAD_HIGH_WM = "jaxrs.server.overload.highWM";
	public static final String PROP_JAXRS_SERVER_OVERLOAD_INJECT = "jaxrs.server.overload.inject";
	public static final String PROP_JAXRS_SERVER_OVERLOAD_INJECT_PATH = "jaxrs.server.overload.inject.path";
    
	public static final String APPLICATION_HANDLER = "application.handler";
	public static final String COMMON_LOG_FORMAT = "common.log.format";
	public static final byte[] HTTP10_404;
	public static final byte[] HTTP10_100;
	public static final byte[] HTTP10_503;
	public static final byte[] HTTP11_404;
	public static final byte[] HTTP11_100;
	public static final byte[] HTTP11_503;
	public static java.nio.charset.Charset UTF_8 = null;
	static {
		try {
			UTF_8 = java.nio.charset.Charset.forName("utf-8");
		} catch (Exception e) {
		}
		HTTP10_404 = "HTTP/1.0 404 Not Found\r\nContent-Length:0\r\n\r\n".getBytes(UTF_8);
		HTTP10_100 = "HTTP/1.0 100 Continue\r\n\r\n".getBytes(UTF_8);
		HTTP10_503 = "HTTP/1.0 503 Service Unavailable\r\nContent-Length:0\r\n\r\n".getBytes(UTF_8);
		HTTP11_404 = "HTTP/1.1 404 Not Found\r\nContent-Length:0\r\n\r\n".getBytes(UTF_8);
		HTTP11_100 = "HTTP/1.1 100 Continue\r\n\r\n".getBytes(UTF_8);
		HTTP11_503 = "HTTP/1.1 503 Service Unavailable\r\nContent-Length:0\r\n\r\n".getBytes(UTF_8);
	}

	@Inject
	BundleContext _bc;
	protected volatile List<ApplicationConfiguration> _appConfigurations = new ArrayList<>();
	/**
	 * Map used to register jaxrs resources. We use an ident hashmap in order to avoid calling equals/hashcode of 
	 * jaxrs resources.
	 */
	protected volatile Map<Object, Map<String, String>> _jaxrsResources = Collections.synchronizedMap(new IdentityHashMap<>());
	
	private volatile List<ServerContext> _serverContexts = new ArrayList<>();

	/**
	 * Executor used to protect concurrent public method calls. We don't synchronized methods 
	 * because some bind methods may register services.  
	 */
	private final SerialExecutor _serial = new SerialExecutor();
	
	@Start
	public void start() {
		_appConfigurations.add(new DefaultApplicationConfiguration());
	}

	/**
	 * Bind JAX-RS resources when servers may not be created
	 * 
	 * @param resource
	 * @param properties
	 */
	@Override
	@ServiceDependency(service = Any.class, required = false, removed = "unbindJaxRsResource")
	public void bindJaxRsResource(Object resource, Map<String, String> properties) {
		_serial.execute(() -> bindJaxRsResourceSafe(resource, properties));
	}
	
	private void bindJaxRsResourceSafe(Object resource, Map<String, String> properties) {
		if (hasRegisterableAnnotation(resource) || resource instanceof javax.ws.rs.core.Feature
				|| resource instanceof Resource) {
			if(log.isInfoEnabled())
				log.info("bindJaxRsResource." + resource);

			_jaxrsResources.put(resource, properties);
			if (_serverContexts.isEmpty()) {
				if(log.isInfoEnabled())
					log.info("Resource binding: Servers not ready for configuration loading...");
				return;
			}

			applyConfigurations(resource, properties, false);
		}
	}

	/**
	 * Bind {@link ApplicationConfiguration} when servers may not be created
	 * 
	 * @param appConfiguration
	 */
	@ServiceDependency(required = false)
	public void bindAppConfiguration(ApplicationConfiguration appConfiguration) {
		_serial.execute(() -> bindAppConfigurationSafe(appConfiguration));
	}
	
	private void bindAppConfigurationSafe(ApplicationConfiguration appConfiguration) {
		_appConfigurations.add(appConfiguration);

		if (_serverContexts.isEmpty()) {
			if(log.isInfoEnabled())
				log.info("AppConfiguration binding: Servers not ready for configuration loading...");
			return;
		}

		if(log.isInfoEnabled())
			log.info("Available resources are " + _jaxrsResources);

		for (ServerContext server : _serverContexts) {
			applyConfigurations(server);
		}
	}

	@Override
	public void unbindJaxRsResource(Object resource, Map<String, String> properties) {
		_serial.execute(() -> unbindJaxRsResourceSafe(resource, properties));
	}
	
	private void unbindJaxRsResourceSafe(Object resource, Map<String, String> properties) {
		if (_jaxrsResources.remove(resource) != null) {
			if (log.isDebugEnabled())
				log.debug(this + " unbindJaxRsResource() : removed " + resource);
			applyConfigurations(resource, properties, true);
		}
	}

	private boolean hasRegisterableAnnotation(Object service) {
		boolean result = isRegisterableAnnotationPresent(service.getClass());
		if (!result) {
			Class<?>[] interfaces = service.getClass().getInterfaces();
			for (Class<?> type : interfaces) {
				result = result || isRegisterableAnnotationPresent(type);
			}
		}
		return result;
	}

	private boolean isRegisterableAnnotationPresent(Class<?> type) {
		return type.isAnnotationPresent(Path.class) || type.isAnnotationPresent(Provider.class);
	}

	public Set<Object> findResourcesByServer(ServerContext serverCtx) {
		return _jaxrsResources.entrySet().stream()
				.filter(entry -> entry.getValue() == null || entry.getValue().get(HTTP_PORT) == null
						|| entry.getValue().get(HTTP_PORT).equals(String.valueOf(serverCtx.getServerPort())))
				.map(entry -> entry.getKey())
				.collect(Collectors.toSet());
	}

	private void applyConfigurations(Object resource, Map<String, String> properties, boolean remove) {
		String resourcePort = properties.get(HTTP_PORT);

		for (ServerContext serverCtx : _serverContexts) {
			String serverPort = String.valueOf(serverCtx.getServerPort());
			if (resourcePort == null || resourcePort.isEmpty() || serverPort.equals(resourcePort)) {
				if(log.isInfoEnabled())
					log.info("Resource@Component detected: " + resource + " (port=" + resourcePort + "). Trying to "
							+ (remove ? "remove from" : "host on") + " " + serverPort);

				Set<Object> existingResources = findResourcesByServer(serverCtx);
				if (!remove)
					existingResources.add(resource);
				doApplyConfiguration(serverCtx, registerExisting(existingResources));
			}
		}
	}

	private void applyConfigurations(ServerContext serverCtx) {
		ResourceConfig rootApplication = registerExisting(findResourcesByServer(serverCtx));
		doApplyConfiguration(serverCtx, rootApplication);

		if(log.isInfoEnabled())
			log.info("Available resources=" + _jaxrsResources + "; (Server)Loaded on " + serverCtx + " = "
					+ rootApplication.getClasses() + " properties= " + rootApplication.getProperties()
					+ " dynamicResources=" + rootApplication.getResources());
	}

	private void doApplyConfiguration(ServerContext serverCtx, ResourceConfig rootApplication) {
		for (ApplicationConfiguration appConfiguration : _appConfigurations) {
			rootApplication.addProperties(appConfiguration.getProperties());
		}

		registerDefaultFeatures(rootApplication);

		Map<String, Object> properties = serverCtx.getProperties();
		if (properties != null)
			rootApplication.addProperties(properties.entrySet().stream().filter(n -> n.getKey().startsWith("jersey"))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

		if(log.isInfoEnabled())
			log.info("doApplyConfiguration: classes=" + rootApplication.getClasses() + "; singletons="
					+ rootApplication.getSingletons() + " dynamicResources=" +"\u001B[34m"+rootApplication.getResources()+"\u001B[0m"
					+ " with props " + rootApplication.getProperties() );

		serverCtx.setApplicationHandler(new ApplicationHandler(rootApplication));
		registerResourceConfigAsService(serverCtx, rootApplication);
	}

	private void registerDefaultFeatures(ResourceConfig rootApplication) {
		rootApplication.register(RolesAllowedDynamicFeature.class);
		rootApplication.register(JacksonFeature.class);
		rootApplication.register(ValidationFeature.class);
		rootApplication.register(MultiPartFeature.class);
	}

	private ResourceConfig registerExisting(Set<Object> existingResources) {
		ResourceConfig rootApplication = new ResourceConfig();
		for (Object r : existingResources) {
			if (r instanceof Resource)
				rootApplication.registerResources((Resource) r);
			else
				rootApplication.register(r);
		}
		return rootApplication;
	}

	private void registerResourceConfigAsService(ServerContext serverCtx, ResourceConfig rootApplication) {
		ServiceRegistration<Application> registration = getResouceConfigRegistration(serverCtx);
		if (registration == null) {
			ResourceConfigImpl rci = new ResourceConfigImpl(rootApplication);
			registration = _bc.registerService(Application.class, rci, new Hashtable<>(serverCtx.getProperties()));
			serverCtx.setRegistration(registration);
		} else {
			ResourceConfigImpl rci = (ResourceConfigImpl) _bc.getService(registration.getReference());
			rci.setResourceConfig(rootApplication);
			registration.setProperties(new Hashtable<>(serverCtx.getProperties()));
		}
	}

	private ServiceRegistration<Application> getResouceConfigRegistration(ServerContext serverCtx) {
		return (ServiceRegistration<Application>) serverCtx.getRegistration();
	}
	
	@Override
	public void add(ServerContext serverContext) {
		_serial.execute(() -> {
			_serverContexts.add(serverContext);
			applyConfigurations(serverContext);
		});
	}

	/********************************
	 * Debugging with {@link Shell} *
	 ********************************/

	private ResourceConfig getResouceConfiguration(ServerContext server) {
		return server.getApplicationHandler().getConfiguration();
	}

	@Override
	public Map<InetSocketAddress, Set<Class<?>>> getLoadedClasses() {
		return _serverContexts.stream().collect(Collectors.toMap(ServerContext::getAddress,
				(ServerContext server) -> getResouceConfiguration(server).getClasses()));
	}

	public Map<InetSocketAddress, Set<Class<?>>> getLoadedClasses(Integer port) {
		return _serverContexts.stream().filter(portFilter(port)).collect(Collectors.toMap(ServerContext::getAddress,
				(ServerContext server) -> getResouceConfiguration(server).getClasses()));
	}

	public Map<InetSocketAddress, Set<Object>> getLoadedSingletons() {
		return _serverContexts.stream().collect(Collectors.toMap(ServerContext::getAddress,
				(ServerContext server) -> getResouceConfiguration(server).getSingletons()));
	}

	public Map<InetSocketAddress, Set<Object>> getLoadedSingletons(Integer port) {
		return _serverContexts.stream().filter(portFilter(port)).collect(Collectors.toMap(ServerContext::getAddress,
				(ServerContext server) -> getResouceConfiguration(server).getSingletons()));
	}

	public Map<InetSocketAddress, Set<Resource>> getLoadedResources() {
		return _serverContexts.stream().collect(Collectors.toMap(ServerContext::getAddress,
				(ServerContext server) -> getResouceConfiguration(server).getResources()));
	}

	public Map<InetSocketAddress, Set<Resource>> getLoadedResources(Integer port) {
		return _serverContexts.stream().filter(portFilter(port)).collect(Collectors.toMap(ServerContext::getAddress,
				(ServerContext server) -> getResouceConfiguration(server).getResources()));
	}

	public Map<InetSocketAddress, Map<String, Object>> getLoadedProperties() {
		return _serverContexts.stream().collect(Collectors.toMap(ServerContext::getAddress,
				(ServerContext server) -> getResouceConfiguration(server).getProperties()));
	}

	public Map<InetSocketAddress, Map<String, Object>> getLoadedProperties(Integer port) {
		return _serverContexts.stream().filter(portFilter(port)).collect(Collectors.toMap(ServerContext::getAddress,
				(ServerContext server) -> getResouceConfiguration(server).getProperties()));
	}

	private Predicate<? super ServerContext> portFilter(Integer port) {
		return server -> ((Integer) server.getServerPort()).equals(port);
	}
}
