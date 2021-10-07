package com.nsn.ood.cls.util.exception;

public class ReaderException extends Exception {

	private static final long serialVersionUID = 2865158210500521752L;

	public ReaderException() {
		super();
	}

	public ReaderException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ReaderException(final String message) {
		super(message);
	}

	public ReaderException(final Throwable cause) {
		super(cause);
	}
}
