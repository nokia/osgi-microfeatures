/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import static com.nsn.ood.cls.model.test.ResourceTestUtil.assertResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.util.HalResource;


/**
 * @author marynows
 * 
 */
public class ErrorBuilderTest {

	@Test
	public void testBuild() throws Exception {
		final Error error = new ErrorBuilder(1337L).build();
		assertError(error, 1337L, "!1337.dev!", "!1337.user!", 0);
	}

	@Test
	public void testMessage() throws Exception {
		{
			final Error error = new ErrorBuilder(1337L).message("dev").build();
			assertError(error, 1337L, "dev", "!1337.user!", 0);
		}
		{
			final Error error = new ErrorBuilder(1337L).message("dev", "user").build();
			assertError(error, 1337L, "dev", "user", 0);
		}
		{
			final Error error = new ErrorBuilder(1337L).message(null, null).build();
			assertError(error, 1337L, "!1337.dev!", "!1337.user!", 0);
		}
	}

	@Test
	public void testEmbedded() throws Exception {
		final Error error = new ErrorBuilder(1337L).embedded("name", "value").embedded("test", null).build();
		assertError(error, 1337L, "!1337.dev!", "!1337.user!", 1);
		assertEmbedded(error, "name", "value");
		assertEmbedded(error, "test", null);
	}

	private void assertEmbedded(final Error error, final String name, final Object object) {
		assertEquals(object, HalResource.getEmbedded(error, name));
	}

	private void assertError(final Error error, final Long expectedCode, final String expectedDevMsg,
			final String expectedUserMsg, final int expectedEmbeddedSize) {
		assertEquals(expectedCode, error.getErrorCode());
		assertEquals(expectedDevMsg, error.getDeveloperMessage());
		assertEquals(expectedUserMsg, error.getUserMessage());
		assertNull(error.getMoreInfo());
		assertResource(error, 0, expectedEmbeddedSize);
	}
}
