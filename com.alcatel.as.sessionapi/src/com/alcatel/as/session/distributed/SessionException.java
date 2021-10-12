// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed;

/**
 * Signals that an exception occurred in the API implementation.
 * 
 */
@SuppressWarnings("serial")
public class SessionException extends Exception {

    /**
     * The default Constructor.
     */
    public SessionException() {
	super();
    }

    /**
     * A Constructor.
     * @param rootCause the underlying exception
     */
    public SessionException(Throwable rootCause) {
	super(rootCause);
    }

    /**
     * A Constructor.
     * @param debugMessage an informative message
     */
    public SessionException(String debugMessage) {
	super(debugMessage);
    }
    
    /**
     * A Constructor.
     * @param debugMessage an informative message
     * @param rootCause the underlying exception
     */
    public SessionException(String debugMessage, Throwable rootCause) {
	super(debugMessage, rootCause);
    }
}
