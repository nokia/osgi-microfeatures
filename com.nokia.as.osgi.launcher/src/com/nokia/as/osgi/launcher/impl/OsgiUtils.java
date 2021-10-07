package com.nokia.as.osgi.launcher.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class OsgiUtils {
	
	/**
	 * Resource file containing custom bundle start levels
	 */
	private final static String START_LEVEL = "META-INF/startlevel.txt";

	/**
	 * Specific Bundle-SymbolicName start levels that are optionally load from a
	 * config file
	 */
	private final static Map<String, Integer> _startLevels = new HashMap<>();

	/**
	 * Boolean telling if we have loaded our startlevel.txt file
	 */
	private static boolean _startLevelsloaded;

	/**
	 * Creates and starts an embedded Osgi Framework
	 * @param config the configuration to give to the FrameworkFactory
	 * @param exception what to do if exception
	 * @return an optional of the created framework
	 */
	public static Optional<Framework> createFramework(Map<String, String> config, Consumer<Throwable> exception) {
		Objects.requireNonNull(config);
		Objects.requireNonNull(exception);
		
		ServiceLoader<FrameworkFactory> factoryServiceLoader = ServiceLoader.load(FrameworkFactory.class, OsgiUtils.class.getClassLoader());
		FrameworkFactory factory = factoryServiceLoader.iterator().next();
		
		Framework framework = factory.newFramework(config);
		
		try {
			framework.start();
		} catch(BundleException e) {
			exception.accept(e);
			return Optional.empty();
		}
		return Optional.of(framework);		
	}
	
	/**
	 * Stop a running framework
	 * 0 timeout means no timeout
	 * @param framework the framework to stop
	 * @param timeout how much time to wait (in millis)
	 * @param exception what to do if exception
	 */
	public static void stopFramework(Framework framework, int timeout, Consumer<Throwable> exception) {
		Objects.requireNonNull(framework);
		Objects.requireNonNull(exception);
		if(timeout < 0) throw new IllegalArgumentException("timeout time cannot be negative");
		
		try {
			framework.stop();
			framework.waitForStop(timeout);
		} catch(Exception e) {
			exception.accept(e);
		}
	}
	
	/**
	 * Install bundles in the framework
	 * @param framework the framework
	 * @param exception what to do if exception
	 * @param bundles the urls of the bundles 
	 * @return the framework
	 */
	private static Framework installBundles(Framework framework, Consumer<Throwable> exception, List<String> bundles) {
		Objects.requireNonNull(framework);
		Objects.requireNonNull(exception);
		
		BundleContext bc = framework.getBundleContext();
		bundles.forEach(b -> {
				  try {
					  bc.installBundle(b);
				  } catch(Exception e) {
					  exception.accept(e);
				  }
		});
		
		return framework;
	}
	
	private static boolean shouldBeInstalled(URL resource, String userFilter, Consumer<Throwable> exception) {
		
		String everything = "(Bundle-SymbolicName=*)(!(Require-Capability=must.not.resolve))";
		
		String filter = "(&" + everything + userFilter + ")";
		
		if(getStartLevel(resource.toString(), 25, exception) == -1) return false;
		
		try(JarInputStream jarInput = new JarInputStream(resource.openStream())) {
			
			Filter ldapFilter = FrameworkUtil.createFilter(filter);
			Manifest manifest = jarInput.getManifest();
			
			Map<String, String> headers = new HashMap<>();
			manifest.getMainAttributes()
					.entrySet()
					.forEach(e -> headers.put(e.getKey().toString(), e.getValue().toString()));
			
			return ldapFilter.match(MapUtils.mapToDictionary(headers));
		} catch(Exception e) {
			exception.accept(e);
			return false;
		}
	}
	
	public synchronized static int getStartLevel(String resource, int defLevel, Consumer<Throwable> exception) {
		if (! _startLevelsloaded) {
			loadStartLevels(exception);
			_startLevelsloaded = true;
		}

		try (JarInputStream jarInput = new JarInputStream((new URL(resource)).openStream())) {
			Manifest manifest = jarInput.getManifest();
			Map<String, String> headers = new HashMap<>();
			manifest.getMainAttributes().entrySet()
					.forEach(e -> headers.put(e.getKey().toString(), e.getValue().toString()));

			// See if the bundle symbolic name is configured in our startlevel.txt
			String bsn = headers.get("Bundle-SymbolicName");
			if (_startLevels.containsKey(bsn)) {
				return _startLevels.getOrDefault(bsn, defLevel);
			} else {
				// See if the bundle contains the legacy Bundle-StartLevel header
				String level = headers.get("Bundle-StartLevel");
				return level == null ? defLevel : Integer.parseInt(level);
			}
		} catch (Exception e) {
			exception.accept(e);
			return defLevel;
		}
	}

	private static void loadStartLevels(Consumer<Throwable> exception) {
		InputStream in = OsgiUtils.class.getClassLoader().getResourceAsStream(START_LEVEL);
		if (in != null) {
			Properties props = new Properties();
			try (BufferedInputStream bin = new BufferedInputStream(in)) {
				props.load(bin);
				for (Map.Entry<Object, Object> entry : props.entrySet()) {
					String bsn = entry.getKey().toString().trim();
					Integer startLevel = Integer.valueOf(entry.getValue().toString().trim());
					_startLevels.put(bsn, startLevel);
				}
			} catch (IOException e) {
				exception.accept(e);
			}
		}
	}	
	
	private static boolean shouldBeStarted(Bundle bundle, Consumer<Throwable> exception) {
		String fragment = "(!(Fragment-Host=*))";
		try {
			Filter ldapFilter = FrameworkUtil.createFilter(fragment);
			return ldapFilter.match(bundle.getHeaders());
		} catch(Exception e) {
			exception.accept(e);
			return false;
		}
	}
	
	public static Framework startBundles(Framework framework, String filter, List<String> dirs, 
					     List<String[]> bundles, Consumer<Throwable> exception) {
		
		List<String> toStart = new ArrayList<>();
		
		for(String dir : dirs) toStart.addAll(dirBundles(framework, dir, filter, exception));
		
		Function<String, Stream<URL>> toURL = 
				f -> {
					try {
						return Stream.of(new URL(f));
					} catch(Exception e) {
						exception.accept(e);
						return Stream.empty();
					}
				};
		
		bundles.stream()
			   .flatMap(urls -> Stream.of(urls))
			   .flatMap(toURL)
			   .filter(u -> shouldBeInstalled(u, filter, exception))
			   .map(URL::toString)
			   .forEach(toStart::add);
					
		return startBundles(framework, exception, toStart);
	}
		
	/**
	 * Starts the bundles on the framework from a directory
	 * The filter acts as a blacklist: by default everything is started. For example filter=> Bundle-SymbolicName=bsn,Require-Capability=cap
	 * blacklists the bundles with "bsn" as symbolic name and those which require the "cap" capability
	 * @param framework the framework
	 * @param directory the directory to scan
	 * @param filter a comma-separated blacklist of headers
	 * @param exception what to do in case of exception
	 * @return the framework
	 */
	private static List<String> dirBundles(Framework framework, String directory, String filter, Consumer<Throwable> exception) {
		Objects.requireNonNull(framework);
		Objects.requireNonNull(directory);
		Objects.requireNonNull(exception);
		
		Function<Path, Stream<URL>> toURL = 
				f -> {
					try {
						return Stream.of(f.toUri().toURL());
					} catch(Exception e) {
						exception.accept(e);
						return Stream.empty();
					}
				};
				
		try {
			List<String> urls =
				Files.walk(Paths.get(directory))
					.filter(p -> p.toString().endsWith(".jar"))
				 	.flatMap(toURL)
				 	.filter(u -> shouldBeInstalled(u, filter, exception))
				 	.map(URL::toString)
				 	.collect(Collectors.toList());
			return urls;
		} catch(Exception e) {
			exception.accept(e);
			return Collections.emptyList();
		}
	}
	
	/**
	 * Starts the installed bundles in framework
	 * @param framework the framework
	 * @param exception what to do if exception
	 * @param urls the urls of the bundles to start
	 */
	private static Framework startBundles(Framework framework, Consumer<Throwable> exception, List<String> urls) {
		Objects.requireNonNull(framework);
		Objects.requireNonNull(exception);

		urls.sort((u1, u2) -> {
				int sl1 = getStartLevel(u1, 25, exception);
				int sl2 = getStartLevel(u2, 25, exception);
				if(sl1 == sl2) return 0;
				if(sl1 > sl2) return 1; //smaller start level has more priority
				return -1;
			});

		installBundles(framework, exception, urls);
		BundleContext bc = framework.getBundleContext();
		Bundle[] bundles = bc.getBundles();
		Stream.of(bundles)
			  .forEach(b -> {
				  try {
				  	if(shouldBeStarted(b, exception)) {
				  		b.start();
				  	}
				  } catch(Exception e) {
					  exception.accept(e);
				  }
			  });
		
		return framework;
	}
	
	/**
	 * Registers a service in the framework
	 * @param framework the framework
	 * @param service the class of the service to register
	 * @param implementation the implementation of the registered service
	 * @return a service registration
	 */
	public static <T> ServiceRegistration<T> registerService(Framework framework, Class<T> service, T implementation, Dictionary<String, ?> properties) {
		return framework.getBundleContext().registerService(service, implementation, properties);
	}
	
	/**
	 * Listens to a service from the framework
	 * @param framework the framework
	 * @param clazz the class of the service
	 * @param onAdded what to do when the service is added
	 * @param onModified what to do when the service is modified
	 * @param onRemoved what to do when the service is removed
	 */
	public static <T> ServiceTracker<?, ?> listenService(Framework framework, Class<T> clazz,
									  	 				 Consumer<T> onAdded, Consumer<T> onModified, Consumer<T> onRemoved) {
		Objects.requireNonNull(framework);
		Objects.requireNonNull(clazz);
		Objects.requireNonNull(onAdded);
		Objects.requireNonNull(onModified);
		Objects.requireNonNull(onRemoved);

		ServiceTrackerCustomizer<T, Object> custom = new ServiceTrackerCustomizer<T, Object>() {
			@Override
			public Object addingService(ServiceReference<T> reference) {
				T service = (T) framework.getBundleContext().getService(reference);
				onAdded.accept(service);
				return service;
			}

			@Override
			public void modifiedService(ServiceReference<T> reference, Object service) { 
				onModified.accept(clazz.cast(service));
			}

			@Override
			public void removedService(ServiceReference<T> reference, Object service) { 
				onRemoved.accept(clazz.cast(service));
				framework.getBundleContext().ungetService(reference);
			}
		};
		
		ServiceTracker<?, ?> tracker = new ServiceTracker<>(framework.getBundleContext(), clazz, custom);
		tracker.open();
		return tracker;
	}
	
	/**
	 * Gets a service instance from inside the framework (async method)
	 * @param framework the framework
	 * @param clazz the class of the service
	 * @return a completable future of the service class
	 */
	public static <T> CompletableFuture<T> getService(Framework framework, Class<T> clazz, Consumer<Throwable> exception) {
		return getService(framework, clazz.getName(), exception);
	}
	
	public static <T> CompletableFuture<T> getService(Framework framework, String clazz, Consumer<Throwable> exception) {		
		Objects.requireNonNull(framework);
		Objects.requireNonNull(clazz);

		CompletableFuture<T> futureService = new CompletableFuture<>();
		
		ServiceTrackerCustomizer<T, Object> custom = new ServiceTrackerCustomizer<T, Object>() {
			@Override
			public Object addingService(ServiceReference<T> reference) {
				T service = (T) framework.getBundleContext().getService(reference);
				futureService.complete(service);
				return service;
			}

			@Override
			public void modifiedService(ServiceReference<T> reference, Object service) { }

			@Override
			public void removedService(ServiceReference<T> reference, Object service) { 
				framework.getBundleContext().ungetService(reference);
			}
		};
		
		ServiceTracker<?, ?> tracker = new ServiceTracker<>(framework.getBundleContext(), clazz, custom);
		tracker.open();
		return futureService;
	}

	/**
	 * Gets a service instance from inside the framework (async method)
	 * @param framework the framework
	 * @param clazz the class of the service
	 * @param filter an ldap filter
	 * @return a completable future of the service class
	 */
	public static <T> CompletableFuture<T> getService(Framework framework, Class<T> clazz, String filter, Consumer<Throwable> exception) {
		return getService(framework, clazz.getName(), filter, exception);
	}
	
	public static <T> CompletableFuture<T> getService(Framework framework, String clazz, String filter, Consumer<Throwable> exception) {
		Objects.requireNonNull(framework);
		Objects.requireNonNull(clazz);

		CompletableFuture<T> futureService = new CompletableFuture<>();
		
		ServiceTrackerCustomizer<T, Object> custom = new ServiceTrackerCustomizer<T, Object>() {
			@Override
			public Object addingService(ServiceReference<T> reference) {
				T service = (T) framework.getBundleContext().getService(reference);
				futureService.complete(service);
				return service;
			}

			@Override
			public void modifiedService(ServiceReference<T> reference, Object service) { }

			@Override
			public void removedService(ServiceReference<T> reference, Object service) { 
				framework.getBundleContext().ungetService(reference);
			}
		};
		
		String classFilter = "(" + Constants.OBJECTCLASS + "=" + clazz + ")";
		String ldapFilter = "(&" + classFilter + filter + ")";
		
		ServiceTracker<?, ?> tracker;
		try {
			tracker = new ServiceTracker<>(framework.getBundleContext(), FrameworkUtil.createFilter(ldapFilter), custom);
		} catch(InvalidSyntaxException e) {
			exception.accept(e);
			tracker = new ServiceTracker<>(framework.getBundleContext(), clazz, custom);
		}
		tracker.open();
		return futureService;
	}

}
