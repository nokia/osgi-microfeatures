package com.nokia.as.microfeatures.features.impl.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;

/**
 * Obtain informations about an existing CASR runtime.
 * The class allows to get the OBR url, the installed features, etc ...
 */
class RuntimeInfo {
	private final static String PROVIDE_CAPABILITY = "Provide-Capability";
	private final static String FEATURE_NAMESPACE = "com.nokia.as.feature";
	private final static String FEATURE_SNAPSHOT = "SNAPSHOT";
	private final static String OBR = "X-CSF-OBR";
	private final static String REQUIREMENTS = "X-CSF-Requirements";

	/**
	 * CASR runtime installation directory.
	 */
	private final String _installDir;
	
	/**
	 * OBR url used to generate the existing CASR runtime
	 */
	private final String _obr;
	
	/**
	 * Features already installed in existing CASR runtime
	 */
	private final List<String> _features;
	
	/**
	 * Bundle symbolic name of the snapshot bundle.
	 */
	private final String _bsn;
	
	/**
	 * snapshot file name
	 */
	private final String _snapshotPath;
	
	/**
	 * snapshot version
	 */
	private final String _version;
	
	/**
	 * Creates a new RuntimeInfo class
	 * @param installDir the path to the existing CASR installation
	 * @throws IOException 
	 */
	RuntimeInfo(String installDir) throws IOException {
		File dir = new File(installDir);
		if (! dir.exists()) {
			throw new IOException ("runtime " + installDir + " does not exist");
		}
		
		_installDir = installDir;		
		
		Optional<JarFile> snapshot = lookupSnapshotBundle();
		if (!snapshot.isPresent()) {
			throw new IOException("CASR microfeatures snapshot bundle not found from " + _installDir);
		}
		_features = getFeatures(snapshot.get());
		
		Optional<String> obr = getHeader(snapshot.get(), OBR);
		if (! obr.isPresent()) {
			throw new IOException("snapshot bundle does not contain OBR header");
		}
		_obr = obr.get();
		
		Optional<String> bsn = getHeader(snapshot.get(), "Bundle-SymbolicName");
		if (! bsn.isPresent()) {
			throw new IOException("snapshot bundle does not contain Bundle-SymbolicName header");
		}
		String bsymb = bsn.get();
		int index = bsymb.lastIndexOf(".snapshot");
		if (index != -1) {
			bsymb = bsymb.substring(0, index);
		}
		_bsn = bsymb;
		
		_snapshotPath = snapshot.get().getName();	
		
		Optional<String> version = getHeader(snapshot.get(), "Bundle-Version");
		if (version == null) {
			throw new IOException("snapshot bundle does not contain Bundle-Version header");
		}
		_version = version.get();
	}
	
	String getSnapshotPath() {
		return _snapshotPath;
	}
	
	String getBSN() {
		return _bsn;
	}
	
	String getOBR() {
		return _obr;
	}
	
	String getSnapshotVersion() {
		return _version;
	}
		
	List<String> getFeatures() {
		return _features;
	}
		
	private Optional<JarFile> lookupSnapshotBundle() throws IOException {
		return Files.find(Paths.get(new File(_installDir + File.separator + "bundles").toURI()), 
				Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.getFileName().toFile().getName().endsWith(".jar"))
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
			throw new RuntimeException(e);
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
	
	private List<String> getFeatures(JarFile snapshot) throws IOException {
		final List<String> result = new ArrayList<>();
		Optional<String> features = getHeader(snapshot, REQUIREMENTS);
		if (features.isPresent()) {
			Pattern p = Pattern.compile("\\\"([^\\\"]*)\\\"");
			Matcher m = p.matcher(features.get());
			while (m.find()) {
				String filter = m.group(1);
				Pattern p2 = Pattern.compile(".*\\(com.nokia.as.feature=(.*)\\)\\(version=(.*)\\)\\)\\)");
				Matcher m2 = p2.matcher(filter);
				if (m2.find()) {
					StringBuilder sb = new StringBuilder();
					sb.append(m2.group(1)).append(":").append(m2.group(2));
					result.add(sb.toString());
				}
			}
			return result;
		} else {
			throw new IOException("Missing " + REQUIREMENTS + " header from snapshot bundle");
		}
	}

}
