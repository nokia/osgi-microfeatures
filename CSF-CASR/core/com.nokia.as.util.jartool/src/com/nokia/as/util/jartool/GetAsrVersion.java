package com.nokia.as.util.jartool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;

public class GetAsrVersion {
	private final static String PROVIDE_CAPABILITY = "Provide-Capability";
	private final static String FEATURE_NAMESPACE = "com.nokia.as.feature";
	private final static String FEATURE_SNAPSHOT = "SNAPSHOT";
	private final static String OBR = "X-CSF-OBR";
	private final static String REQUIREMENTS = "X-CSF-Requirements";

	public static void main(String... args) throws Exception {
		GetAsrVersion getVer = new GetAsrVersion();
		getVer.getVersion(args[0]);
	}

	private void getVersion(String jarDir) throws IOException {
		Optional<JarFile> snapshot = lookupSnapshotBundle(jarDir);
		if (!snapshot.isPresent()) {
			System.err.println("can't find version: CASR microfeatures snapshot bundle not found");
			return;
		}
		
		// get obr version
		Optional<String> obr = getHeader(snapshot.get(), OBR);
			
		if (! obr.isPresent()) {
			System.err.println("can't find version: snapshot bundle does not contain OBR used");
			return;
		}
		
		System.out.println("OBR: " + obr.get());
		displayFeatures(snapshot.get());
	}

	private Optional<JarFile> lookupSnapshotBundle(String jarDir) throws IOException {
		return Files.find(Paths.get(new File(jarDir).toURI()), Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.getFileName().toFile().getName().endsWith(".jar"))
			 .map(path -> toJarFile(path))
			 .filter(jarFile -> isSnapshot(jarFile))
			 .findFirst();
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
	
	private void displayFeatures(JarFile snapshot) throws IOException {
		Optional<String> features = getHeader(snapshot, REQUIREMENTS);
		if (features.isPresent()) {
			StringBuilder sb = new StringBuilder();
			Pattern p = Pattern.compile("\\\"([^\\\"]*)\\\"");
			Matcher m = p.matcher(features.get());
			while (m.find()) {
				String filter = m.group(1);
				Pattern p2 = Pattern.compile(".*\\(com.nokia.as.feature=(.*)\\)\\(version=(.*)\\)\\)\\)");
				Matcher m2 = p2.matcher(filter);
				if (m2.find()) {
					sb.append(m2.group(1)).append(":").append(m2.group(2)).append(",");
				}
			}
			if (sb.length() > 0) {
				sb.setLength(sb.length()-1);
				System.out.println("Features: " + sb);
			}
		}
	}
	
	private Optional<String> getHeader(JarFile jar, String header) throws IOException {
		return jar.getManifest()
				.getMainAttributes().entrySet()
				.stream()
				.filter(entry -> entry.getKey().toString().equals(header))
				.map(entry -> entry.getValue().toString())
				.findFirst();
	}

}
