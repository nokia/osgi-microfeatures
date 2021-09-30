package com.nokia.as.features.admin;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * Helper class used to probe mime types of requested resource files
 */
public class K8sHttpContextImpl implements HttpContext {
	
	private final Bundle _bundle;
	
	K8sHttpContextImpl(Bundle b) {
		_bundle = b;
	}

	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return true;
	}

	@Override
	public URL getResource(String name) {
		return _bundle.getResource(name);
	}

	@Override
	public String getMimeType(String name) {
		Path p = Paths.get(name);
		try {
			return Files.probeContentType(p);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
