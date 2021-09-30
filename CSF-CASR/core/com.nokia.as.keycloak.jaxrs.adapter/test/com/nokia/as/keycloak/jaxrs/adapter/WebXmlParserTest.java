package com.nokia.as.keycloak.jaxrs.adapter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class WebXmlParserTest {

	@Test
	public void parseFileAndString() throws Exception {
		File file = new File("resources/web.xml");
		WebXml webXml = new WebXmlParser().parse(file);
		assertEquals(webXml.getSecurityConstraints().get(0).getAuthRoles().iterator().next(), "premium");

		String fileContents = new String(Files.readAllBytes(file.toPath()), "UTF-8");
		WebXml webXml2 = new WebXmlParser().parse(fileContents);
		assertEquals(webXml2.getSecurityConstraints().get(0).getAuthRoles().iterator().next(), "premium");
	}
	
	@Test
	public void parsePatterns() throws Exception {
		File file = new File("resources/web.xml");
		WebXml webXml = new WebXmlParser().parse(file);

		List<String> methods = webXml.getSecurityConstraints().get(0).getWebResourceCollection().get(0).getPatterns();
		assertEquals(2, methods.size());
		Iterator<String> iterator = methods.iterator();
		assertEquals(iterator.next(), "/profile.jsp");
		assertEquals(iterator.next(), "/admin.jsp");
	}

	@Test
	public void parseHttpMethods() throws Exception {
		File file = new File("resources/web.xml");
		WebXml webXml = new WebXmlParser().parse(file);

		List<String> methods = webXml.getSecurityConstraints().get(0).getWebResourceCollection().get(0).getMethods();
		assertEquals(4, methods.size());
		Iterator<String> iterator = methods.iterator();
		assertEquals(iterator.next(), "GET");
		assertEquals(iterator.next(), "POST");
		assertEquals(iterator.next(), "PUT");
		assertEquals(iterator.next(), "DELETE");

		List<String> omittedMethods = webXml.getSecurityConstraints().get(1).getWebResourceCollection().get(0)
				.getOmittedMethods();
		assertEquals(2, omittedMethods.size());
		iterator = omittedMethods.iterator();
		assertEquals(iterator.next(), "GET");
		assertEquals(iterator.next(), "POST");
	}
}