// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig;

import static com.nokia.as.autoconfig.Configuration.FACTORY_ID;
import static com.nokia.as.autoconfig.Configuration.SPID_ID;
import static com.nokia.as.autoconfig.Configuration.containsFactoryId;
import static com.nokia.as.autoconfig.Configuration.isPid;
import static com.nokia.as.autoconfig.Configuration.whoContainsFactoryId;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigurationDelta {

    public final Configuration added;
    public final Configuration deleted;
    public final Configuration updated;
    
    public ConfigurationDelta(Configuration previous, Configuration current) {
        
        added = new Configuration();
        added.config.putAll(configAdded(previous.config, current.config));
        added.factoryConfig.putAll(factoryConfigAdded(previous.factoryConfig, current.factoryConfig));

        deleted = new Configuration();
        deleted.config.putAll(configDeleted(previous.config, current.config));
        deleted.factoryConfig.putAll(factoryConfigDeleted(previous.factoryConfig, current.factoryConfig));

        updated = new Configuration();
        updated.config.putAll(configUpdated(previous.config, current.config));
        updated.factoryConfig.putAll(factoryConfigUpdated(previous.factoryConfig, current.factoryConfig));
    }

    /*
     * pid not in previous and 
     * pid in current
     */
    private Map<String, Map<String, Object>> configAdded(Map<String, Map<String, Object>> previous,
                                                          Map<String, Map<String, Object>> current) {
        
        return filterConfiguration(current, e -> !previous.containsKey(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /*
     * pid in previous 
     * and pid not in current
     */
    private Map<String, Map<String, Object>> configDeleted(Map<String, Map<String, Object>> previous,
                                                            Map<String, Map<String, Object>> current) {
        return configAdded(current, previous); // deleted(previous, current) === added(current, previous)
    }

    /*
     * pid in previous and 
     * pid in current and 
     * different properties
     */
    private Map<String, Map<String, Object>> configUpdated(Map<String, Map<String, Object>> previous,
                                                            Map<String, Map<String, Object>> current) {
        
        return filterConfiguration(current, e -> previous.containsKey(e.getKey())) // at the end we need the values from current
                    .filter(e -> !equalMaps(e.getValue(), previous.get(e.getKey()))) // if the properties are different
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /*
     * factoryPid not in previous and 
     * factoryPid in current
     * 
     * or
     * 
     * factoryPid in previous and
     * factoryPid in current and
     * properties not in previous.factoryPid and
     * properties in current.factoryPid
     */
    private Map<String, List<Map<String, Object>>> factoryConfigAdded(Map<String, List<Map<String, Object>>> previous,
                                                                       Map<String, List<Map<String, Object>>> current) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        
        // not in previous, in current
        Map<String, List<Map<String, Object>>> caseA = 
            filterConfiguration(current, e -> !previous.containsKey(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // in previous, in current, properties not in previous, properties in current
        // the uniqueness of the properties is defined by FACTORY_ID
        Map<String, List<Map<String, Object>>> caseB = 
            filterConfiguration(current, e -> previous.containsKey(e.getKey()))
                .map(e -> new AbstractMap.SimpleEntry<>(
                            e.getKey(),
                            filterFactoryConfiguration(e.getValue(),
                                                       m -> !containsFactoryId(previous.get(e.getKey()), m.get(FACTORY_ID)))
                           ))
                .filter(se -> !se.getValue().isEmpty()) // take only non-empty
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        result.putAll(caseA);
        result.putAll(caseB);
        return result;

    }

    /*
     * factoryPid in previous and 
     * factoryPid not in current
     * 
     * or
     * 
     * factoryPid in previous and
     * factoryPid in current and
     * properties in previous.factoryPid and
     * properties not in current.factoryPid
     */
    private Map<String, List<Map<String, Object>>> factoryConfigDeleted(Map<String, List<Map<String, Object>>> previous,
                                                                         Map<String, List<Map<String, Object>>> current) {
        return factoryConfigAdded(current, previous); // deleted(previous, current) === added(current, previous)
    }

    /*
     * factoryPid in previous and
     * factoryPid in current and
     * properties.FACTORY_ID match and
     * properties do not match
     */
    private Map<String, List<Map<String, Object>>> factoryConfigUpdated(Map<String, List<Map<String, Object>>> previous,
                                                                         Map<String, List<Map<String, Object>>> current) {

        Map<String, List<Map<String, Object>>> result = 
                filterConfiguration(current, e -> previous.containsKey(e.getKey())) // in previous and in current
                    .map(e -> new AbstractMap.SimpleEntry<>(
                            e.getKey(), 
                            filterFactoryConfiguration(e.getValue(),
                                                       m -> containsFactoryId(previous.get(e.getKey()), m.get(FACTORY_ID)) // FACTODY_ID match
                                                            && !equalMaps(m, whoContainsFactoryId(previous.get(e.getKey()), m.get(FACTORY_ID)))) //properties don't
                              ))
                        .filter(se -> !se.getValue().isEmpty()) // take only non-empty
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        /*
         * post-processing needed to recognize the configuration as config admin adds a
         * generated pid that we cannot know when we scan the configuration, we need to
         * retrieve this value from the previous config
         */
        result.entrySet().stream() // for every factory configuration
              .forEach(e -> e.getValue().forEach(m -> // for every configuration
                                                 m.put(SPID_ID, 
                                                       whoContainsFactoryId(previous.get(e.getKey()), m.get(FACTORY_ID)) // find respective configuration in previous
                                                           .get(SPID_ID)))); // put previous PID_ID

        return result;
    }

    private <K, V> Stream<Map.Entry<K, V>> filterConfiguration(Map<K, V> a, Predicate<Map.Entry<K, V>> filter) {
        return a.entrySet().stream().filter(filter);
    }

    private <K, V> List<Map<K, V>> filterFactoryConfiguration(List<Map<K, V>> a, Predicate<Map<K, V>> filter) {
        return a.stream().filter(filter).collect(Collectors.toList());
    }

    private boolean equalMaps(Map<String, Object> a, Map<String, Object> b) {
        return filterPids(a).equals(filterPids(b));
    }

    // pids should be ignored when comparing maps
    private Map<String, Object> filterPids(Map<String, Object> map) {
        return map.entrySet().stream().filter(e -> !isPid(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public boolean isEmpty() {
        return added.isEmpty() && deleted.isEmpty() && updated.isEmpty();
    }

    @Override
    public String toString() {
        return "ConfigurationDelta [added=" + added + System.lineSeparator() + "deleted=" + deleted
                + System.lineSeparator() + "updated=" + updated + "]";
    }
}
