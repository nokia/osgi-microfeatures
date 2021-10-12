// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.core.service.error;

import com.nsn.ood.cls.util.exception.CLSRuntimeException;


public final class UnknownRuntimeErrorException extends CLSRuntimeException {

	private static final long serialVersionUID = -1998689205720141274L;

	public UnknownRuntimeErrorException(final String message, final Throwable e) {
		super(message, e);
	}

	public UnknownRuntimeErrorException(final String message) {
		super(message);
	}

}
