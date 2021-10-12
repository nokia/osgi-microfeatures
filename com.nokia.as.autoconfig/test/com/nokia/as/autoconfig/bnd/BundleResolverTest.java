// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

// package com.nokia.as.autoconfig.bnd;

// import static org.junit.Assert.assertEquals;

// import java.util.concurrent.TimeUnit;

// import org.junit.Test;

// import com.nokia.as.autoconfig.Activator;
// import com.nokia.as.autoconfig.ResolverTestBase;
// import com.nokia.as.autoconfig.Utils;
// import com.nokia.as.autoconfig.test.bundle.api.HelloService;

// public class BundleResolverTest extends ResolverTestBase {

//     @Test
//     public void bundleConfigTestJson() throws Exception {
//         framework.withBundles(Utils.url(BND1_JSON_JAR).toString());
//         framework = framework.start();
//         TimeUnit.SECONDS.sleep(1);

//         HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
//         assertEquals("Hola Spanish", helloService.sayHello("Spanish"));
//     }

//     @Test
//     public void bundleVersionConfigTestJson() throws Exception {
//         framework.withBundles(Utils.url(BND1_JSON_JAR).toString());
//         framework.withBundles(Utils.url(BND2_JSON_JAR).toString());
//         framework = framework.start();
//         TimeUnit.SECONDS.sleep(1);

//         HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
//         assertEquals("Bonjour French", helloService.sayHello("French"));
//     }

//     @Test
//     public void bundleFactoryConfigTestJson() throws Exception {
//         framework.withBundles(Utils.url(BND_FACTORY_JSON_JAR).toString());
//         framework.start();
//         TimeUnit.SECONDS.sleep(1);

//         HelloService helloService = framework.getService(HelloService.class, "(type=Test-1)").get(5, TimeUnit.SECONDS);
//         assertEquals("Ni hao Chinese", helloService.sayHello("Chinese"));

//         HelloService helloService2 = framework.getService(HelloService.class, "(type=Test-2)").get(5, TimeUnit.SECONDS);
//         assertEquals("Konnichiwa Japanese", helloService2.sayHello("Japanese"));
//     }
    
//     @Test
//     public void bundleConfigTestYaml() throws Exception {
//         framework.withBundles(Utils.url(BND1_YAML_JAR).toString());
//         framework = framework.start();
//         TimeUnit.SECONDS.sleep(1);

//         HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
//         assertEquals("Guten Tag German", helloService.sayHello("German"));
//     }

//     @Test
//     public void bundleVersionConfigTestYaml() throws Exception {
//         framework.withBundles(Utils.url(BND1_YAML_JAR).toString());
//         framework.withBundles(Utils.url(BND2_YAML_JAR).toString());
//         framework = framework.start();
//         TimeUnit.SECONDS.sleep(1);

//         HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
//         assertEquals("Ciao Italian", helloService.sayHello("Italian"));
//     }

//     @Test
//     public void bundleFactoryConfigTestYaml() throws Exception {
//         framework.withBundles(Utils.url(BND_FACTORY_YAML_JAR).toString());
//         framework.start();
//         TimeUnit.SECONDS.sleep(1);

//         HelloService helloService = framework.getService(HelloService.class, "(type=Test-1)").get(5, TimeUnit.SECONDS);
//         assertEquals("Bom dia Portuguese", helloService.sayHello("Portuguese"));

//         HelloService helloService2 = framework.getService(HelloService.class, "(type=Test-2)").get(5, TimeUnit.SECONDS);
//         assertEquals("Namaste India", helloService2.sayHello("India"));
//     }
    
//     @Test
//     public void bundleFileConfigTest() throws Exception {
//         System.setProperty(Activator.CONFIG_DIR, SIMPLE_CONFDIR_CFG);
//         framework.withBundles(Utils.url(BND1_JSON_JAR).toString());
//         framework = framework.start();
//         TimeUnit.SECONDS.sleep(1);

//         HelloService helloService = framework.getService(HelloService.class).get(5, TimeUnit.SECONDS);
//         assertEquals("Privet Russia", helloService.sayHello("Russia"));
//     }
// }
