// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package javax.servlet.sip;

/**
 * @author christophe
 *
 */
public class IllegalSessionStateException extends IllegalStateException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    SipSession _session;
    public SipSession getSipSession() {
        return _session;
    }
    /**
     * 
     */
    public IllegalSessionStateException(SipSession s) {
        super();
        _session=s;
    }

    /**
     * @param message
     * @param cause
     */
    public IllegalSessionStateException(String message, Throwable cause,SipSession s) {
        super(message, cause);
        _session=s;
    }

    /**
     * @param s
     */
    public IllegalSessionStateException(String m,SipSession s) {
        super(m);
        _session=s;
    }

    /**
     * @param cause
     */
    public IllegalSessionStateException(Throwable cause,SipSession s) {
        super(cause);
        _session=s;
    }
    
}
