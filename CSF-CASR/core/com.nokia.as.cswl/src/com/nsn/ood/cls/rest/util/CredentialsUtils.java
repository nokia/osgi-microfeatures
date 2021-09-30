/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;

import com.nsn.ood.cls.util.Strings;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 *
 */
public final class CredentialsUtils {
	private static final String SEPARATOR = ":";

	private CredentialsUtils() {
	}

	public static String encodeAsHeader(final Credentials credentials) {
		return "Basic " + encode(credentials.getUser(), credentials.getPassword());
	}

	public static Credentials decodeFromHeader(final String authorization) {
		return decode(removeHeaderPrefix(authorization));
	}

	private static String removeHeaderPrefix(final String authorization) {
		final String encodedAuth = authorization.replaceFirst("Basic ", "");
		if (encodedAuth.isEmpty() || (encodedAuth.length() == authorization.length())) {
			throw new CLSIllegalArgumentException("Incorrect authorization string: \"" + authorization + "\"");
		}
		return encodedAuth;
	}

	private static String encode(final String user, final String password) {
		final String auth = Strings.nullToEmpty(user) + SEPARATOR + Strings.nullToEmpty(password);
		return Base64.encodeBase64String(auth.getBytes(Charset.forName("UTF-8")));
	}

	private static Credentials decode(final String authorization) {
		final String encodedAuth = getEncodedAuthorizationString(authorization);
		final String decodedAuth = new String(Base64.decodeBase64(encodedAuth), Charset.forName("UTF-8"));
		final String[] credentials = splitToList(decodedAuth);

		final String user = getArrayValue(credentials, 0);
		final String password = getArrayValue(credentials, 1);
		return new CredentialsImpl(user, password);
	}

	private static String getEncodedAuthorizationString(final String authorization) {
		if (Strings.isNullOrEmpty(authorization)) {
			throw new CLSIllegalArgumentException("Incorrect authorization string: \"" + authorization + "\"");
		}
		return authorization;
	}

	private static String[] splitToList(final String string) {
		final int index = string.indexOf(SEPARATOR);
		if (index >= 0) {
			return new String[] {
					string.substring(0, index), string.substring(index + SEPARATOR.length()) };
		} else {
			return new String[0];

		}
	}

	private static String getArrayValue(final String[] array, final int index) {
		return array.length > index ? array[index] : "";
	}

	private static final class CredentialsImpl implements Credentials {
		private final String user;
		private final String password;

		private CredentialsImpl(final String user, final String password) {
			this.user = user;
			this.password = password;
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
