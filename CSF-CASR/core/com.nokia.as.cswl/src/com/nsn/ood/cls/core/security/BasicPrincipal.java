/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.security;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceScope;


/**
 * @author marynows
 * 
 */
@Component(provides = BasicPrincipal.class, scope = ServiceScope.PROTOTYPE)
public class BasicPrincipal {
	private String user = "anonymous";

	public String getUser() {
		return this.user;
	}

	public void setUser(final String user) {
		this.user = user;
	}
}
