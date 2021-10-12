// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.osgi.launcher.impl;

import static com.nokia.as.osgi.launcher.impl.MapUtils.entry;
import static com.nokia.as.osgi.launcher.impl.MapUtils.toMapCollector;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT;
import static org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationPlugin;
import org.osgi.util.tracker.ServiceTracker;

import com.nokia.as.osgi.launcher.OsgiLauncher;

public class OsgiLauncherImpl implements OsgiLauncher, ConfigurationPlugin {

	private Framework framework;

	private Consumer<Throwable> exception = Throwable::printStackTrace;
	private String filter = "";

	private List<String> directory = new ArrayList<String>();
	private List<String[]> bundles = new ArrayList<String[]>();
	
	private boolean started = false;
	
	private Map<String, Map<String, Object>> pidConfiguration = new HashMap<>();

	private Map<String, String> userConfiguration = new HashMap<>();
	private Map<String, String> propertyConfiguration = new HashMap<>();
	private Map<String, String> defaultConfiguration = 
			Stream.of(
					entry(FRAMEWORK_STORAGE_CLEAN, FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT),
					entry("gosh.args", "--noshutdown")
					//entry("gosh.args", "--noshutdown --command telnetd --ip=127.0.0.1 --port=9876 start")
				  )
				  .collect(toMapCollector());

	private Map<String, String> defaultSystemProps = 
			Stream.of(
					entry("log4j.configuration", "file:" + System.getProperty("as.config.file.confdir", "conf") + "/log4j.properties"), 
					entry("log4j.configurationFile", "file:" + System.getProperty("as.config.file.confdir", "conf") + "/log4j2.xml"), 
					entry("com.nokia.as.stdout2log4j", "false"),
					entry("system.failstop", "false"),
					entry("INSTALL_DIR", "."),
					entry("group.name", "group"),
					entry("component.name", "component"),
					entry("instance.name", "instance"),
					entry("host.name", "localhost"),
					entry("platform.name", "csf")
				  )
				  .collect(toMapCollector());
	    
	private String systemPackages = calculateSystemPackages();

	private String calculateSystemPackages() {
		StringBuffer buffer = new StringBuffer();
		String configAdmin = "org.osgi.service.cm; version=1.6";
		try {
			// Lookup the snapshot bundle which contains X-CSF-EmbeddableAPIs header. 
			// This header contains list of embeddable apis (bundle symbolic names)
			Set<String> embeddableAPIBsns = getEmbeddableApiBsns();
			
			Enumeration<URL> urls = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
			while(urls.hasMoreElements()) {
				URL url = urls.nextElement();
				
				try(InputStream is = url.openStream()) {                        
					Manifest manifest = new Manifest(is);
                        
					Map<String, String> headers = new HashMap<>();
					manifest.getMainAttributes()
						.entrySet()
						.forEach(e -> headers.put(e.getKey().toString(), e.getValue().toString()));
                        
					String bsn = (String) headers.get("Bundle-SymbolicName");
                            
					if(embeddableAPIBsns.contains(bsn)) {
						String exportPackageString = headers.get("Export-Package");
						ManifestHeaderProcessor headerProcessor = new ManifestHeaderProcessor();
						List<NameValuePair> exportHeaders = headerProcessor.parseExportString(exportPackageString);
						for(NameValuePair header : exportHeaders) {
							String packge = header.getName();
							String version = header.getAttributes().get("version");
							buffer.append(packge).append("; version = ").append(version).append(",");
						}
					}
				} catch(Exception e) {
					exception.accept(e);
				}
			}
		} catch(Exception e) {
			exception.accept(e);
			return configAdmin;
		}
		return buffer.append(configAdmin).toString();
	}

	private Set<String> getEmbeddableApiBsns() {
		Set<String> embeddableApiBsns = new HashSet<>();
		try {
			Enumeration<URL> urls = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();

				try (InputStream is = url.openStream()) {
					Manifest manifest = new Manifest(is);

					Map<String, String> headers = new HashMap<>();
					manifest.getMainAttributes().entrySet()
							.forEach(e -> headers.put(e.getKey().toString(), e.getValue().toString()));

					String bsn = (String) headers.get("Bundle-SymbolicName");
					
					if ("true".equals(headers.get("CASR-API"))) {
						embeddableApiBsns.add(bsn);
					} else {
						// the snapshot bundle now provides a header which provides all embeddable api bsns
						String embeddableBsns = headers.get("X-CASR-EMBEDDABLE-API");
						if (embeddableBsns != null) {
							Stream.of(embeddableBsns.split(",")).forEach(embeddableApiBsns::add);
						}
					}
				} catch (Exception e) {
					exception.accept(e);
				}
			}
		} catch (Exception e) {
			exception.accept(e);
		}
		return embeddableApiBsns;
	}

	public Framework getFramework() {
		return framework;
	}
	
	public OsgiLauncher withFrameworkConfig(Map<String, String> config) {
		Objects.requireNonNull(config);
		userConfiguration.putAll(config);
		return this;
	}
	
	public OsgiLauncher withFrameworkConfig(String propertyFile) {
		try {
			Properties props = new Properties();
			props.load(new FileInputStream(propertyFile));
			props.entrySet().forEach(e -> propertyConfiguration.put(e.getKey().toString(), 
																	e.getValue().toString()));
		} catch(IOException e) {
			exception.accept(e);
		}
		return this;
	}
	
	public OsgiLauncher useDirectory(String directory) {
		Objects.requireNonNull(directory);
		this.directory.add(directory);
		return this;
	}

	public OsgiLauncher withBundles(String... bundles) {
		Objects.requireNonNull(bundles);
		this.bundles.add(bundles);
		return this;
	}
	
	public OsgiLauncher filter(String filter) {
		this.filter = 
				this.filter.isEmpty() ?
				filter :
				this.filter + filter;
		return this;
	}

	public OsgiLauncher blacklist(String filter) {
		return filter("(!" + filter + ")");
	}
	
	public OsgiLauncher start() {
		loadDefaultSystemProps();
		String defaultJreExtraPkgs = loadDefaultJreExtraPkgs();
		
		Map<String, String> configuration = new HashMap<>();
		configuration.putAll(defaultSystemProps);
		configuration.putAll(defaultConfiguration);
		configuration.putAll(propertyConfiguration);
		configuration.putAll(userConfiguration);
		
		String extraPackages = configuration.get(FRAMEWORK_SYSTEMPACKAGES_EXTRA);
		extraPackages = extraPackages == null ? defaultJreExtraPkgs : extraPackages;
		String prev = extraPackages != null ? (extraPackages + ", ") : "";
		configuration.put(FRAMEWORK_SYSTEMPACKAGES_EXTRA, prev + systemPackages);
		System.out.println("XXXXX: " + systemPackages);
				
		framework = OsgiUtils.createFramework(configuration, exception)
							 .map(f -> registerConfigurationPlugin(f))
							 .map(f -> OsgiUtils.startBundles(f, filter, directory, bundles, exception))
							 .orElseThrow(RuntimeException::new);
		started = true;
		return this;
	}
	
	private void loadDefaultSystemProps() {
		defaultSystemProps.forEach((key,val) -> System.getProperties().setProperty(key, System.getProperty(key, val)));
		if ("8" != System.getProperty("java.specification.version")) {
			if (System.getProperty("felix.systempackages.calculate.uses") == null) {
				defaultSystemProps.put("felix.systempackages.calculate.uses", "true");
			}
		}
	}

	private String loadDefaultJreExtraPkgs() {
		if (System.getProperty("java.version").startsWith("1.8")) {
			URL resource = getClass().getClassLoader().getResource("META-INF/jre-export-1.8.properties");
			if (resource != null) {
				try (InputStream in = new BufferedInputStream(resource.openStream())) {
					Properties props = new Properties();
					props.load(in);
					return props.getProperty("jre-1.8");
				} catch (Exception e) {
					exception.accept(e);
				}
			}
		}	
		return null;
	}

	private Framework registerConfigurationPlugin(Framework f) {
		f.getBundleContext().registerService(ConfigurationPlugin.class.getName(), this, null);
		return f;
	}

	public OsgiLauncherImpl useExceptionHandler(Consumer<Throwable> handler) {
		Objects.requireNonNull(handler);
		exception = handler;
		return this;
	}

	public <T> ServiceRegistration<T> registerService(Class<T> service, T implementation) {
		if(!started) throw new IllegalStateException("framework is not started");
		return OsgiUtils.registerService(framework, service, implementation, null);
	}

	public <T> ServiceRegistration<T> registerService(Class<T> service, T implementation, Dictionary<String, ?> properties) {
		if(!started) throw new IllegalStateException("framework is not started");
		return OsgiUtils.registerService(framework, service, implementation, properties);
	}

	public <T> CompletableFuture<T> getService(Class<T> service) {
		if(!started) throw new IllegalStateException("framework is not started");
		return OsgiUtils.getService(framework, service, exception);
	}
	
	public <T> CompletableFuture<T> getService(String service) {
		if(!started) throw new IllegalStateException("framework is not started");
		return OsgiUtils.getService(framework, service, exception);
	}
	
	public <T> CompletableFuture<T> getService(Class<T> service, String filter) {
		if(!started) throw new IllegalStateException("framework is not started");
		return OsgiUtils.getService(framework, service, filter, exception);
	}

	public <T> CompletableFuture<T> getService(String service, String filter) {
		if(!started) throw new IllegalStateException("framework is not started");
		return OsgiUtils.getService(framework, service, filter, exception);
	}

	public <T> ServiceTracker<?, ?> listenService(Class<T> service, Consumer<T> onAdded, Consumer<T> onModified,
			Consumer<T> onRemoved) {
		if(!started) throw new IllegalStateException("framework is not started");
		return OsgiUtils.listenService(framework, service, onAdded, onModified, onRemoved);
	}

	public void stop(int timeout) {
		if(!started) throw new IllegalStateException("framework is not started");
		started = false;
		OsgiUtils.stopFramework(framework, timeout, exception);
	}

	public void configureService(String pid, Map<String, Object> configuration) {
		Objects.requireNonNull(pid);
		Objects.requireNonNull(configuration);
		
		if(started) {
			if(!existsConfigAdmin())
				throw new IllegalStateException("A ConfigurationAdmin implementation is not installed on the framework");
			
			CompletableFuture<ConfigurationAdmin> configAdmin = getService(ConfigurationAdmin.class);
			configAdmin.thenAccept(ca -> {
				try {
					Configuration conf = ca.getConfiguration(pid, "?");
					Dictionary<String, Object> oldConf = conf.getProperties();
					Dictionary<String, Object> dicProp = oldConf != null ?
							MapUtils.mapToDictionary(MapUtils.merge(configuration, MapUtils.dictionaryToMap(conf.getProperties())))
							: MapUtils.mapToDictionary(configuration);
							
					conf.update(dicProp);
				} catch (Exception e) {
					exception.accept(e);
				}
			});
			configAdmin.join();
		} else {
			pidConfiguration.put(pid, configuration);
		}
	}
	
	private boolean existsConfigAdmin() {
		return (framework.getBundleContext().getServiceReference(ConfigurationAdmin.class) != null);
	}

	/********** ConfigurationPlugin methods **********/
	
	public void modifyConfiguration(ServiceReference<?> servRef, Dictionary<String, Object> conf) {
		Object pid = conf.get("service.pid");
		if (pid != null) {
			Map<String, Object> configuration = pidConfiguration.remove(pid.toString());
			if(configuration != null) {
				Dictionary<String, Object> customConf = MapUtils.mapToDictionary(configuration);
				Collections.list(customConf.keys())
						   .forEach(k -> conf.put(k, customConf.get(k)));
			}
		}
	}
	
}
