package com.nokia.as.service.discovery.k8s.itest.client;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
public class KubernetesClientDiscoveryIOHTest extends IntegrationTestBase {
    
    private static final String TEST_NAMESPACE = "test";
    private static final String ORIG_DIR = "k8s_resources_tmp";
	private static final String DEST_DIR = "k8s_resources_test";
    
    private final Ensure ensure = new Ensure();
    
    private DependencyManager dm = new DependencyManager(_context);
    private Component c;
    
    @Before
    public void setUp() throws Exception {
    	c = DependencyManagerActivator.component(dm).impl(new AdvertTracker()).withSvc(Advertisement.class, svc -> svc.add(AdvertTracker::check).remove(AdvertTracker::check)).build();
    	dm.add(c);
    	TimeUnit.SECONDS.sleep(1);
    	
    	Files.copy(Paths.get(ORIG_DIR + File.separator + "httpIOH.yaml"), 
		   	   	   Paths.get(DEST_DIR + File.separator + "httpIOH.yaml"), 
			       StandardCopyOption.REPLACE_EXISTING);
    	Files.copy(Paths.get(ORIG_DIR + File.separator + "diameterIOH.yaml"), 
		   	   	   Paths.get(DEST_DIR + File.separator + "diameterIOH.yaml"), 
			       StandardCopyOption.REPLACE_EXISTING);
    	Files.copy(Paths.get(ORIG_DIR + File.separator + "radiusIOH.yaml"), 
		   	       Paths.get(DEST_DIR + File.separator + "radiusIOH.yaml"), 
			       StandardCopyOption.REPLACE_EXISTING);
    	Files.copy(Paths.get(ORIG_DIR + File.separator + "metersIOH.yaml"), 
		   	       Paths.get(DEST_DIR + File.separator + "metersIOH.yaml"), 
			       StandardCopyOption.REPLACE_EXISTING);
    	Files.copy(Paths.get(ORIG_DIR + File.separator + "slessIOH.yaml"), 
		   	       Paths.get(DEST_DIR + File.separator + "slessIOH.yaml"), 
			       StandardCopyOption.REPLACE_EXISTING);
    }
    
    @Test
    public void test() throws Exception {
    	
    	ready(DEST_DIR + File.separator + "httpIOH.yaml");
        ensure.waitForStep(1);
        ready(DEST_DIR + File.separator + "diameterIOH.yaml");
        ensure.waitForStep(2);
        ready(DEST_DIR + File.separator + "radiusIOH.yaml");
        ensure.waitForStep(3);
        ready(DEST_DIR + File.separator + "metersIOH.yaml");
        ensure.waitForStep(4);
        ready(DEST_DIR + File.separator + "slessIOH.yaml");
        ensure.waitForStep(5);
        
        unready(DEST_DIR + File.separator + "httpIOH.yaml");
        ensure.waitForStep(6);
        unready(DEST_DIR + File.separator + "diameterIOH.yaml");
        ensure.waitForStep(7);
        unready(DEST_DIR + File.separator + "radiusIOH.yaml");
        ensure.waitForStep(8);
        unready(DEST_DIR + File.separator + "metersIOH.yaml");
        ensure.waitForStep(9);
        unready(DEST_DIR + File.separator + "slessIOH.yaml");
        ensure.waitForStep(10);
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
                assertEquals("http", props.get("pod.name"));
                assertEquals("httpioh", props.get("container.name"));
                assertEquals("mux-h-test", props.get("container.port.name"));
                assertEquals("TCP", props.get("container.port.protocol"));
                assertEquals("172.1.2.3", props.get(ConfigConstants.SERVICE_IP));
                assertEquals(8080, props.get(ConfigConstants.SERVICE_PORT));
                assertEquals(true, props.get("container.isReady"));
                assertEquals("abcde12345", props.get("containerID"));
                assertEquals("ioh", props.get("mux.factory.remote"));
                assertEquals("*", props.get("group.target"));
                assertEquals(TEST_NAMESPACE, props.get("group.name"));
                assertEquals("test", props.get("instance.name"));
                assertEquals("HttpIOH", props.get("component.name"));
                assertEquals("csf", props.get("platform.name"));
                assertEquals(286, props.get("module.id"));
                ensure.inc();
            }
            
            if("172.4.5.6".equals(advert.getIp())) {
                assertEquals("diameter", props.get("pod.name"));
                assertEquals("diameterioh", props.get("container.name"));
                assertEquals("mux-d-Tcp1-", props.get("container.port.name"));
                assertEquals("TCP", props.get("container.port.protocol"));
                assertEquals("172.4.5.6", props.get(ConfigConstants.SERVICE_IP));
                assertEquals(8080, props.get(ConfigConstants.SERVICE_PORT));
                assertEquals(true, props.get("container.isReady"));
                assertEquals("12345abcde", props.get("containerID"));
                assertEquals("ioh", props.get("mux.factory.remote"));
                assertEquals("*", props.get("group.target"));
                assertEquals(TEST_NAMESPACE, props.get("group.name"));
                assertEquals("Tcp1-", props.get("instance.name"));
                assertEquals("DiameterIOH", props.get("component.name"));
                assertEquals("csf", props.get("platform.name"));
                assertEquals(289, props.get("module.id"));
                ensure.inc();
            }
            
            if("172.7.8.9".equals(advert.getIp())) {
                assertEquals("radius", props.get("pod.name"));
                assertEquals("radiusioh", props.get("container.name"));
                assertEquals("TCP", props.get("container.port.protocol"));
                assertEquals("172.7.8.9", props.get(ConfigConstants.SERVICE_IP));
                assertEquals(true, props.get("container.isReady"));
                assertEquals("fghij67890", props.get("containerID"));
                
                if(8080 == advert.getPort()) {
                    assertEquals("radius-tcp", props.get("container.port.name"));
                    assertEquals(8080, props.get(ConfigConstants.SERVICE_PORT));
                } else { //25000
                    assertEquals("mux-r-AAA", props.get("container.port.name"));
                    assertEquals(25000, props.get(ConfigConstants.SERVICE_PORT));
                    assertEquals("ioh", props.get("mux.factory.remote"));
                    assertEquals("*", props.get("group.target"));
                    assertEquals(TEST_NAMESPACE, props.get("group.name"));
                    assertEquals("AAA", props.get("instance.name"));
                    assertEquals("RadiusIOH", props.get("component.name"));
                    assertEquals(296, props.get("module.id"));
                    assertEquals("csf", props.get("platform.name"));
                }
                ensure.inc();
            }
            
            if("172.1.3.5".equals(advert.getIp()) && 8080 == advert.getPort()) {
                assertEquals("meters", props.get("pod.name"));
                assertEquals("metersioh", props.get("container.name"));
                assertEquals("mux-m-meters", props.get("container.port.name"));
                assertEquals("TCP", props.get("container.port.protocol"));
                assertEquals("172.1.3.5", props.get(ConfigConstants.SERVICE_IP));
                assertEquals(8080, props.get(ConfigConstants.SERVICE_PORT));
                assertEquals(true, props.get("container.isReady"));
                assertEquals("wcxds77862", props.get("containerID"));
                assertEquals("ioh", props.get("mux.factory.remote"));
                assertEquals("*", props.get("group.target"));
                assertEquals(TEST_NAMESPACE, props.get("group.name"));
                assertEquals("meters", props.get("instance.name"));
                assertEquals("MetersIOH", props.get("component.name"));
                assertEquals("csf", props.get("platform.name"));
                assertEquals(324, props.get("module.id"));
                ensure.inc();
            }
            
            if("172.2.4.6".equals(advert.getIp()) && 8080 == advert.getPort()) {
                assertEquals("sless", props.get("pod.name"));
                assertEquals("slessioh", props.get("container.name"));
                assertEquals("mux-s-1234b", props.get("container.port.name"));
                assertEquals("TCP", props.get("container.port.protocol"));
                assertEquals("172.2.4.6", props.get(ConfigConstants.SERVICE_IP));
                assertEquals(8080, props.get(ConfigConstants.SERVICE_PORT));
                assertEquals(true, props.get("container.isReady"));
                assertEquals("jahcl18739", props.get("containerID"));
                assertEquals("ioh", props.get("mux.factory.remote"));
                assertEquals("*", props.get("group.target"));
                assertEquals(TEST_NAMESPACE, props.get("group.name"));
                assertEquals("1234b", props.get("instance.name"));
                assertEquals("SlessIOH", props.get("component.name"));
                assertEquals("csf", props.get("platform.name"));
                assertEquals(499, props.get("module.id"));
                ensure.inc();
            }
        }
    }
    
    private void ready(String filename) throws Exception {
    	replaceInFile(filename, "false", "true");
    }
    
    private void unready(String filename) throws Exception {
    	replaceInFile(filename, "true", "false");
    }
    
    private void replaceInFile(String filename, String from, String to) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        lines = lines.stream().map(s -> s.replaceAll(from, to)).collect(Collectors.toList());
        Files.write(Paths.get(filename), String.join(System.lineSeparator(), lines).getBytes());
    }
}