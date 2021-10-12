// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.impl.charging.ro.factory;

import org.osgi.service.component.annotations.Component;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ro.RoClientFactory;

/**
 * The client factory for TS 32.299 v8.6.0
 */
@Component(name= "RoClientFactory86", service={RoClientFactory.class}, property={"version3gpp=8.6.0"})
public class RoClientFactory86
		extends RoClientFactoryImpl {

	public RoClientFactory86() {
		super(new Version(8,6));
	}

}
