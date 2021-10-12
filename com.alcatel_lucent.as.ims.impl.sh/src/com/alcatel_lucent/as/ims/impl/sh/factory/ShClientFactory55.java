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
 * The client factory for TS 29.329 v5.5.0
 */
@Component(name= "ShClientFactory55", service={ShClientFactory.class}, property={"version3gpp=5.5.0"})
public class ShClientFactory55
		extends ShClientFactoryImpl {

	public ShClientFactory55() {
		super(new Version(5,5));
	}

}
