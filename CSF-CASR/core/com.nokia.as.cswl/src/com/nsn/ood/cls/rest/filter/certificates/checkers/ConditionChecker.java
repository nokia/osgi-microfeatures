/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates.checkers;

import javax.ws.rs.container.ContainerRequestContext;


/**
 * @author wro50095
 * 
 */
public interface ConditionChecker {

	boolean checkCondition(ContainerRequestContext requestContext);

	String getErrorMessaage();
}
