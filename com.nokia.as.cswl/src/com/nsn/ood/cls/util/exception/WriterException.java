// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.util.exception;

public class WriterException extends Exception {

	private static final long serialVersionUID = -478823857588523996L;

	public WriterException() {
		super();
	}

	public WriterException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public WriterException(final String message) {
		super(message);
	}

	public WriterException(final Throwable cause) {
		super(cause);
	}
}
