/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.interfaces;

/**
 * Custom Exception used by the Licensing Component component.
 *
 * @version 1.0
 */
public class LicenseException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Error code signifies the source of the exception. The error codes will help in identifying the source of the
     * exception and thus can be logged along with the message
     */
    private String errorCode;

    public LicenseException(final String errorCode, final String message) {
        super(message);
        setErrorCode(errorCode);
    }

    /**
     * Constructor which take in the exception message
     *
     * @param message
     */
    public LicenseException(final String message) {
        super(message);
    }

    /**
     * Constructor which takes in Throwable instance
     *
     * @param t
     */
    public LicenseException(final Throwable t) {
        super(t);
    }

    public LicenseException(final String message, final Throwable t) {
        super(message, t);
    }

    /**
     * Gets the Error Code Error code signifies the source of the exception. The error codes will help in identifying
     * the source of the exception and thus can be logged along with the message
     *
     * @return the errorCode
     */
    public String getErrorCode() {
        return this.errorCode;
    }

    /**
     * Sets the Error Code Error code signifies the source of the exception. The error codes will help in identifying
     * the source of the exception and thus can be logged along with the message
     *
     * @param errorCode
     *            the errorCode to set
     */
    public void setErrorCode(final String errorCode) {
        this.errorCode = errorCode;
    }
}
