package com.nokia.as.osgi.launcher.impl;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MapUtils {

	/**
     * Converts a map to a dictionary
     * @param source = the source map
     * @return a dictionary with the contents of the map
     */
    public static <T, U> Dictionary<T, U> mapToDictionary(Map<T, U> source) {
    	Objects.requireNonNull(source);
    	
        Dictionary<T, U> dictionary = new Hashtable<T, U>();
        source.entrySet()
        	  .stream()
        	  .forEach(e -> dictionary.put(e.getKey(), e.getValue()));
        return dictionary;
    }
    
    /**
     * Converts a dictionary to a map
     * @param source = the source dictionary
     * @return a map with the contents of the dictionary
     */
    public static <T, U> Map<T, U> dictionaryToMap(Dictionary<T, U> source) {
    	Objects.requireNonNull(source);
    	
        Map<T, U> map = new HashMap<T, U>();
        Collections.list(source.keys())
        		   .forEach(e -> map.put(e, source.get(e)));
        return map;
    }

    /**
     * Merges two maps. In case of conflict, the values of 'kept' are kept
     * @param kept = map that will take priority
     * @param replaced = map that will be replaced
     * @return the merge
     */
    public static <T, U> Map<T, U> merge(Map<T, U> kept, Map<T, U> replaced) {
    	Objects.requireNonNull(kept);
    	Objects.requireNonNull(replaced);
    	
        Map<T, U> res = new HashMap<T, U>();
        res.putAll(replaced);
        res.putAll(kept);
        return res;	
    }

    public static <T, U> Map.Entry<T, U> entry(T key, U value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static <T, U> Collector<Map.Entry<T, U>, ?, Map<T, U>> toMapCollector() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }
	
}
