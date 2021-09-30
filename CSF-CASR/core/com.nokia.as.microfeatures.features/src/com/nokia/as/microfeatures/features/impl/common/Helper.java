package com.nokia.as.microfeatures.features.impl.common;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.osgi.framework.BundleContext;

public class Helper {
	
	/**
	 * Bundle SymbolicName of CASR fragment system bundle, which reexports internal jdk packages
	 */
	public final static String JRE18_BSN = "com.nokia.as.osgi.jre18";
			
	// this is the install dir used to generate the runtime (by default /tmp/runtime)
	// you can override this using -DINSTALL_DIR system property, or by specifying INSTALL_DIR in your
	// bundle context properties.
	public final static String INSTALL_DIR = "INSTALL_DIR";
	
	public static String getTmpDir() {
		// use OS tmp dir
		String tmpDir = System.getProperty("java.io.tmpdir");
		if (tmpDir == null) {
			return File.separator + "tmp";
		} else {
			// remove trailing slash, if any
			if (tmpDir.endsWith(File.separator)) {
				tmpDir = tmpDir.substring(0, tmpDir.length() - 1);
			}
			return tmpDir;
		}
	}
		
	public static String getUserDir() {
		String userDir = System.getProperty("user.dir");
		return remoteTrailingSlash(userDir);
	}
		
	public static void removeDirectory(String dir) {
		removeDirectory(new File(dir));
	}
	
	public static void removeDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			if (files != null && files.length > 0) {
				for (File aFile : files) {
					removeDirectory(aFile);
				}
			}
			dir.delete();
		} else {
			dir.delete();
		}
	}
	
	public static String getMicrofeaturesVersion(BundleContext bc) {
		// Look for bundle version from admin bundle
		String adminBundleVersion = Stream.of(bc.getBundles())
				.filter(b -> b.getSymbolicName().equals("com.nokia.as.microfeatures.admin") || b.getSymbolicName().equals("com.nokia.as.microfeatures.k8sadmin"))
				.map(b -> b.getVersion())
				.findFirst()
				.get().toString();
		// Now look for microfeatures.version from admin bundle context, and return it if it is found, else return the bundle version of the admin bundle by default.
		return Stream.of(bc.getBundles())
				.filter(b -> b.getSymbolicName().equals("com.nokia.as.microfeatures.admin") || b.getSymbolicName().equals("com.nokia.as.microfeatures.k8sadmin"))
				.map(b -> b.getBundleContext().getProperty("microfeatures.version"))
				.findFirst()
				.orElse(adminBundleVersion);		
	}
	
	private static String remoteTrailingSlash(String path) {
		if (path.endsWith(File.separator)) {
			path = path.substring(0, path.length()-1);
		}
		return path;
	}

	private static boolean isUnixOS() {
		String OS = System.getProperty("os.name").toLowerCase();
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") >= 0 || OS.indexOf("sunos") > 0 );
	}
	
    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
    	unzip(new File(zipFilePath), new File(destDirectory));
    }
    
    public static void unzip(File zipFilePath, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDir.getPath() + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }
    
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[4096];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
        if (filePath.endsWith(".sh")) {
        	File dest = new File(filePath);
        	dest.setExecutable(true); 
        }
    }

}
