// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.common.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.log4j.Level;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;

import com.nokia.as.jaxrs.jersey.common.ApplicationConfiguration;
import com.nokia.as.jaxrs.jersey.common.ServerContext;

@RunWith(MockitoJUnitRunner.class)
public class JaxRsResourceRegistrationTest {


	private JaxRsResourceRegistration _registration;

	@Path("/foo")
	class TestResource {
		@GET
		public String getFoo() {
			return "foo";
		}
	}

	class TestFeature implements Feature {
		@Override
		public boolean configure(FeatureContext arg0) {
			return false;
		}
	}

	@Before
	public void setUp() throws Exception {
		_registration = new JaxRsResourceRegistration();
		_registration.start();
		_registration._bc = mock(BundleContext.class);
	}

	@Test
	public void testBindJaxRsResource_not_registrable_no_annotation() {
		_registration.bindJaxRsResource(new Object(), null);
		assertThat(_registration.getLoadedSingletons()).isEmpty();
	}

	@Test
	public void testBindJaxRsResource_not_registrable_no_server_context() {
		JaxRsResourceRegistration.log.setLevel(Level.INFO);
		_registration.bindJaxRsResource(new TestResource(), null);
		assertThat(_registration.getLoadedSingletons()).isEmpty();

		JaxRsResourceRegistration.log.setLevel(Level.ERROR);
		_registration.bindJaxRsResource(new TestResource(), null);
		assertThat(_registration.getLoadedSingletons()).isEmpty();
	}

	@Test
	public void testBindJaxRsResource_registrable_path() {
		ServerContext serverContext = new ServerContext();
		serverContext.setAddress(new InetSocketAddress("localhost", 8080));
		_registration.add(serverContext);
		_registration.bindJaxRsResource(new TestResource(), new HashMap<>());
		assertThat(_registration.getLoadedSingletons()).isNotEmpty();
	}

	@Test
	public void testBindJaxRsResource_registrable_feature() {
		JaxRsResourceRegistration.log.setLevel(Level.ERROR);
		ServerContext serverContext = new ServerContext();
		serverContext.setAddress(new InetSocketAddress("localhost", 8080));
		TestFeature resource = new TestFeature();
		_registration.add(serverContext);
		_registration.bindJaxRsResource(resource, new HashMap<>());
		assertThat(_registration.getLoadedSingletons(8080)).isNotEmpty();
		assertThat(_registration.getLoadedSingletons().entrySet().iterator().next().getValue()).contains(resource);
	}

	@Test
	public void testBindJaxRsResource_registrable_resource() {
		JaxRsResourceRegistration.log.setLevel(Level.INFO);
		ServerContext serverContext = new ServerContext();
		serverContext.setAddress(new InetSocketAddress("localhost", 8080));
		_registration.add(serverContext);
		Resource resource = Resource.builder().build();
		_registration.bindJaxRsResource(resource, new HashMap<>());
		assertThat(_registration.getLoadedResources(8080)).isNotEmpty();
		assertThat(_registration.getLoadedResources().entrySet().iterator().next().getValue()).contains(resource);
	}

	@Test
	public void testBindAppConfiguration_no_server_context() {
		JaxRsResourceRegistration.log.setLevel(Level.INFO);
		_registration.bindAppConfiguration(new ApplicationConfiguration() {
			@Override
			public Map<String, Object> getProperties() {
				return null;
			}
		});
		assertThat(_registration.getLoadedProperties()).isEmpty();

		JaxRsResourceRegistration.log.setLevel(Level.ERROR);
		_registration.bindAppConfiguration(new ApplicationConfiguration() {
			@Override
			public Map<String, Object> getProperties() {
				return null;
			}
		});
		assertThat(_registration.getLoadedProperties()).isEmpty();
	}

	@Test
	public void testBindAppConfiguration() {
		ServerContext serverContext = new ServerContext();
		serverContext.setAddress(new InetSocketAddress("localhost", 8080));
		_registration.add(serverContext);

		JaxRsResourceRegistration.log.setLevel(Level.INFO);
		_registration.bindAppConfiguration(new ApplicationConfiguration() {
			@Override
			public Map<String, Object> getProperties() {
				HashMap<String, Object> hashMap = new HashMap<String, Object>();
				hashMap.put("any.property.name", "any.value");
				return hashMap;
			}
		});
		assertThat(_registration.getLoadedProperties()).isNotEmpty();

		JaxRsResourceRegistration.log.setLevel(Level.ERROR);
		_registration.bindAppConfiguration(new ApplicationConfiguration() {
			@Override
			public Map<String, Object> getProperties() {
				HashMap<String, Object> hashMap = new HashMap<String, Object>();
				hashMap.put("jersey.foo", "");
				return hashMap;
			}
		});
		assertThat(_registration.getLoadedProperties(8080)).isNotEmpty();
	}
	
	@Test
	public void testAddJerseyProperties() {
		ServerContext serverContext = new ServerContext();
		serverContext.setAddress(new InetSocketAddress("localhost", 8080));
		_registration.add(serverContext);
		HashMap<String, Object> hashMap = new HashMap<String, Object>();
		String value = "any.value";
		hashMap.put("jersey.prefixed.property.name", value);
		serverContext.setProperties(hashMap);
		_registration.bindAppConfiguration(mock(ApplicationConfiguration.class));
		assertThat(serverContext.getProperties()).containsValue(value);
		assertThat(_registration.getLoadedProperties()).isNotEmpty();
		assertThat(_registration.getLoadedProperties().entrySet().iterator().next().getValue()).containsValue(value);
	}

	@Test
	public void testUnbindJaxRsResource() {
		ServerContext serverContext = new ServerContext();
		serverContext.setAddress(new InetSocketAddress("localhost", 8080));
		_registration.add(serverContext);
		TestResource resource = new TestResource();
		_registration.bindJaxRsResource(resource, new HashMap<>());
		assertThat(_registration.getLoadedSingletons().entrySet().iterator().next().getValue()).contains(resource);
		JaxRsResourceRegistration.log.setLevel(Level.DEBUG);
		_registration.unbindJaxRsResource(resource, new HashMap<>());
		assertThat(_registration.getLoadedSingletons().entrySet().iterator().next().getValue()).doesNotContain(resource);
		TestResource resource2 = new TestResource();
		_registration.unbindJaxRsResource(resource2, new HashMap<>());
		assertThat(_registration.getLoadedSingletons().entrySet().iterator().next().getValue()).doesNotContain(resource);
	}
	
	@Path("/foo")
	public interface TestInterface {
	}
	
	class TestResource2 implements TestInterface {
		@GET
		public String getFoo() {
			return "foo";
		}
	}
	
	@Test
	public void testHasRegisterableAnnotation_interface() {
		ServerContext serverContext = new ServerContext();
		serverContext.setAddress(new InetSocketAddress("localhost", 8080));
		_registration.add(serverContext);
		TestResource2 resource = new TestResource2();
		_registration.bindJaxRsResource(resource, new HashMap<>());
		assertThat(_registration.getLoadedSingletons().entrySet().iterator().next().getValue()).contains(resource);
	}
	
	@Path("/foo")
	class SuperClass {
	}
	
	class TestResource3 extends SuperClass {
		@GET
		public void get() {
		}
	}
	
	@Test
	public void testHasRegisterableAnnotation_extends_not_supported() {
		ServerContext serverContext = new ServerContext();
		serverContext.setAddress(new InetSocketAddress("localhost", 8080));
		_registration.add(serverContext);
		TestResource3 resource = new TestResource3();
		_registration.bindJaxRsResource(resource, new HashMap<>());
		assertThat(serverContext.getApplicationHandler()).isNotNull();
	}
	
	@Test
	public void testFindResourcesByServer_deploy_on_specific_port() {
		ServerContext serverContext = new ServerContext();
		serverContext.setAddress(new InetSocketAddress("localhost", 8080));
		ServerContext serverContext2 = new ServerContext();
		serverContext2.setAddress(new InetSocketAddress("localhost", 8443));
		_registration.add(serverContext);
		_registration.add(serverContext2);

		TestResource resource = new TestResource();
		HashMap<String, String> properties = new HashMap<>();
		properties.put("http.port", "8443");
		_registration.bindJaxRsResource(resource, properties);
		assertThat(_registration.getLoadedSingletons(8443).entrySet().iterator().next().getValue()).contains(resource);
		assertThat(serverContext.getApplicationHandler()).isNotNull();
	}

	@Test
	public void testGetLoadedClasses() {
		ServerContext serverContext = new ServerContext();
		serverContext.setAddress(new InetSocketAddress("localhost", 8080));
		_registration.add(serverContext);

		_registration.bindAppConfiguration(mock(ApplicationConfiguration.class));
		assertThat(_registration.getLoadedClasses(8080)).hasSize(1);
		assertThat(_registration.getLoadedClasses().entrySet().iterator().next().getValue()).contains(RolesAllowedDynamicFeature.class);
	}

}
