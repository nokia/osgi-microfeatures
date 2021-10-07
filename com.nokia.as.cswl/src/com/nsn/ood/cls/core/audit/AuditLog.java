/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wro50095
 * 
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({
		ElementType.METHOD, ElementType.TYPE })
public @interface AuditLog {
	AuditLogType value() default AuditLogType.UNDEFINED;
}
