/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.error;

import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.errors.FeatureError;
import com.nsn.ood.cls.model.gen.errors.ViolationError;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.util.CollectionUtils;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = ServiceExceptionFactory.class)
@Loggable
public class ServiceExceptionFactory {
	@ServiceDependency
	private ErrorExceptionFactory errorFactory;

	public ServiceException violation(final Throwable cause, final ViolationError violation) {
		return new ServiceException(this.errorFactory.violation(cause, violation), false);
	}

	public ServiceException license(final ErrorCode errorCode, final Throwable cause, final String cljlErrorCode,
			final License license) {
		return new ServiceException(this.errorFactory.license(errorCode, cause, cljlErrorCode, license), false);
	}

	public ServiceException licenseNotFound(final License license) {
		return new ServiceException(this.errorFactory.licenseNotFound(license), true);
	}

	public ServiceException client(final ErrorCode errorCode, final Throwable cause, final Client client) {
		return new ServiceException(this.errorFactory.client(errorCode, cause, client), false);
	}

	public ServiceException clientNotFound(final Client client) {
		return new ServiceException(this.errorFactory.clientNotFound(client), true);
	}

	public ServiceException feature(final ErrorCode errorCode, final Throwable cause, final FeatureError feature) {
		return new ServiceException(this.errorFactory.feature(errorCode, cause, feature), false);
	}

	public ServiceException featureNotFound(final FeatureError feature) {
		return new ServiceException(this.errorFactory.featureNotFound(feature), true);
	}

	public ServiceException setting(final ErrorCode errorCode, final Throwable cause, final Setting setting) {
		return new ServiceException(this.errorFactory.setting(errorCode, cause, setting), false);
	}

	public ServiceException error(final ErrorCode errorCode, final Throwable cause) {
		return new ServiceException(this.errorFactory.error(errorCode, cause), false);
	}

	public ServiceException exceptions(final List<ErrorException> errorExceptions) {
		verifyErrorsList(errorExceptions);
		return new ServiceException(errorExceptions);
	}

	public ServiceException internalError(final Throwable cause) {
		return new ServiceException(this.errorFactory.error(ErrorCode.INTERNAL_ERROR, cause), false);
	}

	private void verifyErrorsList(final List<ErrorException> exceptions) {
		if (CollectionUtils.isEmpty(exceptions)) {
			throw new CLSIllegalArgumentException("Must contain at least one error");
		}
	}
}
