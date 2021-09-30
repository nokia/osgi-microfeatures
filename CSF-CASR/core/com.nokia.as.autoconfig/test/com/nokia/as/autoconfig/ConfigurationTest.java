package com.nokia.as.autoconfig;

import static com.nokia.as.autoconfig.Utils.newMap;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigurationTest {
    
    private static Configuration config;
    
    @BeforeClass
    public static void setUpConfigurations() {
        
        config = new Configuration();
        config.config.put("a", newMap("a", "a"));
        config.config.put("b", newMap("b", "b", "c", "c"));
        config.factoryConfig.put("fa", Arrays.asList(newMap("a", "a", Configuration.FACTORY_ID, "fa")));
        config.factoryConfig.put("fb", Arrays.asList(newMap("b1", "b1", Configuration.FACTORY_ID, "fb1"), 
                                                     newMap("b2", "b2", Configuration.FACTORY_ID, "fb2")));
    }

    @Test
    public void copyOfTest() {
        assertEquals(config, Configuration.copyOf(config));
    }
    
    @Test
    public void mergeTest() {
        Configuration other = new Configuration();
        other.config.put("c", newMap("c", "c"));
        other.config.put("b", newMap("b", "d", "c", "c"));
        other.factoryConfig.put("fc", Arrays.asList(newMap("c", "c", Configuration.FACTORY_ID, "fc")));
        other.factoryConfig.put("fb", Arrays.asList(newMap("b1", "b3", Configuration.FACTORY_ID, "fb1"), 
                                                    newMap("b2", "b2", Configuration.FACTORY_ID, "fb2")));
        
        Configuration expected = new Configuration();
        expected.config.put("a", newMap("a", "a"));
        expected.config.put("c", newMap("c", "c"));
        expected.config.put("b", newMap("b", "d", "c", "c"));
        expected.factoryConfig.put("fa", Arrays.asList(newMap("a", "a", Configuration.FACTORY_ID, "fa")));
        expected.factoryConfig.put("fc", Arrays.asList(newMap("c", "c", Configuration.FACTORY_ID, "fc")));
        expected.factoryConfig.put("fb", Arrays.asList(newMap("b1", "b3", Configuration.FACTORY_ID, "fb1"), 
                                                       newMap("b2", "b2", Configuration.FACTORY_ID, "fb2")));
        
        assertEquals(expected, Configuration.merge(config, other));
    }
}
