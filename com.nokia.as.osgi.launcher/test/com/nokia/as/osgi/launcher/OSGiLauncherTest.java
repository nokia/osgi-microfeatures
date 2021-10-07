package com.nokia.as.osgi.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import osgi.demo.helloservice.HelloService;

public class OSGiLauncherTest {
	
	private OsgiLauncher framework;
	
	@Before
	public void setUp() {		
		ServiceLoader <OsgiLauncher> servLoad = ServiceLoader.load(OsgiLauncher.class);
		framework = servLoad.iterator().next();
		framework = framework.useExceptionHandler(Throwable::printStackTrace)
							 .useDirectory("dependencies");
	}
	
	@Test
	public void sampleTest() throws Exception {
		framework = framework.start();
		createDefaultConfig(framework, "Namaste");		
		HelloService helloService = framework.getService(HelloService.class)
				 							 .get(5, TimeUnit.SECONDS);
		assertEquals("Namaste Test", helloService.sayHello("Test"));
	}
    
	@Test
	public void registerTest() throws Exception {
		framework = framework.filter("(!(Bundle-SymbolicName=osgi.demo.helloservice.impl))")
	 			 			 .start();

		ServiceRegistration<HelloService> servReg = 
				framework.registerService(HelloService.class, name -> "Güten Tag " + name);
		
		HelloService helloService = framework.getService(HelloService.class)
				 							 .get(5, TimeUnit.SECONDS);
		assertEquals("Güten Tag Test", helloService.sayHello("Test"));
		servReg.unregister();
	}
	
	@Test
	public void listenTest() throws Exception {
		framework = framework.start();
		createDefaultConfig(framework, "Namaste");		

		Consumer<HelloService> doTest = (hs) -> {
			try {
				HelloService helloService = framework.getService(HelloService.class)
						 							 .get(5, TimeUnit.SECONDS);
				assertEquals("Namaste Test", helloService.sayHello("Test"));
			} catch(Exception e) {
				fail("No HelloService service found");
			}
		};
		
		ServiceTracker<?, ?> servTrack = 
				framework.listenService(HelloService.class, doTest, 
															(hs) -> {}, 
															(hs) -> {});
		
		servTrack.close();
	}
    
	@Test
	public void configBeforeTest() throws Exception {
		Map<String, Object> helloServiceConfig = new HashMap<>();
		helloServiceConfig.put("greetingMessage", "Hola");
		framework.configureService("osgi.demo.helloservice.impl.HelloServiceImpl", helloServiceConfig);
		
		framework = framework.start();
		createDefaultConfig(framework, "Namaste");
		
		HelloService helloService = framework.getService(HelloService.class)
				 							 .get(5, TimeUnit.SECONDS);
		assertEquals("Hola Test", helloService.sayHello("Test"));
	}
    
	@Test
	public void configAfterTest() throws Exception {
		framework = framework.start();
		createDefaultConfig(framework, "Namaste");

		HelloService helloService = framework.getService(HelloService.class)
			 							 	 .get(5, TimeUnit.SECONDS);
		
		Map<String, Object> helloServiceConfig = new HashMap<>();
		helloServiceConfig.put("greetingMessage", "Hola");
		framework.configureService("osgi.demo.helloservice.impl.HelloServiceImpl", helloServiceConfig);
		
		Thread.sleep(1000); //to let ConfigAdmin do the update
		
		assertEquals("Hola Test", helloService.sayHello("Test"));
	}
    
	@After
	public void tearDown() {
		framework.stop(0);
		framework = null;
	}


    private void createDefaultConfig(OsgiLauncher launcher, String greetingMessage) throws Exception{
    	// make sure ConfigurationAdmin service is available.
    	launcher.getService("org.osgi.service.cm.ConfigurationAdmin").get(5, TimeUnit.SECONDS);
    	Map<String, Object> helloServiceConfig = new HashMap<>();
    	helloServiceConfig.put("greetingMessage", greetingMessage);
    	launcher.configureService("osgi.demo.helloservice.impl.HelloServiceImpl", helloServiceConfig);
    	Thread.sleep(1000); //to let ConfigAdmin do the update
    }
	
}
