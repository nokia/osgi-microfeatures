// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.agnosticImpl;

import java.util.HashMap;

import com.nokia.licensing.interfaces.CredentialAccess;


public class CredentialAccessImpl implements CredentialAccess {

    @Override
    public HashMap<String, String> getCredentials() {
        final HashMap<String, String> credentialMap = new HashMap<String, String>(10);
        credentialMap.put(CredentialAccess.USERNAME_KEY, "license");
        credentialMap.put(CredentialAccess.PASSWORD_KEY, "license");
        return credentialMap;
    }
}
