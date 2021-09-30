package com.nokia.as.autoconfig;

import static com.nokia.as.autoconfig.Utils.newMap;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigurationDeltaFactoryConfigTest {
	
	private static Configuration previous;
	private static Configuration current;
	private static ConfigurationDelta delta;
	
	@BeforeClass
	public static void setUpConfigurations() {
		
		//Old configuration
		previous = new Configuration();
		Map<String, Object> noChange = newMap("noChange", "noChange",
		                                      Configuration.FACTORY_ID, "noChange");
		previous.factoryConfig.put("noChange", Arrays.asList(noChange));
		
		//Removed - whole factoryPid
		Map<String, Object> deleted = newMap("deletedValue", "deleted");
		previous.factoryConfig.put("deleted", Arrays.asList(deleted));
		
		//Removed - only one configuration
        Map<String, Object> deletedOne = newMap("deletedValue", "deleted",
                                                Configuration.FACTORY_ID, "deletedOne");
        previous.factoryConfig.put("deletedOne", Arrays.asList(deletedOne, noChange));
        
        //Added - only one configuration
        previous.factoryConfig.put("addedOne", Arrays.asList(noChange));
        
		//Updated
		Map<String, Object> updatedBefore = newMap("updatedValue", "before",
		                                           "oldValue", "old",
		                                           "sameValue", "same",
		                                           Configuration.FACTORY_ID, "updated");
		previous.factoryConfig.put("updated", Arrays.asList(updatedBefore, noChange));
		
		//New configuration
		current = new Configuration();
		current.factoryConfig.put("noChange", Arrays.asList(noChange));
		
        //Removed - only one configuration
        current.factoryConfig.put("deletedOne", Arrays.asList(noChange));
        
        //Added - whole factoryPid
        Map<String, Object> added = newMap("addedValue", "added");
        current.factoryConfig.put("added", Arrays.asList(added));
        
        //Added - only one configuration
        Map<String, Object> addedOne = newMap("addedValue", "added",
                                              Configuration.FACTORY_ID, "addedOne");
        current.factoryConfig.put("addedOne", Arrays.asList(addedOne, noChange));
        
        //Updated
        Map<String, Object> updatedAfter = newMap("updatedValue", "after",
                                                  "newValue", "new",
                                                  "sameValue", "same",
                                                  Configuration.FACTORY_ID, "updated");
        current.factoryConfig.put("updated", Arrays.asList(updatedAfter, noChange));
		
		delta = new ConfigurationDelta(previous, current);
	}

	@Test
	public void factoryConfigAddedTest() {
	    Configuration expected = new Configuration();
	    
	    Map<String, Object> added = newMap("addedValue", "added");
        expected.factoryConfig.put("added", Arrays.asList(added));
        
        Map<String, Object> addedOne = newMap("addedValue", "added",
                                              Configuration.FACTORY_ID, "addedOne");
        expected.factoryConfig.put("addedOne", Arrays.asList(addedOne));
        
        assertEquals(expected.factoryConfig, delta.added.factoryConfig);
	}
	
	@Test
	public void factoryConfigDeletedTest() {
		Configuration expected = new Configuration();

		Map<String, Object> deleted = newMap("deletedValue", "deleted");
        expected.factoryConfig.put("deleted", Arrays.asList(deleted));
        
        Map<String, Object> deletedOne = newMap("deletedValue", "deleted",
                                                Configuration.FACTORY_ID, "deletedOne");
        expected.factoryConfig.put("deletedOne", Arrays.asList(deletedOne));
		
		assertEquals(expected.config, delta.deleted.config);
	}
	
	@Test
	public void factoryConfigUpdatedTest() {
		Configuration expected = new Configuration();
		
		Map<String, Object> updatedAfter = newMap("updatedValue", "after",
                                                  "newValue", "new",
                                                  "sameValue", "same",
                                                  Configuration.FACTORY_ID, "updated");
		expected.factoryConfig.put("updated", Arrays.asList(updatedAfter));
		
		assertEquals(expected.config, delta.updated.config);
	}
}
