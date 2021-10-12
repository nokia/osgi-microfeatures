// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.logging;

import java.util.logging.Level;


public class OESAuditLevel extends Level {
    private static final long serialVersionUID = 1L;
    public static final Level SUCCESS = new OESAuditLevel("OES_AUDIT_SUCCESS", 811);
    public static final Level FAILURE = new OESAuditLevel("OES_AUDIT_FAILURE", 889);

    protected OESAuditLevel(final String name, final int value) {
        super(name, value);
    }
}
