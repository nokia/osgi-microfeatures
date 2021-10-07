/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.convert;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * Status type <-> Error code number
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "statusType")
@Property(name = "to", value = "errorCode")
public class StatusType2ErrorCodeConverter implements Converter<StatusType, Long> {
	private static final long CLS_LOWEST_CODE = 100;
	private static final long CLS_HIGHEST_CODE = 199;
	private static final long HTTP_CLIENT_ERROR_CODE = 400;
	private static final long HTTP_MAX_CODE = 600;

	@Override
	public Long convertTo(final StatusType status) {
		if (status == null) {
			return 0L;
		} else if (status.getFamily() == Family.CLIENT_ERROR) {
			return Long.valueOf(status.getStatusCode() - HTTP_CLIENT_ERROR_CODE);
		}
		return Long.valueOf(status.getStatusCode());
	}

	@Override
	public StatusType convertFrom(final Long code) {
		if (code < CLS_LOWEST_CODE) {
			return statusFromStatusCode(code + HTTP_CLIENT_ERROR_CODE);
		} else if (code > CLS_HIGHEST_CODE && code < HTTP_MAX_CODE) {
			return statusFromStatusCode(code);
		} else if (code == ErrorCode.DUPLICATED_CLIENT_ID.getCode()) {
			return Status.PRECONDITION_FAILED;
		}
		return Status.BAD_REQUEST;
	}

	private static Status statusFromStatusCode(final Long code) {
		final Status status = Status.fromStatusCode(code.intValue());
		return status == null ? Status.BAD_REQUEST : status;
	}
}
