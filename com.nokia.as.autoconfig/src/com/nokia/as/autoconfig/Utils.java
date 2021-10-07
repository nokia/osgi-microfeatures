package com.nokia.as.autoconfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import org.osgi.service.cm.ConfigurationAdmin;

import com.alcatel.as.service.log.LogService;
import com.nokia.as.autoconfig.parser.Parser;

public class Utils {
    
    private Utils() { }
    
    public static Properties getSystemProperties() {
        return System.getProperties();
    }
    
    public static Map<String, String> getEnvProperties() {
        return System.getenv();
    }
    
    public static String getSystemProperty(String key) {
        return System.getProperty(key);
    }
    
    public static String getEnvProperty(String key) {
        return System.getenv(key);
    }
    
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> newMap(Object... vars) {
        assert(vars.length % 2 == 0);
        Map<K, V> map = new HashMap<>();
        for(int i = 0; i < vars.length; i += 2) {
            map.put((K) vars[i], (V) vars[i + 1]); 
        }
        return map;
    }
    
    public static URL url(String filename) throws MalformedURLException {
    	return Paths.get(filename).toUri().toURL();
    }
    
    public static String extension(String name, boolean withDot) {
    	return name.substring(name.lastIndexOf(".") + (withDot ? 0 : 1));
    }
    
    public static Configuration getConfigFromConfigAdmin(ConfigurationAdmin configAdmin, Consumer<Throwable> exception) {
        Configuration previousConfig = new Configuration();
        
        try {
            org.osgi.service.cm.Configuration[] configs = configAdmin.listConfigurations(null);
            if(configs == null) return previousConfig;
            for(org.osgi.service.cm.Configuration conf : configs) {
                String factoryPid = conf.getFactoryPid();
                if(factoryPid == null) {
                    previousConfig.config.put(conf.getPid(), mapFromDictionary(conf.getProperties()));
                } else {
                    List<Map<String, Object>> fConf = previousConfig.factoryConfig.get(factoryPid);
                    if(fConf == null) previousConfig.factoryConfig.put(factoryPid, new ArrayList<>());
                    fConf = previousConfig.factoryConfig.get(factoryPid);
                    Map<String, Object> props = mapFromDictionary(conf.getProperties());
                    fConf.add(props);
                }
            }
        } catch (Exception e) {
            exception.accept(e);
        }       
        return previousConfig;
    }
    
    public static <T, U> Dictionary<T, U> dictionaryFromMap(Map<T, U> source) {
        Dictionary<T, U> dictionary = new Hashtable<>();
        source.entrySet().stream().forEach(e -> dictionary.put(e.getKey(), e.getValue()));
        return dictionary;
    }
    
    public static <T, U> Map<T, U> mapFromDictionary(Dictionary<T, U> source) {
        Map<T, U> map = new HashMap<>();
        Collections.list(source.keys()).forEach(e -> map.put(e, source.get(e)));
        return map;
    }
    
    public static Optional<Parser> getCorrectParser(URL url, Map<String, Parser> parsers, LogService logger) {
    	
    	try {
    		if(url.getFile().endsWith(".patch"))
    			url = new URL(url.toExternalForm().substring(0, url.toExternalForm().length() - 6));
    	} catch(Exception e ) {
    		logger.warn("Error while stripping patch extensions from %s", url);
    	}
    	
		String filename = url.getFile();
		logger.debug("Determining extension for file %s", filename);
		String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
		logger.debug("Extension is %s", extension);
		logger.debug("Fetching parser");
		return Optional.ofNullable(parsers.getOrDefault(extension, null));
	}

}
