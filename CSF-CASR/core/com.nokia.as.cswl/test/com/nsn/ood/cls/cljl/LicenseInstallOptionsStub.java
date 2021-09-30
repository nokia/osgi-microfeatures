/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl;

/**
 * @author marynows
 * 
 */
public class LicenseInstallOptionsStub implements LicenseInstallOptions {
	public static final String TARGET_ID = "87654321";
	public static final String USER_NAME = "user";

	@Override
	public boolean isForce() {
		return false;
	}

	@Override
	public String getTargetId() {
		return TARGET_ID;
	}

	@Override
	public String getUsername() {
		return USER_NAME;
	}
}