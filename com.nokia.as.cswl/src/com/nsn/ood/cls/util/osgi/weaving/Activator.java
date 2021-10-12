// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.util.osgi.weaving;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;

public class Activator implements BundleActivator {

	private ServiceRegistration<?> weaverReg;

	public void start(BundleContext context) throws Exception {
		weaverReg = context.registerService(WeavingHook.class, new AspectWeaver(), null);
	}

	public void stop(BundleContext context) throws Exception {
		weaverReg.unregister();
	}

}
