package com.alcatel.as.service.bundleinstaller.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;

/**
 * Class used to load custom bundle start levels. We assume that this class is part of a bundle which contains 
 * a META-INF/startlevel.txt file,
 */
public class StartLevels {
    private final static int DEFAULT_START_LEVEL = 25;
    private final static String BUNDLE_START_LEVEL = "Bundle-StartLevel";

    /**
     * Specific Bundle-SymbolicName start levels that are optionally load from a config file
     */
    private final Map<String, Integer> _startLevels = new HashMap<>();
	
	public synchronized int getStartLevel(String bsn, String legacyStartLevel) {
		// if the bsn is configured in the startlevel.txt, return it's configured level
		if (_startLevels.containsKey(bsn)) {
			return _startLevels.getOrDefault(bsn, DEFAULT_START_LEVEL);
		} else {
			// bsn not configured in startlevel.txt, if a legacy start level is defined in the bundle, return it, else return default start level
			if (legacyStartLevel != null) {
				try {
					return Integer.parseInt(legacyStartLevel);
				} catch (NumberFormatException e) {
                	System.out.println("Found invalid Bundle-StartLevel header from bundle : " + bsn + ": " + legacyStartLevel);
				}
			}
			return DEFAULT_START_LEVEL;			
		}
	}
	
	public synchronized int getStartLevel(Bundle b) {
		int sl = DEFAULT_START_LEVEL;
		String bsn = b.getSymbolicName();
		if (_startLevels.containsKey(bsn)) {
			// bsn is found from startlevel property file, return the start level configured in the property file
	        sl = _startLevels.getOrDefault(bsn, DEFAULT_START_LEVEL);
		} else {
			// bsn not found from startlevel property file, see if the bundle contains a (legacy) specific Bundle-StartLevel header
            Dictionary<String, String> headers = b.getHeaders();
            if (headers != null) {
                String bsl = headers.get(BUNDLE_START_LEVEL);
                if (bsl != null) {
                    try {
                        sl = Integer.parseInt(bsl.trim());
                    } catch (NumberFormatException e) {
                    	System.out.println("Found invalid Bundle-StartLevel header from bundle : " + bsn + "/" + b.getVersion() + ": " + bsl);
                    }
                }	
            }
		}
		return sl;
    }
	
	public synchronized void load(InputStream in) throws IOException {
		_startLevels.clear();
		if (in == null) {
			return;
		}
		Properties props = new Properties();
		try (BufferedInputStream bin = new BufferedInputStream(in)) {
			props.load(bin);
			for (Map.Entry<Object, Object> entry : props.entrySet()) {
				String bsn = entry.getKey().toString().trim();
				Integer startLevel = Integer.valueOf(entry.getValue().toString().trim());
				_startLevels.put(bsn, startLevel);
			}
		}
	}
}
