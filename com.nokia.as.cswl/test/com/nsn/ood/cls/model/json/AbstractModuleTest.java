/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.json;

import static org.easymock.EasyMock.capture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.easymock.Capture;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module.SetupContext;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.SimpleType;


/**
 * @author marynows
 * 
 */
public abstract class AbstractModuleTest {
	private Serializers serializers = null;
	private Deserializers deserializers = null;

	protected void assertNameAndVersion(final SimpleModule module, final String name) {
		assertEquals(name, module.getModuleName());
	}

	protected void captureConfig(final SimpleModule module, final boolean serializers, final boolean deserializers) {
		final SetupContext contextMock = createMock(SetupContext.class);

		final Capture<Serializers> capturedSerializers = new Capture<>();
		if (serializers) {
			contextMock.addSerializers(capture(capturedSerializers));
		}

		final Capture<Deserializers> capturedDeserializers = new Capture<>();
		if (deserializers) {
			contextMock.addDeserializers(capture(capturedDeserializers));
		}

		replayAll();
		module.setupModule(contextMock);
		verifyAll();

		if (serializers) {
			this.serializers = capturedSerializers.getValue();
		}
		if (deserializers) {
			this.deserializers = capturedDeserializers.getValue();
		}
	}

	protected void assertSerializer(final Class<?> typeClass, final Class<? extends JsonSerializer<?>> serializerClass) {
		final SimpleType type = SimpleType.construct(typeClass);
		final JsonSerializer<?> serializer = this.serializers.findSerializer(null, type, null);
		assertTrue(serializerClass.isInstance(serializer));
	}

	protected void assertDeserializer(final Class<?> typeClass,
			final Class<? extends JsonDeserializer<?>> deserializerClass) throws Exception {
		final SimpleType type = SimpleType.construct(typeClass);
		final JsonDeserializer<?> deserializer = this.deserializers.findBeanDeserializer(type, null, null);
		assertTrue(deserializerClass.isInstance(deserializer));
	}
}