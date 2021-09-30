/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.log;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.util.Annotations;
import com.nsn.ood.cls.util.log.Loggable.Level;


/**
 * @author marynows
 * 
 */
@Aspect
public class LoggableInterceptor {
	
	@Pointcut("@within(com.nsn.ood.cls.util.log.Loggable)")
	public void loggableMethods() {}
	
	@Pointcut("execution(public * *(..))")
	public void publicMethod() {}

	@Around("loggableMethods() && publicMethod()")
	public Object aroundInvoke(final ProceedingJoinPoint pjp) throws Throwable {
		final Class<?> loggerClass;
		Signature signature = pjp.getSignature();
		if (signature instanceof MethodSignature) {
			Method method = ((MethodSignature) signature).getMethod();
			Loggable loggable = Annotations.getAnnotation(method, Loggable.class);
			if(loggable != null) {
				loggerClass = method.getDeclaringClass();
			} else {
				loggerClass = pjp.getTarget().getClass().getSuperclass();
				loggable = Annotations.getAnnotation(loggerClass, Loggable.class); 
			}
			
			final Logger logger = LoggerFactory.getLogger(loggerClass);
			final long start = System.currentTimeMillis();
			try {
				logEntry(logger, method, loggable);
				return pjp.proceed();
			} finally {
				logExit(logger, method, loggable, start);
			}
		} else return pjp.proceed();
	}
	
	private void logEntry(final Logger logger, final Method method, final Loggable loggable) {
		log(logger, loggable.value(), "{} - entry", method.getName());
	}

	private void logExit(final Logger logger, final Method method, final Loggable loggable, final long start) {
		if (loggable.duration()) {
			log(logger, loggable.value(), "{} - exit [{}]", method.getName(), System.currentTimeMillis() - start);
		} else {
			log(logger, loggable.value(), "{} - exit", method.getName());
		}
	}

	private void log(final Logger logger, final Level level, final String message, final Object... arguments) {
		switch (level) {
			case ERROR:
				logger.error(message, arguments);
				break;
			case WARNING:
				logger.warn(message, arguments);
				break;
			case INFO:
				logger.info(message, arguments);
				break;
			case DEBUG:
				logger.debug(message, arguments);
				break;
			case TRACE:
				logger.trace(message, arguments);
				break;
		}
	}
}
