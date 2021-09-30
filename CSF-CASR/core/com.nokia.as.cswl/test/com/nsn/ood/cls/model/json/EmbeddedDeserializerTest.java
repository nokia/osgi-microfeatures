/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.json;

import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static org.easymock.EasyMock.cmp;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.getInternalState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.easymock.LogicalOperator;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.errors.ETagError;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.errors.FeatureError;
import com.nsn.ood.cls.model.gen.errors.ViolationError;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.hal.Embedded;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.metadata.MetaData;
import com.nsn.ood.cls.model.test.TypeReferenceComparator;


/**
 * @author marynows
 * 
 */
public class EmbeddedDeserializerTest {
	private static final TypeReferenceComparator COMPARATOR = new TypeReferenceComparator();

	@SuppressWarnings("unchecked")
	@Test
	public void testInit() throws Exception {
		final EmbeddedDeserializer deserializer = new EmbeddedDeserializer();
		deserializer.addType("test", new TypeReference<EmbeddedDeserializer>() {
		});

		final Map<String, TypeReference<?>> types = getInternalState(deserializer, Map.class);
		assertEquals(11 + 1, types.size());
		assertTypeRef(new TypeReference<ArrayList<Client>>() {
		}, types.get("clients"));
		assertTypeRef(new TypeReference<ArrayList<License>>() {
		}, types.get("licenses"));
		assertTypeRef(new TypeReference<ArrayList<Feature>>() {
		}, types.get("features"));
		assertTypeRef(new TypeReference<ArrayList<Error>>() {
		}, types.get("errors"));
		assertTypeRef(new TypeReference<ETagError>() {
		}, types.get("etags"));
		assertTypeRef(new TypeReference<FeatureError>() {
		}, types.get("feature"));
		assertTypeRef(new TypeReference<ViolationError>() {
		}, types.get("violation"));
		assertTypeRef(new TypeReference<ArrayList<ViolationError>>() {
		}, types.get("violations"));
		assertTypeRef(new TypeReference<Client>() {
		}, types.get("client"));
		assertTypeRef(new TypeReference<License>() {
		}, types.get("license"));
		assertTypeRef(new TypeReference<MetaData>() {
		}, types.get("metadata"));

		assertTypeRef(new TypeReference<EmbeddedDeserializer>() {
		}, types.get("test"));
	}

	private void assertTypeRef(final TypeReference<?> expected, final TypeReference<?> actual) {
		assertEquals(expected.getType(), actual.getType());
	}

	@Test
	public void testDeserialize() throws Exception {
		final JsonParser jsonParserMock = createMock(JsonParser.class);
		final DeserializationContext deserializationContextMock = createMock(DeserializationContext.class);
		final ObjectMapper objectMapperMock = createMock(ObjectMapper.class);
		final JsonNode rootNodeMock = createMock(JsonNode.class);

		final Map<String, JsonNode> nodesMap = new LinkedHashMap<>();
		nodesMap.put("client", createMock(JsonNode.class));
		nodesMap.put("test", createMock(JsonNode.class));

		expect(jsonParserMock.getCodec()).andReturn(objectMapperMock);
		expect(objectMapperMock.readTree(jsonParserMock)).andReturn(rootNodeMock);
		expect(rootNodeMock.fields()).andReturn(nodesMap.entrySet().iterator());

		expect(objectMapperMock.convertValue(eq(nodesMap.get("client")), cmp(new TypeReference<Client>() {
		}, COMPARATOR, LogicalOperator.EQUAL))).andReturn(client("ccc"));
		expect(objectMapperMock.convertValue(eq(nodesMap.get("test")), cmp(new TypeReference<Object>() {
		}, COMPARATOR, LogicalOperator.EQUAL))).andReturn("value");

		replayAll();
		final EmbeddedDeserializer deserializer = new EmbeddedDeserializer();
		final Embedded result = deserializer.deserialize(jsonParserMock, deserializationContextMock);
		verifyAll();

		assertEquals(2, result.getAdditionalProperties().size());
		assertEquals(client("ccc"), result.getAdditionalProperties().get("client"));
		assertEquals("value", result.getAdditionalProperties().get("test"));
	}
}
