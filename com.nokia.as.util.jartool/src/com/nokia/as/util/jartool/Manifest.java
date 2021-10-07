package com.nokia.as.util.jartool;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.jar.JarFile;

public class Manifest {
	
	public static void main(String ... args) throws Exception {
		new Manifest().doit(args);
	}
	
	public void doit(String ... args) throws Exception {
		String jarFile = null;
		String headerName = null;
		String jarDir = null;
		
	    int index = 0;
		while (index < args.length) {
			String arg = args[index++];
			if (arg.equals("-j")) {
				jarFile = args[index++];
			} else if (arg.equals("-h")) {
				headerName = args[index++];
			} else if (arg.equals("-d")) {
				jarDir = args[index++];
			}
		}
	    
	    if (jarFile != null && headerName != null) {
	    	// dump header value from the specified jar file
	    	if (! dumpHeader(jarFile, headerName, this::dumpEntryValue)) {
	    		System.out.println("Header " + headerName + " not found.");
	    		System.exit(1);
	    	}
    		System.exit(0);
	    }
	    
	    if (jarFile != null) {
	    	dumpHeader(jarFile, headerName, this::dumpEntryKeyValue);
    		System.exit(0);
	    }
	    
	    if (jarDir != null && headerName != null) {
	    	// recursively scan jars from specified jarDir and dump header 
	    	ScanJarsAndDumpHeader(jarDir, headerName);
    		System.exit(0);
	    } 
	    
	    if (headerName != null) {
	    	// recursively scan jars from specified jarDir and dump header 
	    	ScanJarsAndDumpHeader(".", headerName);
    		System.exit(0);
	    }
	    
	    if (jarDir != null) {
	    	ScanJarsAndDumpHeader(".", null);
    		System.exit(0);
	    }	    
	    
	    usage();
		System.exit(1);
	}

	private void ScanJarsAndDumpHeader(String jarDir, String headerName) throws Exception {
		Files.find(Paths.get(new File(jarDir).toURI()), Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.getFileName().toFile().getName().endsWith(".jar"))
			 .forEach(jarFile -> {
				 dumpHeader(jarFile.toFile().toString(), headerName, this::dumpEntryJarKeyValue);
				 System.out.println("\n");
			 });
	}

	private boolean dumpHeader(String jarFile, String headerName, BiConsumer<String, Map.Entry<Object, Object>> dumpFunction) {
		try (JarFile jarf = new JarFile(jarFile)){
			AtomicBoolean found = new AtomicBoolean(false);
			jarf.getManifest()
        		.getMainAttributes()
        		.entrySet()
        		.stream()
        		.filter(entry -> headerName != null ? entry.getKey().toString().equals(headerName) : true)
        		.forEach(entry -> { found.set(true); dumpFunction.accept(jarFile, entry); });
			return found.get();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
			return false;
		}
	}
	
	private void usage() {
		System.out.println("Manifest [-d jardir] [-j jarfile] [-h headerName]");
		System.exit(1);		
	}
	
	private void dumpEntryJarKeyValue(String jarFile, Map.Entry<Object, Object> manifestHeaderEntry) {
		System.out.println(new File(jarFile).getName() + ": " + manifestHeaderEntry.getKey() + ": " + manifestHeaderEntry.getValue());
	}
	
	private void dumpEntryValue(String jarFile, Map.Entry<Object, Object> manifestHeaderEntry) {
		System.out.println(manifestHeaderEntry.getValue());
	}
	
	private void dumpEntryKeyValue(String jarFile, Map.Entry<Object, Object> manifestHeaderEntry) {
		System.out.println(manifestHeaderEntry.getKey() + ": " + manifestHeaderEntry.getValue());
	}

}
