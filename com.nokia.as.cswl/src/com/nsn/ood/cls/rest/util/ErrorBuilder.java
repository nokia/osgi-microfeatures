/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.util.HalResource;
import com.nsn.ood.cls.model.util.HalResourceBuilder;


/**
 * @author marynows
 * 
 */
public class ErrorBuilder {
	private final Error error;
	private final HalResourceBuilder<Error> halBuilder;

	ErrorBuilder(final long code) {
		this.error = new Error()//
				.withErrorCode(code)//
				.withDeveloperMessage(Messages.getErrorDevMessage(code))//
				.withUserMessage(Messages.getErrorUserMessage(code));
		this.halBuilder = HalResource.builder(this.error);
	}

	public Error build() {
		return this.halBuilder.build();
	}

	public ErrorBuilder message(final String devMessage) {
		return message(devMessage, null);
	}

	public ErrorBuilder message(final String devMessage, final String userMessage) {
		if (devMessage != null) {
			this.error.setDeveloperMessage(devMessage);
		}
		if (userMessage != null) {
			this.error.setUserMessage(userMessage);
		}
		return this;
	}

	public ErrorBuilder embedded(final String name, final Object object) {
		this.halBuilder.addEmbeddedIfNotNull(name, object);
		return this;
	}
}
