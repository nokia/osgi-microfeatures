/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.json;

import static com.nsn.ood.cls.model.test.LinkTestUtil.link;
import static com.nsn.ood.cls.model.test.LinkTestUtil.linksList;
import static org.easymock.EasyMock.cmp;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.easymock.LogicalOperator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Links;
import com.nsn.ood.cls.model.test.TypeReferenceComparator;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	JsonNode.class })
public class LinksDeserializerTest {
	private static final TypeReferenceComparator COMPARATOR = new TypeReferenceComparator();

	@Test
	public void testDeserialize() throws Exception {
		final JsonParser jsonParserMock = createMock(JsonParser.class);
		final DeserializationContext deserializationContextMock = createMock(DeserializationContext.class);
		final ObjectMapper objectMapperMock = createMock(ObjectMapper.class);
		final JsonNode rootNodeMock = createMock(JsonNode.class);
		final JsonNode linkNodeMock = createMock(JsonNode.class);
		final JsonNode arrayNodeMock = createMock(JsonNode.class);

		final Map<String, JsonNode> nodesMap = new LinkedHashMap<>();
		nodesMap.put("link", linkNodeMock);
		nodesMap.put("array", arrayNodeMock);

		expect(jsonParserMock.getCodec()).andReturn(objectMapperMock);
		expect(objectMapperMock.readTree(jsonParserMock)).andReturn(rootNodeMock);
		expect(rootNodeMock.fields()).andReturn(nodesMap.entrySet().iterator());

		expect(linkNodeMock.isArray()).andReturn(false);
		expect(objectMapperMock.convertValue(eq(linkNodeMock), cmp(new TypeReference<Link>() {
		}, COMPARATOR, LogicalOperator.EQUAL))).andReturn(link("href"));

		expect(nodesMap.get("array").isArray()).andReturn(true);
		expect(objectMapperMock.convertValue(eq(nodesMap.get("array")), cmp(new TypeReference<List<Link>>() {
		}, COMPARATOR, LogicalOperator.EQUAL))).andReturn(linksList(link("l1"), link("l2")));

		/*
		replayAll();
		final LinksDeserializer deserializer = new LinksDeserializer();
		final Links result = deserializer.deserialize(jsonParserMock, deserializationContextMock);ppg
		verifyAll();

		assertEquals(2, result.getAdditionalProperties().size());
		assertEquals(link("href"), result.getAdditionalProperties().get("link"));
		assertEquals(linksList(link("l1"), link("l2")), result.getAdditionalProperties().get("array"));
		*/
	}
}
