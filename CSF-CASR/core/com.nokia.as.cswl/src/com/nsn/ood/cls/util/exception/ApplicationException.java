package com.nsn.ood.cls.util.exception;

public class ApplicationException extends Exception {

	private static final long serialVersionUID = -7607477027711836624L;

	public ApplicationException() {
		super();
	}

	public ApplicationException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ApplicationException(final String message) {
		super(message);
	}

	public ApplicationException(final Throwable cause) {
		super(cause);
	}
}
