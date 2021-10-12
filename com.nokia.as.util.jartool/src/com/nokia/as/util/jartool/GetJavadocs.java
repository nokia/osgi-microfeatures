// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.jartool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;

public class GetJavadocs {
	private final static String PROVIDE_CAPABILITY = "Provide-Capability";
	private final static String FEATURE_NAMESPACE = "com.nokia.as.feature";
	private final static String FEATURE_SNAPSHOT = "SNAPSHOT";
	private final static String FEATURE_BUNDLES = "X-CSF-Bundles";
	private final static String M2_ROOTPATH = "/.m2/repository/";
	private final static String CASR_API = "X-CASR-API";

	private final Map<String, String> _options;
	private final static String BUNDLES = "-b";
	private final static String OUTDIR = "-o";
	private final static String HELP = "-h";
	private Path _outdir;

	static class Artifact {
		final String rootUrl;
		final String groupId;
		final String artifactId;
		final String version;
		String bsn;

		Artifact(String rootUrl, String groupId, String artifactId, String version) {
			this.rootUrl = rootUrl;
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
		}

		void setBsn(String bsn) {
			this.bsn = bsn;
		}
	}

	private static void usage() {
		System.out.println("Usage: getArtifacts -b <bundles dir> -o <javadoc output dir>");
		System.exit(0);
	}

	public static void main(String... argv) throws Exception {
		Map<String, String> options = new HashMap<>();

		if (argv.length == 0) {
			usage();
		}
		int index = 0;
		while (index < argv.length) {
			String arg = argv[index++];
			if (arg.equals(BUNDLES)) {
				options.put(BUNDLES, argv[index++]);
			} else if (arg.equals(OUTDIR)) {
				options.put(OUTDIR, argv[index++]);
			} else {
				usage();
			}
		}

		if (options.get(BUNDLES) == null) {
			System.err.println("Missing -b <bundles dir>");
			System.exit(1);
		}
		if (options.get(OUTDIR) == null) {
			usage();
			System.exit(1);
		}

		GetJavadocs getArtifacts = new GetJavadocs(options);
		getArtifacts.downloadJavadocs();
	}

	public GetJavadocs(Map<String, String> options) throws IOException {
		_options = options;
		String javadocDir = options.get(OUTDIR);
		_outdir = Paths.get(javadocDir);
	    if(Files.notExists(_outdir)){
	    	_outdir = Files.createDirectory(Paths.get(javadocDir));
	    }
	}

	private void downloadJavadocs() throws IOException {
		String jarDir = _options.get(BUNDLES);
		JarFile snapshot = lookupSnapshotBundle(jarDir);
		Map<String, Artifact> artifacts = loadArtifacts(snapshot);

		// Lookup all bundles, and download corresponding javadoc for each bundle whose bsn matches the api bsns
		List<File> javadocs = Files.find(Paths.get(new File(jarDir).toURI()), Integer.MAX_VALUE,
				(filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.getFileName().toFile().getName().endsWith(".jar"))
			.map(path -> toJarFile(path)).filter(jarFile -> isAPI(jarFile))
			.map(jarFile -> artifacts.get(getArtifactBasenameFile(jarFile))).filter(jarFile -> jarFile != null)
			.map(this::downloadJavadoc)
			.collect(Collectors.toList());
		System.out.println("Javadoc downloaded");
	}
	
	private File downloadJavadoc(Artifact artifact) {
		try {
			URL url = new URL(artifact.rootUrl + "/" + artifact.groupId + "/" + artifact.artifactId + "/"
					+ artifact.version + "/" + artifact.artifactId + "-" + artifact.version + "-javadoc.jar");
			return downloadUrl(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private File downloadUrl(URL url) {
		Path outputDir;
		try (InputStream in = url.openStream()) {
			System.out.println("Downloading " + url);
			Path out = _outdir.resolve(Paths.get(url.getFile()).getFileName().toString());
			Files.copy(in, out);
			return out.toFile();
		} catch (Exception e) {
			System.err.println("Can't download " + url + ": " + e.toString());
			return null;
		}
	}

	private Object getArtifactBasenameFile(JarFile jarFile) {
		String fileName;
		int lastSlash = jarFile.getName().lastIndexOf("/");
		if (lastSlash != -1) {
			fileName = jarFile.getName().substring(lastSlash + 1);
		} else {
			fileName = jarFile.getName();
		}
		return fileName;
	}

	private boolean isAPI(JarFile jarFile) {
		try {
			return "true".equals(getHeader(jarFile, CASR_API));
		} catch (Exception e) {
			return false;
		}
	}

	private JarFile lookupSnapshotBundle(String jarDir) {
		try {
			return Files
					.find(Paths.get(new File(jarDir).toURI()), Integer.MAX_VALUE,
							(filePath, fileAttr) -> fileAttr.isRegularFile()
									&& filePath.getFileName().toFile().getName().endsWith(".jar"))
					.map(path -> toJarFile(path)).filter(jarFile -> isSnapshot(jarFile)).findFirst()
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
		return Stream.of(bundles.split(",")).map(this::parseArtifact).filter(this::artifactHasBsn)
				.collect(Collectors.toMap(artifact -> toArtifactFileName(artifact), artifact -> artifact));
	}

	private String toArtifactFileName(Artifact artifact) {
		return artifact.artifactId + "-" + artifact.version + ".jar";
	}

	private boolean artifactHasBsn(Artifact artifact) {
		String bundlesDir = _options.get(BUNDLES);
		File jar = new File(bundlesDir + "/" + artifact.artifactId + "-" + artifact.version + ".jar");
		if (!jar.exists()) {
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
		String rootUrl = getArtifactRootUrl(url);
		String groupId = getGroupId(url);
		String artifactId = getArtifactId(url);
		String version = getVersion(url);
		return new Artifact(rootUrl, groupId, artifactId, version);
	}

	private int stripUrlPrefix(String url) {
		try {
			URL u = new URL(url);
			if (u.getPath().indexOf(M2_ROOTPATH) != -1) {
				// url starts with something like "file:/home/user/.m2/repository/group/artifact/1.0.0/artifact-1.0.0.jar"
				return url.indexOf(M2_ROOTPATH) + M2_ROOTPATH.length();
			} else {
				// url starts with something like "https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/group/artifact/1.0.0/artifact-1.0.0.jar"
				// now find index where the actual groupId is starting (group/artifact/1.0.0/artifact-1.0.0.jar)
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
			throw new RuntimeException(e);
		}
	}

	private String getArtifactRootUrl(String url) {
		int artifactUrlIndex = stripUrlPrefix(url);
		return url.substring(0, artifactUrlIndex);
	}
	
	private String getGroupId(String url) {
		// url is like
		// file:/home/user/.m2/repository/org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1/org.apache.servicemix.bundles.junit-4.12_1.jar
		// or
		// https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/com/nokia/casr/com.nokia.casr.obr/19.3.2/

		// let's strip the prefix (either file:/home/user/.m2/repository/ or
		// https://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered-local/
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

		return u;
	}

	private String getArtifactId(String url) {
		// url is like file:/home/user/.m2/repository/org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1/org.apache.servicemix.bundles.junit-4.12_1.jar

		int artifactUrlIndex = stripUrlPrefix(url);

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
		u = u.substring(index + 1);
		// url is now like org.apache.servicemix.bundles.junit

		return u;
	}

	private String getVersion(String url) {
		int artifactUrlIndex = stripUrlPrefix(url);

		// url is like
		// file:/home/user/.m2/repository/org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1/org.apache.servicemix.bundles.junit-4.12_1.jar

		// skip url start using the artifactUrlIndex
		String u = url.substring(artifactUrlIndex);
		// url is now like
		// org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1/org.apache.servicemix.bundles.junit-4.12_1.jar

		int index = u.lastIndexOf("/");
		if (index == -1) {
			System.err.println("invalid bundle url found: " + url);
		}
		u = u.substring(0, index);
		// url is now like
		// org/apache/servicemix/bundles/org.apache.servicemix.bundles.junit/4.12_1

		index = u.lastIndexOf("/");
		if (index == -1) {
			System.err.println("invalid bundle url found: " + url);
		}
		u = u.substring(index + 1);
		// url is now like 4.12_1

		return u;
	}

	private String getHeader(JarFile jar, String header) {
		try {
			return jar.getManifest().getMainAttributes().entrySet().stream()
					.filter(entry -> entry.getKey().toString().equals(header)).map(entry -> entry.getValue().toString())
					.findFirst().orElseThrow(() -> new RuntimeException("header " + header + " not found from " + jar));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
