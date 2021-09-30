package com.nokia.as.microfeatures.bundlerepository.impl;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Registry;

/**
 * We are using an OBR repository implementation from bndlib, and such OBR must
 * be initialized using a bndlib "Registry". The BndRegistry class initializes such
 * registry, in which we put a bndlib "Workspace", as well a bndlib "HttpClient".
 * For the HttpClient, this is tricky: the bndlib HttpClient constructor is
 * trying to create a cache directory in ~/.bnd/urlcache directory, which is not
 * possible when running under docker CASR images. So, to fool bndlib, we create
 * an HttpClient without invoking its class constructor using Usafe tool, and we
 * manually initialize the HttpClient class fields. Notice
 * that we use the Unsafe tool using java reflection, in order to avoid to have
 * to import sun.misc.
 * TODO: We should post a change request to bndlib github in order to be able to create
 * an HttpClient with a specific urlcache path.
 */
public class BndRegistry {
	private volatile File _workspaceTmpDir;
	private volatile File _httpClientTmpDir;
	private volatile Registry _registry;

	public static BndRegistry INSTANCE = new BndRegistry();

	private BndRegistry() {
		try {
			// Load unsafe tool.
			Class<?> unsafeClass = ClassLoader.getSystemClassLoader().loadClass("sun.misc.Unsafe");
			Field singleoneInstanceField = unsafeClass.getDeclaredField("theUnsafe");
			singleoneInstanceField.setAccessible(true);
			Object unsafe = singleoneInstanceField.get(null);
			
			// Create two temp dir: one for bnd workspace, and one for httpclient
			_workspaceTmpDir = Files.createTempDirectory("microfeatures.bndworkspace").toFile();
			_httpClientTmpDir = Files.createTempDirectory("microfeatures.httpclient.cache").toFile();
			
			// Make sure this dir is removed once the jvm exits
			Runtime.getRuntime().addShutdownHook(new Thread(() -> cleanup()));	
			
			// Initialize Bnd Registry
			_registry = new BasicRegistry()
					.put(Workspace.class, new Workspace(_workspaceTmpDir))
					.put(HttpClient.class, createHttpClient(unsafe, unsafeClass));						
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void cleanup() {
		if (_workspaceTmpDir != null) {
			removeDirectory(_workspaceTmpDir);
		}
		if (_httpClientTmpDir != null) {
			removeDirectory(_httpClientTmpDir);
		}
	}

	public Registry getRegistry() {
		return _registry;
	}

	private static void removeDirectory(File dir) {
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

	private HttpClient createHttpClient(Object unsafe, Class<?> unsafeClass) throws Exception {
		// initialize static field of HttpClient
		try {
			new HttpClient();
		} catch (Exception e) {
		}

		// instantiate HttpClient without calling constructor
		Method allocateInstance = unsafeClass.getDeclaredMethod("allocateInstance", Class.class);
		HttpClient instance = (HttpClient) allocateInstance.invoke(unsafe, HttpClient.class);

		// set instance fields
		setField(instance, "proxyHandlers", new ArrayList<>());
		setField(instance, "connectionHandlers", new ArrayList<>());
		setField(instance, "passwordAuthentication", new ThreadLocal<>());
		setField(instance, "inited", false);
		setField(instance, "promiseFactory", Processor.getPromiseFactory());

		Path tmpDir = Files.createTempDirectory("bnd.urlcache");
		File tmpDirFile = tmpDir.toFile();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> removeDirectory(tmpDirFile)));
		instance.setCache(tmpDirFile);
		return instance;
	}

	private static void setField(Object instance, String field, Object value) throws Exception {
		Field declaredField = HttpClient.class.getDeclaredField(field);
		declaredField.setAccessible(true);
		declaredField.set(instance, value);
	}

}
