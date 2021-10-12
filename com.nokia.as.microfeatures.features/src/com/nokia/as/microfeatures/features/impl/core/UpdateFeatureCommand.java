// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.features.impl.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.nokia.as.microfeatures.features.impl.common.Helper;

class UpdateFeatureCommand {

	/**
	 * Runtime path to update
	 */
	private final String _installDir;
	
	/**
	 * if not null, this obr will be used to upgrade the runtime. it can be newer than the OBR that was used to create the runtime initially.
	 */
	private final String _obrUpgrade;
	
	/**
	 * The Shell command, contains methods to create runtimes.
	 */
	private final Shell _shell;
	
	/**
	 * The temporary dir where we'll create some temporary runtimes.
	 */
	private final static File TMPDIR = new File(System.getProperty("java.io.tmpdir"), "microfeatures.tmp");

	/**
	 * Creates a new Update command.
	 * @param installDir the runtime path to update
	 * @param obr the obr to use when updating the runtime. If null, the OBR that was initially used to create the runtime is used.
	 * @param shell the shell command allowing to create some runtimes.
	 */
	UpdateFeatureCommand(String installDir, String obr, Shell shell) {
		_installDir = installDir;
		_obrUpgrade = obr;
		_shell = shell;
	}
	
	private void setOBR(String obr) throws Exception {
		_shell.getBundleRepository().setObrUrls(Arrays.asList(_shell.getLocalObr(), obr));
		_shell.getBundleRepository().reloadLocalObr();
		_shell.setOBR(obr);
	}

	void updateFeatures(List<String> features) throws Exception {
		// Get info about the existing runtime
		RuntimeInfo info = new RuntimeInfo(_installDir);
		
		// create /tmp/microfeatures.tmp directory
		setupTmpDir();
		
		// Enable the OBR that was used to initially create the runtime.
		setOBR(info.getOBR());
		
		// Make a backup of the current user runtime
		String backupDir = backupUserRuntime();
		
		// Calculate files that have been added/modified by user. To do so, we'll create a a fresh runtime with same original user features, and same runtime OBR 
		Set<String> added = new HashSet<>();
		Set<String> modified = new HashSet<>();
		System.out.println("Calculating files modified or added by user");
		getModifiedFiles(features, info, modified, added);
				
		// Now, see if we need to use a new OBR (which is newer than the one that was used to create the runtime.
		if (_obrUpgrade != null) {
			setOBR(_obrUpgrade);
		}
				
		// Create a temporary runtime with some additional features (possibly using the newer OBR).
		System.out.println("Updating runtime " + _installDir + " using obr " + ((_obrUpgrade != null) ? _obrUpgrade : info.getOBR()));
		List<String> newFeatures = info.getFeatures();
		newFeatures.addAll(features);
		_shell.create(info.getBSN(), info.getSnapshotVersion(), newFeatures.toArray(new String[0]));	
		String tmpDir = System.getProperty("java.io.tmpdir");
		File runtimeZip = new File(tmpDir + "/" + info.getBSN() + "-" + info.getSnapshotVersion() + ".zip");
		File runtime = new File(TMPDIR, info.getBSN() + "-" + info.getSnapshotVersion());
		Helper.removeDirectory(runtime);
		Helper.unzip(runtimeZip, TMPDIR);
		String userRuntimeName = new File(_installDir).getName();
		if (! runtime.getName().equals(userRuntimeName)) {
			runtime.renameTo(new File(TMPDIR, userRuntimeName));
			runtime = new File(TMPDIR, userRuntimeName);
		}
		
		// Copy new runtime into user runtime (but ignores files modified by users)
		copyFolder(runtime.toPath(), Paths.get(_installDir), modified);
		runtimeZip.delete();
		
		// from user runtime: remove all files that are not in tmp runtime, except the files contained in the added list
		removeOldFiles(runtime, added); 
				
		System.out.println("Runtime updated, modified files have been left intact: " + modified);
		System.out.println("You can do the following command to diff between the previous and new version:");
		System.out.println("meld " + backupDir + " " + _installDir);
	}
	
	private void setupTmpDir() {
		Helper.removeDirectory(TMPDIR);
		TMPDIR.mkdir();
	}

	private void removeOldFiles(File tmpRuntime, Set<String> added) throws IOException {
	    // remove all files which are not in the tmp runtime, except if they are present in the added list
		Path userRuntime = Paths.get(_installDir);
	    Files.walk(userRuntime)
	    	.filter(source -> source.toFile().isFile())
	    	.filter(source -> doNotExistsInTmpRuntime(userRuntime, source, tmpRuntime.toPath()))
	    	.filter(source -> ! added.contains(userRuntime.relativize(source).toString()))
	    	.peek(source -> System.out.println("removing " + source))
	        .forEach(source -> source.toFile().delete());	    
	}
	
	private boolean doNotExistsInTmpRuntime(Path userRuntime, Path userFile, Path tmpRuntime) {
		return ! tmpRuntime.resolve(userRuntime.relativize(userFile)).toFile().exists();
	}

	private void getModifiedFiles(List<String> features, RuntimeInfo info, Set<String> modified, Set<String> added) throws Exception {
		// Create a fresh runtime with same original user features
		String runtimeName = info.getBSN() + ".tmp";
		_shell.create(runtimeName, info.getSnapshotVersion(), info.getFeatures().toArray(new String[0]));
		String tmpDir = System.getProperty("java.io.tmpdir");
		String runtime = runtimeName + "-" + info.getSnapshotVersion();
		File runtimeZip = new File(tmpDir + "/" + runtime + ".zip");
		Helper.unzip(runtimeZip, TMPDIR);
	
		// Obtain list of files modified or added by user
		File runtimeDir = new File(TMPDIR, runtime);
		getDiff(runtimeDir, new File(_installDir), modified, added);
		Helper.removeDirectory(runtimeDir);
		runtimeZip.delete();
	}
	
	private void getDiff(File tmpRuntime, File userRuntime, Set<String> modified, Set<String> added) throws IOException {
		// collect all files from tmp runtime
		Set<Path> tmpRuntimeSet = new HashSet<>();
	    Files.walk(tmpRuntime.toPath())
	    	.filter(source -> source.toFile().isFile())
	    	.forEach(source -> tmpRuntimeSet.add(tmpRuntime.toPath().relativize(source)));

	    // scan user runtime files and detect files modified by user
	    Files.walk(userRuntime.toPath())
	    	.filter(source -> source.toFile().isFile())
	    	.filter(source -> tmpRuntimeSet.contains(userRuntime.toPath().relativize(source)))
	    	.filter(source -> isModified(tmpRuntime.toPath(), userRuntime.toPath(), source))
	    	.forEach(source -> modified.add(userRuntime.toPath().relativize(source).toString()));

	    // scan user runtime files and detect files added by user
	    Files.walk(userRuntime.toPath())
	    	.filter(source -> source.toFile().isFile())
	    	.filter(source -> !tmpRuntimeSet.contains(userRuntime.toPath().relativize(source)))
	    	.forEach(source -> added.add(userRuntime.toPath().relativize(source).toString()));
	}
	
	private boolean isModified(Path tmpRuntime, Path userRuntime, Path userRuntimeFile) {
		String cSum1 = checksum(tmpRuntime.resolve(userRuntime.relativize(userRuntimeFile)).toFile());
		String cSum2 = checksum(userRuntime.resolve(userRuntime.relativize(userRuntimeFile)).toFile());
		return ! cSum1.equals(cSum2);
	}
	
	private String checksum(File file) {
		try {
			InputStream fin = new FileInputStream(file);
			java.security.MessageDigest md5er = MessageDigest.getInstance("MD5");
			byte[] buffer = new byte[1024];
			int read;
			do {
				read = fin.read(buffer);
				if (read > 0)
					md5er.update(buffer, 0, read);
			} while (read != -1);
			fin.close();
			byte[] digest = md5er.digest();
			if (digest == null)
				return null;
			String strDigest = "0x";
			for (int i = 0; i < digest.length; i++) {
				strDigest += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1).toUpperCase();
			}
			return strDigest;
		} catch (Exception e) {
			return null;
		}
	}
	
	private String backupUserRuntime() throws IOException {
		// before making the backup, clean var/tmp
		File installDir = new File(_installDir);
		File tmp = new File(installDir, "var/tmp");
		Helper.removeDirectory(tmp);
		// now make a backup
		String userRuntimeDir = installDir.getName();
		File backupDir = new File(TMPDIR, userRuntimeDir + ".backup");
		Path src = Paths.get(_installDir);
		Path dest = backupDir.toPath();		
		System.out.println("Creating backup: " + backupDir);
		Files.walk(src).forEach(source -> copy(source, dest.resolve(src.relativize(source))));
		return backupDir.getPath();
	}
	
	private  void copyFolder(Path src, Path dest, Set<String> modified) throws IOException {
	    Files.walk(src)
	        .filter(source -> source.toFile().isFile())
	    	.filter(source -> ! modified(src.relativize(source), modified))
	        .forEach(source -> copy(source, dest.resolve(src.relativize(source))));
	}

	private boolean modified(Path path, Set<String> modified) {
		return modified.contains(path.toString());
	}

	private void copy(Path source, Path dest) {
	    try {
	        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
	    } 	    
	    catch (Exception e) {
	        throw new RuntimeException(e.getMessage(), e);
	    }
	}

}
