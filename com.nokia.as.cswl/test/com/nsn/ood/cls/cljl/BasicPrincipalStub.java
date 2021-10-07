/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl;

import com.nsn.ood.cls.core.security.BasicPrincipal;

/**
 * @author marynows
 * 
 */
public class BasicPrincipalStub extends BasicPrincipal {
	public static String user = "anonymous";

	public String getUser() {
		return user;
	}

	public void setUser(final String user) { }
}
