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
 * The client factory for TS 29.329 v7.4.0
 */
@Component(name= "ShClientFactory74", service={ShClientFactory.class}, property={"version3gpp=7.4.0"})
public class ShClientFactory74
		extends ShClientFactoryImpl {

	public ShClientFactory74() {
		super(new Version(7,4));
	}

}
