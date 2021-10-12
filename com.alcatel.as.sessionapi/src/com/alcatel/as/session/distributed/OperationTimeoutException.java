// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed;

import com.alcatel.as.session.distributed.SessionException;

/**
 * Signals that an exception occurred in the API implementation.
 * 
 */
@SuppressWarnings("serial")
public class OperationTimeoutException extends SessionException {

    /**
     * The default Constructor.
     */
    public OperationTimeoutException() {
	super();
    }

    /**
     * A Constructor.
     * @param rootCause the underlying exception
     */
    public OperationTimeoutException(Throwable rootCause) {
	super(rootCause);
    }

    /**
     * A Constructor.
     * @param debugMessage an informative message
     */
    public OperationTimeoutException(String debugMessage) {
	super(debugMessage);
    }
    
    /**
     * A Constructor.
     * @param debugMessage an informative message
     * @param rootCause the underlying exception
     */
    public OperationTimeoutException(String debugMessage, Throwable rootCause) {
	super(debugMessage, rootCause);
    }
}
