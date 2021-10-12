// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.features.impl.core;

import static java.lang.System.out;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Registered;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import com.nokia.as.microfeatures.bundlerepository.BundleRepository;
import com.nokia.as.microfeatures.bundlerepository.BundleRepository.InstallationResult;
import com.nokia.as.microfeatures.bundlerepository.RequirementBuilder;
import com.nokia.as.microfeatures.features.Feature;
import com.nokia.as.microfeatures.features.FeatureRepository;

@Component(provides = Object.class)
@Property(name = CommandProcessor.COMMAND_SCOPE, value = "microfeatures")
@Property(name = CommandProcessor.COMMAND_FUNCTION, value = { 
	"checkLocalOBR", "resolveLocalOBR", "getFeatures", "create", "updateFeature", "updateOBR", "resolveFeatures"
})		
@Descriptor("CASR Micro Features Commands")
public class Shell {
	
	/**
	 * javax.annotation API is already provided by CASR, we can ignore such API
	 */
	private final static Set<String> JAVA_ANNOTATION_PKG = new HashSet<>(Arrays.asList("javax.annotation", "javax.annotation.security", "javax.annotation.sql"));
	
	/**
	 * JSR250 API has a splitpackage with javax.annotation API. This API is usually used by findbugs and is not usually needed at runtime. the jsr-250 API should be removed
	 * and should be imported optionally
	 */
	private final static Set<String> JSR250_ANNOTATION_PKG = new HashSet<>(Arrays.asList("javax.annotation", "javax.annotation.security", "javax.annotation.sql"));
	private final static String JSR_305 = "org.jsr-305"; // bundle symbolic name for jsr 305 API
	
	/**
	 * JAXB API is already provided by CASR, we can ignore such API
	 */
	private final static Set<String> JAXB_PKG = new HashSet<>(Arrays.asList("javax.xml.bind", "javax.xml.bind.annotation", "javax.xml.bind.annotation.adapters", 
			"javax.xml.bind.attachment", "javax.xml.bind.helpers", "javax.xml.bind.util"));
	
	/**
	 * javax.validation already provided by CASR
	 */
	private final static Set<String> JAVAX_VALID_PKG = new HashSet<>(Arrays.asList("javax.validation", 
			"javax.validation.constraints", "javax.validation.constraintvalidation", "javax.validation.groups",
			"javax.validation.bootstrap", "javax.validation.executable", 
			"javax.validation.metadata", "javax.validation.spi", "javax.validation.valueextraction"));

	@Inject
	BundleContext _bc;
	
	@ServiceDependency
	FeatureRepository _featureRepos;
	
	BundleRepository _repos;
	
	@ServiceDependency
	CommandProcessor _cmdProc;
	
	private volatile String _obr;
	private volatile String _localObr;
	private volatile boolean _info = true;
	
	@ServiceDependency
	private void bind(BundleRepository repos, Map<String, Object> properties) {
		_repos = repos;
		_obr = (String) properties.get(BundleRepository.OBR_CONFIGURED);
		_localObr =  (String) properties.get(BundleRepository.OBR_LOCAL);
	}
			
	String getLocalObr() {
		return _localObr;
	}
	
	@Registered
	void registered() {
		Thread t = new Thread(() -> {
			try {		
				// Check if a gogo script is provided as a command.
				Optional<String> script = Optional.ofNullable(_bc.getProperty("script"));
				script.ifPresent(this::runScriptCommand);
				
				// Check if a "list" command is provided.
				Optional<String> command = Optional.ofNullable(_bc.getProperty("list"));
				command.ifPresent(c -> runListCommand());

				// Check if a "create" command is provided.
				command = Optional.ofNullable(_bc.getProperty("create"));
				command.ifPresent(this::runCreateCommand);
		
				// Check if a "create-all" command is provided.
				command = Optional.ofNullable(_bc.getProperty("create-all"));
				command.ifPresent(this::runCreateAllCommand);

				// Check if a "create-legacy" command is provided.
				command = Optional.ofNullable(_bc.getProperty("create-legacy"));
				command.ifPresent(this::createLegacyCommand);
		
				// Check if an "update.features" command is provided (it allows to add some more features into an existing runtime)
				command = Optional.ofNullable(_bc.getProperty("update.features"));
				command.ifPresent(this::doUpdateFeatures);
				
				// Check if an "update.obr" command is provided (it allows to update the existing runtime with a newer OBR, with optional more features)
				command = Optional.ofNullable(_bc.getProperty("update.obr"));
				command.ifPresent(this::doUpdateObr);
				
				// Check if an "shell" command is provided (it allows to execute a gogo shell command)
				command = Optional.ofNullable(_bc.getProperty("shell"));
				command.ifPresent(this::shell);

			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		t.start();
	}	
	
	@Descriptor("Gets available features")
	public void getFeatures(
			@Descriptor("type (FEATURE|ASMB|SNAPSHOT, FEATURE by default)")
			@Parameter(names = "-t", absentValue="FEATURE")
			String type)
	{
		try {
			_repos.reloadLocalObr();
			Feature.Type ftype = Feature.Type.valueOf(type);
			Set<Feature> features = _featureRepos.findFeatures(ftype);
			if (features.isEmpty()) {
				System.out.println("No features found");
			} else {
				features.stream()
					.sorted(Comparator.comparing(Feature::getName).thenComparing(Feature::getVersion))
					.forEach(ft -> {
						if (ft.getDesc() != null) {
							System.out.printf("%-50s %s\n", ft.getName() + ":" + ft.getVersion(), ft.getDesc());
						} else {
							System.out.printf("%-50s\n", ft.getName() + ":" + ft.getVersion());
						}
				});
			}
		} catch (Exception e) {
			error(e);
		}
	}
	
	@Descriptor("Creates an assembly")
	public void createAssembly(
			@Descriptor("assembly symbolic name")
			String asmbBsn, 			 
			
			@Descriptor("assembly version")
			String asmbVersion,
			
			@Descriptor("assembly description")			
			String asmbName, 
			
			@Descriptor("assembly doc url")			
			String asmbDoc, 
			
			@Descriptor("list of features.")
			String ... ftNames)	
	{
		try {
			doCreateAssembly(asmbBsn, asmbVersion, asmbName, asmbDoc, ftNames);
		} catch (Exception e) {
			error(e);
		}
	}
	
	private void doCreateAssembly(String asmbBsn, String asmbVersion, String asmbName, String asmbDoc, String... ftNames) throws Exception {
		// get list of features, sorted by name/version
		Set<Feature> features = _featureRepos.findFeatures(Feature.Type.FEATURE, true);
		if (features.isEmpty()) {
			System.out.println("No features found");
		} else {
			List<Feature> l = new ArrayList<>();
			for (String ft : ftNames) {
				String[] split = ft.split(":");
				String ftName = (split.length > 1) ? split[0] : ft;
				String ftVersion = (split.length > 1) ? split[1] : null;
				boolean found = false;

				for (Feature f : features) {
					if (f.getName().equals(ftName) || f.getAliases().contains(ftName)) {
						if (ftVersion != null && !f.getVersion().equals(ftVersion)) {
							continue;
						}
						l.add(f);
						found = true;
						break;
					}
				}
				if (!found) {
					if (ftVersion != null) {
						throw new Exception("feature not found: " + ftName + ":" + ftVersion);
					} else {
						throw new Exception("feature not found: " + ft);
					}
				}
			}
			_featureRepos.createAssembly(asmbName, asmbBsn, asmbVersion, asmbDoc, l);
		}
	}

	@Descriptor("Creates an assembly")
	public void createSnapshot(@Descriptor("assembly bsn") String bsn, @Descriptor("assembly doc url") String version, @Descriptor("assembly version") String doc) {
		try {
			doCreateSnapshot(bsn, version, doc);
		} catch (Exception e) {
			error(e);
		}
	}	

	public void doCreateSnapshot(String bsn, String version, String doc) throws Exception {
		_featureRepos.createSnapshot(bsn, version, doc);
	}

	@Descriptor("Creates a runtime")
	public void createRuntime(@Descriptor("snapshot bsn") String bsn, @Descriptor("assembly version") String version) {
		try {
			doCreateRuntime(bsn, version);
		} catch (Exception e) {
			error(e);
		}
	}
	
	public void doCreateRuntime(@Descriptor("snapshot bsn") String bsn, @Descriptor("assembly version") String version) throws Exception {
		_featureRepos.createRuntime(bsn, version);
	}

	@Descriptor("Creates a legacy runtime")
	public void createLegacyRuntime(
			@Descriptor("snapshot bsn") String bsn, 
			@Descriptor("assembly version") String version,
			@Descriptor("program name") String program,
			@Descriptor("group name") String group,
			@Descriptor("component name") String component,
			@Descriptor("instance name") String instance)			
	{							  
		try {
			doCreateLegacyRuntime(bsn, version, program, group, component, instance);			
		} catch (Exception e) {
			error(e);
		}
	}
		
	private void doCreateLegacyRuntime(
			@Descriptor("snapshot bsn") String bsn, 
			@Descriptor("assembly version") String version,
			@Descriptor("program name") String program,
			@Descriptor("group name") String group,
			@Descriptor("component name") String component,
			@Descriptor("instance name") String instance) throws Exception	
	{							  
		_featureRepos.createRuntime(bsn, version, program, group, component, instance);			
	}

	@Descriptor("Deletes a feature")
	public void deleteFeature(@Descriptor("feature type (FEATURE|ASMB|SNAPSHOT)") String type, @Descriptor("feature bsn") String bsn, @Descriptor("feature version") String version) {
		try {
			_featureRepos.delete(type, bsn, version);
		} catch (Exception e) {
			error(e);
		}
	}
	
	@Descriptor("Creates a runtime in one single command")
	public void create(				
			@Descriptor("Application name") String appName, 
			@Descriptor("Application version") String appVersion, 
			@Descriptor("Features list") String ... features)
	{
		try {
			_repos.reloadLocalObr();
			List<String> featuresList = Stream.of(features).map(feature -> stripQuot(feature))
					.collect(Collectors.toList());
			if (_info) {
				System.out.println("Creating app \"" + appName + "\" " + appVersion + " using Java " + System.getProperty("java.specification.version") + " with features: " + featuresList);
				if (_obr != null) System.out.println("OBR used: " + _obr);
			}
			String docUrl = null;
			doCreateAssembly(appName, appVersion, "Application", docUrl, featuresList.toArray(new String[featuresList.size()]));
			doCreateSnapshot(appName, appVersion, docUrl); // will create a appName.snapshot bundle
			doCreateRuntime(appName + ".snapshot", appVersion);
			if (_info) {
				System.out.println("Runtime created to " + System.getProperty("java.io.tmpdir") + File.separator + appName + "-" + appVersion + ".zip");
			}
		} catch (Exception e) {
			error(e);
		}
	}

	@Descriptor("Resolve features")
	public void resolveFeatures(				
			@Descriptor("Features list") String ... features)
	{
		try {
			_repos.reloadLocalObr();
			String tmpAsmb = "tmpasmb";
			String version = "1.0.0";
			_featureRepos.delete(Feature.Type.ASMB.toString(), tmpAsmb, version);
			List<String> featuresList = Stream.of(features).map(feature -> stripQuot(feature))
					.collect(Collectors.toList());
			if (_info) {
				System.out.println("Resolving using Java " + System.getProperty("java.specification.version") + " with features: " + featuresList);
				if (_obr != null) System.out.println("OBR used: " + _obr);
			}
			String docUrl = null;
			doCreateAssembly(tmpAsmb, version, "Application", docUrl, featuresList.toArray(new String[featuresList.size()]));
			Set<Resource> resources = _featureRepos.resolveSnapshot(tmpAsmb, version, docUrl);
			resources
				.stream()
				.sorted(Comparator.comparing(r -> FeatureRepositoryImpl.getBSN(r)))
				.forEach(r -> System.out.println("     " + FeatureRepositoryImpl.getBSN(r) + "; version=" + FeatureRepositoryImpl.getVersion(r) + ",\\"));
		} catch (Exception e) {
			error(e);
		}
	}
	
	@Descriptor("Creates a legacy runtime in one single command")
	public void createLegacy(
			@Descriptor("Application name") String appName, 
			@Descriptor("Application version") String appVersion,
			@Descriptor("Platform name") String platform,
			@Descriptor("Group name") String group,
			@Descriptor("Component name") String compName,
			@Descriptor("Instance name") String instName,
			@Descriptor("Features list") String ... features)
	{
		try {
			List<String> featuresList = Stream.of(features).map(feature -> stripQuot(feature)).collect(Collectors.toList());	
			if (_info) {
				System.out.println("Creating app \"" + appName + "\" " + appVersion + " with features: " + featuresList);
			}

			String docUrl = null;
			doCreateAssembly(appName, appVersion, "Application", docUrl, featuresList.toArray(new String[featuresList.size()]));
			doCreateSnapshot(appName, appVersion, docUrl);
			doCreateLegacyRuntime(appName + ".snapshot", appVersion, platform, group, compName, instName);
			if (_info ) {
				System.out.println("Runtime created to " + System.getProperty("java.io.tmpdir") + File.separator + appName + "-" + appVersion + ".zip");
			}
		} catch (Exception e) {
			error(e);
		}
	}
	
	public BundleRepository getBundleRepository() {
		return _repos;
	}
	
	public void setOBR(String obr) {
		_obr = obr;
	}
	
	/**
	 * Executes a gogo shell script
	 * @param cmd the path to the gogo script
	 */
	private void runScriptCommand(String cmd) {
		CommandSession session = _cmdProc.createSession(System.in, System.out, System.err);
		try {
			if (! new File(cmd).exists()) {
				System.out.println("script " + cmd + " not found");
				System.exit(1);
			}
			if (_info) {
				System.out.println("Executing script " + cmd);
			}
			session.execute("gogo:sh " + cmd);
			System.exit(0);
		} catch (Exception e) {
			error(e);
			System.exit(1);
		}
	}
	
	/**
	 * Lists all available public features.
	 */
	private void runListCommand() {
		if (_info) {
			System.out.println("Features list:");
			System.out.println();
		}
		getFeatures("FEATURE");
		System.exit(0);
	}
	
	/**
	 * Creates a runtime.
	 */
	private void runCreateCommand(String cmd) {
		String[] tokens = cmd.split("[,;]");
		if (tokens.length < 3) {
			System.out.println("wrong params for create option. Usage: create appName,appVersion,feature2,feature2,...");
			System.exit(1);
		}
		String appName = tokens[0];
		String appVersion = tokens[1];
		String[] features = new String[tokens.length - 2];
		System.arraycopy(tokens, 2, features, 0, tokens.length - 2);
		List<String> featuresList = Stream.of(features).map(feature -> stripQuot(feature)).collect(Collectors.toList());
		features = featuresList.toArray(new String[featuresList.size()]);			
		create(appName, appVersion, features);
		System.exit(0);
	}
	
	/**
	 * Creates a runtime with all bundles available in the OBR.
	 */
	private void runCreateAllCommand(String cmd) {
		String[] tokens = cmd.split("[,;]");
		if (tokens.length != 2) {
			System.out.println("params for create option. Usage: create-all appName,appVersion");
			System.exit(1);
		}
		String appName = tokens[0];
		String appVersion = tokens[1];
		try {
			// Dumps all bundles from the repository
			RequirementBuilder requirementBuilder = _repos.newRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE, "*");
			requirementBuilder.addBundleIdentityFilter();
			Set<Resource> resources = new HashSet<Resource>();
			for (Capability c : _repos.findProviders(requirementBuilder.build())) {
				resources.add(c.getResource());
			}	
			
			if (_info) {
				System.out.println("Creating runtime with all bundles found from obr (it may take a while).");
			}
						
			// Creates a snapshot bundle which will include all available bundles
			_featureRepos.createSnapshot(appName, appVersion, new ArrayList(resources));
			
			
			// Now create the corresponding runtime.
			createRuntime(appName + ".snapshot", appVersion);
			if (_info)
				System.out.println("Runtime created to " + System.getProperty("java.io.tmpdir") + File.separator + appName + "-" + appVersion + ".zip");
		} catch (Exception e) {
			error(e);
			System.exit(1);
		}
		System.exit(0);
	}
	
	/**
	 * Creates a runtime with legacy blueprint structure.
	 */
	private void createLegacyCommand(String cmd) {
		String[] tokens = cmd.split("[,;]");
		if (tokens.length < 7) {
			System.out.println("wrong params for create-legacy option. Usage: create-legacy appName,appVersion,platformName,groupName,componentName,instanceName,feature1,feature2,...");
			System.exit(1);
		}
		String appName = tokens[0];
		String appVersion = tokens[1];
		String platformName = tokens[2];
		String groupName = tokens[3];
		String componentName = tokens[4];
		String instanceName = tokens[5];
		String[] features = new String[tokens.length - 6];
		System.arraycopy(tokens, 6, features, 0, tokens.length - 6);
		List<String> featuresList = Stream.of(features).map(feature -> stripQuot(feature)).collect(Collectors.toList());
		features = featuresList.toArray(new String[featuresList.size()]);			
		createLegacy(appName, appVersion, platformName, groupName, componentName, instanceName, features);
		System.exit(0);
	}
		
	/**
	 * Adds more features into an existing runtime.
	 */
	private void doUpdateFeatures(String cmd) {
		// parse update command parameters (first param is the casr runtime path which must be updated, and other params are the features to add)
		String[] tokens = cmd.split("[,;]");
		if (tokens.length < 2) {
			System.out.println("wrong params for add option. Usage: -Dupdate.features=runtime_path,feature1,feature2,...");
			System.exit(1);
		}
		String runtimePath = tokens[0];
		String[] features = new String[tokens.length - 1];
		System.arraycopy(tokens, 1, features, 0, tokens.length - 1);
		features = Stream.of(features).map(feature -> stripQuot(feature)).toArray(String[]::new);
		
		// Execute command
		updateFeature(runtimePath, features);		
		System.exit(0);
	}
	
	@Descriptor("Adds more features into an existing runtime")
	public void updateFeature(				
			@Descriptor("Runtime path") String runtimePath,
			@Descriptor("Features list to add") String ... features) 
	{
		try {
			setInfo(false);
			UpdateFeatureCommand addCmd = new UpdateFeatureCommand(runtimePath, null, this);
			addCmd.updateFeatures(Arrays.asList(features));						
		} catch (Exception e) {
			error(e);
		}
	}

	/**
	 * Adds more features coming from a newer OBR into an existing runtime
	 */
	private void doUpdateObr(String cmd) {
		// parse update command parameters (first param is the casr runtime path which must be updated, and other params are the features to add)
		String[] tokens = cmd.split("[,;]");
		if (tokens.length < 2) {
			System.out.println("wrong params for add option. Usage: -Dupdate.obr=runtime_path,obr url[,feature1,feature2,...]");
			System.exit(1);
		}
		String runtimePath = tokens[0];
		String obr = tokens[1];
		String[] features = new String[tokens.length - 2];
		if (features.length > 0) {
			System.arraycopy(tokens, 2, features, 0, tokens.length - 2);
		}
		features = Stream.of(features).map(feature -> stripQuot(feature)).toArray(String[]::new);
		
		// Parse existing runtime
		updateOBR(runtimePath, obr, features);				
		System.exit(0);
	}

	@Descriptor("Adds more features into an existing runtime")
	public void updateOBR(				
			@Descriptor("Runtime path") String runtimePath,
			@Descriptor("OBR url") String obrURL,
			@Descriptor("Features list to add") String ... features) 
	{
		try {
			setInfo(false);
			UpdateFeatureCommand addCmd = new UpdateFeatureCommand(runtimePath, obrURL, this);
			addCmd.updateFeatures(Arrays.asList(features));						
		} catch (Exception e) {
			error(e);
		}
	}

	/**
	 * Adds more features coming from a newer OBR into an existing runtime
	 */
	private void shell(String args) {
		CommandSession session = _cmdProc.createSession(new FileInputStream(FileDescriptor.in), new FileOutputStream(FileDescriptor.out), new FileOutputStream(FileDescriptor.err));
		try {
			//session.execute("gosh --login --noshutdown");
			session.execute(args);
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String stripQuot(String feature) {
		if (feature.startsWith("\'") && feature.endsWith("\'")) {
			return feature.substring(1, feature.length()-1);
		}
		return feature;
	}
	
	private void error(Exception e) {
		Optional<String> displayStackTrace = Optional.ofNullable(_bc.getProperty("x"));
		if (displayStackTrace.isPresent()) {
			System.err.print("\nCommand failed: ");
			e.printStackTrace(System.err);
		} else {
			if (e.getClass().getName().equals("org.apache.felix.resolver.reason.ReasonException")) {
				Throwable rootCause = e;
				while (rootCause.getCause() != null) {
					rootCause = rootCause.getCause();
				}
				System.err.println(rootCause.getMessage());
				System.err.println("(Use -Dx to dump full stack traces)");
			} else {			
				System.err.println("\nCommand failed: " + e.getMessage());
				System.err.println("(Use -Dx to dump full stack traces)");
			}
		}
	}
	
	private void setInfo(boolean info) {
		_info = info;
	}

	/**
	 * Returns true if the local OBR seems to be well
	 */
	@Descriptor("Check local OBR and compare local bundles with ones already available from OBR).")
	public void checkLocalOBR()
		throws Exception 
	{		
		System.out.println("\nChecking local OBR ...\n");
		_repos.reloadLocalObr();
		
		// Check if some bundles are already provided

		String store = _bc.getProperty("org.apache.ace.obr.storage.file:fileLocation");
		File[] bundles = new File(store).listFiles((File dir, String name) -> name.endsWith(".jar"));
		if (bundles != null) {
			Set<URL> probableUselessBundles = new HashSet<>();
			for (File bundle : bundles) {
				// ignore microfeatures
				if (isFeature(bundle)) {
					continue;
				}
				
				// detect empty jars
				checkEmptyJar(bundle, probableUselessBundles);
				
				// detect bundles that are exporting packages without any versions
				checkBundleExportingPkgsWithoutVersion(bundle.toURI().toURL());
								
				// detect bundles already provided by OBR
				try {
					checkAlreadyAvailable(bundle.toURI().toURL(), probableUselessBundles);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}

			if (probableUselessBundles.size() > 0) {
				System.out.println("\n ---- The following bundles are probably useless, and could be removed (check each one): ----\n");
				for (URL u : probableUselessBundles) {
					String path =  getFile(u.getPath());
					System.out.println("rm -f " + path + " " + (path.substring(0, path.length() -  4) + ".bnd"));
				}
				System.out.println("\nWARNING: if you remove some bundles, please redo \"bndtools build\"");
				return;
			}
			
			// check for split packages (a package is said to be "split" in case multiple bundles exports the same package with the same version
			checkSplitPackagesFromLocalObr();
		}
		
		return;
	}
		
	/**
	 * Check if the same package is exported by multiple bundles.
	 * @throws MalformedURLException
	 */
//	private void checkSplitPackagesFromLocalObr() throws MalformedURLException {
//		Map<String, List<Capability>> packages = loadAllPackagesFromLocalObr();
//		
//		packages.forEach((pkg, capabilities) -> {
//			List<Version> versions = capabilities
//					.stream()
//					.filter(cap -> ! isFragment(cap))
//					.map(cap -> new Version(cap.getAttributes().get("version").toString()))
//					.collect(Collectors.toList());
//			
//			List<Version> distinctVersions = versions.stream().distinct().collect(Collectors.toList());
//			if (versions.size() != distinctVersions.size()) {				
//				List<String> urls = capabilities.stream()
//						.map(cap -> cap.getResource().getCapabilities("osgi.content").get(0).getAttributes().get("url").toString())
//						.map(url -> getFileName(url))
//						.collect(Collectors.toList());
//				
//				System.out.println("package " + pkg + " exported by multiple bundles: " + urls);
//			}
//		});
//	}
	
	private void checkSplitPackagesFromLocalObr() throws MalformedURLException {
		Map<String, List<Capability>> packages = loadAllPackagesFromLocalObr();
		packages.
			forEach((pkg, capabilities) -> {
				long exports = capabilities
					.stream()
					.filter(cap -> ! isFragment(cap))
					.collect(Collectors.counting());
				
			// a package is exported by multiple bundles
			if (exports > 1) {
				List<String> urls = capabilities.stream()
						.map(cap -> cap.getResource().getCapabilities("osgi.content").get(0).getAttributes().get("url").toString())
						.map(url -> getFileName(url))
						.collect(Collectors.toList());
				System.out.println("Please double check package " + pkg + " which is exported by multiple bundles: " + urls);
			}
		});
	}

		
	private boolean isFragment(Capability cap) {
		return "osgi.fragment".equals(cap.getResource().getCapabilities("osgi.identity").get(0).getAttributes().get("type"));
	}
	
	private Map<String, List<Capability>> loadAllPackagesFromLocalObr() throws MalformedURLException {
		String store = _bc.getProperty("org.apache.ace.obr.storage.file:fileLocation");
		URL storeUrl = new File(store).toURI().toURL();
		String storeUrlDir = storeUrl.toString();
		RequirementBuilder requirementBuilder = _repos.newRequirementBuilder("osgi.content");
		requirementBuilder.addDirective("filter", "(url=" + storeUrlDir + "*)");
		
		return _repos.findProviders(requirementBuilder.build())
			.stream()
			.flatMap(cap -> cap.getResource().getCapabilities("osgi.wiring.package").stream())
			.collect(Collectors.groupingBy(c -> c.getAttributes().get("osgi.wiring.package").toString()));
	}

	private String getFileName(String path) {
		int lastSlash = path.toString().lastIndexOf("/");
		if (lastSlash != -1) {
			path = path.substring(lastSlash+1);
		}
		return path;
	}

	@Descriptor("Resolves all bundles found from microfeature's local OBR (/tmp/microfeatures.obr/ dir).")
	public void resolveLocalOBR(
			@Descriptor("display verbose informations about resolution result")
			@Parameter(names = "-v", absentValue="false", presentValue="true")
			boolean verbose
			)
		throws Exception 
	{		
		_repos.reloadLocalObr();
		
		String store = _bc.getProperty("org.apache.ace.obr.storage.file:fileLocation");
		File[] bundles = new File(store).listFiles((File dir, String name) -> name.endsWith(".jar"));
		if (bundles != null) {			
			// resolve all bundles found from local OBR
			RequirementBuilder requirementBuilder = _repos.newRequirementBuilder("osgi.content");
			StringBuilder sb = new StringBuilder("(|");
			for (File bundle : bundles) {
				sb.append("(url=" + bundle.toURI().toURL().toString() + ")");
			}
			sb.append(")");
			requirementBuilder.addDirective("filter", sb.toString());
			List<Capability> capabilities = _repos.findProviders(requirementBuilder.build());
			Set<Resource> resources = new HashSet<>();
			for (Capability c : capabilities) {
				resources.add(c.getResource());
			}
			System.out.println("\nResolving ...");
			List<Feature> defaultFeatures = new ArrayList<Feature>();
			_featureRepos.findFeature(Feature.Type.FEATURE, "lib.log.log4j", "1.0.0").ifPresent(log4j1Feature -> defaultFeatures.add(log4j1Feature));
			_featureRepos.findFeature(Feature.Type.FEATURE, "lib.log.log4j", "2.0.0").ifPresent(log4j2Feature -> defaultFeatures.add(log4j2Feature));
			_featureRepos.checkDefaultFeatures(defaultFeatures);
			List<String> featureBlacklistFilters = new ArrayList<String>();
			defaultFeatures.stream()
				.map(defFeature -> defFeature.getAttributes(Feature.BLACKLIST_IDENTITY))
				.filter(Optional::isPresent)
				.map(blacklist -> blacklist.get().toString())
				.forEach(featureBlacklistFilters::add);
			Set<Resource> blacklist = _featureRepos.resolveBlacklistedResources(featureBlacklistFilters);
			InstallationResult result = new InstallationResult();
			Set<Resource> resolvedResources = _repos.findResolution(new ArrayList<>(resources), false, blacklist);	
			result.resources.addAll(resolvedResources);
			printResources(out, resolvedResources, "osgi.identity", verbose);
		}
	}
		
	private void checkEmptyJar(File jar, Set<URL> probableUselessBundles) throws FileNotFoundException, IOException {
		try (JarInputStream jarFile = new JarInputStream(new FileInputStream(jar))) {
			JarEntry jarEntry;
			int classes = 0;
			while (true) {
				jarEntry = jarFile.getNextJarEntry();
				if (jarEntry == null) {
					break;
				}
				if ((jarEntry.getName().endsWith(".class"))) {
					classes++;
				}
			}
			
			if (classes == 0) {
				System.out.println("\t" + getFile(jar.toURI().toURL().getPath()) + " is empty (please double check it)");
				probableUselessBundles.add(jar.toURI().toURL());
			}
		}
	}
	
	private boolean isFeature(File jar) throws FileNotFoundException, IOException {
		// ignore any microfeatures, which are empty
		try(JarFile jarFile = new JarFile(jar)) {
			Optional<String> provideCap = getHeader(jarFile, "Provide-Capability");
			return provideCap.isPresent() && provideCap.get().startsWith(Feature.NAMESPACE);
		}
		
	}

	private Optional<String> getHeader(JarFile jar, String header) {
		try {
			return jar.getManifest()
				.getMainAttributes().entrySet()
				.stream()
				.filter(entry -> entry.getKey().toString().equals(header))
				.map(entry -> entry.getValue().toString())
				.findFirst();
		} catch (IOException e) {
			throw new RuntimeException (e);
		}
	}

	private void checkBundleExportingPkgsWithoutVersion(URL url) throws Exception {
		String jarURL = url.toString();
		// find the jar resource
		RequirementBuilder requirementBuilder = _repos.newRequirementBuilder("osgi.content");
		requirementBuilder.addDirective("filter", "(url=" + jarURL + ")");
		List<Capability> capabilities = _repos.findProviders(requirementBuilder.build());
		if (capabilities.isEmpty()) {
			System.out.println("no resource found for jar " + jarURL);
			return;
		}

		// check if the resource is exporting some packages without any versions
		boolean detectedExportedPkgsWithoutVersions = false;
		for (Capability cap : capabilities.get(0).getResource().getCapabilities("osgi.wiring.package")) {
			Version v = new Version(cap.getAttributes().get("version").toString());
			if (v.toString().equals("0.0.0")) {
				detectedExportedPkgsWithoutVersions = true;
				break;
			}
		}
		if (detectedExportedPkgsWithoutVersions) {
			System.out.println("\t" + getFile(url.getPath()) + " seems to export some packages without any versions (you may consider to repackage this wrong bundle).");
		}
	}
	
	private void checkAlreadyAvailable(URL url, Set<URL> probableUselessBundles) throws Exception {
		String jarURL = url.toString();
		// find the jar resource
		RequirementBuilder requirementBuilder = _repos.newRequirementBuilder("osgi.content");
		requirementBuilder.addDirective("filter", "(url=" + jarURL + ")");
		List<Capability> capabilities = _repos.findProviders(requirementBuilder.build());
		if (capabilities.isEmpty()) {
			System.out.println("no resource found for jar " + jarURL);
			return;
		}

		// check if the resource corresponds to a well known javax.annotation api 
		boolean containsJavaxAnnotationAPI = true;
		for (Capability cap : capabilities.get(0).getResource().getCapabilities("osgi.wiring.package")) {
			String pkg = cap.getAttributes().get("osgi.wiring.package").toString();
			if (! JAVA_ANNOTATION_PKG.contains(pkg)) {
				containsJavaxAnnotationAPI = false;
				break;
			}
		}
		if (containsJavaxAnnotationAPI) {
			System.out.println("\t" + getFile(url.getPath()) + " seems to provide javax.annotation API (you can ignore it) ");
			probableUselessBundles.add(url);
			return;
		}
		
		// check if the resource corresponds to jsr 305 API (find-bugs)
		boolean containsJsr305 = true;
		for (Capability cap : capabilities.get(0).getResource().getCapabilities("osgi.wiring.package")) {
			String pkg = cap.getAttributes().get("osgi.wiring.package").toString();
			if (! JSR250_ANNOTATION_PKG.contains(pkg)) {
				containsJsr305 = false;
				break;
			}
		}
		if (containsJsr305 == false) {
			if (capabilities.get(0).getResource().getCapabilities("osgi.identity").get(0).getAttributes().get("osgi.identity").equals(JSR_305)) {
				containsJsr305 = true;
			}
		}
		if (containsJsr305) {
			System.out.println("\t" + getFile(url.getPath()) + " seems to provide jsr305 findbugs API (you should ignore it, and other bundles should import its package opionally) ");
			probableUselessBundles.add(url);
			return;
		}
		
		// check if resource corresponds to jaxb api
		boolean containsJaxbAPI = true;
		for (Capability cap : capabilities.get(0).getResource().getCapabilities("osgi.wiring.package")) {
			String pkg = cap.getAttributes().get("osgi.wiring.package").toString();
			if (! JAXB_PKG.contains(pkg)) {
				containsJaxbAPI = false;
				break;
			}
		}
		if (containsJaxbAPI) {
			System.out.println("\t" + getFile(url.getPath()) + " seems to provide JAXB API (you should ignore it) ");
			probableUselessBundles.add(url);
			return;
		}
				
		// check if resource corresponds to validation api
//		boolean containsJavaxValidation = true;
//		for (Capability cap : capabilities.get(0).getResource().getCapabilities("osgi.wiring.package")) {
//			String pkg = cap.getAttributes().get("osgi.wiring.package").toString();
//			if (! JAVAX_VALID_PKG.contains(pkg)) {
//				containsJavaxValidation = false;
//				break;
//			}
//		}
//		if (containsJavaxValidation) {
//			System.out.println("\t" + getFile(url.getPath()) + " seems to provide javax.validation (you could ignore it) ");
//			probableUselessBundles.add(url);
//			return;
//		}

		// check if the resource is already resolved from OBR
		StringBuilder filter = new StringBuilder("(|");
		for (Capability cap : capabilities.get(0).getResource().getCapabilities("osgi.wiring.package")) {
			filter.append("(&");
			String pkg = cap.getAttributes().get("osgi.wiring.package").toString();
			filter.append("(osgi.wiring.package=").append(pkg).append(")");
			Version v = new Version(cap.getAttributes().get("version").toString());
			if (! v.toString().equals("0.0.0"))	{
				filter.append("(version>=" + v.toString() + ")(!(version>=" + String.valueOf(v.getMajor()+1) + ".0.0))");
			}
			filter.append(")");			
		}
		filter.append(")");

		if (filter.toString().equals("(|)")) {
			// the bundle does not export any packages. we can't check.
			return;
		}
		FrameworkUtil.createFilter(filter.toString());
		requirementBuilder = _repos.newRequirementBuilder("osgi.wiring.package");
		requirementBuilder.addDirective("filter", filter.toString());
		capabilities = _repos.findProviders(requirementBuilder.build());
		if (capabilities.isEmpty()) {
			//out.println("No matching existing resources found for filter " + filter);
			return;
		}
		Set<Resource> resources = new HashSet<>();
		for (Capability c : capabilities) {
			resources.add(c.getResource());
		}
		
		String store = _bc.getProperty("org.apache.ace.obr.storage.file:fileLocation");
		URL storeUrl = new File(store).toURI().toURL();
		String storeUrlDir = storeUrl.toString();

		for (Resource resource : resources) {
			List<Capability> osgiContent = resource.getCapabilities("osgi.content");
			for (Capability cap : osgiContent) {
				String candidate = cap.getAttributes().get("url").toString();
				if (! candidate.startsWith(storeUrlDir)) {
					System.out.println("\t" + getFile(url.getPath()) + " already provided by " + candidate);
					probableUselessBundles.add(url);
				}
			}
		}
	}

	private String getFile(String path) {
		int lastIndexSlash = path.lastIndexOf("/");
		if (lastIndexSlash != -1) {
			return path.substring(lastIndexSlash+1);
		}
		return path;
	}

	private void printResources(PrintStream out, Collection<Resource> resources, String namespace, boolean verbose) {
		out.println("Resolution result:");
		for (Resource r : resources) {
			final List<Capability> capabilities = r.getCapabilities(namespace);
			List<Capability> osgiContent = r.getCapabilities("osgi.content");
			String resourceUrl = osgiContent.get(0).getAttributes().get("url").toString();
			Map<String, Object> nsAttrs = capabilities.iterator().next().getAttributes();
			out.println(resourceUrl);

			if (verbose) {
				out.println("---------------");
				out.println("  Capabilities:");
				final List<Capability> caps = new ArrayList<Capability>(r.getCapabilities(null));
				Collections.sort(caps, new Comparator<Capability>() {
					@SuppressWarnings("unchecked")
					@Override
					public int compare(Capability c1, Capability c2) {
						final String ns = c1.getNamespace();
						int res = ns.compareTo(c2.getNamespace());
						if (res == 0) {
							Object a1 = c1.getAttributes().get(ns);
							if (a1 instanceof Comparable) {
								res = ((Comparable<Object>) a1).compareTo(c2.getAttributes().get(ns));
							}
						}
						return res;
					}
				});
				String oldNs = null;
				for (Capability rc : caps) {
					String ns = rc.getNamespace();
					if (!ns.equals(oldNs)) {
						out.println("    Namespace: " + ns);
						oldNs = ns;
					} else {
						out.println("     --");
					}
					printMap(out, rc.getAttributes(), "       ", " = ");
					printMap(out, rc.getDirectives(), "       ", " := ");
				}
				out.println("  Requirements:");
				final List<Requirement> reqs = new ArrayList<Requirement>(r.getRequirements(null));
				Collections.sort(reqs, new Comparator<Requirement>() {
					@SuppressWarnings("unchecked")
					@Override
					public int compare(Requirement r1, Requirement r2) {
						final String ns = r1.getNamespace();
						int res = ns.compareTo(r2.getNamespace());
						if (res == 0) {
							Object a1 = r1.getAttributes().get(ns);
							if (a1 instanceof Comparable) {
								res = ((Comparable<Object>) a1).compareTo(r2.getAttributes().get(ns));
							}
						}
						return res;
					}
				});
				oldNs = null;
				for (Requirement rr : r.getRequirements(null)) {
					String ns = rr.getNamespace();
					if (!ns.equals(oldNs)) {
						out.println("    Namespace: " + ns);
						oldNs = ns;
					} else {
						out.println("     --");
					}
					printMap(out, rr.getAttributes(), "       ", " = ");
					printMap(out, rr.getDirectives(), "       ", " := ");
				}
				out.println();
			}
		}
	}

	private void printMap(PrintStream out, Map<String, ?> m, String prefix, String div) {
		for (Entry<String, ?> e : m.entrySet()) {
			out.print(prefix);
			out.print(e.getKey());
			out.print(div);
			out.println(e.getValue());
		}
	}
}
