// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.servlet.sip.services;

/**
 * Basic interface uses when cloning local messages in a cohosting server.
 * localclone() perform a logic than differs from clone() 
 */
public interface LocalClone extends Cloneable {
    Object localclone();

}
