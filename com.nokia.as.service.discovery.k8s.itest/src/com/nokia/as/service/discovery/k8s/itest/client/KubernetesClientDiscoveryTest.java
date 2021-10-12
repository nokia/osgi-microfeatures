// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.discovery.k8s.itest.client;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.DependencyManagerActivator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.alcatel.as.service.discovery.Advertisement;
import com.alcatel.as.util.config.ConfigConstants;
import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class KubernetesClientDiscoveryTest extends IntegrationTestBase {
    
	private static final String TEST_NAMESPACE = "test";
	private static final String ORIG_DIR = "k8s_resources_tmp";
	private static final String DEST_DIR = "k8s_resources_test";
    
    private final Ensure ensure = new Ensure();
    
    private DependencyManager dm = new DependencyManager(_context);
    private Component c;
    
    @Before
    public void setUp() throws Exception {
    	Files.copy(Paths.get(ORIG_DIR + File.separator + "permanent.yaml"), 
 			   Paths.get(DEST_DIR + File.separator + "permanent.yaml"), 
 			   StandardCopyOption.REPLACE_EXISTING);
    	Files.copy(Paths.get(ORIG_DIR + File.separator + "volatile.yaml"), 
  		   	   Paths.get(DEST_DIR + File.separator + "volatile.yaml"), 
			   StandardCopyOption.REPLACE_EXISTING);
    	c = DependencyManagerActivator.component(dm).impl(new AdvertTracker()).withSvc(Advertisement.class, svc -> svc.add(AdvertTracker::check).remove(AdvertTracker::check)).build();
    	dm.add(c);
    	TimeUnit.SECONDS.sleep(1);
    }
     
    @Test
    public void test() throws Exception {
        ensure.waitForStep(1);
        
        replaceInFile(DEST_DIR + File.separator + "volatile.yaml", "ready: false", "ready: true");
        ensure.waitForStep(2);
        
        replaceInFile(DEST_DIR + File.separator + "volatile.yaml", "ready: true", "ready: false");
        ensure.waitForStep(3);
        
        replaceInFile(DEST_DIR + File.separator + "permanent.yaml", "ready: true", "ready: false");
        ensure.waitForStep(4);
    }
    
    @After
    public void tearDown() throws Exception {
    	dm.remove(c);
    	dm.clear();
    	
    	File dest = new File(DEST_DIR);
    	Stream.of(dest.listFiles()).filter(f -> !f.getName().equals(".keepme")).forEach(File::delete);
    	TimeUnit.SECONDS.sleep(1);
    }
    
    //Class used to check the advertisements when they are registered
    public class AdvertTracker {
        void check(Advertisement advert, Map<String, Object> props) {
        	System.out.println(advert);
            assertEquals("kubernetes", props.get("provider"));
            assertEquals(TEST_NAMESPACE, props.get("namespace"));
            
            if("172.1.2.3".equals(advert.getIp()) && 8080 == advert.getPort()) {
                assertEquals("permanent", props.get("pod.name"));
                assertEquals("permanent", props.get("container.name"));
                assertEquals("permanent-tcp", props.get("container.port.name"));
                assertEquals("TCP", props.get("container.port.protocol"));
                assertEquals("172.1.2.3", props.get(ConfigConstants.SERVICE_IP));
                assertEquals(8080, props.get(ConfigConstants.SERVICE_PORT));
                assertEquals(true, props.get("container.isReady"));
                assertEquals("abcde12345", props.get("containerID"));
                assertEquals("test", props.get("pod.label.com.nokia.casr.loadbalancer"));
                ensure.inc();
            }
            
            if("172.4.5.6".equals(advert.getIp())) {

                HashMap<String, String> labels = (HashMap<String, String>) props.get("k8s.labels");
                assertEquals(2, labels.keySet().size());
                assertEquals("value1", labels.get("label1"));
                assertEquals("value2", labels.get("label2"));

                assertEquals("volatile", props.get("pod.name"));
                assertEquals("volatile", props.get("container.name"));
                assertEquals("172.4.5.6", props.get(ConfigConstants.SERVICE_IP));
                assertEquals(true, props.get("container.isReady"));
                assertEquals("12345abcde", props.get("containerID"));
                
                if(8080 == advert.getPort()) {
                    assertEquals("volatile-tcp", props.get("container.port.name"));
                    assertEquals("TCP", props.get("container.port.protocol"));
                    assertEquals(8080, props.get(ConfigConstants.SERVICE_PORT));
                    ensure.inc();
                } else { //10000
                    assertEquals("volatile-udp", props.get("container.port.name"));
                    assertEquals("UDP", props.get("container.port.protocol"));
                    assertEquals(10000, props.get(ConfigConstants.SERVICE_PORT));
                    ensure.inc();
                }
            }
        }
    }
    
    private void replaceInFile(String filename, String from, String to) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        lines = lines.stream().map(s -> s.replaceAll(from, to)).collect(Collectors.toList());
        Files.write(Paths.get(filename), String.join(System.lineSeparator(), lines).getBytes());
    }
}