/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.error;

import org.apache.felix.dm.annotation.api.Component;

import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.errors.FeatureError;
import com.nsn.ood.cls.model.gen.errors.ViolationError;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Loggable
@Component(provides = ErrorExceptionFactory.class)
public class ErrorExceptionFactory {

	public ErrorException violation(final Throwable cause, final ViolationError violation) {
		return new ErrorException(ErrorCode.CONDITIONS_FAIL, cause, "violation", violation);
	}

	public ErrorException license(final ErrorCode errorCode, final Throwable cause, final String cljlErrorCode,
			final License license) {
		return new ErrorException(errorCode, cause, "license", license, cljlErrorCode);
	}

	public ErrorException licenseNotFound(final License license) {
		return new ErrorException(ErrorCode.RESOURCE_NOT_FOUND, "License does not exist", "license", license);
	}

	public ErrorException client(final ErrorCode errorCode, final Throwable cause, final Client client) {
		return new ErrorException(errorCode, cause, "client", client);
	}

	public ErrorException clientNotFound(final Client client) {
		return new ErrorException(ErrorCode.RESOURCE_NOT_FOUND, "Client does not exist", "client", client);
	}

	public ErrorException feature(final ErrorCode errorCode, final Throwable cause, final FeatureError feature) {
		return new ErrorException(errorCode, cause, "feature", feature);
	}

	public ErrorException featureNotFound(final FeatureError feature) {
		return new ErrorException(ErrorCode.RESOURCE_NOT_FOUND, "Feature does not exist", "feature", feature);
	}

	public ErrorException setting(final ErrorCode errorCode, final Throwable cause, final Setting setting) {
		return new ErrorException(errorCode, cause, "setting", setting);
	}

	public ErrorException error(final ErrorCode errorCode, final Throwable cause) {
		return new ErrorException(errorCode, cause, null, null);
	}
}
