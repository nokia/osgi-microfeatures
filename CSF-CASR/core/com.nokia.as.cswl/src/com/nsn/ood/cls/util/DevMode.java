/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util;

/**
 * @author marynows
 * 
 */
public final class DevMode {

	private DevMode() {
	}

	public static boolean isEnable() {
		return Boolean.valueOf(System.getProperty("com.nsn.ood.cls.devMode"));
	}
}
