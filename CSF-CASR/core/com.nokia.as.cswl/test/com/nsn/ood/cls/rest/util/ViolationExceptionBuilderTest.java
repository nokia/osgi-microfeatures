/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.rest.exception.ViolationException;


/**
 * @author marynows
 * 
 */
public class ViolationExceptionBuilderTest {

	@Test
	public void testCreate() throws Exception {
		assertException(new ViolationExceptionBuilder("message").build(), "!message!", null, null);
	}

	@Test
	public void testPath() throws Exception {
		assertException(new ViolationExceptionBuilder("p").path("first", 7, "second").build(), "!p!",
				"first[7].second", null);

		assertException(new ViolationExceptionBuilder("p").path("", 7, "second").build(), "!p!", "[7].second", null);
		assertException(new ViolationExceptionBuilder("p").path(null, 7, "second").build(), "!p!", "[7].second", null);
		assertException(new ViolationExceptionBuilder("p").path("first", null, "second").build(), "!p!",
				"first.second", null);
		assertException(new ViolationExceptionBuilder("p").path("first", 7, "").build(), "!p!", "first[7]", null);
		assertException(new ViolationExceptionBuilder("p").path("first", 7, null).build(), "!p!", "first[7]", null);

		assertException(new ViolationExceptionBuilder("p").path(null, 7, null).build(), "!p!", "[7]", null);
		assertException(new ViolationExceptionBuilder("p").path(null, null, "second").build(), "!p!", "second", null);
		assertException(new ViolationExceptionBuilder("p").path("first", null, null).build(), "!p!", "first", null);
		assertException(new ViolationExceptionBuilder("p").path(null, null, null).build(), "!p!", null, null);
	}

	@Test
	public void testValue() throws Exception {
		assertException(new ViolationExceptionBuilder("v").value(null).build(), "!v!", null, null);
		assertException(new ViolationExceptionBuilder("v").value("").build(), "!v!", null, "");
		assertException(new ViolationExceptionBuilder("v").value("value").build(), "!v!", null, "value");
		assertException(new ViolationExceptionBuilder("v").value(77).build(), "!v!", null, "77");
		assertException(new ViolationExceptionBuilder("v").value(SettingKey.EMAIL_SENDER).build(), "!v!", null,
				"emailSender");
	}

	private void assertException(final ViolationException exception, final String expectedMessage,
			final String expectedPath, final String expectedValue) {
		assertEquals(expectedMessage, exception.getMessage());
		assertEquals(violationError(expectedMessage, expectedPath, expectedValue), exception.getError());
	}
}
