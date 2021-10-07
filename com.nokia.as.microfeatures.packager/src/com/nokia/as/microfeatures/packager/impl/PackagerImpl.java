package com.nokia.as.microfeatures.packager.impl;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.osgi.framework.Constants.BUNDLE_CATEGORY;
import static org.osgi.framework.Constants.PROVIDE_CAPABILITY;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.apache.commons.compress.archivers.zip.AsiExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.alcatel.as.service.metatype.MetaData;
import com.alcatel.as.service.metatype.MetatypeParser;
import com.alcatel.as.service.metatype.PropertiesDescriptor;
import com.alcatel.as.service.metatype.PropertyDescriptor;
import com.nokia.as.microfeatures.packager.Packager;

/*
 * ISSUES:
 * ======
 *
 *    - The tool parses the "ASR-MuxHandlerDesc" Manifest header to setup the proper Agent protocols.
 *      However, someone declares the "meters" protocol which is not placed in the resulting muxhandlers
 *      property. Why?
 *          Bypassed by explicitly filtering out this protocol. 
 *      TODO: is that right? May also be as I am not processing the right Jar list...
 *
 *    - com.nokia.as.features.common.jar contains an assembly.cfg entry which holds weird properties for
 *      the "system" PID (CLUSTER_NAME=localhost and EXTERNAL_IP=127.0.0.1). localhost should probably
 *      be for "host.name" I guess. 
 *      TODO: what are the rules?
 *          Bypassed by discarding updates to the "system" PID when processing assembly.cfg files
 *
 *    - com.nokia.as.features.felix.jar holds the scripts/start.sh script which differs from the installed one:
 *          Jar line:
 *              DTLS_JAR=$(find bundles -name *com.nokia.as.dtls.provider*.jar|sort|tail -1)
 *          Installed line:
 *              DTLS_JAR=$(find bundles -name *com.nokia.as.dtls.provider-*.jar|sort|tail -1)
 *          Jar line:
 *              -Xbootclasspath/p:${DTLS_JAR} \
 *          Installed line:
 *              -Xbootclasspath/p:bundles/${DTLS_JAR} \
 *      How come??? Could be again because my Jar list is not right...
 *      TODO: Proper handling...
 */
@Component
public class PackagerImpl implements Packager {
	/** ZIP extra field to use for executable files */
	private static final int ZIP_SCRIPTS_PERMS = 0100500;
	private static final int ZIP_CFG_PERMS = 0100600;
	private static final int ZIP_BUNDLES_PERMS = 0100400;

	// osgi Capability feature namespace (like Provide-Capability: com.nokia.as.feature; com.nokia.as.feature="Common Bundles"; internal=true)
	private final static String FEATURE_NAMESPACE = "com.nokia.as.feature";

	// feature type: snapshot 
	private final static String FEATURE_SNAPSHOT = "SNAPSHOT";
	
	// Executor used to download urls concurrently
	private final static Executor _tpool = Executors.newFixedThreadPool(20);
	
	// Newline constant
	private final static String NL = System.getProperty("line.separator");
	
	// CSF delivered repository
	private final static String DELIVERED_REPO = "https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/";
	
	// well known csf candidate repos
	private final static List<String> CANDIDATES_REPO = Arrays.asList("/csf-mvn-candidates/", "/csf-mvn-inprogress/", "/csf-mvn-snapshots/", "/sandbox-mvn-candidates/");
	
	/**
	 * Tells if a jar is a feature, a bundle with an assembly, or an application jar
	 */
	enum JarType {
		FEATURE,           // The jar is a microfeature (contains Provide-Capability for a feature namespace)
		JAR_WITH_ASSEMBLY, // The jar seems to contain a META-INF/assembly.cfg, but does not provide a capability with feature namespace
		JAR                // The jar is not a feature and does not contain META-INF/assembly.cfg      
	};

	/**
	 * File permissions we use for executables. As JAR files do not maintain file
	 * permissions, we assume that each file whose name ends with ".sh" needs to be
	 * forced as an executable
	 */
	private static final Set<PosixFilePermission> execPermissions = new TreeSet<>(Arrays.asList(
			PosixFilePermission.OWNER_READ,
			PosixFilePermission.OWNER_WRITE,
			PosixFilePermission.OWNER_EXECUTE,
			PosixFilePermission.GROUP_READ,
			PosixFilePermission.GROUP_EXECUTE,
			PosixFilePermission.OTHERS_READ,
			PosixFilePermission.OTHERS_EXECUTE));

	/** Logger service dependency */
	private final static Logger logger = Logger.getLogger(PackagerImpl.class);

	/** MBean parser dependency */
	@ServiceDependency
	private MetatypeParser parser;

	/**
	 * Our activator
	 */
	@Start
	protected void activate() {
		logger.info("Service activated");
		String candidateRepo = System.getProperty("candidate.repo",null);
		if (candidateRepo != null) {
			CANDIDATES_REPO.add(candidateRepo);
		}
	}

	/**
	 * Asynchronously creates a runtime zip.
	 * 
	 * @param urls
	 *            the urls of the artifacts (bundles) that are part of the runtime.
	 * @param filter
	 *            a function which takes as argument the manifest headers of each
	 *            bundles specified in the urls parameter. If the function return
	 *            false, it means the bundle must be excluded from the target
	 *            runtime.
	 * @param params
	 *            parameters.
	 * @return the zip file represented as a CompletableFuture.
	 */
	public CompletableFuture<Path> packageRuntime(List<URL> urls, Map<Params, Object> params) {			
		return new App().handleDeployment(urls, params);
	}

	/**
	 * Our class representing our whole application
	 */
	private class App {

		/** Application name */
		private String appName;
		/** Group name */
		private String groupName;
		/** Component name */
		private String compName;
		/** Instance name */
		private String instName;
		/** Instance log banner */
		private String banner;
		/** Root output directory */
		private String target;
		/** Legacy deployment flag */
		private boolean legacy;
		/** Output directory */
		private Path outputDir;
		/** Root component directory */
		private Path rootDir;
		/** Configuration directory */
		private Path confDir;
		/** Bundle directory */
		private Path bundleDir;
		/** Temporary directory */
		private Path tmpDir;
		/** Property PIDs */
		private Map<String, Pid> pids = new HashMap<String, Pid>();
		/** Agents to handle */
		private Set<String> protocols = new TreeSet<String>();  
		/** Log4j entries to handle */
		private final List<String> _log4jEntries = new ArrayList<>();
		/** file name containing overriden properties */
		private final String ASSEMBLY_CONFIG = "META-INF/assembly.cfg";

		/**
		 * Private constructor to avoid instantiations
		 */
		private App() {
		}

		/**
		 * Handle the actual deployment for this instance
		 * 
		 * @param urls
		 *            the urls of the artifacts (bundles) that are part of the runtime.
		 * @param filter
		 *            a function which takes as argument the manifest headers of each
		 *            bundles specified in the urls parameter. If the function return
		 *            false, it means the bundle must be excluded from the target
		 *            runtime.
		 * @param params
		 *            parameters.
		 * @return the zip file represented as a CompletableFuture.
		 */
		private CompletableFuture<Path> handleDeployment(List<URL> urls, Map<Params, Object> params) {
			CompletableFuture<Path> future = CompletableFuture.supplyAsync(() -> {
				try {
					createOutputDirs(params);
					loadUrls(urls);
					deploy();
					Path path = createZipFile();
					logger.info(banner + ": Deployment done to " + path);
					return path;
				} catch (Throwable t) {
					logger.warn(banner + ": Deployment failure: " + t.getMessage(), t);
					throw new CompletionException(t);
				} finally {
					if (outputDir != null) {
						destroyDir(outputDir);
					}
				}
			});
			return future;
		}

		/**
		 * Create our output directory structure
		 * 
		 * @param params
		 *            Deployment parameters
		 */
		private void createOutputDirs(Map<Params, Object> params) throws Exception {
			legacy = (Boolean) params.getOrDefault(Params.LEGACY, false);
			appName = (String) params.getOrDefault(Params.PLATFORM, "csf");
			groupName = (String) params.getOrDefault(Params.GROUP, "group");
			compName = (String) params.getOrDefault(Params.COMPONENT, "component");
			instName = (String) params.getOrDefault(Params.INSTANCE, "instance");
			target = (String) params.get(Params.TARGET);
			if (target == null) {
				throw new RuntimeException("Target not specified");
			}
			StringBuilder buf = new StringBuilder();
			buf.append(appName).append('/').append(groupName).append('/').append(compName).append('/').append(instName)
					.append('(').append((legacy) ? "legacy" : "flat").append(')');
			banner = buf.toString();
			logger.info(banner + ": Deployment requested to " + target);
			outputDir = Files.createTempDirectory("deployer");
			rootDir = outputDir.resolve(target);
			if (legacy) {
				rootDir = rootDir.resolve("localinstances");
				rootDir = rootDir.resolve(appName);
				rootDir = rootDir.resolve(groupName);
				rootDir = rootDir.resolve(compName);
				confDir = rootDir.resolve(instName);
				confDir = confDir.resolve(".config");
			} else {
				confDir = rootDir.resolve(instName);
			}
			Files.createDirectories(rootDir);
			Files.createDirectories(confDir);
			bundleDir = rootDir.resolve("bundles");
			Files.createDirectories(bundleDir);
			tmpDir = rootDir.resolve("tmp");
			Files.createDirectories(tmpDir);
		}

		/**
		 * Load all our URLs
		 * 
		 * @param urls
		 *            The urls of the artifacts that are part of the runtime.
		 */
		private void loadUrls(List<URL> urls) throws Exception {
			//
			// Transfer all jar files to our target directory so we can operate on these
			// Jars locally
			//
			downloadUrls(urls);
			
			//
			// Now that we have our local copies, find the bundle which must be installed and move them to the
			// bundles directory, others are left where they are.
			//
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(tmpDir)) {
				for (Path path : stream) {
					if (!Files.isDirectory(path)) {
						/*
						 * TODO: Actually use the provided Predicate!!! Not done for now as I did not
						 * find a way to create this Predicate for my testing... Could not find the
						 * trick on Manifest content... For now, we just "false" from the Predicate
						 * standpoint means that the jar name starts with com.nokia.as.features.
						 */
						switch(getJarType((JarURLConnection) new URL("jar:file:" + path.toString() + "!/").openConnection())) {
						case JAR:
							Files.move(path, bundleDir.resolve(path.getFileName()));
							break;
						case JAR_WITH_ASSEMBLY:
							Files.copy(path, bundleDir.resolve(path.getFileName()));
							break;
						case FEATURE:
							break;						
						}
					}
				}
			}

			//
			// Now, process all these Jars locally. We start with the actual installed
			// bundles to ensure
			// we have our properties first
			//
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(bundleDir)) {
				for (Path path : stream) {
					if (!Files.isDirectory(path)) {
						handleInstalledJar(
								(JarURLConnection) (new URL("jar:file:" + path.toString() + "!/").openConnection()));
					}
				}
			}

			//
			// Create our system PID configuration file
			//
			pids.put("system", new Pid("system", appName, groupName, compName, instName, legacy));

			//
			// And process the jars we need to handled but do not install
			//
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(tmpDir)) {
				for (Path path : stream) {
					if (!Files.isDirectory(path)) {
						handleFeatureJar(
								(JarURLConnection) (new URL("jar:file:" + path.toString() + "!/").openConnection()));
					}
				}
			}
			destroyDir(tmpDir);
		}

		private void downloadUrls(List<URL> urls) throws Exception {
			CompletableFuture<File>[] resources = urls.stream()
					.map(url -> CompletableFuture.supplyAsync(() -> downloadUrl(url), _tpool))
					.toArray(CompletableFuture[]::new);

			CompletableFuture<Object> all = CompletableFuture.allOf(resources)
					.thenApply(v -> Stream.of(resources).map(CompletableFuture::join).collect(Collectors.toList()));

			all.get();
		}
		
		private File downloadUrl(URL url) {
			try (InputStream in = url.openStream()) {
				Path out = tmpDir.resolve(Paths.get(url.getFile()).getFileName().toString());
				Files.copy(in, out);
				return out.toFile();
			} 
			catch (FileNotFoundException e) {
				Optional<String> candidate = CANDIDATES_REPO.stream().filter(repo -> url.getPath().startsWith(repo)).findFirst();
				if (candidate.isPresent()) {
					URL deliveredURL;
					try {
						deliveredURL = new URL(DELIVERED_REPO + url.getPath().substring(candidate.get().length()));						
						logger.info("url not found from " + url + ", Trying to download from " + deliveredURL);
					} catch (MalformedURLException e2) {
						logger.error("Can't download from delivered repo: " + url, e2);
						return null;
					}
					return downloadUrl(deliveredURL);					
				}
				logger.error("Can't download " + url, e);
				if (Boolean.getBoolean("failok")) {
					return null;
				} else {
					System.exit(1);
					return null;
				}
			}
			catch (Exception e) {
				logger.error("Can't download " + url, e);
				return null;
			}
		}
		
		/**
		 * Checks if a bundle must be included in the target runtime
		 * @param openConnection the jar url connection
		 * @throws Exception 
		 */
		private JarType getJarType(JarURLConnection con) throws Exception {
			JarFile jar = con.getJarFile();
			try {
				Manifest m = jar.getManifest();
				Attributes headers = m.getMainAttributes();

				for (Map.Entry e : headers.entrySet()) {
					String key = e.getKey().toString();
					if (BUNDLE_CATEGORY.equalsIgnoreCase(key)) {
						String s = ((String) e.getValue());
						if (s == null || (s = s.trim()).length() == 0)
							continue;
						String[] cats = s.split(",");
						for (String cat : cats) {
							/*
							 * note: assemblies may NOT contain code!
							 */
							if ("assembly".equalsIgnoreCase(cat.trim())) {
								if (logger.isDebugEnabled())
									logger.debug("skipping copy for " + jar + " Bundle-Category:" + cat);
								return JarType.FEATURE; // consider this old blueprint assembly as a feature
							}
						}
					} else if (PROVIDE_CAPABILITY.equalsIgnoreCase(key)) {
						String s = ((String) e.getValue());
						if (s == null || (s = s.trim()).length() == 0) {
							continue;
						}
						// We don't copy features, except feature with type = SNAPSHOT.
						// Since the Provide-Capability header is hard to parse, we reuse a parser from
						// the aries-utils library (ManifestHeaderProcessor).

						if (s.startsWith(FEATURE_NAMESPACE)) {
							boolean snapshotFeature = false;

							List<GenericMetadata> capabilities = ManifestHeaderProcessor.parseCapabilityString(s);
							for (GenericMetadata cap : capabilities) {
								if (FEATURE_NAMESPACE.equals(cap.getNamespace())) {
									if (FEATURE_SNAPSHOT.equals(cap.getAttributes().get("type"))) {
										snapshotFeature = true;
										break;
									}
								}
							}

							if (!snapshotFeature) {
								if (logger.isDebugEnabled())
									logger.debug("skipping copy for " + jar + " Provide-Capability:" + s);
								return JarType.FEATURE; // won't be copied in target runtime
							} else {
								if (logger.isDebugEnabled())
									logger.debug("copying snapshot feature " + jar + ", Provide-Capability:" + s);
								break;
							}
						}
					}
				}
				
				// if jar is not a feature, but contains a META-INF/assembly.cfg file, consider it as a feature, but it will be copied to target runtime.
				if (jar.getEntry(ASSEMBLY_CONFIG) != null) {
					return JarType.JAR_WITH_ASSEMBLY;
				}				
			} finally {
				jar.close();
			}
			
			return JarType.JAR; // will be copied in target runtime
		}

		/**
		 * Handle a jar which is actually part of our installed bundles
		 * 
		 * @param JarURLConnection
		 *            URL connection for this Jar file
		 */
		private void handleInstalledJar(JarURLConnection conn) throws Exception {
			//
			// Start by loading properties
			//
			MetaData md = parser.loadMetadata(conn);
			if (md != null) {
				Map<String, PropertiesDescriptor> mbds = md.getProperties();
				if (mbds != null && !mbds.isEmpty()) {
					for (PropertiesDescriptor mbd : mbds.values()) {
						Pid pid = new Pid(mbd);
						pids.put(pid.getPid(), pid);
					}
				}
			}

			//
			// Handle manifest information. We look for agent MuxHandler definitions and, as
			// we may not yet
			// have the related property, store them locally so we can update them when done
			// with the scan.
			// We also record any META-INF/*.log4j files and we'll update the final loG4j pid
			// after the scan.
			//
			try (JarFile jarFile = conn.getJarFile()) {
				Manifest manifest = jarFile.getManifest();
				Attributes attrs = manifest.getMainAttributes();
				String value = attrs.getValue("ASR-MuxHandlerDesc");
				if (value != null) {
					Properties props = new Properties();
					props.load(jarFile.getInputStream(jarFile.getJarEntry(value)));
					String protocol = props.getProperty("protocol");
					if (protocol != null && !protocol.equals("meters")) { // TODO: Meters?????
						protocols.add(protocol);
					}
				}
				handleLog4jFile(jarFile); // Check if the jar contains a log4j custom file
			}
		}
		
		private void handleLog4jFile(JarFile jar) throws Exception {
			for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
				JarEntry je = e.nextElement();
				if (!je.isDirectory() && je.getName().startsWith("META-INF/") && je.getName().endsWith(".log4j")) {
					try {						
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(je)))) {
							String line;
							StringBuilder sb = new StringBuilder();
							while ((line = reader.readLine()) != null) {
								sb.append(line);
								sb.append(NL);
							}
							_log4jEntries.add(sb.toString());
						}

					} catch (Exception x) {
						logger.warn("failed to load " + je.getName() + " from " + jar.getName(), x);
					}
				}
			}
		}

		/**
		 * Handle a jar which is not actually installed but contains deployment
		 * information
		 * 
		 * @param JarURLConnection
		 *            URL connection for this Jar file
		 */
		private void handleFeatureJar(JarURLConnection conn) throws Exception {
			try (JarFile jarFile = conn.getJarFile()) {
				//
				// Look for a "X-Unpack" Manifest entry: this tells us to install additional
				// files
				//
				Manifest manifest = jarFile.getManifest();
				Attributes attrs = manifest.getMainAttributes();
				String value = attrs.getValue("X-Unpack");
				if (value != null) {
					StringTokenizer tok = new StringTokenizer(value, ",");
					while (tok.hasMoreTokens()) {
						copyFile(jarFile, tok.nextToken().trim());
					}
				}

				//
				// Now, see if we have an assembly configuration file
				//
				JarEntry entry = jarFile.getJarEntry("META-INF/assembly.cfg");
				if (entry != null) {
					byte[] arr = new byte[(int) entry.getSize()];
					try (DataInputStream in = new DataInputStream(jarFile.getInputStream(entry))) {
						in.readFully(arr);
					}
					handleAssemblyConfig(jarFile, new String(arr));
				}
				
				//
				// Check if the jar contains a log4j custom file
				//
				handleLog4jFile(jarFile); 
			}
		}

		/**
		 * Handle assembly specific configuration data
		 * 
		 * @param jarFile
		 *            Jar file we are handling
		 * @param data
		 *            Json configuration data
		 */
		private void handleAssemblyConfig(JarFile jarFile, String data) throws Exception {
			JSONObject json = new JSONObject(data);
			for (String pidName : JSONObject.getNames(json)) {
				Pid pid = pids.get(pidName);
				if (pid != null && !pidName.equals("system")) { // TODO: Proper handling? Is the source jar wrong?
					JSONObject jPid = json.getJSONObject(pidName);
					for (String propName : JSONObject.getNames(jPid)) {
						String propValue = jPid.getString(propName);
						String fileName = null;
						if (propValue.startsWith("META-INF/")) {
							JarEntry entry = jarFile.getJarEntry(propValue);
							byte[] arr = new byte[(int) entry.getSize()];
							try (DataInputStream in = new DataInputStream(jarFile.getInputStream(entry))) {
								in.readFully(arr);
							}
							fileName = propValue.substring("META-INF/".length());
							propValue = new String(arr);
						}
						Property prop = pid.properties.get(propName);
						if (prop == null) {
							prop = new Property(propName);
							pid.properties.put(propName, prop);
						}
						prop.setValue(fileName, propValue);
					}
				}
			}
		}

		/**
		 * Actual application deployment
		 */
		private void deploy() throws Exception {
			//
			// We may have had agent muxhandlers defined through the Manifest. If this is
			// the case, do the property update
			//
			if (protocols.size() != 0) {
				Pid agentPid = pids.get("agent");
				if (agentPid != null) {
					agentPid.updateProtocols(protocols);
				}
			}
			
			// 
			// We may have had custom log4j files from some jars. For each one, append it to default log4j
			// configuration.
			if (_log4jEntries.size() != 0) {
				Pid log4jPid = pids.get("log4j");
				if (log4jPid != null) {
					log4jPid.updateLog4j(_log4jEntries);
				}
			}

			//
			// Create all our PID files
			//
			for (Pid pid : pids.values()) {
				pid.store(confDir);
			}

			//
			// Create our .instance file. Not sure it is really useful for microfeatures, so
			// we silently discard an
			// eventual exception
			//
			if (legacy) {
				try {
					Files.write(rootDir.resolve(".instances"), instName.getBytes());
				} catch (Throwable t) {
				}
			}

			//
			// Create our root start.sh and stop.sh scripts
			//
			if (legacy) {
				writeLegacyScript("start.sh");
				writeLegacyScript("stop.sh");
				// in case log4j2 is used, mv p/g/c/instance/log4j2.xml to p/g/c/i/.config/log4j2.xml
				String instanceDir = String.join(File.separator, rootDir.toString(), "instance");
				File log4j2 = new File(instanceDir, "log4j2.xml");
				if (log4j2.exists()) {
					File dest = new File(String.join(File.separator, rootDir.toString(), instName, ".config"), "log4j2.xml");
					if (!log4j2.renameTo(dest)) {
						throw new RuntimeException("Can't rename " + log4j2 + " to " + dest);
					}
					// cleanup the "instance" directory
					new File(instanceDir).delete();
				}
			} else {
				// setup legacy start/stop scripts
				Path target = rootDir.resolve("start.sh");
				Files.write(target, loadResource("/META-INF/start.sh"), Charset.defaultCharset());
				Files.setPosixFilePermissions(target, execPermissions);
				target = rootDir.resolve("stop.sh");
				Files.write(target, loadResource("/META-INF/stop.sh"), Charset.defaultCharset());
				Files.setPosixFilePermissions(target, execPermissions);				
			}
		}

		/**
		 * Create the legacy root start.sh or stop.sh scripts
		 * 
		 * @param name
		 *            Script name
		 */
		private void writeLegacyScript(String name) throws Exception {
			String path = name.equals("start.sh") ? 
					"/META-INF/startLegacy.sh" : "/META-INF/stopLegacy.sh";
			List<String> lines = loadResource(path);
			Path target = rootDir.resolve(name);
			Files.write(target, lines, Charset.defaultCharset());
			Files.setPosixFilePermissions(target, execPermissions);
		}
		
		/**
		 * Load a resource file from META-INF
		 * @throws Exception 
		 */
		private List<String> loadResource(String path) throws Exception {
			List<String> lines = new ArrayList<>();
	    	  try(InputStream in = getClass().getResourceAsStream(path);
		          BufferedReader reader = new BufferedReader(new InputStreamReader(in)))
		      {
		    	  String str;
		    	  while ((str = reader.readLine()) != null) {
		    		  str= str.replace("_PF_", appName).replace("_GRP_", groupName).replace("_COMP_", compName);
		    		  lines.add(str);
		    	  }
		      }
	    	  return lines;
		}

	    /**
	     * Create our ZIP file
	     * @return Path to output ZIP file
	     */
	    private Path createZipFile() throws Exception {
			Path outputFile = Paths.get(System.getProperty("java.io.tmpdir") + File.separator + target + ".zip");
			try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(new FileOutputStream(outputFile.toFile()))) {
				Files.walkFileTree(outputDir, new SimpleFileVisitor<Path>() {
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						ZipArchiveEntry entry = new ZipArchiveEntry(outputDir.relativize(file).toString());
						if (isBundleFile(file)) {
							entry.setUnixMode(ZIP_BUNDLES_PERMS);
							AsiExtraField a = new AsiExtraField();
							a.setMode(ZIP_BUNDLES_PERMS);
							entry.setExtraFields(new ZipExtraField[] { a });
						} else if (Files.isExecutable(file)) {
							entry.setUnixMode(ZIP_SCRIPTS_PERMS);
							AsiExtraField a = new AsiExtraField();
							a.setMode(ZIP_SCRIPTS_PERMS);
							entry.setExtraFields(new ZipExtraField[] { a });
						} else if (isCfgFile(file)) {
							entry.setUnixMode(ZIP_CFG_PERMS);
							AsiExtraField a = new AsiExtraField();
							a.setMode(ZIP_CFG_PERMS);
							entry.setExtraFields(new ZipExtraField[] { a });
						}
						out.putArchiveEntry(entry);
						Files.copy(file, out);
						out.closeArchiveEntry();
						return FileVisitResult.CONTINUE;
					}

					private boolean isCfgFile(Path file) {
						Path parent = file.getParent();
						if (parent != null && parent.endsWith("instance")) {
							return true;
						}
						return false;
					}
					
					private boolean isBundleFile(Path file) {
						Path parent = file.getParent();
						if (parent != null && parent.endsWith("bundles")) {
							return true;
						}
						return false;
					}


					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						out.putArchiveEntry(new ZipArchiveEntry(outputDir.relativize(dir).toString() + "/"));
						out.closeArchiveEntry();
						return FileVisitResult.CONTINUE;
					}
				});
			}
			return outputFile;
	    }
		
		/**
		 * Copy one or more files from our jar file
		 * 
		 * @param jarFile
		 *            JarFile instance
		 * @param filespec
		 *            File or directory specification
		 */
		private void copyFile(JarFile jarFile, String filespec) throws Exception {
			boolean copyAllMatches = false;
			for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
				JarEntry entry = e.nextElement();
				String name = entry.getName();
				if (name.startsWith(filespec)) {
					if (name.equals(filespec + "/")) {
						copyAllMatches = true;
					} else if (!entry.isDirectory()) {
						Path parent = Paths.get(name).getParent();
						if (parent != null) {
							Path dir = Paths.get(rootDir.toString(), parent.toString());
							if (!Files.isDirectory(dir)) {
								Files.createDirectories(Paths.get(rootDir.toString(), parent.toString()));
							}
						}
						Path target = Paths.get(rootDir.toString(), name);
						Files.copy(jarFile.getInputStream(entry), target, REPLACE_EXISTING);
						if (name.endsWith(".sh")) {
							Files.setPosixFilePermissions(target, execPermissions);
						}
					}
				}
			}
		}

		/**
		 * Destroy the specified directory
		 * 
		 * @param path
		 *            Directory to destroy
		 */
		private void destroyDir(Path path) {
			try {
				if (Files.exists(path)) {
					Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
								throws IOException {
							return FileVisitResult.CONTINUE;
						}

						public FileVisitResult postVisitDirectory(Path file, IOException e) throws IOException {
							Files.delete(file);
							return FileVisitResult.CONTINUE;
						}

						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Files.delete(file);
							return FileVisitResult.CONTINUE;
						}

						public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
							return FileVisitResult.CONTINUE;
						}
					});
				}
			} catch (Throwable t) {
				logger.warn("Failed to destroy directory " + path + ": " + t.getMessage(), t);
			}
		}
	}

	/**
	 * Class describing a PID
	 */
	private class Pid {
		/** Pid name */
		private String pid;
		/** List of properties */
		private Map<String, Property> properties = new HashMap<String, Property>();

		/**
		 * Our constructor from property descriptors
		 * 
		 * @param pid
		 *            Pid name
		 */
		private Pid(PropertiesDescriptor mbd) {
			pid = mbd.getPid();
			for (PropertyDescriptor propDesc : mbd.getProperties().values()) {
				Property prop = new Property(propDesc);
				properties.put(prop.getName(), prop);
			}
		}

		/**
		 * Our constructor from scratch used to build the system configuration file
		 * 
		 * @param pid
		 *            PID name
		 * @param appName
		 *            Application name
		 * @param groupName
		 *            Group name
		 * @param compName
		 *            Component name
		 * @param instName
		 *            Instance name
		 * @param legacy
		 *            True for a legacy deployment
		 */
		private Pid(String pid, String appName, String groupName, String compName, String instName, boolean legacy) {
			this.pid = pid;
			properties.put("platform.name", new Property("platform.name", (legacy) ? appName : "csf"));
			properties.put("group.name", new Property("group.name", (legacy) ? groupName : "group"));
			properties.put("component.name", new Property("component.name", (legacy) ? compName : "component"));
			properties.put("instance.name", new Property("instance.name", (legacy) ? instName : "instance"));
			properties.put("host.name", new Property("host.name", "localhost"));
			properties.put("environment.CLUSTER_NAME", new Property("environment.CLUSTER_NAME", "csf"));
			properties.put("instance.pid", new Property("instance.pid", "${instance.pid}"));
			Random random = new Random(System.currentTimeMillis());
			properties.put("group.id",
					new Property("group.id", Integer.toString(random.nextInt(Integer.MAX_VALUE >> 4) << 1)));
			properties.put("instance.id",
					new Property("instance.id", Integer.toString(random.nextInt(Integer.MAX_VALUE >> 4) << 1)));
			properties.put("platform.id",
					new Property("platform.id", Integer.toString(random.nextInt(Integer.MAX_VALUE >> 4) << 1)));
			properties.put("component.id",
					new Property("component.id", Integer.toString(random.nextInt(Integer.MAX_VALUE >> 4) << 1)));
		}

		/**
		 * Retrieve the PID name
		 * 
		 * @return PID
		 */
		private String getPid() {
			return pid;
		}

		/**
		 * Update the protocols associated with this PID
		 * 
		 * @param protocols
		 *            Protocols to add
		 */
		private void updateProtocols(Set<String> protocols) {
			Property prop = properties.get("agent.muxhandlers");
			if (prop != null) {
				prop.updateProtocols(protocols);
			}
		}
		
		/**
		 * Append some custom log4j declarations to the default log4j configuration
		 * 
		 * @param log4j
		 *            Log4 entries to add
		 */
		public void updateLog4j(List<String> log4jEntries) {
			Property prop = properties.get("log4j.configuration");
			if (prop != null) {
				prop.updateLog4j(log4jEntries);
			}			
		}

		/**
		 * Write the PID file associated with this PID
		 * 
		 * @param dirName
		 *            Directory to write to
		 */
		private void store(Path dirName) throws Exception {
			List<String> content = new ArrayList<String>();
			Map<String, Property> sorted = properties.entrySet().stream()
				    .sorted(Map.Entry.comparingByKey()) 			
				    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				    		 			      (oldValue, newValue) -> oldValue, LinkedHashMap::new));
			for (Property prop : sorted.values()) {
				content.add(prop.store(dirName));
			}
			Files.write(dirName.resolve(pid + ".cfg"), content);
		}
	}

	/**
	 * Class describing a specific property
	 */
	private class Property {
		/** Property name */
		private String propName;
		/** Property value */
		private String propValue;
		/** Property description */
		private final String help;
		/** File name for file property */
		private String fileName;

		/**
		 * Our constructor from a property descriptor from an MBD
		 * 
		 * @param desc
		 *            Property descriptor
		 */
		private Property(PropertyDescriptor desc) {
			propName = desc.getName();
			propValue = desc.getValue();
			help = desc.getAttribute(PropertyDescriptor.HELP);
			if (desc.getAttribute(PropertyDescriptor.TYPE).equals(PropertyDescriptor.FILEDATA)) {
				fileName = desc.getAttribute(PropertyDescriptor.FILENAME);
			}
		}

		/**
		 * Create a new property
		 * 
		 * @param propName
		 *            Property name
		 * @param propValue
		 *            Property value
		 */
		private Property(String propName, String propValue) {
			this.propName = propName;
			this.propValue = propValue;
			this.help = null;
		}

		/**
		 * Create a new property
		 * 
		 * @param propName
		 *            Property name
		 */
		private Property(String propName) {
			this.propName = propName;
			this.help = null;
		}

		/**
		 * Retrieve the name of this property
		 * 
		 * @return Property name
		 */
		private String getName() {
			return propName;
		}

		/**
		 * Set the value of this file property
		 * 
		 * @param fileName
		 *            File name
		 * @param propValue
		 *            Property value
		 */
		private void setValue(String fileName, String propValue) {
			this.fileName = fileName;
			this.propValue = propValue;
		}

		/**
		 * Update the list of protocols. Only applicable to the agent.muxhandlers
		 * property
		 * 
		 * @param protocols
		 *            Protocols to add
		 */
		private void updateProtocols(Set<String> protocols) {
			StringBuilder buf = new StringBuilder();
			for (String protocol : protocols) {
				buf.append(protocol).append(' ');
			}
			buf.setLength(buf.length() - 1);
			propValue = buf.toString();
		}

		/**
		 * Update the log4j configuration. Only applicable to the "log4j.configuration" property.
		 * 
		 * @param log4jEntries
		 *            Log4j entries to add
		 */
		private void updateLog4j(List<String> log4jEntries) {
			StringBuilder buf = new StringBuilder();
			if (propValue != null) {
				buf.append(propValue);
			}
			for (String log4jEntry : log4jEntries) {
				buf.append(log4jEntry);
				buf.append(NL);
			}
			buf.setLength(buf.length() - 1);
			propValue = buf.toString();
		}

		/**
		 * Write this property to our PID file
		 * 
		 * @param dirName
		 *            Output directory
		 * @return Property entry line
		 */
		private String store(Path dirName) throws Exception {
			StringBuilder sb = new StringBuilder();
		
			if (help != null && help.length() > 0) {
				// add the comments in a pretty way
				splitString(help, 80).forEach((line) -> sb.append("# ").append(line).append(NL));					
			}

			if (fileName != null) {
				if (propValue.length() > 0) {
					Files.write(dirName.resolve(fileName), propValue.getBytes());
				}
				sb.append("file-").append(propName).append("=").append(fileName);
			} else {
				sb.append(propName).append("=").append(propValue);				
			}	
			if (help != null && help.length() > 0) {
				sb.append(NL);
			}
			return sb.toString();
		}
		
		/**
		 * Splits a string into fixed length rows, without breaking the words.
		 * See http://www.davismol.net/2015/02/03/java-how-to-split-a-string-into-fixed-length-rows-without-breaking-the-words/
		 */
	    public  List<String> splitString(String msg, int lineSize) {
			List<String> res = new ArrayList<>();
			Pattern p = Pattern.compile("\\b.{1," + (lineSize - 1) + "}\\b\\W?");
			Matcher m = p.matcher(msg);
			while (m.find()) {
				//System.out.println(m.group().trim());
				res.add(m.group());
			}
			return res;
		}
	}
}
