/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.audit;

import java.lang.reflect.Method;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.security.BasicPrincipal;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author wro50095
 * 
 */
@Aspect
public class AuditLogInterceptor {
	private BasicPrincipal basicPrincipal = new BasicPrincipal();
	
	@Pointcut("@within(com.nsn.ood.cls.core.audit.AuditLog)")
	public void loggableMethods() {}
	
	@SuppressWarnings("unchecked")
	@Before("loggableMethods()")
	public void auditLog(JoinPoint jp) {
		MethodSignature signature = (MethodSignature) jp.getSignature();
		Method method = signature.getMethod();
		AuditLog auditLog = method.getAnnotation(AuditLog.class);
		AuditLogType auditType = auditLog.value();
		
		final Logger logger = LoggerFactory.getLogger(method.getDeclaringClass());
		switch(auditType) {
		case UNDEFINED: {
			throw new CLSIllegalArgumentException("Illegal type of audit log");
		}
		case CHANGE_SETTING: {
			Object[] parameters = jp.getArgs();
			if (parameters.length > 0 && parameters[0] instanceof List) {
				try {
					for (final Setting setting : (List<Setting>) parameters[0]) {
						logger.info("AUDIT: user {}, operation {}, setting {}, value {}",
								this.basicPrincipal.getUser(), auditType.toString(), setting.getKey().name(),
								setting.getValue());
					}
				} catch (final ClassCastException e) {
				}
			}
			break;
		}
		default: {
			logger.info("AUDIT: user {}, operation {}", this.basicPrincipal.getUser(), auditType.toString());
			break;
		}
		}
	}
}