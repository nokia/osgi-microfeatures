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
 * The client factory for TS 29.329 v9.0.0
 */
@Component(name= "ShClientFactory90", service={ShClientFactory.class}, property={"version3gpp=9.0.0"})
public class ShClientFactory90
		extends ShClientFactoryImpl {

	public ShClientFactory90() {
		super(new Version(9,0));
	}

}
