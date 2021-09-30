/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author marynows
 * 
 */
@Inherited
@Retention(value = RetentionPolicy.RUNTIME)
@Target({
		ElementType.TYPE, ElementType.METHOD })
public @interface Loggable {

	Level value() default Level.TRACE;

	boolean duration() default false;

	public enum Level {
		ERROR, //
		WARNING, //
		INFO, //
		DEBUG, //
		TRACE, //
	}
}
