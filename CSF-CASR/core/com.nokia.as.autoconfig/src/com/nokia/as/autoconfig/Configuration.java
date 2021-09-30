package com.nokia.as.autoconfig;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Configuration {

	public static final String FACTORY_ID = "com.nokia.as.conf.id";
	public static final String PID_ID = "pid";
	public static final String SPID_ID = "service.pid";
	public static final String FPID_ID = "service.factoryPid";
	public static final String AUTOCONF_ID = ".com.nokia.as.autoconfig";
	public final Map<String, Map<String, Object>> config;
	public final Map<String, List<Map<String, Object>>> factoryConfig;
	
	public Configuration() {
		this.config =  new HashMap<>();
		this.factoryConfig = new HashMap<>();
	}
	
	public Configuration(Map<String, Map<String, Object>> config, Map<String, List<Map<String, Object>>> factoryConfig) {
        this.config =  config;
        this.factoryConfig = factoryConfig;
    }
	
	/* Merges two configurations:
	 *     we make a copy of each configuration
	 *     we merge the singleton configurations
	 *     we merge the factory configurations
	 *  the contents of the second configuration override the first one
	 */
	public static Configuration merge(Configuration one, Configuration other) {
		Configuration oneCopy = Configuration.copyOf(one);
		Configuration otherCopy = Configuration.copyOf(other);
		
		Configuration merged = new Configuration();
		merged.config.putAll(mergeConfig(oneCopy.config, otherCopy.config));
		merged.factoryConfig.putAll(mergeFactoryConfig(oneCopy.factoryConfig, otherCopy.factoryConfig));
		return merged;
	}

	private static Map<String, Map<String, Object>> mergeConfig(Map<String, Map<String, Object>> one, 
	                                                              Map<String, Map<String, Object>> other) {
	    return mergeMaps(one, other, //merge the two maps
	                     (a, b) -> mergeMaps(a, b, //if there is collision, merge the two maps
	                                        (c, d) -> d)); //if there is collision, take the second value
	}
	
    private static Map<String, List<Map<String, Object>>> mergeFactoryConfig(Map<String, List<Map<String, Object>>> one, 
                                                                               Map<String, List<Map<String, Object>>> other) {
        
        return mergeMaps(one, other, //merge the two maps
                         Configuration::mergeFactoryLists); //if there is collision merge the two lists
        
    }

	
	// merges two maps according to a mergeFunction
	private static <K, V> Map<K, V> mergeMaps(Map<K, V> one, Map<K, V> other, BiFunction<V, V, V> mergeFunction) {
	    Map<K, V> merged = new HashMap<>(one);
	    other.forEach((key, value) -> merged.merge(key, value, mergeFunction));
	    return merged;
	}
	
	/*
	 * merge according FACTORY_ID
	 * for each map in other, if there is collision, merge the maps
	 * take all other values
	 */
	private static List<Map<String, Object>> mergeFactoryLists(List<Map<String, Object>> one, List<Map<String, Object>> other) {
	    
	    List<Map<String, Object>> merged = new ArrayList<>();
	    
	    //add all maps unique to the first map
	    merged.addAll(one.stream()
	                     .filter(m -> whoContainsFactoryId(other, m.get(FACTORY_ID)).isEmpty())
	                     .collect(Collectors.toList()));
	    
	    Map<Boolean, List<AbstractMap.SimpleEntry<Map<String, Object>, Map<String, Object>>>> collisionSplit =  
	            one.stream()
	                .map(m -> new AbstractMap.SimpleEntry<>(m, whoContainsFactoryId(other, m.get(FACTORY_ID))))
	                .collect(Collectors.partitioningBy(se -> se.getValue().isEmpty()));
	    
	    //all maps unique to the second map
	    merged.addAll(collisionSplit.get(true).stream()
	                                .map(Map.Entry::getKey)
	                                .collect(Collectors.toList()));
	    
	    //merge the conflicts
	    merged.addAll(collisionSplit.get(false).stream()
	                                .map(se -> mergeMaps(se.getKey(), se.getValue(), (a, b) -> b))
	                                .collect(Collectors.toList()));
	                  
	    return merged;
	}
	
	public ConfigurationDelta getDelta(Configuration previous) {
		return new ConfigurationDelta(previous, this);
	}
	
	//clones a configuration
	public static Configuration copyOf(Configuration original) {
		Configuration copy = new Configuration();
		
		//clone the singleton configuration
		original.config.forEach((k, v) -> copy.config.put(k, new HashMap<>(v)));
		
		//clone the factory configuration
		original.factoryConfig.forEach((k, v) -> {
			List<Map<String, Object>> l = new ArrayList<>();
			v.forEach(prev -> l.add(new HashMap<>(prev)));
			copy.factoryConfig.put(k, l);
		});
		
		return copy;
	}
	
	public static boolean isPid(String key) {
        return key.equals(PID_ID) || key.equals(SPID_ID) || key.equals(FPID_ID);
    }

    // looks for get(FACTORY_ID) == id
    public static boolean containsFactoryId(List<Map<String, Object>> list, Object id) {
        return list.stream()
                    .map(m -> m.get(FACTORY_ID))
                    .anyMatch(v -> v.equals(id));
    }

    public static Map<String, Object> whoContainsFactoryId(List<Map<String, Object>> list, Object id) {
        return list.stream()
                    .filter(m -> id.equals(m.get(FACTORY_ID)))
                    .findFirst().orElseGet(HashMap::new);
    }

	public boolean isEmpty() {
		return config.isEmpty() && factoryConfig.isEmpty();
	}

	@Override
	public String toString() {
		return "Configuration [config=" + config + System.lineSeparator() + "factoryConfig=" + factoryConfig + "]";
	}
	
	@Override
	public boolean equals(Object other) {
	    if(other ==  null || !(other instanceof Configuration)) return false;
	    else {
	        Configuration otherConfig = (Configuration) other;
	        return this.config.equals(otherConfig.config) && this.factoryConfig.equals(otherConfig.factoryConfig); 
	    }
	}
	
	@Override
	public int hashCode() {
	    return Objects.hash(this.config, this.factoryConfig);
	}
}
