/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import java.util.Objects;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * Error exception <-> Activity detail
 * 
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "errorException")
@Property(name = "to", value = "activityDetail")
public class ErrorException2ActivityDetailConverter implements Converter<ErrorException, ActivityDetail> {

	@Override
	public ActivityDetail convertTo(final ErrorException errorException) {
		if (errorException == null) {
			throw new CLSIllegalArgumentException("Error must not be null");
		}

		final ActivityDetail activityDetail = new ActivityDetail()//
				.withStatus(Status.FAILURE)//
				.withErrorCode(getErrorCode(errorException));

		handleValue(errorException, activityDetail);

		return activityDetail;
	}

	private String getErrorCode(final ErrorException errorException) {
		String errorCode = String.valueOf(errorException.getErrorCode().getCode());
		if (errorException.getInfo() != null) {
			errorCode += "," + errorException.getInfo();
		}
		return errorCode;
	}

	private void handleValue(final ErrorException errorException, final ActivityDetail activityDetail) {
		final Object value = errorException.getValue();
		if (value instanceof License) {
			activityDetail.setFileName(((License) value).getFileName());
		} else if (value instanceof Setting) {
			final Setting setting = (Setting) value;
			activityDetail.setSettingKey(Objects.toString(setting.getKey(), null));
			activityDetail.setSettingValue(setting.getValueAsString());
		}
	}

	@Override
	public ErrorException convertFrom(final ActivityDetail activityDetail) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
