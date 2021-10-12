// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.bundlerepository.impl;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ace.obr.storage.BundleStore;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.resolver.ResolverImpl;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.Resolver;

import com.nokia.as.microfeatures.bundlerepository.BundleRepository;
import com.nokia.as.microfeatures.bundlerepository.RequirementBuilder;

import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.service.Registry;

@Component
public class BundleRepositoryImpl implements BundleRepository {

	/**
	 * CASR Artifactory OBR url prefix
	 */
	public final static String ARTIFACTORY_OBR = "https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/com/nokia/casr/com.nokia.casr.obr/%VERSION%/com.nokia.casr.obr-%VERSION%.xml";
	
	/**
	 * System property used to configure resolver parallelism (default = use all available cores)
	 */
	public final static String PARALLELISM = "parallelism";

	/**
	 * System property for remote obr version
	 */
	private final static String VERSION = "version";

	/**
	 * System property for remote obr urls list (comma sepeparated)
	 */
	private final static String OBR = "obr";
	
	/**
	 * Environment property for remote obr urls
	 */
	private final static String ENV_OBR = "MICROFEATURES_OBR";
	
	/**
	 * same parameter as OBR, but deprecated
	 */
	private final static String OBR_REMOTE = "obr.remote";
		
	/**
	 * system property to use as a shortcut for using ~/.m2/repository/obr.xml
	 */
	private final static String M2 = "m2";
	
	/**
	 * local M2 obr repository
	 */
	private final static String M2_OBR = "file:" + String.join(File.separator, System.getProperty("user.home"), ".m2", "repository", "obr.xml");
	
	/**
	 * OBR base url
	 */
	private final static String OBR_BASE_URL = "obr.baseurl";

	/**
	 * OBR urls
	 */
	private volatile List<String> _obrUrls = new ArrayList<>();
	
	/**
	 * Local obr
	 */
	private String _localObr;
	
	/**
	 * Our logger
	 */
	private final static Logger _log = Logger.getLogger(BundleRepositoryImpl.class);
	
	/**
	 * Do we need to resolve optional dependencies ? (For OBR <= 18.3.2, we need to resolve optional dependencies)
	 */
	private volatile boolean _resolveOptionalDependencies = true;
	
	/**
	 * Resolves OBR by forcing selection of highest bundle versions, which is normally bad.
	 * We have to do this for Nokia OBRS <= 19.4.2
	 */
	private volatile boolean _resolveWithHighestBundleVersion;
		
	/*
	 * Even if we don't need this service, we however still need to depend on it because it is this service which initializes the local OBR.
	 */
	@ServiceDependency
	private BundleStore _bundleStore;

	//@ServiceDependency
	private Resolver _resolver;

    // The repositories that will be queries for providers
    private final Map<URI, OSGiRepository> _repositories = new HashMap<>();
		
	@Inject
	private BundleContext _bc;
	
	/**
	 * This is the service registration that we'll use to update our service properties.
	 */
	@Inject 
	private ServiceRegistration<?> _serviceRegistration;

	/**
	 * This is our service properties
	 */	
	private final Map<String, Object> _serviceProperties = new HashMap<>();
	
    private Registry _registry;

	private boolean _hasCSFAR_1822Bug;
	
	@Start
	public Map<String, Object> start() {
		try {
			if (Boolean.getBoolean("debug")) {
				_log.setLevel(Level.DEBUG);
				System.setProperty("felix.log.level", "4");
			}
			// Initialize the OSGi resolver
	        _resolver = new ResolverImpl(new Log4jResolverLog(_log), Integer.getInteger(PARALLELISM, Runtime.getRuntime().availableProcessors()));
			
	        // Initialize the registry needed by the Bnd OBR
	        _registry = BndRegistry.INSTANCE.getRegistry();

			// by default, we resolve optional dependencies unless if you specify -Doptional=false
	        String resolveOptional = _bc.getProperty("optional");
	        if (resolveOptional != null) {
	        	_resolveOptionalDependencies = "true".equals(resolveOptional);
	        } else {
	        	_resolveOptionalDependencies = true; 
	        }
			
			// Calculate service properties.
			String store = _bc.getProperty("org.apache.ace.obr.storage.file:fileLocation");
			URL storeUrl = new File(store).toURI().toURL();
			_localObr = storeUrl + "/index.xml";
			String m2 = _bc.getProperty(M2);
			String configuredObrs = null;
			if (m2 != null) {
				configuredObrs = M2_OBR;
			} else {
				// Check if -Dversion option is specified:
				String version = _bc.getProperty(VERSION);
				if (version != null) {
					configuredObrs = ARTIFACTORY_OBR.replace("%VERSION%", version);
				} else {
					// If no version specified, check -Dobr option
					configuredObrs = _bc.getProperty(OBR_REMOTE); // deprecated, use "obr" instead of obr.remote
					if (configuredObrs == null) {
						configuredObrs = _bc.getProperty(OBR);
						if (configuredObrs == null) {
							// If no -Dobr specified, finally check if "MICROFEATURES_OBR" variable is specified in env
							configuredObrs = System.getenv(ENV_OBR);
						}
					}
				}
			}
			
			List<String> releases = Collections.emptyList();
			List<String> obrs = new ArrayList<>();
			obrs.add(_localObr);
			if (configuredObrs != null) {
				for (String configuredObr : configuredObrs.split(",")) {
					obrs.add(configuredObr);
				}				
			} else {
				ObrVersions obrVersions = new ObrVersions(_bc.getProperty(OBR_BASE_URL));
				releases = obrVersions.downloadAndParseLinks();				
				obrs.add(releases.get(releases.size() - 1));
			}
						
			_serviceProperties.put(BundleRepository.OBR_URLS, obrs.stream().collect(joining(",")));
			_serviceProperties.put(BundleRepository.OBR_LOCAL, _localObr);
			_serviceProperties.put(BundleRepository.OBR_RELEASES, releases.stream().collect(joining(",")));
			if (configuredObrs != null) {
				_serviceProperties.put(BundleRepository.OBR_CONFIGURED, configuredObrs);
			}
			
			// Set default obr urls
			setObrUrlsInternal(obrs);
			return _serviceProperties;			
		} catch (Exception e) {
			_log.error("Could not initialize bundle repository service",  e);
			return null;
		}
	}
	
	@Override
	public synchronized void setObrUrls(List<String> newUrls) throws Exception {
		setObrUrlsInternal(newUrls);
		Hashtable<String, Object> newServiceProperties = new Hashtable<>(_serviceProperties);
		newServiceProperties.put(BundleRepository.OBR_URLS, newUrls.stream().collect(joining(",")));
		_serviceRegistration.setProperties(newServiceProperties);
	}
	
	private void setObrUrlsInternal(List<String> newUrls) throws Exception {
		_hasCSFAR_1822Bug = false;
		_resolveWithHighestBundleVersion = false;
		
		// remove old repositories which are not present in specified urls list
		for (String oldUrl : _obrUrls) {
			if (! newUrls.contains(oldUrl)) {
				OSGiRepository oldRepo = _repositories.remove(new URI(oldUrl));
				if (oldRepo != null) {
					oldRepo.close();
				}
			}
		}
		// add new repositories which we don't currently have
		for (String newUrl : newUrls) {
			if (! _obrUrls.contains(newUrl)) {
				OSGiRepository repo = createRepository(newUrl);
	            _repositories.put(new URI(newUrl), repo);
				if (ResolverUtil.detectNokiaObrsWithCSFAR1822Bug(newUrl)) {
					_hasCSFAR_1822Bug = true;
				}
				if (ResolverUtil.resolveWithHigestBundleVersion(newUrl)) {
					_resolveWithHighestBundleVersion = true;
				}
			}
		}
		
		// Store loaded OBR urls
		_obrUrls = new ArrayList<>(newUrls);
	}

	@Override
	public synchronized void reloadLocalObr() throws Exception {
		InputStream localOBR = null;
		try {
			localOBR = _bundleStore.get("index.xml"); // reload local obr
		} catch (IOException e) {}
		finally {
			if (localOBR != null) {
				localOBR.close();
			}
		}
		URI localObr = new URI(_localObr);
		OSGiRepository old = _repositories.get(localObr);
		if (old != null) {
			old.refresh();
		}
	}

	public synchronized List<Capability> findProviders(List<Requirement> requirements) {		
		ArrayList<Capability> res = new ArrayList<Capability>();
        for (Entry<URI, OSGiRepository> repoEntry : _repositories.entrySet()) {
            Repository repository = repoEntry.getValue();
            Map<Requirement, Collection<Capability>> providers = repository.findProviders(requirements);
            if (providers != null) {
        		requirements.stream().forEach(requirement -> {
        			res.addAll(providers.get(requirement));
        		});
            }
        }
		return res;
	}

	@Override
	public synchronized Set<Resource> findResolution(List<Resource> resources, boolean localResources) throws Exception {
		return findResolution(resources, localResources, Collections.emptySet());
	}
	
	@Override
	public synchronized Set<Resource> findResolution(List<Resource> resources, boolean localResources, Set<Resource> blacklist) throws Exception {
		List<Resource> optional = new ArrayList<>();
		ResolveContextImpl rc = new ResolveContextImpl(_repositories, _bc, resources, optional, localResources, blacklist, _resolveOptionalDependencies, _hasCSFAR_1822Bug);	
		
		Map<Resource, List<Wire>> resolution = _resolver.resolve(rc);
		Set<Resource> resultSet = resolution.keySet();
		if (!localResources) {
			// remove the system bundle from the result set since we ignore installed bundles
			for (Resource r : resolution.keySet()) {
				List<Capability> capabilities = r.getCapabilities("osgi.identity");
				Map<String, Object> nsAttrs = capabilities.iterator().next().getAttributes();
				String bsn = nsAttrs.get("osgi.identity").toString();
				String version = nsAttrs.get("version").toString();
				
				Bundle systemBundle = _bc.getBundle(0);
				if (bsn.equals(systemBundle.getSymbolicName()) && version.equals(systemBundle.getVersion().toString())) {						
					resultSet.remove(r);
					break;
				}
			}
		}
		
		if (_resolveWithHighestBundleVersion) {
			// for OBRS <= 19.4.2, we so far did something bad: we always selected highest bundle version. This is bad; but to be safe, we prefer to continue to
			// do this for OBRS <= 19.4.2
			return latestVersion(resultSet);		
		} else {
			// return the resolved set in mutable result set
			return new HashSet<>(resultSet);
		}
	}
	
	private Set<Resource> latestVersion(Set<Resource> resources) {
		Map<String, Resource> latest = new HashMap<>();
		
		for (Resource r : resources) {
			String bsn = getIdentityAttribute(r, "osgi.identity");
			Resource previous = latest.get(bsn);
			if (previous == null) {
				latest.put(bsn, r);
			} else {
				Version version = getIdentityAttribute(r, "version");
				Version previousVersion = getIdentityAttribute(previous, "version");
				if (version.compareTo(previousVersion) > 0) {
					latest.put(bsn, r);
				}				
			}
		}
		return new HashSet<>(latest.values());
	}
	
	@SuppressWarnings("unchecked")
	private <T> T getIdentityAttribute(Resource r, String attribute) {
		return (T) r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0).getAttributes().get(attribute);
	}

	private String getKey(String bsn, Version version) {
		return bsn + ":" + version.getMajor() + "." + version.getMinor() + "." + version.getMicro();
	}

	private String getKey(Resource r) {
		List<Capability> capabilities = r.getCapabilities("osgi.identity");
		Map<String, Object> nsAttrs = capabilities.iterator().next().getAttributes();
		String bsn = nsAttrs.get("osgi.identity").toString();
		Version version = Version.parseVersion(nsAttrs.get("version").toString());
		return getKey(bsn, version);
	}

	@Override
	public InstallationResult install(List<Resource> requestedResources, boolean resolve, boolean start) throws Exception {
		Set<String> alreadyInstalled = Stream.of(_bc.getBundles()).map(b -> getKey(b.getSymbolicName(), b.getVersion()))
				.collect(toSet());

		InstallationResult installationResult = new InstallationResult();

		if (resolve) {
			Set<Resource> resolvedResources = findResolution(requestedResources,
					true /* don't ignore local resources */);
			installationResult.resources.addAll(resolvedResources);
		} else {
			installationResult.resources.addAll(requestedResources);
		}

		ArrayList<Bundle> installedBundles = new ArrayList<Bundle>();

		// First install the dependencies
		for (Resource r : installationResult.resources) {
			// Skip the requested resources for now
			if (requestedResources.contains(r)) {
				continue;
			}
			String bundleLocation = null;
			Bundle currentBundle = null;
			try {
				bundleLocation = (String) r.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0).getAttributes()
						.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
				// do not install, if already installed.
				String key = getKey(r);
				if (alreadyInstalled.contains(key)) {
					installationResult.userFeedback.add("Skipping already installed bundle: " + bundleLocation);
					continue;
				}
				currentBundle = _bc.installBundle(bundleLocation);
			} catch (Exception be) {
				installationResult.userFeedback
						.add("Failed to install dependency: " + r.getCapabilities("osgi.identity"));
				return installationResult;
			}
			installationResult.userFeedback.add("Installed dependency: " + bundleLocation);
			installedBundles.add(currentBundle);
		}
		
		// Now install the requested resources
		for (Resource r : requestedResources) {
			String bundleLocation = null;
			Bundle currentBundle = null;
			try {
				bundleLocation = (String) r.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0).getAttributes()
						.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
				// do not install, if already installed.
				String key = getKey(r);
				if (alreadyInstalled.contains(key)) {
					installationResult.userFeedback.add("Skipping already installed bundle: " + bundleLocation);
					continue;
				}
				currentBundle = _bc.installBundle(bundleLocation);
			} catch (Exception be) {
				installationResult.userFeedback.add("Failed to install: " + bundleLocation);
				return installationResult;
			}
			installationResult.userFeedback.add("Installed: " + bundleLocation);
			installedBundles.add(currentBundle);
		}

		if (start) {
			for (Bundle ib : installedBundles) {
				ib.start(Bundle.START_ACTIVATION_POLICY);
				installationResult.userFeedback.add("Started: " + ib.getSymbolicName());
			}
		}
		return installationResult;
	}

	@Override
	public RequirementBuilder newRequirementBuilder(String namespace) {
		return new RequirementBuilderImpl(namespace);
	}

	@Override
	public RequirementBuilder newRequirementBuilder(String namespace, String filter) {
		return new RequirementBuilderImpl(namespace, filter);
	}

	/**
	 * Creates an OSGi bundle repository service using Bnd OSGiRepository implementation (don't use felix BundleRepository, which does not seem to work well with fragments)
	 */
	private OSGiRepository createRepository(String url) throws Exception {
	    Map<String, String> repoProps = new HashMap<>();
	    repoProps.put("locations", url);
	    repoProps.put("poll.time", "0");
        OSGiRepository repo = new OSGiRepository();
        repo.setRegistry(_registry);
	    repo.setProperties(repoProps);
        return repo;
	}

}
