/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl;

/**
 * @author marynows
 * 
 */
public interface LicenseInstallOptions {

	boolean isForce();

	String getTargetId();

	String getUsername();
}
