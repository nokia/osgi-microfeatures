// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.file;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Hashtable;
import java.util.Dictionary;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import org.junit.Test;

import com.nokia.as.autoconfig.Activator;
import com.nokia.as.autoconfig.ResolverTestBase;
import com.nokia.as.autoconfig.Utils;
import com.nokia.as.autoconfig.test.bundle.api.HelloService;

public class FileConfigResolverTest extends ResolverTestBase {

    @Test
    public void fileTest() throws Exception {
        System.setProperty(Activator.CONFIG_DIR, SIMPLE_CONFDIR_CFG);
        framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("Privet Russia", helloService.sayHello("Russia"));
    }

    @Test
    public void fileTestTrim() throws Exception {
        System.setProperty(Activator.CONFIG_DIR, SIMPLE_CONFDIR_CFG_TRIM);
        framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("Yassas Greece", helloService.sayHello("Greece"));
    }

    @Test
    public void fileTestWithFile() throws Exception {
        System.setProperty(Activator.CONFIG_DIR, FILE_CONFDIR);
        framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("Annyeong Korea", helloService.sayHello("Korea"));
    }

    @Test
    public void fileFactoryConfigTest() throws Exception {
        System.setProperty(Activator.CONFIG_DIR, FACTORY_CONFDIR_CFG);
        framework.withBundles(Utils.url(FACTORY_JAR).toString());
        framework.start();

        HelloService helloService = framework.getService(HelloService.class, "(type=Test-1)").get(5, TimeUnit.SECONDS);
        assertEquals("Merhaba Turkey", helloService.sayHello("Turkey"));

        HelloService helloService2 = framework.getService(HelloService.class, "(type=Test-2)").get(5, TimeUnit.SECONDS);
        assertEquals("Chao ban Vietnam", helloService2.sayHello("Vietnam"));
    }
    
    @Test
    public void fileModifConfigTest() throws Exception {
        System.setProperty(Activator.CONFIG_DIR, SIMPLE_CONFDIR_CFG);
        framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("Privet Russia", helloService.sayHello("Russia"));
         
        replaceInFile(CURRENT_DIR + SIMPLE_CONFDIR_CFG + File.separator + "com.nokia.as.autoconfig.test.bundle.def.HelloConfiguration.cfg", 
                      "Privet", "Dobry den");
        TimeUnit.SECONDS.sleep(2);

        try {
            HelloService helloService2 = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
            assertEquals("Dobry den Czech Republic", helloService2.sayHello("Czech Republic"));
        } finally {
            replaceInFile(CURRENT_DIR + SIMPLE_CONFDIR_CFG + File.separator + "com.nokia.as.autoconfig.test.bundle.def.HelloConfiguration.cfg", 
                          "Dobry den", "Privet");
        }
    }
    
    @Test
    public void fileModifFileDataTest() throws Exception {
        System.setProperty(Activator.CONFIG_DIR, FILE_CONFDIR);
        framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("Annyeong Korea", helloService.sayHello("Korea"));
         
        replaceInFile(CURRENT_DIR + FILE_CONFDIR + File.separator + "greeting.txt", 
                      "Annyeong", "Dzien dobry");
        TimeUnit.SECONDS.sleep(2);

        try {
            HelloService helloService2 = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
            assertEquals("Dzien dobry Poland", helloService2.sayHello("Poland"));
        } finally {
            replaceInFile(CURRENT_DIR + FILE_CONFDIR + File.separator + "greeting.txt", 
                          "Dzien dobry", "Annyeong");
        }
    }
     
    @Test
    public void fileModifFactoryConfigTest() throws Exception {
    	System.setProperty(Activator.CONFIG_DIR, FACTORY_CONFDIR_CFG);
    	framework.withBundles(Utils.url(FACTORY_JAR).toString());
    	framework = framework.start();

    	HelloService helloService = framework.getService(HelloService.class, "(type=Test-1)").get(5, TimeUnit.SECONDS);
    	assertEquals("Merhaba Turkey", helloService.sayHello("Turkey"));

    	HelloService helloService2 = framework.getService(HelloService.class, "(type=Test-2)").get(5, TimeUnit.SECONDS);
    	assertEquals("Chao ban Vietnam", helloService2.sayHello("Vietnam"));

    	replaceInFile(CURRENT_DIR + FACTORY_CONFDIR_CFG + File.separator + "com.nokia.as.autoconfig.test.bundle.factory.HelloConfiguration-1.cfg", 
    				  "Merhaba", "Kumusta");
    	replaceInFile(CURRENT_DIR + FACTORY_CONFDIR_CFG + File.separator + "com.nokia.as.autoconfig.test.bundle.factory.HelloConfiguration-2.cfg", 
    				  "Test-2", "Test-3");
    	TimeUnit.SECONDS.sleep(2);

    	try {
    		HelloService helloService3 = framework.getService(HelloService.class, "(type=Test-1)").get(5, TimeUnit.SECONDS);
    		assertEquals("Kumusta Phillippines", helloService3.sayHello("Phillippines"));

    		HelloService helloService4 = framework.getService(HelloService.class, "(type=Test-3)").get(5, TimeUnit.SECONDS);
    		assertEquals("Chao ban Vietnam", helloService4.sayHello("Vietnam"));
    	} finally {
    		replaceInFile(CURRENT_DIR + FACTORY_CONFDIR_CFG + File.separator + "com.nokia.as.autoconfig.test.bundle.factory.HelloConfiguration-1.cfg", 
    				      "Kumusta", "Merhaba");
    		replaceInFile(CURRENT_DIR + FACTORY_CONFDIR_CFG + File.separator + "com.nokia.as.autoconfig.test.bundle.factory.HelloConfiguration-2.cfg", 
    				      "Test-3", "Test-2");
    	}
    }
     
    @Test
    public void extraDirTest() throws Exception {
    	System.setProperty(Activator.CONFIG_DIR, SIMPLE_CONFDIR_CFG + "," + SIMPLE_CONFDIR_EXTRA);
    	framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("Zdravo Serbia", helloService.sayHello("Serbia"));
    }
    
    @Test
    public void extraDirFileModifTest() throws Exception {
    	System.setProperty(Activator.CONFIG_DIR, SIMPLE_CONFDIR_CFG + "," + SIMPLE_CONFDIR_EXTRA);
    	framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("Zdravo Serbia", helloService.sayHello("Serbia"));
        
        replaceInFile(CURRENT_DIR + SIMPLE_CONFDIR_EXTRA + File.separator + "com.nokia.as.autoconfig.test.bundle.def.HelloConfiguration.cfg", 
                	  "Zdravo", "Salam");
        TimeUnit.SECONDS.sleep(2);

        try {
        	HelloService helloService2 = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        	assertEquals("Salam Azerbaijan", helloService2.sayHello("Azerbaijan"));
        } finally {
        	replaceInFile(CURRENT_DIR + SIMPLE_CONFDIR_EXTRA + File.separator + "com.nokia.as.autoconfig.test.bundle.def.HelloConfiguration.cfg", 
        				  "Salam", "Zdravo");
        }
    }

    @Test
    public void extraDirFileMissingTest() throws Exception {
    	System.setProperty(Activator.CONFIG_DIR, FILE_CONFDIR + "," + FILE_CONFDIR_EXTRA_MISSING);
    	framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("Pershendetje Albania", helloService.sayHello("Albania"));
    }

    @Test
    public void extraDirFileOverrideTest() throws Exception {
    	System.setProperty(Activator.CONFIG_DIR, FILE_CONFDIR + "," + FILE_CONFDIR_EXTRA_OVERRIDE);
    	framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("Tere Estonia", helloService.sayHello("Estonia"));
    }
    
    @Test
    public void extraDirFileFactoryConfigTest() throws Exception {
        System.setProperty(Activator.CONFIG_DIR, FACTORY_CONFDIR_CFG + "," + FACTORY_CONFDIR_EXTRA);
        framework.withBundles(Utils.url(FACTORY_JAR).toString());
        framework.start();

        HelloService helloService = framework.getService(HelloService.class, "(type=Test-1)").get(5, TimeUnit.SECONDS);
        assertEquals("Zdraveite Bulgary", helloService.sayHello("Bulgary"));

        HelloService helloService2 = framework.getService(HelloService.class, "(type=Test-2)").get(5, TimeUnit.SECONDS);
        assertEquals("Hej Denmark", helloService2.sayHello("Denmark"));
    }
    
    @Test
    public void fileYamlTest() throws Exception {
        System.setProperty(Activator.CONFIG_DIR, SIMPLE_CONFDIR_YAML);
        framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("Saluton Esperanto", helloService.sayHello("Esperanto"));
    }
    
    @Test
    public void fileFactoryYamlTest() throws Exception {
    	System.setProperty(Activator.CONFIG_DIR, FACTORY_CONFDIR_YAML);
        framework.withBundles(Utils.url(FACTORY_JAR).toString());
        framework.start();

        HelloService helloService = framework.getService(HelloService.class, "(type=Test-1)").get(5, TimeUnit.SECONDS);
        assertEquals("Gamarjoba Georgia", helloService.sayHello("Georgia"));

        HelloService helloService2 = framework.getService(HelloService.class, "(type=Test-2)").get(5, TimeUnit.SECONDS);
        assertEquals("Sawubona Zulu", helloService2.sayHello("Zulu"));
    }
    
    @Test
    public void fileFactoryYamlExtraOverrideTest() throws Exception {
    	System.setProperty(Activator.CONFIG_DIR, FACTORY_CONFDIR_CFG + "," + FACTORY_CONFDIR_YAML);
        framework.withBundles(Utils.url(FACTORY_JAR).toString());
        framework.start();

        HelloService helloService = framework.getService(HelloService.class, "(type=Test-1)").get(5, TimeUnit.SECONDS);
        assertEquals("Gamarjoba Georgia", helloService.sayHello("Georgia"));

        HelloService helloService2 = framework.getService(HelloService.class, "(type=Test-2)").get(5, TimeUnit.SECONDS);
        assertEquals("Sawubona Zulu", helloService2.sayHello("Zulu"));
    }
    
    @Test
    public void fileTestPatch() throws Exception {
        System.setProperty(Activator.CONFIG_DIR, SIMPLE_CONFDIR_CFG_PATCH);
        framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("Hiha Maori", helloService.sayHello("Maori"));
    }
    
    @Test
    public void fileTestPatchRemove() throws Exception {
        System.setProperty(Activator.CONFIG_DIR, SIMPLE_CONFDIR_CFG + "," + SIMPLE_CONFDIR_EXTRA_PATCH);
        framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("null Test", helloService.sayHello("Test"));
    }
    
    @Test
    public void fileFactoryConfigPatchTest() throws Exception {
        System.setProperty(Activator.CONFIG_DIR, FACTORY_CONFDIR_PATCH);
        framework.withBundles(Utils.url(FACTORY_JAR).toString());
        framework.start();

        HelloService helloService = framework.getService(HelloService.class, "(type=Test-1)").get(5, TimeUnit.SECONDS);
        assertEquals("Cowabunga Turtle", helloService.sayHello("Turtle"));

        HelloService helloService2 = framework.getService(HelloService.class, "(type=Test-2)").get(5, TimeUnit.SECONDS);
        assertEquals("Sawubona Zulu", helloService2.sayHello("Zulu"));
    }
    
    private void replaceInFile(String filename, String from, String to) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        lines = lines.stream().map(s -> s.replaceAll(from, to)).collect(Collectors.toList());
        Files.write(Paths.get(filename), String.join(System.lineSeparator(), lines).getBytes());
    }

    @Test
    public void fileTestEnvAndRefresh() throws Exception {
        System.setProperty(Activator.CONFIG_DIR, FILE_CONFDIR_ENV);
        System.setProperty("greeting", "Barev");
        framework.withBundles(Utils.url(DEF_JAR).toString());
        framework = framework.start();

        HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
        assertEquals("Barev Armenia", helloService.sayHello("Armenia"));

        System.setProperty("greeting", "Zdraveĭte");
        EventAdmin admin = framework.getService(EventAdmin.class).get(5, TimeUnit.SECONDS);
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("config.pid", "com.nokia.as.autoconfig.test.bundle.def.HelloConfiguration");
        admin.postEvent(new Event("com/nokia/casr/ConfigEvent/UPDATED", properties));
        TimeUnit.SECONDS.sleep(2);
        
        helloService = framework.getService(HelloService.class)
                .get(5, TimeUnit.SECONDS);
        assertEquals("Zdraveĭte Bulgaria", helloService.sayHello("Bulgaria"));
        System.getProperties().remove("greeting");
    }

}
