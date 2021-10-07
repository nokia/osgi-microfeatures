package com.nokia.as.autoconfig.transform;

import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_ID;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_PID;
import static com.alcatel.as.util.config.ConfigConstants.PLATFORM_NAME;
import static com.nokia.as.autoconfig.Utils.newMap;
import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

import org.junit.Test;

import com.nokia.as.autoconfig.Configuration;
import com.nokia.as.autoconfig.Utils;
import com.nokia.as.autoconfig.legacy.SystemPidGenerator;

public class TransformerTest {
    
    private static final String TMP_DIR = "java.io.tmpdir";
    private static final String TMP_DIR_NON_EXISTENT = "java.io.tmpdir.nonexistent";
    private static final String HOME = "HOME";
    private static final String HOME_NON_EXISTENT = "HOME_NON_EXISTENT";
    
    private static Transformer transformer = new Transformer();
    
    @Test
    public void environmentTransformerTest() {
        
        Configuration config = new Configuration();
        config.config.put("unrelated", newMap("foo", "bar")); //unrelated prop
        config.config.put("existent", newMap("tmp", "${" + TMP_DIR + "}")); //existent system property
        config.config.put("nonExistentDefault", newMap("tmp", "${" + TMP_DIR_NON_EXISTENT + "}{default}")); //non existent system property with default
        config.config.put("nonExistentNoDefault", newMap("tmp", "${" + TMP_DIR_NON_EXISTENT + "}")); //non existent system property without default
        config.factoryConfig.put("unrelated", Arrays.asList(newMap("foo", 0))); //unrelated prop
        config.factoryConfig.put("existent", Arrays.asList(newMap("home", "${" + HOME + "}"))); //existent environment variable
        config.factoryConfig.put("nonExistent", Arrays.asList(
                                                    newMap("home", "${" + HOME_NON_EXISTENT + "}{default}", Configuration.FACTORY_ID, "default"), //non existent environment variable with default
                                                    newMap("home", "${" + HOME_NON_EXISTENT + "}", Configuration.FACTORY_ID, "noDefault"))); //non existent environment variable without default
        
        Configuration expected = new Configuration();
        expected.config.put("unrelated", newMap("foo", "bar")); //unrelated prop string
        expected.config.put("existent", newMap("tmp", Utils.getSystemProperty(TMP_DIR))); //existent system property
        expected.config.put("nonExistentDefault", newMap("tmp", "default")); //non existent system property with default
        expected.config.put("nonExistentNoDefault", newMap("tmp", "${" + TMP_DIR_NON_EXISTENT + "}")); //non existent system property without default
        expected.factoryConfig.put("unrelated", Arrays.asList(newMap("foo", 0))); //unrelated prop no string
        expected.factoryConfig.put("existent", Arrays.asList(newMap("home", Utils.getEnvProperty(HOME)))); //existent environment variable
        expected.factoryConfig.put("nonExistent", Arrays.asList(
                                                    newMap("home", "default", Configuration.FACTORY_ID, "default"), //non existent environment variable with default
                                                    newMap("home", "${" + HOME_NON_EXISTENT + "}", Configuration.FACTORY_ID, "noDefault"))); //non existent environment variable without default
        
        assertEquals(expected, transformer.transform(config, new EnvironmentTransformer()));
    }
    
    @Test
    public void filePropertyTransformerTest() throws Exception {
    	//create temp file
    	File tempFile = File.createTempFile("filePropertyTransformer", ".cfg");
    	tempFile.deleteOnExit();

    	//fill temp file
    	FileWriter fileWriter = new FileWriter(tempFile);
    	BufferedWriter bw = new BufferedWriter(fileWriter);
    	bw.write("baz");
    	bw.close();

    	Configuration config = new Configuration();
    	config.config.put("unrelated", newMap("foo", "bar"));
    	config.config.put("file", newMap("file-foo", tempFile.getAbsolutePath()));
    	config.factoryConfig.put("unrelated", Arrays.asList(newMap("foo", 0)));
    	config.factoryConfig.put("file", Arrays.asList(newMap("file-foo", "non existent file")));

    	Configuration expected = new Configuration();
    	expected.config.put("unrelated", newMap("foo", "bar"));
    	expected.config.put("file", newMap("foo", "baz"));
    	expected.factoryConfig.put("unrelated", Arrays.asList(newMap("foo", 0)));
    	expected.factoryConfig.put("file", Arrays.asList(newMap("foo", "")));

    	assertEquals(expected, transformer.transform(config, new FilePropertyTransformer()));
    }
    
    @Test
    public void instancePidTransformerTest() {
        
        String pid = SystemPidGenerator.getProcessId().orElseGet(() -> "");
        int instanceId = SystemPidGenerator.getInstanceId(newMap(INSTANCE_ID, "${instance.pid}", PLATFORM_NAME, "asr"), 0);
        
        Configuration config = new Configuration();
        config.config.put("unrelated", newMap("foo", "bar"));
        config.config.put("instanceCalculated", newMap(INSTANCE_PID, 42)); //already calculated
        config.config.put("instancePid", newMap(INSTANCE_PID, "${instance.pid}", PLATFORM_NAME, "not asr")); //not calculated
        config.factoryConfig.put("unrelated", Arrays.asList(newMap("foo", 0)));
        config.factoryConfig.put("instanceId", Arrays.asList(newMap(INSTANCE_PID, "${instance.pid}", PLATFORM_NAME, "asr")));
        
        Configuration expected = new Configuration();
        expected.config.put("unrelated", newMap("foo", "bar"));
        expected.config.put("instanceCalculated", newMap(INSTANCE_PID, 42)); //already calculated
        expected.config.put("instancePid", newMap(INSTANCE_PID, pid, PLATFORM_NAME, "not asr")); //not calculated
        expected.factoryConfig.put("unrelated", Arrays.asList(newMap("foo", 0)));
        expected.factoryConfig.put("instanceId", Arrays.asList(newMap(INSTANCE_PID, pid, 
                                                                      PLATFORM_NAME, "asr", 
                                                                      INSTANCE_ID, Integer.toString(instanceId))));
        
        assertEquals(expected, transformer.transform(config, new InstancePidTransformer(() -> 0l)));
    }
    
    @Test
    public void javaLangTransformerTest() {
        Configuration config = new Configuration();
        config.config.put("integer", newMap("java.lang.Integer-int", "1"));
        config.config.put("boolean", newMap("java.lang.Boolean-bool", "true"));
        config.config.put("byte", newMap("java.lang.Byte-byte", "1"));
        config.config.put("character", newMap("java.lang.Character-char", "a"));
        config.config.put("short", newMap("java.lang.Short-short", "1"));
        config.factoryConfig.put("long", Arrays.asList(newMap("java.lang.Long-long", "1")));
        config.factoryConfig.put("float", Arrays.asList(newMap("java.lang.Float-float", "1.0")));
        config.factoryConfig.put("double", Arrays.asList(newMap("java.lang.Double-double", "1.0")));
        config.factoryConfig.put("string", Arrays.asList(newMap("java.lang.String-str", "foo")));
        config.factoryConfig.put("none", Arrays.asList(newMap("test-str", "baz")));
        
        Configuration expected = new Configuration();
        expected.config.put("integer", newMap("int", 1));
        expected.config.put("boolean", newMap("bool", true));
        expected.config.put("byte", newMap("byte", (byte) 1));
        expected.config.put("character", newMap("char", 'a'));
        expected.config.put("short", newMap("short", (short) 1));
        expected.factoryConfig.put("long", Arrays.asList(newMap("long", 1l)));
        expected.factoryConfig.put("float", Arrays.asList(newMap("float", 1.0f)));
        expected.factoryConfig.put("double", Arrays.asList(newMap("double", 1.0d)));
        expected.factoryConfig.put("string", Arrays.asList(newMap("str", "foo")));
        expected.factoryConfig.put("none", Arrays.asList(newMap("test-str", "baz")));
        
        assertEquals(expected, transformer.transform(config, new JavaLangTransformer()));
    }
}
