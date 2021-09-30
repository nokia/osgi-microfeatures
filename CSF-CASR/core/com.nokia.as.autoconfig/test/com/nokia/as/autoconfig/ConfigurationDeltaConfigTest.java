package com.nokia.as.autoconfig;

import static com.nokia.as.autoconfig.Utils.newMap;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigurationDeltaConfigTest {
	
    private static Configuration previous;
    private static Configuration current;
    private static ConfigurationDelta delta;
    
    @BeforeClass
    public static void setUpConfigurations() {
		
		//Old configuration
		previous = new Configuration();
		
		//Singleton configuration
		//Removed
		Map<String, Object> deleted = newMap("deletedValue", "deleted");
		previous.config.put("deleted", deleted);
		
		//Updated (before)
		Map<String, Object> updatedBefore = newMap("updatedValue", "before",
		                                           "oldValue", "old",
		                                           "sameValue", "same",
		                                           Configuration.PID_ID, "before pid_id",
		                                           Configuration.SPID_ID, "before spid_id",
		                                           Configuration.FPID_ID, "before fpid_id");
		previous.config.put("updated", updatedBefore);
		
		//New configuration
		current = new Configuration();
		
		//Added
		Map<String, Object> added = newMap("addedValue", "added");
		current.config.put("added", added);
		
		//Updated (after)
		Map<String, Object> updatedAfter = newMap("updatedValue", "after",
                                                  "newValue", "new",
                                                  "sameValue", "same");
		current.config.put("updated", updatedAfter);
		
		//No change
		Map<String, Object> noChange = newMap("noChange", "noChange");
		previous.config.put("noChange", noChange);
		current.config.put("noChange", noChange);
		
		delta = new ConfigurationDelta(previous, current);
	}

	@Test
	public void configAddedTest() {
		Configuration expected = new Configuration();
		Map<String, Object> expectedValues = newMap("addedValue", "added");
		expected.config.put("added", expectedValues);
		
		assertEquals(expected.config, delta.added.config);
	}
	
	@Test
	public void configDeletedTest() {
		Configuration expected = new Configuration();
		Map<String, Object> expectedValues = newMap("deletedValue", "deleted");
		expected.config.put("deleted", expectedValues);
		
		assertEquals(expected.config, delta.deleted.config);
	}
	
	@Test
	public void configUpdatedTest() {
		Configuration expected = new Configuration();
		Map<String, Object> expectedValues = newMap("updatedValue", "after",
                                                    "newValue", "new",
                                                    "sameValue", "same");
		expected.config.put("updated", expectedValues);
		
		assertEquals(expected.config, delta.updated.config);
	}
}
