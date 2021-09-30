/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
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


/**
 * @author marynows
 * 
 */
public class EmbeddedDeserializer extends JsonDeserializer<Embedded> {
	private static final TypeReference<Object> DEFAULT_TYPE_REF = new TypeReference<Object>() {
	};

	private final Map<String, TypeReference<?>> types = new HashMap<>();

	public EmbeddedDeserializer() {
		this.types.put("clients", new TypeReference<ArrayList<Client>>() {
		});
		this.types.put("licenses", new TypeReference<ArrayList<License>>() {
		});
		this.types.put("features", new TypeReference<ArrayList<Feature>>() {
		});
		this.types.put("errors", new TypeReference<ArrayList<Error>>() {
		});
		this.types.put("etags", new TypeReference<ETagError>() {
		});
		this.types.put("feature", new TypeReference<FeatureError>() {
		});
		this.types.put("violation", new TypeReference<ViolationError>() {
		});
		this.types.put("violations", new TypeReference<ArrayList<ViolationError>>() {
		});
		this.types.put("client", new TypeReference<Client>() {
		});
		this.types.put("license", new TypeReference<License>() {
		});
		this.types.put("metadata", new TypeReference<MetaData>() {
		});
	}

	public TypeReference<?> addType(final String name, final TypeReference<?> typeRef) {
		return this.types.put(name, typeRef);
	}

	@Override
	public Embedded deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
		final ObjectMapper objectMapper = (ObjectMapper) jp.getCodec();
		return createEmbedded((JsonNode) objectMapper.readTree(jp), objectMapper);
	}

	private Embedded createEmbedded(final JsonNode rootNode, final ObjectMapper objectMapper) {
		final Embedded embedded = new Embedded();
		final Iterator<Entry<String, JsonNode>> fieldsIterator = rootNode.fields();
		while (fieldsIterator.hasNext()) {
			final Entry<String, JsonNode> field = fieldsIterator.next();
			final TypeReference<?> typeRef = getTypeRef(field.getKey());
			final Object object = createObject(field.getValue(), typeRef, objectMapper);
			embedded.setAdditionalProperty(field.getKey(), object);
		}
		return embedded;
	}

	private Object createObject(final JsonNode node, final TypeReference<?> typeRef, final ObjectMapper objectMapper) {
		return objectMapper.convertValue(node, typeRef);
	}

	private TypeReference<?> getTypeRef(final String nodeName) {
		final TypeReference<?> tr = this.types.get(nodeName);
		return tr == null ? DEFAULT_TYPE_REF : tr;
	}
}
