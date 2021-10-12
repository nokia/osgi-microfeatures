// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.transform;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.nokia.as.autoconfig.Activator;

public class FilePropertyTransformer implements Function<Map<String, Object>, Map<String, Object>>{
	
	private static final String FILEKEY = "file-";

	@Override
	public Map<String, Object> apply(Map<String, Object> t) {
		return replaceFileProperties(t);
	}
	
	private Map<String, Object> replaceFileProperties(Map<String, Object> map) {
		List<String> dirs = Activator.getConfDirs();
		if(dirs == null) dirs = Arrays.asList("");
		
		for(int i = dirs.size() - 1; i >= 0; i--) { //we look for files in reverse order
			String dir = dirs.get(i);
			
			map =  map.entrySet().stream()
					  .map(e -> replaceFileProperty(e, dir + "/", false))
					  .collect(Collectors.toMap(e -> e.getKey(), 
												e -> e.getValue()));
		}

		return map.entrySet().stream()
				  .map(e -> replaceFileProperty(e, "", true))
				  .collect(Collectors.toMap(e -> e.getKey(), 
											e -> e.getValue()));
	}
	
	private Map.Entry<String, Object> replaceFileProperty(Map.Entry<String, Object> e, String directory, boolean replace) {
		String key = e.getKey();
		if(key.startsWith(FILEKEY)) {
			if(replace) return new AbstractMap.SimpleEntry<String, Object>(key.replace(FILEKEY, ""), "");
			Optional<String> fileContent = fileToString(directory + e.getValue());
			if(fileContent.isPresent()) {
				return new AbstractMap.SimpleEntry<String, Object>(key.replace(FILEKEY, ""), fileContent.get());
			}
		}
		return e;
	}
	
	private Optional<String> fileToString(String filename) {
		try {	
			return Optional.of(new String(Files.readAllBytes(Paths.get(filename))));	
		} catch(Exception e) {
			return Optional.empty();
		}
	}

}
