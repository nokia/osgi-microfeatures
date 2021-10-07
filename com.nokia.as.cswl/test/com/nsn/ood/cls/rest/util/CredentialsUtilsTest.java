/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public class CredentialsUtilsTest {

	@Test
	public void testEncode() throws Exception {
		assertEncode("Basic Og==", "", "");
		assertEncode("Basic Og==", "", null);
		assertEncode("Basic Og==", null, "");
		assertEncode("Basic Og==", null, null);
		assertEncode("Basic dXNlcjo=", "user", "");
		assertEncode("Basic dXNlcjo=", "user", null);
		assertEncode("Basic dXNlcjpwYXNzd29yZA==", "user", "password");
	}

	private void assertEncode(final String expected, final String user, final String password) {
		assertEquals(expected, CredentialsUtils.encodeAsHeader(new CredentialsTestImpl(user, password)));
	}

	@Test
	public void testDecode() throws Exception {
		assertDecode("", "", "Basic Og==");
		assertDecode("", "", "Basic x");
		assertDecode("user", "", "Basic dXNlcjo=");
		assertDecode("", "password", "Basic OnBhc3N3b3Jk=");
		assertDecode("user", "password", "Basic dXNlcjpwYXNzd29yZA==");
	}

	@Test
	public void testDecodeFromHeader() throws Exception {
		assertDecodeFromHeaders("", "", "Basic Og==");
		assertDecodeFromHeaders("", "", "Basic x");
		assertDecodeFromHeaders("user", "", "Basic dXNlcjo=");
		assertDecodeFromHeaders("", "password", "Basic OnBhc3N3b3Jk=");
		assertDecodeFromHeaders("user", "password", "Basic dXNlcjpwYXNzd29yZA==");
	}

	@Test
	public void testDecodeIllegalValues() throws Exception {
		expectDecodeException("");
		expectDecodeFromHeadersException("Basic ");
		expectDecodeFromHeadersException("BasicOg==");
		expectDecodeFromHeadersException("Test Og==");
	}

	@Test
	public void testEncodeAsHeaders() throws Exception {
		assertEquals("Basic dXNlcjpwYXNzd29yZA==",
				CredentialsUtils.encodeAsHeader(new CredentialsTestImpl("user", "password")));
	}

	private void expectDecodeException(final String auth) {
		try {
			CredentialsUtils.decodeFromHeader(auth);
			fail();
		} catch (final CLSIllegalArgumentException e) {
		}
	}

	private void expectDecodeFromHeadersException(final String auth) {
		try {
			CredentialsUtils.decodeFromHeader(auth);
			fail();
		} catch (final CLSIllegalArgumentException e) {
		}
	}

	private void assertDecode(final String expectedUser, final String expectedPassword, final String auth) {
		final Credentials credentials = CredentialsUtils.decodeFromHeader(auth);
		assertDecode(expectedUser, expectedPassword, credentials);
	}

	private void assertDecode(final String expectedUser, final String expectedPassword, final Credentials credentials) {
		assertEquals(expectedUser, credentials.getUser());
		assertEquals(expectedPassword, credentials.getPassword());
	}

	private void assertDecodeFromHeaders(final String expectedUser, final String expectedPassword, final String auth) {
		final Credentials credentials = CredentialsUtils.decodeFromHeader(auth);
		assertDecode(expectedUser, expectedPassword, credentials);
	}

	private static class CredentialsTestImpl implements Credentials {
		private final String password;
		private final String user;

		private CredentialsTestImpl(final String user, final String password) {
			this.password = password;
			this.user = user;
		}

		@Override
		public String getUser() {
			return this.user;
		}

		@Override
		public String getPassword() {
			return this.password;
		}
	}
}
