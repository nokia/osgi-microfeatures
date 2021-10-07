package com.nokia.as.util.jartool;

import static java.util.Comparator.comparing;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.osgi.framework.Version;

/**
 * This class allows to display allow 3rd party maven coordinates (GAV).
 * A bundle is assumed to be an external artifact if its group id does not with with casr/cjdi/cdlb names 
 * or if it is referenced in a casr bundle's CSF-Embedded header.
 */
public class GetGav {
	private final static String PROVIDE_CAPABILITY = "Provide-Capability";
	private final static String FEATURE_NAMESPACE = "com.nokia.as.feature";
	private final static String FEATURE_SNAPSHOT = "SNAPSHOT";
	private final static String FEATURE_BUNDLES = "X-CSF-Bundles";
	private final static String M2_ROOTPATH = "/.m2/repository/";
	private List<String> _casrGroupIds = Arrays.asList("com.nokia.casr", "com.nokia.cdlb", "com.nokia.cjdi");
	
    private final Map<String,String> _options;
    private final static String BUNDLES = "-b";
    private final static String MAVEN = "-m";
    
	static class Artifact {
		final String groupId;
		final String artifactId;
		final String version;
		
		Artifact(String groupId, String artifactId, String version) {
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
		}
		
		String artifact() {
			return this.artifactId;
		}
		
		String group() {
			return groupId;
		}
		
		String version() {
			return version;
		}
		
		Version osgiVersion() {
			try {
				return new Version(version);
			} catch (Exception e) {
				// we can't return the version, since this method is only used for sorting, we return a default 0.0.0 version
				return new Version("0.0.0");
			}
		}
		
		public boolean equals(Object that) {
			if (! Artifact.class.equals(that.getClass())) {
				return false;
			}
			
			Artifact thatArtifact = (Artifact) that;
			return thatArtifact.groupId.equals(groupId) && thatArtifact.artifactId.equals(artifactId) && thatArtifact.version.equals(version);
		}
		
		public int hashCode() {
			return Objects.hash(groupId, artifactId, version);
		}
	}
	
	public static void main(String... argv) throws Exception {
		Map<String,String> options = new HashMap<>();  
		
		int index = 0;	
	    while (index < argv.length) {
	        String arg = argv[index++];
	        if (arg.equals(BUNDLES)) {
	        	options.put(BUNDLES, argv[index++]);
	        } else if (arg.equals(MAVEN)) {
	        	options.put(MAVEN, "true");
	        }
	    }
	    
	    if (options.get(BUNDLES) == null) {
	    	System.err.println("Missing -b <bundles dir>");
	    	System.exit(1);
	    }
		
		GetGav getArtifacts = new GetGav(options);
		getArtifacts.get3rdPartyArtifacts();
	}

	public GetGav(Map<String, String> options) {
		_options = options;
	}
	
	private void get3rdPartyArtifacts() throws IOException {
		// First get all 3rd party libs which group id does not start with com.nokia...
		String jarDir = _options.get(BUNDLES);
		JarFile snapshot = lookupSnapshotBundle(jarDir);		
		Set<Artifact> artifacts = load3rdPartyArtifacts(snapshot);
		
		// Now get all 3rd party artifacts wrapped in CASR artifacts (using CSF-Embedded header)
		Set<Artifact> artifacts2 = getWrapped3rdPartyArtifacts();
		
		// merge artifacts and display them
		artifacts.addAll(artifacts2);
		artifacts.stream()
			.sorted(comparing(Artifact::group).thenComparing(Artifact::artifact).thenComparing(Artifact::osgiVersion))
			.forEach(this::displayArtifact);
	}
	
	private Set<Artifact> getWrapped3rdPartyArtifacts() throws IOException {						
		String jarDir = _options.get(BUNDLES);
		return Files.find(Paths.get(new File(jarDir).toURI()), Integer.MAX_VALUE, 
				(filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.getFileName().toFile().getName().endsWith(".jar"))
			.map(path -> toJarFile(path))
			.flatMap(jarFile -> getEmbeddedArtifacts(jarFile).stream())
			.collect(Collectors.toSet());
	}
	
	private Set<Artifact> getEmbeddedArtifacts(JarFile jarFile) {
		Optional<String> csfEmbedded = getHeader(jarFile, "CSF-Embedded");
		if (csfEmbedded.isPresent()) {
		        // Ignore com.nokia.csdc bundle, which contains wrong CSF-Embedded header.
		        // Todo: create a jira for CSDC team and ask them to remove the CSF-Embedded header.
		        Optional<String> bsn = getHeader(jarFile, "Bundle-SymbolicName");
			if (bsn.isPresent() && bsn.get().equals("com.nokia.csdc")) {
			    return Collections.emptySet();
			}
			String[] gavs = csfEmbedded.get().split(",");
			return Arrays.stream(gavs).map(gav -> parseGAV(gav)).collect(Collectors.toSet());
		} else {
			return Collections.emptySet();
		}
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
	
	private Set<Artifact> load3rdPartyArtifacts(JarFile snapshot) throws IOException {
		Optional<String> bundles = getHeader(snapshot, FEATURE_BUNDLES);
		if (bundles.isPresent()) {
			return Stream.of(bundles.get().split(","))
					.map(this::parseArtifact)
					.filter(artifact -> _casrGroupIds.indexOf(artifact.groupId) == -1)
					.collect(Collectors.toSet());
		} else {
			return Collections.emptySet();
		}
	
	}
			
	private Artifact parseArtifact(String url) {
		String groupId = getGroupId(url);
		String artifactId = getArtifactId(url);
		String version = getVersion(url);
		return new Artifact(groupId, artifactId, version);
	}
	
	private Artifact parseGAV(String gav) {
		String[] parts = gav.trim().split(":");		
		if (parts.length == 3) {
			return new Artifact(parts[0], parts[1], parts[2]);
		} else if (parts.length == 4) {
			// gav is like org.webjars.bower:materialize:jar:1.0.0-beta
			return new Artifact(parts[0], parts[1], parts[3]);
		} else if (parts.length == 5) {
			// gav is like wsdl4j:wsdl4j:jar:1.6.2:compile
			new Artifact(parts[0], parts[1], parts[3]);
		}
		throw new IllegalArgumentException("Invalid gav found: " + gav);
	}
	
	private void displayArtifact(Artifact artifact) {		
		if ("true".equals(_options.get(MAVEN))) {
			System.out.println("<dependency>");
			System.out.println("\t<groupId>" + artifact.groupId + "</groupId>");
			System.out.println("\t<artifactId>" + artifact.artifactId + "</artifactId>");
			System.out.println("\t<version>" + artifact.version + "</version>");
			System.out.println("\t<type>jar</type>");
			System.out.println("</dependency>");
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

}
