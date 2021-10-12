// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.cjdi.stest.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.log4j.Logger;

public class Helper {
	final static Logger _log = Logger.getLogger(Helper.class);

	public static String getResource(String resource) throws IOException {
		URL url = Helper.class.getClassLoader().getResource(resource);
		StringBuilder sb = new StringBuilder();
    	try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
    		String line;
    		while ((line = reader.readLine()) != null) {
    			sb.append(line);
    			sb.append("\n");
    		}
    		return sb.toString();
    	}
	}
}
