/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import java.util.Objects;

import com.nsn.ood.cls.rest.exception.ViolationException;
import com.nsn.ood.cls.util.Strings;


/**
 * @author marynows
 * 
 */
public class ViolationExceptionBuilder {
	private final String message;
	private String path;
	private String value;

	ViolationExceptionBuilder(final String message) {
		this.message = Messages.getValidationError(message);
	}

	public ViolationException build() {
		return new ViolationException(this.message, this.path, this.value);
	}

	public ViolationExceptionBuilder path(final String array, final Integer index, final String field) {
		final StringBuilder sb = new StringBuilder(Strings.nullToEmpty(array));
		if (index != null) {
			sb.append("[").append(index).append("]");
		}
		if (!Strings.isNullOrEmpty(field)) {
			if (sb.length() > 0) {
				sb.append(".");
			}
			sb.append(field);
		}
		this.path = Strings.emptyToNull(sb.toString());
		return this;
	}

	public ViolationExceptionBuilder value(final String value) {
		this.value = value;
		return this;
	}

	public ViolationExceptionBuilder value(final Object value) {
		this.value = Objects.toString(value, null);
		return this;
	}
}
