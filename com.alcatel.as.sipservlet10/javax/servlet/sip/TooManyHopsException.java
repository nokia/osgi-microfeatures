// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

/**
 * 
 */
package javax.servlet.sip;

import javax.servlet.ServletException;

public class TooManyHopsException extends ServletException {

    private static final long serialVersionUID = 1L;

    public TooManyHopsException() {
    }

    public TooManyHopsException(String message) {
        super(message);
    } 
}
