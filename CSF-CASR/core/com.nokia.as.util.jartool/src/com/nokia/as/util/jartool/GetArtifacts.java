package com.nokia.as.util.jartool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;

public class GetArtifacts {
	private final static String PROVIDE_CAPABILITY = "Provide-Capability";
	private final static String FEATURE_NAMESPACE = "com.nokia.as.feature";
	private final static String FEATURE_SNAPSHOT = "SNAPSHOT";
	private final static String OBR = "X-CSF-OBR";
	private final static String FEATURE_BUNDLES = "X-CSF-Bundles";
	private final static String M2_ROOTPATH = "/.m2/repository/";

    private final Map<String,String> _options;
    private final static String API = "-a";    
    private final static String BUNDLES = "-b";
    private final static String MAVEN = "-m";
    private final static String BSN = "-bsn";
    
	static class Artifact {
		final String groupId;
		final String artifactId;
		final String version;
		String bsn;
		
		Artifact(String groupId, String artifactId, String version) {
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
		}
		
		void setBsn(String bsn) {
			this.bsn = bsn;
		}
	}
	
	public static void main(String... argv) throws Exception {
		Map<String,String> options = new HashMap<>();  
		
		int index = 0;	
	    while (index < argv.length) {
	        String arg = argv[index++];
	        if (arg.equals(API)) {
	          options.put(API, "true");
	        } else if (arg.equals(BUNDLES)) {
	        	options.put(BUNDLES, argv[index++]);
	        } else if (arg.equals(MAVEN)) {
	        	options.put(MAVEN, "true");
	        } else if (arg.equals(BSN)) {
	        	options.put(BSN, "true");
	        }
	    }
	    
	    if (options.get(BUNDLES) == null) {
	    	System.err.println("Missing -b <bundles dir>");
	    	System.exit(1);
	    }
		
		GetArtifacts getArtifacts = new GetArtifacts(options);
		
		if ("true".equals(options.get(API))) {
			getArtifacts.getEmbeddedAPIs();
		} else {
			getArtifacts.getArtifacts();
		}
	}

	public GetArtifacts(Map<String, String> options) {
		_options = options;
	}
	
	private void getArtifacts() throws IOException {
		String jarDir = _options.get(BUNDLES);
		JarFile snapshot = lookupSnapshotBundle(jarDir);		
		Map<String, Artifact> artifacts = loadArtifacts(snapshot);
		artifacts.values().stream().forEach(this::displayArtifact);
	}

	private Set<String> getEmbeddableApiBsns() {
		// We assume our manifest contains a "X-CASR-EMBEDDABLE-API" header containing list of embeddable bsns
		Set<String> embeddableBsns = new HashSet<>();
		try {
			Enumeration<URL> urls = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();

				try (InputStream is = url.openStream()) {
					Manifest manifest = new Manifest(is);

					Map<String, String> headers = new HashMap<>();
					manifest.getMainAttributes().entrySet()
							.forEach(e -> headers.put(e.getKey().toString(), e.getValue().toString()));

					// the snapshot bundle now provides a header which provides all embeddable api bsns
					String embeddable = headers.get("X-CASR-EMBEDDABLE-API");
					if (embeddable != null) {
						Stream.of(embeddable.split(",")).forEach(embeddableBsns::add);
					}
				}
			}
			return embeddableBsns;
		} catch (Exception e) {
			throw new RuntimeException("Can't get list of embeddable api bsnse", e);
		}		
	}
	
	private void getEmbeddedAPIs() throws IOException {
		String jarDir = _options.get(BUNDLES);
		JarFile snapshot = lookupSnapshotBundle(jarDir);		
		Map<String, Artifact> artifacts = loadArtifacts(snapshot);
				
		// get list of bsns which are part of embedded apis
		Set<String> embeddableBsns = getEmbeddableApiBsns();
		
		// Lookup all bundles, and display each of them whose bsn matches the embeddable bsns
		Files.find(Paths.get(new File(jarDir).toURI()), Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.getFileName().toFile().getName().endsWith(".jar"))
			.map(path -> toJarFile(path))
			.filter(jarFile -> isEmbeddableApi(jarFile, embeddableBsns))
			.map(jarFile -> artifacts.get(getArtifactBasenameFile(jarFile)))
			.filter(jarFile -> jarFile != null)
			.forEach(this::displayArtifact);
	}
	
	private Object getArtifactBasenameFile(JarFile jarFile) {
		String fileName;
		int lastSlash = jarFile.getName().lastIndexOf("/");
		if (lastSlash != -1) {
			fileName =  jarFile.getName().substring(lastSlash+1);
		} else {
			fileName = jarFile.getName();
		}
		return fileName;
	}

	private boolean isEmbeddableApi(JarFile jarFile, Set<String> embeddableApis) {
		String bs = getHeader(jarFile, "Bundle-SymbolicName");
		return embeddableApis.contains(bs);
	}
	
	private JarFile lookupSnapshotBundle(String jarDir) {
		try {
			return Files.find(Paths.get(new File(jarDir).toURI()), Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.getFileName().toFile().getName().endsWith(".jar"))
				 .map(path -> toJarFile(path))
				 .filter(jarFile -> isSnapshot(jarFile))
				 .findFirst()
				 .orElseThrow(() -> new RuntimeException("snapshot bundle not found"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private JarFile toJarFile(Path path) {
		try {
			return new JarFile(path.toFile().toString());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private boolean isSnapshot(JarFile jarFile) {
		try {
			Optional<String> pc = jarFile.getManifest().getMainAttributes().entrySet().stream()
					.filter(entry -> entry.getKey().toString().equals(PROVIDE_CAPABILITY))
					.map(entry -> entry.getValue().toString()).findFirst();

			if (pc.isPresent()) {
				List<GenericMetadata> capabilities = ManifestHeaderProcessor.parseCapabilityString(pc.get());
				for (GenericMetadata cap : capabilities) {
					if (FEATURE_NAMESPACE.equals(cap.getNamespace())) {
						if (FEATURE_SNAPSHOT.equals(cap.getAttributes().get("type"))) {
							// something like Provide-Capability:
							// com.nokia.as.feature;com.nokia.as.feature="Application
							// Snapshot";type=SNAPSHOT;version=1.0.0;assembly.name=test;assembly.version=1.0.0
							return true;
						}
					}
				}
				return false;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
		
	private Map<String, Artifact> loadArtifacts(JarFile snapshot) throws IOException {
		String bundles = getHeader(snapshot, FEATURE_BUNDLES);
		return Stream.of(bundles.split(","))
		      .map(this::parseArtifact)
		      .filter(this::artifactHasBsn)
			  .collect(Collectors.toMap(artifact -> toArtifactFileName(artifact), artifact -> artifact));
	}
	
	private String toArtifactFileName(Artifact artifact) {
		return artifact.artifactId + "-" + artifact.version + ".jar";
	}
	
	private boolean artifactHasBsn(Artifact artifact) {
		String bundlesDir = _options.get(BUNDLES);
		File jar = new File(bundlesDir + "/" + artifact.artifactId + "-" + artifact.version + ".jar");
		if (! jar.exists()) {
			return false;
		}
		try (JarFile jf = new JarFile(jar)) {
			String bsn = getHeader(jf, "Bundle-SymbolicName");
			artifact.setBsn(bsn);
			return true;
		} catch (Exception e) {
			throw new RuntimeException("Can't load artifact bsn", e);
		}
	}
		
	private Artifact parseArtifact(String url) {
		String groupId = getGroupId(url);
		String artifactId = getArtifactId(url);
		String version = getVersion(url);
		return new Artifact(groupId, artifactId, version);
	}
	
	private void displayArtifact(Artifact artifact) {		
		if ("true".equals(_options.get(MAVEN))) {
			System.out.println("<dependency>");
			System.out.println("\t<groupId>" + artifact.groupId + "</groupId>");
			System.out.println("\t<artifactId>" + artifact.artifactId + "</artifactId>");
			System.out.println("\t<version>" + artifact.version + "</version>");
			System.out.println("\t<type>jar</type>");
			System.out.println("</dependency>");
		} else if ("true".equals(_options.get(BSN))) {
			System.out.println(artifact.bsn);
		} else {
			System.out.println(artifact.groupId + ":" + artifact.artifactId + ":" + artifact.version);
		}		
	}

	private int stripUrlPrefix(String url) {
		try {
			URL u = new URL(url);
			if (u.getPath().indexOf(M2_ROOTPATH) != -1) {
				// url starts with something like "file:/home/user/.m2/repository/group/artifact/1.0.0/artifact-1.0.0.jar"
				return url.indexOf(M2_ROOTPATH) + M2_ROOTPATH.length();
			} else {
				// url starts with something "https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/group/artifact/1.0.0/artifact-1.0.0.jar"
				// find index where the actual groupId is starting (group/artifact/1.0.0/artifact-1.0.0.jar) 
				String artifact = url;
				for (int i = 0; i < 4; i++) {
					int idx = artifact.indexOf("/");
					if (idx == -1) {
						throw new IllegalArgumentException("invalid obr url: " + url);
					}
					artifact = artifact.substring(idx + 1);
				}				
				return url.indexOf(artifact); // return index where artifact is starting (group/artifact/1.0.0/artifact-1.0.0.jar) 
			}
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
	}

	private String getGroupId(String url) {
		// url is like file:/home/user/.m2/repository/org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1/org.apache.servicemix.bundles.junit-4.12_1.jar
		// or https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.casr.obr/19.3.2/
		
		// let's strip the prefix (either file:/home/user/.m2/repository/ or https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/
		int artifactUrlIndex = stripUrlPrefix(url);		
		
		// Now url is like org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1/org.apache.servicemix.bundles.junit-4.12_1.jar

		// skip url start using the artifactUrlIndex
		String u = url.substring(artifactUrlIndex);
		// url is now like org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1/org.apache.servicemix.bundles.junit-4.12_1.jar

		int index = u.lastIndexOf("/");
		if (index == -1) {
			System.err.println("invalid bundle url found: " + url);
		}
		u = u.substring(0, index);
		// url is now like org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1

		index = u.lastIndexOf("/");
		if (index == -1) {
			System.err.println("invalid bundle url found: " + url);
		}
		u = u.substring(0, index);
		// url is now like org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit

		index = u.lastIndexOf("/");
		if (index == -1) {
			System.err.println("invalid bundle url found: " + url);
		}
		u = u.substring(0, index);
		// url is now like org/apache/servicemix/bundles
		
		return u.replace("/", ".");
	}
	
	private String getArtifactId(String url) {
		int artifactUrlIndex = stripUrlPrefix(url);		

		// url is like file:/home/user/.m2/repository/org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1/org.apache.servicemix.bundles.junit-4.12_1.jar

		// skip url start using the artifactUrlIndex
		String u = url.substring(artifactUrlIndex);
		// url is now like org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1/org.apache.servicemix.bundles.junit-4.12_1.jar

		int index = u.lastIndexOf("/");
		if (index == -1) {
			System.err.println("invalid bundle url found: " + url);
		}
		u = u.substring(0, index);
		// url is now like org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1
		
		index = u.lastIndexOf("/");
		if (index == -1) {
			System.err.println("invalid bundle url found: " + url);
		}
		u = u.substring(0, index);
		// url is now like org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit

		index = u.lastIndexOf("/");
		if (index == -1) {
			System.err.println("invalid bundle url found: " + url);
		}
		u = u.substring(index+1);
		// url is now like org.apache.servicemix.bundles.junit
		
		return u;
	}

	private String getVersion(String url) {
		int artifactUrlIndex = stripUrlPrefix(url);		

		// url is like file:/home/user/.m2/repository/org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1/org.apache.servicemix.bundles.junit-4.12_1.jar

		// skip url start using the artifactUrlIndex
		String u = url.substring(artifactUrlIndex);
		// url is now like org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1/org.apache.servicemix.bundles.junit-4.12_1.jar

		int index = u.lastIndexOf("/");
		if (index == -1) {
			System.err.println("invalid bundle url found: " + url);
		}
		u = u.substring(0, index);
		// url is now like org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1
		
		index = u.lastIndexOf("/");
		if (index == -1) {
			System.err.println("invalid bundle url found: " + url);
		}
		u = u.substring(index+1);
		// url is now like 4.12_1
		
		return u;
	}

	private String getHeader(JarFile jar, String header) {
		try {
			return jar.getManifest()
				.getMainAttributes().entrySet()
				.stream()
				.filter(entry -> entry.getKey().toString().equals(header))
				.map(entry -> entry.getValue().toString())
				.findFirst()
				.orElseThrow(() -> new RuntimeException("header " + header + " not found from " + jar));
		} catch (IOException e) {
			throw new RuntimeException (e);
		}
	}

}
