/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.cljl.LicenseInstallOptions;
import com.nsn.ood.cls.core.platform.PlatformPreferences;
import com.nsn.ood.cls.core.security.BasicPrincipal;


/**
 * @author marynows
 * 
 */
@Component
public class LicenseInstallOptionsImpl implements LicenseInstallOptions {
	@ServiceDependency
	private BasicPrincipal basicPrincipal;
	@ServiceDependency
	private PlatformPreferences platformPreferences;

	@Override
	public boolean isForce() {
		return true;
	}

	@Override
	public String getTargetId() {
		return this.platformPreferences.getTargetId();
	}

	@Override
	public String getUsername() {
		return this.basicPrincipal.getUser();
	}
}
