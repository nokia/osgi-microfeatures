// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.sh.factory;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory;

import org.osgi.service.component.annotations.Component;

/**
 * The client factory for TS 29.329 v8.4.0
 */
@Component(name= "ShClientFactory84", service={ShClientFactory.class}, property={"version3gpp=8.4.0"})
public class ShClientFactory84
		extends ShClientFactoryImpl {

	public ShClientFactory84() {
		super(new Version(8,4));
	}

}
