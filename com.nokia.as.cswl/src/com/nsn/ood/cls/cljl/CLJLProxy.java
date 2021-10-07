/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl;

import org.apache.felix.dm.annotation.api.Component;

import com.nokia.licensing.factories.LicenseInstallFactory;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.interfaces.LicenseInstall;


/**
 * @author marynows
 *
 */
@Component(provides = CLJLProxy.class)
public class CLJLProxy {
	private static final String IMPLEMENTATION_KEY = "postgres";

	public LicenseInstall getLicenseInstall() throws LicenseException {
		return LicenseInstallFactory.getInstance(null, IMPLEMENTATION_KEY, null);
	}

}