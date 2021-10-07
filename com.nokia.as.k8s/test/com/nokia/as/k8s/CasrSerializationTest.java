package com.nokia.as.k8s;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Dictionary;

import org.junit.Assert;
import org.junit.Test;

import com.nokia.as.k8s.controller.CasrResource;
import com.nokia.as.k8s.controller.CasrResource.Configuration;
import com.nokia.as.k8s.controller.CasrResource.Port;
import com.nokia.as.k8s.controller.CasrResource.Runtime.Build;
import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.registry.Registrar;
import com.nokia.as.k8s.controller.serialization.SerializationUtils;

public class CasrSerializationTest {
	
	
	@Test
	public void testSerialization() throws IOException, ClassNotFoundException {
		CasrResource resource = new CasrResource("test", 
				"ns").build();
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		
		oos.writeObject((CustomResource) resource);
		byte[] bytes = bos.toByteArray();
		Assert.assertNotNull(bytes);
		
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bis);
		
		CustomResource deserialized = (CustomResource) ois.readObject();
		Assert.assertEquals(resource, deserialized);
		Assert.assertNotNull(deserialized.attributes());
		Assert.assertNotNull(deserialized.metadata());
		Assert.assertNotNull(deserialized.definition());
		Assert.assertNotNull(deserialized.spec());
	}
	
	@Test
	public void testJSON() throws Exception {
		CasrResource resource = new CasrResource("test","ns")
				.addPort(new Port("test-port", 1000, "TCP"))
				.configuration(new Configuration().addFile("test-file", "test"))
				.runtime(new CasrResource.Runtime().build(new Build().version("19.3.2")))
				.build();
		
		String json = SerializationUtils.toJSON(resource);
		System.out.println(json);
		CustomResource deserialized = SerializationUtils.fromJSON(json, resource.definition());
		
		Assert.assertEquals(resource, deserialized);
		Assert.assertEquals(resource.attributes(), deserialized.attributes());
		Assert.assertEquals(resource.metadata(), deserialized.metadata());
		Assert.assertEquals(resource.definition(), deserialized.definition());
		Assert.assertEquals(resource.spec(), deserialized.spec());

		Assert.assertNotNull(deserialized.attributes());
		Assert.assertNotNull(deserialized.metadata());
		Assert.assertNotNull(deserialized.definition());
		Assert.assertNotNull(deserialized.spec());		
	}
	
	@Test
	public void testYAML() throws Exception {
		CasrResource resource = new CasrResource("test","ns")
				.addPort(new Port("test-port", 1000, "TCP"))
				.configuration(new Configuration().addFile("test-file", "test"))
				.runtime(new CasrResource.Runtime().build(new Build().version("19.3.2")))
				.build();
		
		String yaml = SerializationUtils.toYAML(resource);
		System.out.println(yaml);
		CustomResource deserialized = SerializationUtils.fromYAML(yaml, resource.definition());
		
		Assert.assertEquals(resource, deserialized);
		Assert.assertEquals(resource.attributes(), deserialized.attributes());
		Assert.assertEquals(resource.metadata(), deserialized.metadata());
		Assert.assertEquals(resource.definition(), deserialized.definition());
		Assert.assertEquals(resource.spec(), deserialized.spec());
		
		Assert.assertEquals(resource, deserialized);
		Assert.assertNotNull(deserialized.attributes());
		Assert.assertNotNull(deserialized.metadata());
		Assert.assertNotNull(deserialized.definition());
		Assert.assertNotNull(deserialized.spec());	
	}
	
}
