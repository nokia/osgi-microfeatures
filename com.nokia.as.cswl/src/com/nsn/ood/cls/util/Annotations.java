/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


/**
 * @author wro50095
 * 
 */
public final class Annotations {

	private Annotations() {
	}

	public static <T extends Annotation> T getAnnotation(final Method method, final Class<T> annotationClass) {
		final T annotation = method.getAnnotation(annotationClass);
		if (annotation != null) {
			return annotation;
		}
		return getAnnotation(method.getDeclaringClass(), annotationClass);
	}

	public static <T extends Annotation> T getAnnotation(final Class<?> clazz, final Class<T> annotationClass) {
		return clazz.getAnnotation(annotationClass);
	}
}
