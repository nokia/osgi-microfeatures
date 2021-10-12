// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.cx.factory;

import org.osgi.service.component.annotations.Component;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.CxClientFactory;

/**
 * The client factory for TS 29.229 v7.1.0
 */
@Component(name = "CxClientFactory71", service = { CxClientFactory.class }, property = { "version3gpp=7.1.0" })
public class CxClientFactory71
		extends CxClientFactoryImpl {

	public CxClientFactory71() {
		super(new Version(7, 1));
	}

}
