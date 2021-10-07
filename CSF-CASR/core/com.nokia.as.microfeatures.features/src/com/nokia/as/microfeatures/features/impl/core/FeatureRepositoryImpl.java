package com.nokia.as.microfeatures.features.impl.core;

import static com.nokia.as.microfeatures.features.Feature.Type.FEATURE;
import static java.util.Comparator.comparing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ace.obr.storage.BundleStore;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

import com.nokia.as.microfeatures.bundlerepository.BundleRepository;
import com.nokia.as.microfeatures.bundlerepository.RequirementBuilder;
import com.nokia.as.microfeatures.features.Feature;
import com.nokia.as.microfeatures.features.Feature.Type;
import com.nokia.as.microfeatures.features.FeatureRepository;
import com.nokia.as.microfeatures.features.impl.common.Helper;
import com.nokia.as.microfeatures.packager.Packager;
import com.nokia.as.microfeatures.packager.Packager.Params;

@Component
public class FeatureRepositoryImpl implements FeatureRepository {

	private final static String DEFAULT_VERSION = "0.0.0";
	private final static Logger _log = Logger.getLogger(FeatureRepositoryImpl.class);
	
	/**
	 * Name of the feature which brings the casr simple file configurator
	 */
	private final static String FEATURE_CONFIG_BASIC = "config.basic";

	/**
	 * Name of the feature which brings the casr new auto configurator
	 */
	private final static String FEATURE_CONFIG_ADVANCED = "config.advanced";

	/**
	 * Name of the feature for log4j2 bridge microfeatures (used to wrap log4j and slg4j to log4j2)
	 */
	private final static String FEATURE_CONFIG_LOG4J2_BRIDGE = "lib.log.log4j.v2.bridge";
	
	/**
	 * Name of the feature for log4j blacklist microfeatures. This feature only exists in CASR 19.6.1
	 */
	private final static String FEATURE_CONFIG_LOG4J_BLACKLIST = "lib.log.log4j.blacklist";

	/**
	 * Features for runtimes supporting log4j
	 */
	private final static Set<String> FEATURE_RUNTIMES_LOG4J = new HashSet<>(Arrays.asList("runtime.felix.log4j1", "runtime.felix.embedded.log4j1"));
	
	/**
	 * Features for runtimes supporting log4j2
	 */
	private final static Set<String> FEATURE_RUNTIMES_LOG4J2 = new HashSet<>(Arrays.asList("runtime.felix", "runtime.felix.embedded"));

	/**
	 * log4j feature.
	 */
	private final static String FEATURE_LOG4J = "lib.log.log4j";

	/**
	 * lib.pax.log feature.
	 */
	private final static String FEATURE_PAXLOG = "lib.pax.log";

	/**
	 * log4j feature version 1.0.0 (for log4j1 support)
	 */
	private final static String FEATURE_LOG4J_V1 = "1.0.0";

	/**
	 * log4j feature version 2.0.0 (for log4j2 support)
	 */
	private final static String FEATURE_LOG4J_V2 = "2.0.0";
	
	/**
	 * env variable that can be used to specify list of bundle symbolic names which must be blacklisted
	 */
	private final static String BLACKLIST = "MICROFEATURES_BLACKLIST";

	/**
	 * Feature 
	 */

	BundleRepository _repos;
	private String _localObr;
	
	/**
	 * List of obr urls (first one is the local obr)
	 */
	private List<String> _obrUrls;
		
	/**
	 * Bundle SymbolicName of felix main bundle 
	 */
	private final static String FELIX_MAIN = "org.apache.felix.main";
	
	/**
	 * Mapping between jre version and jre feature version
	 */
    private final static Map<String,String> _jreVersionToFeatureVersion = Collections.unmodifiableMap(Stream.of(
        	new SimpleEntry<>("1.8", "1.8.0"),
        	new SimpleEntry<>("9", "1.9.0"),
        	new SimpleEntry<>("10", "1.10.0"),
        	new SimpleEntry<>("11", "1.11.0"),
        	new SimpleEntry<>("12", "1.12.0"),
        	new SimpleEntry<>("13", "1.13.0"),
        	new SimpleEntry<>("14", "1.14.0"),
        	new SimpleEntry<>("15", "1.15.0"),
        	new SimpleEntry<>("17", "1.17.0"))
       .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)));
    
	@Inject
	BundleContext _bctx;
	
	@ServiceDependency
	Packager _packager;
	
	@ServiceDependency
	private volatile BundleStore _bundleStore;
	
	@ServiceDependency(changed="update")
	void bind(BundleRepository repo, Map<String, Object> properties) {
		_repos = repo;
		_obrUrls = Arrays.asList(properties.get(BundleRepository.OBR_URLS).toString().split(","));
		_localObr = properties.get(BundleRepository.OBR_LOCAL).toString();
	}
	
	void update(BundleRepository repo, Map<String, Object> properties) {
		_obrUrls = Arrays.asList(properties.get(BundleRepository.OBR_URLS).toString().split(","));
	}

	class BlueprintResource {
		final String bsn;
		final String version;
		
		BlueprintResource(String bsn, String version) {
			this.bsn = bsn;
			this.version = version;
		}
	}
			
	@Override
	public Set<Feature> findFeatures() {
		return findFeatures(FEATURE);
	}		

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> findCategories() {
		_log.debug("findCategories");
		List<Resource> featureResources = findFeatureResources(String.format("(&(%s=*)(visible=true))", Feature.NAMESPACE));
		if (featureResources.isEmpty()) {
			_log.debug("no features found.");
			return Collections.emptySet();
		}
		Set<String> result = new HashSet<>();
		for (Resource r : featureResources) {
			List<Capability> caps = r.getCapabilities(Feature.NAMESPACE);
			for (Capability cap : caps) {
				Map<String, Object> attributes = cap.getAttributes();
				List<Object> categories = (List<Object>) attributes.get(Feature.CATEGORY);
				if (categories != null) {
					categories.forEach(category -> result.add(category.toString()));
				}
			}
		}
		return result;
	}
	
	private List<Resource> latestVersion(List<Resource> resources) {
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
		return new ArrayList<>(latest.values());
	}
	
	@SuppressWarnings("unchecked")
	private <T> T getIdentityAttribute(Resource r, String attribute) {
		return (T) r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0).getAttributes().get(attribute);
	}
		
	private List<Resource> findFeatureResources(String filter) {
		return findResources(Feature.NAMESPACE, filter);
	}

	private List<Resource> findResources(String namespace, String filter) {
		RequirementBuilder requirementBuilder = _repos.newRequirementBuilder(namespace);
		requirementBuilder.addDirective("filter", filter);
		List<Capability> capabilities = _repos.findProviders(requirementBuilder.build());
		if (capabilities.isEmpty()) {
			_log.debug("no features found.");
			return Collections.emptyList();
		}
		List<Resource> features = new ArrayList<>();
		for (Capability c : capabilities) {
			features.add(c.getResource());
		}
		return latestVersion(features);
	}

	// New stuff ...
	
	@Override
	public void createAssembly(String name, String bsn, String version, String doc, List<Feature> features) throws Exception {
		// Check if version has a legal osgi syntax.
		new Version(version);
		
		// Check if some default features should be installed
		checkDefaultFeatures(features);
	
		// Create a simple assembly bundle which contains only the list of features as osgi requirements
		Manifest manifest = new Manifest();
        Attributes global = manifest.getMainAttributes();
        global.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        global.put(new Attributes.Name("Bundle-ManifestVersion"), "2");
        global.put(new Attributes.Name("Bundle-Name"), name);
        global.put(new Attributes.Name("Bundle-SymbolicName"), bsn);
        global.put(new Attributes.Name("Bundle-Version"), version);
                       
        StringBuilder sb = new StringBuilder();
        sb.append(Feature.NAMESPACE).append(";");
        sb.append(Feature.NAMESPACE).append("=\"").append(name).append("\";");
        sb.append(Feature.VERSION).append("=\"").append(version).append("\";");
        if(doc != null){
        	sb.append(Feature.DOC).append("=\"").append(doc).append("\";");
        }
        sb.append(Feature.TYPE).append("=").append(Feature.Type.ASMB.name());
        global.put(new Attributes.Name("Provide-Capability"), sb.toString());
        
        StringBuilder sb2 = new StringBuilder();
        features.forEach(ft -> {
            sb2.append(Feature.NAMESPACE);
            sb2.append(";");
            sb2.append("filter:=\"(");
            sb2.append("&(|(type=" + Feature.Type.FEATURE.name()+")(!(type=*)))");

            //version filter
            boolean hasVersion = ft.getVersion() != null && !DEFAULT_VERSION.equals(ft.getVersion());
            if(hasVersion){
            	sb2.append("(&");
            }
            sb2.append("(" + Feature.NAMESPACE).append("=").append(ft.getName()).append(")");
            if(hasVersion){
            	sb2.append("("+Feature.VERSION+"=").append(ft.getVersion()).append("))");
            }
            sb2.append(")\"");
            sb2.append(",");
        });
        
        sb2.setLength(sb2.length() - 1); // remove last comma
        global.put(new Attributes.Name("Require-Capability"), sb2.toString());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    try (JarOutputStream jos = new JarOutputStream(baos, manifest)) {	    	
            jos.close();            
            addToLocalObr(new ByteArrayInputStream(baos.toByteArray()), bsn, version);
	    }
	    
	    _repos.reloadLocalObr();
	}
	
	@Override
	public void checkDefaultFeatures(List<Feature> features) {
		Set<Feature> allFeatures = findFeatures(Type.FEATURE, true /* internal */);

		// if config.advanced feature not specified, use config.basic by default (if config.basic found from obr)
		Optional<Feature> advancedConfig = features.stream().filter(f -> f.getName().equals(FEATURE_CONFIG_ADVANCED)).findFirst();
		if (! advancedConfig.isPresent()) {
			Optional<Feature> basicConfig = allFeatures.stream().filter(f -> f.getName().equals(FEATURE_CONFIG_BASIC)).findFirst();
			if (basicConfig.isPresent()) {
				features.add(basicConfig.get());
			}
		}

		// Detect current JRE, and add internal lib.jre feature if necessary.
		Optional<Feature> jreFeature = features.stream().filter(f -> f.getName().equals("lib.jre")).findFirst();
		if (! jreFeature.isPresent()) {
			String jreSpecVersion = System.getProperty("java.specification.version");
			String jreFeatureVersion = _jreVersionToFeatureVersion.get(jreSpecVersion);
			if (jreFeatureVersion == null) {
				throw new IllegalStateException("Unsupported JRE specification version: " + jreSpecVersion);
			}
			jreFeature = allFeatures.stream().filter(f -> f.getName().equals("lib.jre") && f.getVersion().equals(jreFeatureVersion)).findFirst();
			if (jreFeature.isPresent()) {
				features.add(jreFeature.get());
			}
		}
		
		// If log4j and log4j2 features and lib.pax.log are not specified, derive a default log feature, based on runtime names.
		Optional<Feature> log4jOrPax = features.stream().filter(f -> FEATURE_LOG4J.equals(f.getName()) || FEATURE_PAXLOG.equals(f.getName())).findFirst();
		if (! log4jOrPax.isPresent()) {
			// use log4j1 feature if runtime is log4j1 based 
			Optional<Feature> runtimeWithLog4j = features.stream().filter(f -> FEATURE_RUNTIMES_LOG4J.contains(f.getName())).findFirst();
			if (runtimeWithLog4j.isPresent()) {
				Optional<Feature> log4j1 = allFeatures.stream().filter(f -> FEATURE_LOG4J.equals(f.getName()) && f.getVersion().equals(FEATURE_LOG4J_V1)).findFirst();
				if (log4j1.isPresent()) {
					features.add(log4j1.get());
				}
			} else 	{
				// use log4j2 feature if runtime is log4j2 based
				Optional<Feature> runtimeWithLog4j2 = features.stream().filter(f -> FEATURE_RUNTIMES_LOG4J2.contains(f.getName())).findFirst();
				if (runtimeWithLog4j2.isPresent()) {
					Optional<Feature> log4j2 = allFeatures.stream().filter(f -> FEATURE_LOG4J.equals(f.getName()) && f.getVersion().equals(FEATURE_LOG4J_V2)).findFirst();
					if (log4j2.isPresent()) {
						features.add(log4j2.get());
					}
				}
			}
		}
		
		// Auto add log4j2 bridges when log4j2 is selected and log4j1 is not selected
		Optional<Feature> log4j2 = features.stream().filter(f -> FEATURE_LOG4J.equals(f.getName()) && f.getVersion().equals(FEATURE_LOG4J_V2)).findFirst();
		if (log4j2.isPresent()) {
			Optional<Feature> log4j1 = features.stream().filter(f -> FEATURE_LOG4J.equals(f.getName()) && f.getVersion().equals(FEATURE_LOG4J_V1)).findFirst();
			if (! log4j1.isPresent()) {
				Optional<Feature> log4j2Bridge = allFeatures.stream().filter(f -> f.getName().equals(FEATURE_CONFIG_LOG4J2_BRIDGE)).findFirst();
				if (log4j2Bridge.isPresent()) {
					features.add(log4j2Bridge.get());
				}
			}
		}
		
		// Auto add log4j blacklist feature if log4j2 and paxlog are not selected
		Optional<Feature> paxlog = features.stream().filter(f -> FEATURE_PAXLOG.equals(f.getName())).findFirst();
		if (! log4j2.isPresent() && !paxlog.isPresent()) {
			Optional<Feature> log4jBlacklist = allFeatures.stream().filter(f -> f.getName().equals(FEATURE_CONFIG_LOG4J_BLACKLIST)).findFirst();
			if (log4jBlacklist.isPresent()) {
				features.add(log4jBlacklist.get());
			}
		} 
	}
	
	private void addToLocalObr(InputStream in, String bsn, String version) throws Exception {
		_bundleStore.put(in, bsn + "-" + version + ".jar", true);
		_bundleStore.get("index.xml"); // this will recreate the index.
	}
	
	private boolean removeFromLocalObr(String bsn, String version) throws IOException {
		// the file name is stored in a subdir in case the bsn contains dot.
		// for example, if bsn is like "test", then the resource to delete is in store/test-version.jar
		// if now bsn is like test.snapshot, then it is stored in store/test/test.snapshot-version.jar
		
		if (! _bundleStore.remove(bsn + "-" + version + ".jar")) {
			return false;
		}
		_bundleStore.get("index.xml"); // this will recreate the index.
		return true;
	}
		
	@Override
	public Set<Feature> findFeatures(Type type) {
		return findFeatures(type, false /* hide internal features */);
	}

	@Override
	public Set<Feature> findFeatures(Type type, boolean internal) {
		List<Resource> resources;
		
		if (internal) {
			// return all features
			resources= (type == FEATURE) ?
					findFeatureResources(String.format("(&(%s=*)(|(!(type=*))(type=%s)))", Feature.NAMESPACE, FEATURE.name())) :
					findFeatureResources(String.format("(&(%s=*)(type=%s))", Feature.NAMESPACE, type.name()));			
		} else {
			// hide internal features
			resources= (type == FEATURE) ?
					findFeatureResources(String.format("(&(%s=*)(|(!(type=*))(type=%s))(!(internal=true)))", Feature.NAMESPACE, FEATURE.name())) :
					findFeatureResources(String.format("(&(%s=*)(type=%s)(!(internal=true)))", Feature.NAMESPACE, type.name()));
		}

		if (resources.isEmpty()) {
			return Collections.emptySet();
		}

		Map<String, FeatureImpl> features = new HashMap<>();
		Set<Feature> result = new HashSet<>();
		for (Resource r : resources) {
			List<Capability> caps = r.getCapabilities(Feature.NAMESPACE);
			List<Capability> osgiCaps = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
			String fbsn = (String) osgiCaps.get(0).getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
			List<Capability> contentCaps = r.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
			String url = (String) contentCaps.get(0).getAttributes().get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
			
			for (Capability cap : caps) {
				Map<String, Object> attributes = cap.getAttributes();
				String fn = (String) attributes.get(Feature.NAMESPACE);				
				Object fvOptional = attributes.get(Feature.VERSION);	
				Object finternal = attributes.get(Feature.INTERNAL);
				String fdoc = (String) attributes.get(Feature.DOC);
				String fdesc = (String) attributes.get(Feature.DESC);
				if (fn == null) {
					_log.warn("ignoring resource " + r + " which does not contain " + Feature.NAMESPACE + " attribute");
					continue;
				}
				if (type == FEATURE) {
					if ((finternal == null || "false".equalsIgnoreCase(finternal.toString())) && fvOptional == null) {				
						_log.warn("ignoring resource " + r + " which is a public feature but wihtout a specified version");
						continue;
					}
				}
				String fv = fvOptional == null ? DEFAULT_VERSION : fvOptional.toString();
				@SuppressWarnings("unchecked")
				List<Object> aliases = (List<Object>) attributes.get(Feature.ALIAS);
				Set<String> aliasesStr = new HashSet<>();;
				if (aliases != null) {			
					aliases.forEach(alias -> aliasesStr.add(alias.toString()));
				}
				FeatureImpl feature = features.computeIfAbsent(fbsn + "-" + fv, (ft) -> new FeatureImpl(type, fn, fbsn, fv, fdesc, fdoc, url, attributes, aliasesStr));
				result.add(feature);

				@SuppressWarnings("unchecked")
				List<Object> categories = (List<Object>) attributes.get(Feature.CATEGORY);
				if (categories != null) {
					categories.forEach(category -> feature.addCategory(category.toString()));
				}
			}
		}
		
		// Sort the feature list by names, then by version
		return result.stream()
				.sorted(comparing(Feature::getName).thenComparing(Feature::getVersion))
				.collect(Collectors.toCollection(LinkedHashSet::new));		
	}
	
	@Override
	public void createSnapshot(String bsn, String version, String doc) throws Exception {
		Optional<Feature> asmb = findFeatureByBSN(Feature.Type.ASMB, bsn, version);
		if (! asmb.isPresent()) {
			throw new Exception("assembly not found: " + bsn + "/" + version);
		}
		
		Feature assembly = asmb.get();
		
		if(doc == null){
			// get assembly doc
			doc = assembly.getDoc();
		}
					
		// Lookup the assembly from the local obr.
		RequirementBuilder requirementBuilder = _repos.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
				
		String snapshotName = assembly.getName() + " Snapshot";
		String snapshotBsn = assembly.getSymbolicName() + ".snapshot";
		String snapshotVersion = calculateNextSnapshotVersion(snapshotBsn, version);
		
		requirementBuilder.addDirective("filter", "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=" + assembly.getSymbolicName() + ")");
		requirementBuilder.addVersionRangeFilter(new VersionRange(version));

		List<Capability> capabilities = _repos.findProviders(requirementBuilder.build());
		if (capabilities.isEmpty()) {
			throw new Exception("No matching bundle found!");
		}

		List<Resource> resourcesMatchingCapabilities = 
				capabilities.stream().map(c -> c.getResource()).collect(Collectors.toList());
		
		// Only take into account features latest version.
		resourcesMatchingCapabilities = latestVersion(resourcesMatchingCapabilities);	
		Resource asmbResource = resourcesMatchingCapabilities.get(0);
		
		// For each features required by the assembly, check if one is specifying some blacklisted resources
		Set<Resource> blacklistedResources = resolveBlacklistedResources(asmbResource);		
		
		// Get the Require-Capability header stored in the assembly bundle (we'll add it in the target snapshot bundle).
		String reqCapAsmbHeader = getFeatureRequireCapHeaderFromAsmb(asmbResource);
		
		// Perform features resolution
		Set<Resource> resolvedResources = _repos.findResolution(resourcesMatchingCapabilities, false, blacklistedResources);
		
		// Store the resolution (list of bundles) in a new snapshot bundle.
		// First, Create a simple assembly bundle which contains only the list of features as osgi requirements
		Manifest manifest = new Manifest();
        Attributes global = manifest.getMainAttributes();
        global.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        global.put(new Attributes.Name("Bundle-ManifestVersion"), "2");
        global.put(new Attributes.Name("Bundle-Name"), snapshotName);
        global.put(new Attributes.Name("Bundle-SymbolicName"), snapshotBsn);
        global.put(new Attributes.Name("Bundle-Version"), snapshotVersion);
                       
        StringBuilder sb = new StringBuilder();
        sb.append(Feature.NAMESPACE).append(";");
        sb.append(Feature.NAMESPACE).append("=\"").append(snapshotName).append("\";");
        sb.append(Feature.TYPE).append("=").append(Feature.Type.SNAPSHOT.name()).append(";");
        sb.append(Feature.VERSION).append("=").append(snapshotVersion).append(";");
        if(doc != null){
        	sb.append(Feature.DOC).append("=").append(doc).append(";");
        }
        sb.append(Feature.ASMB_BSN).append("=").append(bsn).append(";");
        sb.append(Feature.ASMB_VERSION).append("=").append(version);
        global.put(new Attributes.Name("Provide-Capability"), sb.toString());
                		        
        // If no felix framework is found from resolved bundles, find it from OBR, and add it to resolved bundles.
        // (with old OBRs, we had bugged assemblies which was declaring "Require-Bundle: org.apache.felix.main";
        // and in this case, we must manually add the felix.main bundle in the list of resolved resources).
        addResourceIfNotFound(resolvedResources, FELIX_MAIN);
        
        // If no jre18 fragment is found from resolved bundles, find it from OBR and add it to resolved bundles.
        // We only do this if jre = 1.8 because in jre18, system bundle is always blacklisted
		if ("1.8".equals(System.getProperty("java.specification.version"))) {
			addResourceIfNotFound(resolvedResources, Helper.JRE18_BSN);
		}

        // Build collection of resolved bundles.         
		List<String> bundleURLs = resolvedResources.stream()
				.filter(r -> !r.equals(asmbResource))
				.map(r -> getResourceUrl(r))
				.map(url -> fixBundleURL(url))
				.collect(Collectors.toList());

        global.put(new Attributes.Name("X-CSF-OBR"), getObrUrls());
        global.put(new Attributes.Name("X-CSF-Requirements"), reqCapAsmbHeader.toString());
        global.put(new Attributes.Name("X-CSF-Bundles"), bundleURLs.stream().collect(Collectors.joining(",")));
        global.put(new Attributes.Name("X-CSF-MicrofeaturesVersion"), Helper.getMicrofeaturesVersion(_bctx));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    try (JarOutputStream jos = new JarOutputStream(baos, manifest)) {	    	
            jos.close();            
            addToLocalObr(new ByteArrayInputStream(baos.toByteArray()), bsn, version);            
	    }
	    
	    _repos.reloadLocalObr();
	}
	
	@Override
	public Set<Resource> resolveSnapshot(String bsn, String version, String doc) throws Exception {
		Optional<Feature> asmb = findFeatureByBSN(Feature.Type.ASMB, bsn, version);
		if (! asmb.isPresent()) {
			throw new Exception("assembly not found: " + bsn + "/" + version);
		}
		
		Feature assembly = asmb.get();
		
		if(doc == null){
			// get assembly doc
			doc = assembly.getDoc();
		}
					
		// Lookup the assembly from the local obr.
		RequirementBuilder requirementBuilder = _repos.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE);		
		requirementBuilder.addDirective("filter", "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=" + assembly.getSymbolicName() + ")");
		requirementBuilder.addVersionRangeFilter(new VersionRange(version));

		List<Capability> capabilities = _repos.findProviders(requirementBuilder.build());
		if (capabilities.isEmpty()) {
			throw new Exception("No matching bundle found!");
		}

		List<Resource> resourcesMatchingCapabilities = 
				capabilities.stream().map(c -> c.getResource()).collect(Collectors.toList());
		
		// Only take into account features latest version.
		resourcesMatchingCapabilities = latestVersion(resourcesMatchingCapabilities);	
		Resource asmbResource = resourcesMatchingCapabilities.get(0);
		
		// For each features required by the assembly, check if one is specifying some blacklisted resources
		Set<Resource> blacklistedResources = resolveBlacklistedResources(asmbResource);		
				
		// Perform features resolution
		Set<Resource> resolvedResources = _repos.findResolution(resourcesMatchingCapabilities, false, blacklistedResources);
		        
        // If no jre18 fragment is found from resolved bundles, find it from OBR and add it to resolved bundles.
        // We only do this if jre = 1.8 because in jre18, system bundle is always blacklisted
		if ("1.8".equals(System.getProperty("java.specification.version"))) {
			addResourceIfNotFound(resolvedResources, Helper.JRE18_BSN);
		}

        // Build collection of resolved bundles.         
		return resolvedResources.stream()
				.filter(r -> !r.equals(asmbResource))
				.filter(r -> r.getCapabilities(Feature.NAMESPACE).size() == 0)
				.filter(r -> ! getBSN(r).startsWith("com.nokia.as.features"))
				.collect(Collectors.toSet());
	}

	static String getBSN(Resource r) {
		String bsn = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)
				.iterator().next()
				.getAttributes()
				.get(IdentityNamespace.IDENTITY_NAMESPACE)
				.toString();
		return bsn;
	}
	
	static String getVersion(Resource r) {
		Version version = (Version) r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)
				.iterator().next()
				.getAttributes()
				.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		StringBuilder sb = new StringBuilder("'[");
		sb.append(version.toString());
		sb.append(",");
		sb.append(version.getMajor() + "." + version.getMinor() + "." + (version.getMicro()+1));
		sb.append(")'");
		return sb.toString();
	}

	/**
	 * Creates a snapshot bundle with the specified list of bundle resources.
	 */
	@Override
	public void createSnapshot(String bsn, String version, List<Resource> resources) throws Exception {		
		bsn = bsn + ".snapshot";
				
		// Store the resolution (list of bundles) in a new snapshot bundle.
		// First, Create a simple assembly bundle which contains only the list of features as osgi requirements
		Manifest manifest = new Manifest();
        Attributes global = manifest.getMainAttributes();
        global.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        global.put(new Attributes.Name("Bundle-ManifestVersion"), "2");
        global.put(new Attributes.Name("Bundle-Name"), bsn);
        global.put(new Attributes.Name("Bundle-SymbolicName"), bsn);
        global.put(new Attributes.Name("Bundle-Version"), version);
                       
        StringBuilder sb = new StringBuilder();
        sb.append(Feature.NAMESPACE).append(";");
        sb.append(Feature.NAMESPACE).append("=\"").append(bsn).append("\";");
        sb.append(Feature.TYPE).append("=").append(Feature.Type.SNAPSHOT.name()).append(";");
        sb.append(Feature.VERSION).append("=").append(version).append(";");
        sb.append(Feature.ASMB_BSN).append("=").append(bsn).append(";");
        sb.append(Feature.ASMB_VERSION).append("=").append("1.0.0");
        global.put(new Attributes.Name("Provide-Capability"), sb.toString());
                		        
        // Build collection of resolved bundles.         
		List<String> bundleURLs = resources.stream()
				.map(r -> getResourceUrl(r))
				.map(url -> fixBundleURL(url))
				.collect(Collectors.toList());
		        
        global.put(new Attributes.Name("X-CSF-OBR"), getObrUrls());
        global.put(new Attributes.Name("X-CSF-Bundles"), bundleURLs.stream().collect(Collectors.joining(",")));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    try (JarOutputStream jos = new JarOutputStream(baos, manifest)) {	    	
            jos.close();            
            addToLocalObr(new ByteArrayInputStream(baos.toByteArray()), bsn, version);
	    }	    
	    _repos.reloadLocalObr();
	}

	private void addResourceIfNotFound(Set<Resource> resolvedResources, String osgiIdentity) {
        boolean resourceFound = false;
        for (Resource r : resolvedResources) {
        	if (resourceMatches(r, osgiIdentity)) {
        		resourceFound = true;
        		break;
        	}
        }
        if (! resourceFound) {
			//List<Resource> felixMainResource = findResources("osgi.identity", "(osgi.identity=org.apache.felix.main)");
			List<Resource> resource = findResources("osgi.identity", "(osgi.identity=" + osgiIdentity + ")");
			if (resource.size() > 0) {
				resolvedResources.add(resource.get(0));
			}
        }
	}
	
	private boolean resourceMatches(Resource r, String osgiIdentity) {
		String bsn = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)
				.iterator().next()
				.getAttributes()
				.get(IdentityNamespace.IDENTITY_NAMESPACE)
				.toString();
		return osgiIdentity.equals(bsn);
	}

	String getObrUrls() {
		// skip local obr
		return _obrUrls.stream().filter(url -> ! url.equals(_localObr)).collect(Collectors.joining(","));
	}

	String getResourceUrl(Resource r) {
		return r.getCapabilities(ContentNamespace.CONTENT_NAMESPACE)
				.iterator().next()
				.getAttributes()
				.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE)
				.toString();
	}
		
	/**
	 * Get the Require-Capability header for of the given assembly.
	 */
	private String getFeatureRequireCapHeaderFromAsmb(Resource assembly) {
		StringBuilder sb = new StringBuilder();
		List<Requirement> reqs = assembly.getRequirements(Feature.NAMESPACE);
		for (Requirement req : reqs) {
			sb.append(Feature.NAMESPACE).append("; filter:=\"");
			sb.append(req.getDirectives().get("filter"));
			sb.append("\"");
			sb.append(",");
		}
		
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	/** 
	 * Parse in the assembly the Require-Capability headers which are referencing features.
	 * For each feature, then check if some resource are blacklisted, and return the blacklisted resources.
	 */
	private Set<Resource> resolveBlacklistedResources(Resource assembly) {		
		// lookup all features capabilities matching the assembly.
		List<Requirement> reqs = assembly.getRequirements(Feature.NAMESPACE);
		RequirementBuilder reqBuild = _repos.newRequirementBuilder(Feature.NAMESPACE);
		if (reqs.size() == 1) {
			String filter= reqs.get(0).getDirectives().get("filter");			
			reqBuild.addDirective("filter", filter);
		} else {
			StringBuilder sb = new StringBuilder("(|");			
			for (Requirement req : reqs) {
				sb.append(req.getDirectives().get("filter"));
			} 
			sb.append(")");
			reqBuild.addDirective("filter", sb.toString());			
		}
		List<Capability> featuresCaps = _repos.findProviders(reqBuild.build());
		List<Resource> featuresResources = featuresCaps.stream().map(c -> c.getResource()).collect(Collectors.toList());
		featuresResources = latestVersion(featuresResources);		
		
		// for each feature capabilities, see if one is providing some blacklisted resources
		List<String> filters = new ArrayList<>(0);
		for (Resource r : featuresResources) {
			for (Capability featuresCap : r.getCapabilities(Feature.NAMESPACE)) {
				String blacklistIdFilter = (String) featuresCap.getAttributes().get(Feature.BLACKLIST_IDENTITY);
				if (blacklistIdFilter != null) {
					filters.add(blacklistIdFilter);
				}
			}
		}
		
		// see if user has specified a list of blacklisted bundle symbolic names
		getUserBlackList().forEach(filters::add);
		return resolveBlacklistedResources(filters);
	}
	
	private List<String> getUserBlackList() {
		String blacklist = System.getProperty("blacklist");
		if (blacklist == null) {
			blacklist = System.getenv(BLACKLIST);
		}
		if (blacklist == null) {
			return Collections.emptyList();
		}
		String[] bsns = blacklist.split(",");
		List<String> list = new ArrayList<>(bsns.length);
		Stream.of(bsns).forEach(bsn -> list.add("(osgi.identity=" + bsn + ")"));
		return list;
	}

	@Override
	public Set<Resource> resolveBlacklistedResources(List<String> filters) {				
		// if jre=1.8, blacklist com.nokia.as.osgi.jre18 system bundle, because some obrs don't provide it and we have it installed in our runtime.
		filters.add("(osgi.identity=" + Helper.JRE18_BSN + ")");
		
		// Same for the felix framework from obr (the createSnapshot will install it) 
		filters.add("(osgi.identity=" + FELIX_MAIN + ")");

		// some features do provide some blacklisted resources, find all of them
        if (filters.size() > 0) {
        	List<Requirement> requirements = new ArrayList<>();
        	for (String filter : filters) {
        		RequirementBuilder reqBuild = _repos.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
        		reqBuild.addDirective("filter", filter);    
        		reqBuild.addBundleIdentityFilter();
        		requirements.add(reqBuild.build());
        	}
        	
    		List<Capability> caps = _repos.findProviders(requirements);
    		return caps.stream().map(c -> c.getResource()).collect(Collectors.toSet());
        } else {
        	return Collections.emptySet();
        }
	}

	private String fixBundleURL(String url) {
		for (String obrUrl : _obrUrls) {
			int index = obrUrl.lastIndexOf("/");
			String obrUrlDir = obrUrl.substring(0, index+1);
			if (url.startsWith(obrUrlDir)) {
				try {
					// url is something like  http://localhost:8081/artifactory/libs-snapshot/com/nokia/casr/com.nokia.casr.obr/17.9.0/../../../../com/nokia/casr/com.alcatel.as.util/1.0.0/com.alcatel.as.util-1.0.0.jar
					String path = url.substring(obrUrlDir.length());
					// path is now like ../../../../com/nokia/casr/com.alcatel.as.util/1.0.0/com.alcatel.as.util-1.0.0.jar
					url = new URI(obrUrlDir).resolve(path).toString();
					// now, url is clean and like http://localhost:8081/artifactory/libs-snapshot/com/nokia/casr/com.alcatel.as.util/1.0.0/com.alcatel.as.util-1.0.0.jar
					break;
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
		}
		return url;		
	}

	private String calculateNextSnapshotVersion(String asmbBsn, String asmbVersion) {		
		// find all existing snapshots for the given asmbBsn/asmb
		Set<Feature> snapshots = findFeatures(Type.SNAPSHOT);		
		List<Version> versions = snapshots.stream()
				.filter(f -> f.getSymbolicName().equals(asmbBsn))
				.map(f -> new Version(f.getVersion()))
				.collect(Collectors.toList());
		Collections.sort(versions);
		
		if (versions.size() == 0) {
			return asmbVersion;
		}
		
		Version highest = versions.get(versions.size()-1);
		return highest.getMajor() + "." + highest.getMinor() + "." + (highest.getMicro() + 1);
	}

	@Override
	public void createRuntime(String bsn, String version) throws Exception {
		createRuntime(bsn, version, false, "csf", "group", "component", "instance");
	}
	
	@Override
	public void createRuntime(String bsn, String version, String p, String g, String c, String i) throws Exception {
		createRuntime(bsn, version, true, p, g, c, i);
	}
	
	private void createRuntime(String snapshotBsn, String snapshotVersion, boolean bpStructure, String p, String g, String c, String i) throws Exception {
		Optional<Feature> ft = findFeatureByBSN(Feature.Type.SNAPSHOT, snapshotBsn, snapshotVersion);
		if (! ft.isPresent()) {
			throw new Exception("snapshot not found: " + snapshotBsn + "/" + snapshotVersion);
		}
		
		Feature snapshot = ft.get();
		
		// remote the ".snapshot" suffix from the snapshot bsn
		String appName = snapshotBsn;
		int index = snapshotBsn.lastIndexOf(".snapshot");
		if (index != -1) {
			appName = snapshotBsn.substring(0, index);
		}
		
		// Open the snapshot bundle, and extract all the urls of the resolved bundles
		URL url = new URL("jar:" + snapshot.getURL() + "!/");
		URLConnection rawConnection = url.openConnection();
        JarURLConnection jarConnection = (JarURLConnection) rawConnection;

		try (JarFile jarFile = jarConnection.getJarFile()) {
			Manifest mf = jarFile.getManifest();
			Attributes attrs = mf.getMainAttributes();			
			String snapshotBundles = attrs.getValue("X-CSF-Bundles");
			String snapshotRequirements = attrs.getValue("Require-Capability");
		
			// Finally invoke the feature packager with the list of urls
			
			List<URL> urls =
				Stream.of(snapshotBundles.split(",")).map(this::toURL).collect(Collectors.toList());
			urls.add(toURL(snapshot.getURL()));

			Map<Params, Object> params = new HashMap<>();
			//params.put(Params.TARGET, _installDir + "/test-1.0.0");
			params.put(Params.TARGET, appName + "-" + snapshotVersion);
			params.put(Params.LEGACY, new Boolean(bpStructure));
			params.put(Params.PLATFORM, p);
			params.put(Params.GROUP, g);
			params.put(Params.COMPONENT, c);
			params.put(Params.INSTANCE, i);

			CompletableFuture<Path> zip = _packager.packageRuntime(urls, params);
			zip.get();
		}	
	}
	
	private URL toURL(String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			return null;
		}
	}
	
	@Override
	public void delete(String  type, String bsn, String version) throws Exception {
		Feature.Type ftype = Feature.Type.valueOf(type);
		Set<Feature> features = findFeatures(ftype);
		if (! features.isEmpty()) {
			for (Feature f : features) {
				if (f.getSymbolicName().equals(bsn) && f.getVersion().equals(version)) {
					if (! removeFromLocalObr(bsn, version)) {
						throw new RuntimeException("Could not delete feature " + f.getName());
					}
				    _repos.reloadLocalObr();
					break;
				}				
			}				
		}
		_log.info("No features found for type=" + type + ", bsn=" + bsn + ", version=" + version);		
	}
	
	private Optional<Feature> findFeatureByBSN(Feature.Type type, String bsn, String version) {
		return findFeatures(type)
			.stream()
			.filter(f -> f.getSymbolicName().equals(bsn) && f.getVersion().equals(version))
			.findFirst();
	}
	
	@Override
	public Optional<Feature> findFeature(Feature.Type type, String name, String version) {
		return findFeatures(type)
			.stream()
			.filter(f -> f.getName().equals(name) && f.getVersion().equals(version))
			.findFirst();
	}
	
}
