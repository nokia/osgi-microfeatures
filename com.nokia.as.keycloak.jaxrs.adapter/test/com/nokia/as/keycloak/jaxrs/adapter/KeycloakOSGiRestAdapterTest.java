// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.keycloak.jaxrs.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.HashSet;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

//test cases 1-5 from : https://docs.oracle.com/javaee/6/tutorial/doc/gmmku.html
public class KeycloakOSGiRestAdapterTest {

	KeycloakOSGiRestAdapter adapter = new KeycloakOSGiRestAdapter();
	private ContainerRequestContext context;
	private Request request;
	private UriInfo uriInfo;

	@Before
	public void init() throws Exception {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		context = mock(ContainerRequestContext.class);
		request = mock(Request.class);
		uriInfo = mock(UriInfo.class);
		when(context.getRequest()).thenReturn(request);
		when(context.getUriInfo()).thenReturn(uriInfo);
	}

	private HashSet<String> rolesFromToken(String... values) {
		HashSet<String> hashSet = new HashSet<String>();
		for (String value : values)
			hashSet.add(value);
		return hashSet;
	}

	@Test
	public void test1_all_methods_secured_role_sales() throws Exception {
		File file = new File("resources/test1.xml");
		WebXml webXml = new WebXmlParser().parse(file);
		adapter.setWebXml(webXml);
		when(uriInfo.getRequestUri()).thenReturn(new URI("http://localhost:80/company"));

		assertThat(adapter.uriRequiresAuth(context)).isTrue();
		assertThat(adapter.allowAccesByRole(context, rolesFromToken())).isTrue(); // allow everyone
		assertThat(adapter.allowAccesByRole(context, rolesFromToken("doesnt exist"))).isFalse();
		assertThat(adapter.allowAccesByRole(context, rolesFromToken("sales"))).isTrue();
	}

	@Test
	public void test2_secure_only_get_role_sales_nested_wildcard_urls() throws Exception {
		File file = new File("resources/test2.xml");
		WebXml webXml = new WebXmlParser().parse(file);
		adapter.setWebXml(webXml);
		when(uriInfo.getRequestUri()).thenReturn(new URI("http://localhost:80/company/nested"));

		// GET secure
		when(context.getMethod()).thenReturn("GET");
		assertThat(adapter.uriRequiresAuth(context)).isTrue(); // protected because role is set
		assertThat(adapter.allowAccesByRole(context, rolesFromToken())).isTrue(); // allow everyone
		assertThat(adapter.allowAccesByRole(context, rolesFromToken("doesnt exist"))).isFalse();
		assertThat(adapter.allowAccesByRole(context, rolesFromToken("sales"))).isTrue();

		// OPTIONS unsecure
		when(context.getMethod()).thenReturn("OPTIONS");
		assertThat(adapter.uriRequiresAuth(context)).isFalse();
	}

	@Test
	public void test3_4_specific_protection_by_method() throws Exception {
		File file = new File("resources/test3-4.xml");
		WebXml webXml = new WebXmlParser().parse(file);
		adapter.setWebXml(webXml);
		when(uriInfo.getRequestUri()).thenReturn(new URI("http://localhost:80/company"));

		// GET unsecure
		when(context.getMethod()).thenReturn("GET");
		assertThat(adapter.uriRequiresAuth(context)).isFalse();
		assertThat(adapter.allowAccesByRole(context, rolesFromToken())).isTrue(); // allow everyone
		assertThat(adapter.allowAccesByRole(context, rolesFromToken("doesnt exist"))).isTrue();
		assertThat(adapter.allowAccesByRole(context, rolesFromToken("sales"))).isTrue();

		// POST secure role sales
		when(context.getMethod()).thenReturn("POST");
		assertThat(adapter.uriRequiresAuth(context)).isTrue();
		assertThat(adapter.allowAccesByRole(context, rolesFromToken("doesnt exist"))).isFalse();
		assertThat(adapter.allowAccesByRole(context, rolesFromToken("sales"))).isTrue();
	}

	@Test
	public void secure_all_but_one() throws Exception {
		File file = new File("resources/test_not_operator.xml");
		WebXml webXml = new WebXmlParser().parse(file);
		adapter.setWebXml(webXml);

		// /* secure
		when(uriInfo.getRequestUri()).thenReturn(new URI("http://localhost:80/services"));
		assertThat(adapter.uriRequiresAuth(context)).isTrue();
		assertThat(adapter.allowAccesByRole(context, rolesFromToken())).isTrue(); // allow everyone
		assertThat(adapter.allowAccesByRole(context, rolesFromToken("doesnt exist"))).isFalse();
		assertThat(adapter.allowAccesByRole(context, rolesFromToken("sales"))).isTrue();

		// /public/* unsecure
		when(uriInfo.getRequestUri()).thenReturn(new URI("http://localhost:80/public"));
		assertThat(adapter.uriRequiresAuth(context)).isFalse();
	}
	
	@Test
	public void secure_all_but_one_any_role() throws Exception {
		File file = new File("resources/test_any_role.xml");
		WebXml webXml = new WebXmlParser().parse(file);
		adapter.setWebXml(webXml);

		// /* secure
		when(uriInfo.getRequestUri()).thenReturn(new URI("http://localhost:80/services"));
		assertThat(adapter.uriRequiresAuth(context)).isTrue();
		assertThat(adapter.allowAccesByRole(context, rolesFromToken())).isTrue(); // allow everyone
		assertThat(adapter.allowAccesByRole(context, rolesFromToken("doesnt exist"))).isTrue();
		assertThat(adapter.allowAccesByRole(context, rolesFromToken("sales"))).isTrue();

		// /public/* unsecure
		when(uriInfo.getRequestUri()).thenReturn(new URI("http://localhost:80/public"));
		assertThat(adapter.uriRequiresAuth(context)).isFalse();
	}

}
