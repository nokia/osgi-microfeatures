/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Links;


/**
 * @author marynows
 * 
 */
public class LinksDeserializer extends JsonDeserializer<Links> {
	private static final TypeReference<Link> LINK_TYPE_REF = new TypeReference<Link>() {
	};
	private static final TypeReference<ArrayList<Link>> LINKS_ARRAY_TYPE_REF = new TypeReference<ArrayList<Link>>() {
	};

	@Override
	public Links deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
		final ObjectMapper objectMapper = (ObjectMapper) jp.getCodec();
		return createLinks((JsonNode) objectMapper.readTree(jp), objectMapper);
	}

	private Links createLinks(final JsonNode rootNode, final ObjectMapper objectMapper) {
		final Links links = new Links();
		final Iterator<Entry<String, JsonNode>> fieldsIterator = rootNode.fields();
		while (fieldsIterator.hasNext()) {
			final Entry<String, JsonNode> field = fieldsIterator.next();
			final Object object = createLinkObject(field.getValue(), objectMapper);
			links.setAdditionalProperty(field.getKey(), object);
		}
		return links;
	}

	private Object createLinkObject(final JsonNode node, final ObjectMapper objectMapper) {
		return createObject(node, node.isArray() ? LINKS_ARRAY_TYPE_REF : LINK_TYPE_REF, objectMapper);
	}

	private Object createObject(final JsonNode node, final TypeReference<?> typeRef, final ObjectMapper objectMapper) {
		return objectMapper.convertValue(node, typeRef);
	}
}
