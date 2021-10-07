/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * @author marynows
 * 
 */
public class JsonAnnotationTestUtil {

	public static void assertJsonPropertyOrder(final Class<?> clazz, final String... values) {
		final JsonPropertyOrder annotation = clazz.getAnnotation(JsonPropertyOrder.class);
		assertNotNull(annotation);
		assertArrayEquals(values, annotation.value());
	}

	public static void assertJsonProperty(final Class<?> clazz, final String fieldName, final String value)
			throws NoSuchFieldException, SecurityException {
		final JsonProperty annotation = clazz.getDeclaredField(fieldName).getAnnotation(JsonProperty.class);
		assertNotNull(annotation);
		assertEquals(value, annotation.value());
	}

	public static void assertJsonEnum(final Class<? extends Enum<?>> enumType) throws NoSuchMethodException,
			SecurityException {
		assertNotNull(enumType.getMethod("toString").getAnnotation(JsonValue.class));
		assertNotNull(enumType.getMethod("fromValue", String.class).getAnnotation(JsonCreator.class));
	}
}
