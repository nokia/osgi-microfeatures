/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates.checkers;

import javax.ws.rs.container.ContainerRequestContext;


/**
 * @author wro50095
 * 
 */
public class SSLConditionChecker implements ConditionChecker {

	@Override
	public boolean checkCondition(final ContainerRequestContext requestContext) {
		return requestContext.getSecurityContext().isSecure();
	}

	@Override
	public String getErrorMessaage() {
		return "Not SSL request";
	}
}
