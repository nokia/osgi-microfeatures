// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.sh.factory;

import org.osgi.service.component.annotations.Component;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.sh.ShClientFactory;

/**
 * The client factory for TS 29.329 v6.1.0
 */
@Component(name= "ShClientFactory61", service={ShClientFactory.class}, property={"version3gpp=6.1.0"})
public class ShClientFactory61
		extends ShClientFactoryImpl {

	public ShClientFactory61() {
		super(new Version(6,1));
	}

}
