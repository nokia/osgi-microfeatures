// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.common.impl;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Inject;
import org.glassfish.hk2.osgiresourcelocator.ServiceLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * This component waits for Jersey initialization
 * 
 * - Wait for hk2 (Jersey's injector) to become initialized to avoid starting applications while HK2 is still in the resolved state
 * - Wait for jersey bundles to start, so these bundles correctly detect they are running in an osgi environment.
 */
@Component(provides=JerseyTracker.class)
public class JerseyTracker {
	
	/**
	 * We need the bundle context in order to lookup all installed bundles
	 */
	@Inject
	private BundleContext _bc;
	
	/**
	 * Obtain the real DependencyManager Component that is created for our component. We will dynamically add some dependencies into it
	 */
	@Inject
	org.apache.felix.dm.Component _component;
		
	/**
	 * Dynamically add some bundles dependencies on all jersey / hk2 bundles.
	 */
	@Init
	void init() {
		Stream.concat(jerseyBundles(), hk2Bundles())
			.filter(b -> b.getState() != Bundle.ACTIVE)
			.forEach(this::addBundleDependency);
	}
		
	private void addBundleDependency(Bundle bundle) {
		org.apache.felix.dm.BundleDependency bd = _component.getDependencyManager().createBundleDependency();
		bd.setBundle(bundle)
		  .setRequired(true)
		  .setStateMask(Bundle.ACTIVE);
		_component.add(bd);
	}
	
	private Stream<Bundle> jerseyBundles() {
		return Arrays.stream(_bc.getBundles())
			.filter(b -> b.getSymbolicName().startsWith("org.glassfish.jersey"));
	}
	
	private Stream<Bundle> hk2Bundles() {
		// find the hk2 osgi resource locator bundle, in order to wait for it to become active
    	// if hk2 is still in resolved state , we risk running HK2 initialization before activator has run
    	// but loading ServiceLoader looks safe (abstract class without static blocks).
		// as we have loaded a class from the HK2 bundle, this will cause the bundle, which has the lazy activation option, to start
		return Stream.of(FrameworkUtil.getBundle(ServiceLoader.class));
	}

}
