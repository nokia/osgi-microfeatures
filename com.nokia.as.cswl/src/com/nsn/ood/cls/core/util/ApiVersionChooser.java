/*
 * Copyright (c) 2016 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.util;

import org.apache.felix.dm.annotation.api.Component;


/**
 * @author wro50095
 *
 */
@Component(provides = ApiVersionChooser.class)
public class ApiVersionChooser {
	public enum API_VERSION {
		VERSION_1_0, VERSION_1_1;
	}

	private API_VERSION currentVersion = API_VERSION.VERSION_1_1;

	public API_VERSION getCurrentVersion() {
		return this.currentVersion;
	}

	public void setCurrentVersion(final API_VERSION currentVersion) {
		this.currentVersion = currentVersion;
	}

}
