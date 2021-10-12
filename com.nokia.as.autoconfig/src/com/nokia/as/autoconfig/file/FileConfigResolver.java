// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.file;

import static com.alcatel.as.util.config.ConfigConstants.SYSTEM_PID;
import static com.nokia.as.autoconfig.Configuration.FACTORY_ID;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alcatel.as.service.log.LogService;
import com.nokia.as.autoconfig.Activator;
import com.nokia.as.autoconfig.AutoConfigurator;
import com.nokia.as.autoconfig.Configuration;
import com.nokia.as.autoconfig.Utils;
import com.nokia.as.autoconfig.parser.CfgParser;
import com.nokia.as.autoconfig.parser.JsonParser;
import com.nokia.as.autoconfig.parser.Parser;
import com.nokia.as.autoconfig.parser.YamlParser;

import alcatel.tess.hometop.gateways.utils.Log;

public class FileConfigResolver {

    private LogService logger = Log.getLogger(AutoConfigurator.LOGGER);
    private Map<String, Configuration> configurations = new HashMap<>();
    private Configuration diff = new Configuration();
    
    private Map<String, Parser> parsers = new HashMap<>();
	
	public FileConfigResolver() {
		parsers.put("yaml", new YamlParser());
		parsers.put("json", new JsonParser());
		parsers.put("cfg", new CfgParser());
		logger.debug("Defined parsers for FileResolver: %s", parsers.keySet().stream().collect(Collectors.joining(", ")));
	}
    
    public void resolve(String directory) {
    	Configuration configuration = new Configuration();
        configurations.put(directory, configuration);
        logger.debug("Resolving directory: %s", directory);
        
        if(!new File(directory).exists()) {
            logger.warn("%s does not exist. Returning empty configuration", directory);
            return;
        }
        
        try(Stream<Path> paths = Files.walk(Paths.get(directory), 1)) {
            Map<Boolean, List<String>> files =
            paths.map(Path::toString)
            	 .filter(f -> !new File(f).isDirectory())
                 .filter(f -> isConfigurationFile(f, directory))
                 .collect(Collectors.partitioningBy(f -> filename(f).indexOf('-') == -1));
            
            //normal configuration, there is no '-' in the filename
            files.get(true).stream()
                 .map(f -> new FilePair(f))
                 .forEach(f -> configuration.config.put(f.naked, mapFileConfiguration(f.clothed, directory)));
            
            //factory configuration
            files.get(false).stream()
            	 .collect(Collectors.groupingBy(f -> filename(f).split("-")[0]))
            	 .entrySet().forEach(e -> configuration.factoryConfig.put(e.getKey(), mapFactoryConfiguration(e.getValue(), directory)));
        } catch(IOException e) {
            logger.warn("Error while loading file configurations in %s", e, directory);
            return;
        }
        
        logger.debug("Parsing patch files");
        try(Stream<Path> paths = Files.walk(Paths.get(directory), 1)) {   
            //parse patch files
            Map<Boolean, List<String>> diffFiles =
                    paths.map(Path::toString)
                    	 .filter(f -> !new File(f).isDirectory())
                         .filter(f -> "patch".equals(Utils.extension(f, false)))
                         .filter(f -> isConfigurationFile(cleanName(f, directory), directory)) //without '.patch'
                         .collect(Collectors.partitioningBy(f -> filename(f).indexOf('-') == -1));
            
            diffFiles.get(true).stream()
	                 .map(f -> new FilePair(f))
	                 .forEach(f -> diff.config.put(f.naked, mapFileConfiguration(f.clothed, directory)));
	       
            diffFiles.get(false).stream()
	       		     .collect(Collectors.groupingBy(f -> filename(f).split("-")[0]))
	       	         .entrySet().forEach(e -> diff.factoryConfig.put(e.getKey(), mapFactoryConfiguration(e.getValue(), directory)));
          
        } catch(IOException e) {
            logger.warn("Error while loading patch file in %s", e, directory);
            return;
        }
    }
    
    //A file is a configuration file if it ends with .cfg or .cfg.<ext>, where ext has a parser defined
    private boolean isConfigurationFile(String name, String directory) {
    	String extension = Utils.extension(name, false);
    	if("patch".equals(extension)) return false;
    	if("cfg".equals(extension)) return true;
    	
    	if(parsers.containsKey(extension)) {
    		name = cleanName(name, directory);
    		return name.endsWith(".cfg");
    	} else {
    		return false;
    	}
    }
     
    private String cleanName(String name, String directory) {
    	if(name.endsWith("/")) return name.substring(0, name.length() - 1);
        name = name.replaceFirst(directory + "/", "");
        String extension = Utils.extension(name, true);
        return name.substring(0, name.length() - extension.length());
    }
    
    private List<Map<String, Object>> mapFactoryConfiguration(List<String> filenames, String directory) {
        List<Map<String, Object>> factoryConfigs =
        filenames.stream()
        		 .map(f -> mapFileConfiguration(f, directory))
        		 .collect(Collectors.toList());        
        return factoryConfigs;
    }
    
    private Map<String, Object> mapFileConfiguration(String filename, String directory) {
    	
		try {
			URL url = Utils.url(filename);
			if(filename.endsWith(".patch")) filename = cleanName(filename, directory);
			//a file is either cfg or cfg.<ext>: we need to remove the cfg or it will
			//be considered as part of the pid
			String extension = Utils.extension(filename, false);
			if(!"cfg".equals(extension) && parsers.containsKey(extension)) {
				String clean = cleanName(cleanName(filename, directory), directory); //remove the cfg
				filename = clean + "." + extension;
			}
			
			Optional<Parser> p = Utils.getCorrectParser(url, parsers, logger);
	    	if(p.isPresent()) {
	    		Map<String, Object> configuration = new HashMap<>();
	    		configuration.putAll(p.get().parseFile(url));
	    		configuration.put(FACTORY_ID, cleanName(filename(filename), directory));
	    		logger.debug("Parsed %s", configuration);
	            return configuration;
	    	} else {
	    		logger.debug("No parsers found for file %s", filename);
	    	}
		} catch (MalformedURLException e) {
			logger.warn("%s is not a URL", filename);
		}
		return Collections.emptyMap();
    }
    
    private String filename(String path) {
        return Paths.get(path).getFileName().toString();
    }
    
    public Configuration config() {
        List<String> confDirs = Activator.getConfDirs();
        Configuration c = configurations.get(confDirs.get(0));
        for(int i = 1; i < confDirs.size(); i++) {
        	logger.trace("Configuration in directory %s: %s", confDirs.get(i), configurations.get(confDirs.get(i)));
        	c = Configuration.merge(c, configurations.get(confDirs.get(i)));
        	logger.trace("Result of after merge %s", c);
        }
        return c;
    }
    
    public Configuration applyPatch(Configuration config) {
    	logger.debug("Applying patch");
    	
    	//configuration
    	diff.config.entrySet().forEach(e -> {
    		String pid = e.getKey();
    		Map<String, Object> props = e.getValue();
    		Map<String, Object> origProps = config.config.get(pid);
    		applyPatch(origProps, props);
    	});
    	
    	//factory configuration
    	diff.factoryConfig.entrySet().forEach(e -> {
    		String factoryPid = e.getKey();
    		List<Map<String, Object>> listProps = e.getValue();
    		List<Map<String, Object>> origListProps = config.factoryConfig.get(factoryPid);
    		
    		listProps.forEach(props -> {
    			Object id = props.get(FACTORY_ID);
    			origListProps.forEach(origProps -> {
    				if(origProps.get(FACTORY_ID).equals(id)) {
    					applyPatch(origProps, props);
    				}
    			});
    		});
    	});
    	
    	logger.trace("Result is %s", config);
    	return config;
    }
    
    private void applyPatch(Map<String, Object> orig, Map<String, Object> diff) {
    	diff.entrySet().forEach(e -> {
			String key = e.getKey();
			if(key.startsWith("-")) { //remove the property
				key = key.substring(1);
				orig.remove(key);
			} else {
				orig.put(key, e.getValue());
			}
		});
    }
    
}


