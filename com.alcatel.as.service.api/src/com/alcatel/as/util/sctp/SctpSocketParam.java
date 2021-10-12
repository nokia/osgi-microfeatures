// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.sctp;

import java.io.Externalizable;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface SctpSocketParam extends Externalizable {

    public SctpSocketParam merge (SctpSocketParam other);

}
