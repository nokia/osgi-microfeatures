/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import javax.ws.rs.core.Response.StatusType;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = ErrorBuilderFactory.class)
@Loggable
public class ErrorBuilderFactory {
	@ServiceDependency(filter = "(&(from=statusType)(to=errorCode))")
	private Converter<StatusType, Long> statusType2ErrorCodeConverter;

	public ErrorBuilder code(final long code, final String message) {
		return createErrorBuilder(code).message(message);
	}

	public ErrorBuilder status(final StatusType status, final String message) {
		final Long code = statusType2ErrorCodeConverter.convertTo(status);
		return code(code, message);
	}

	public ErrorBuilder exception(final ErrorException exception) {
		return code(exception.getErrorCode().getCode(), exception.getMessage()).embedded(exception.getName(),
				exception.getValue());
	}

	protected ErrorBuilder createErrorBuilder(final long code) {
		return new ErrorBuilder(code);
	}
}
