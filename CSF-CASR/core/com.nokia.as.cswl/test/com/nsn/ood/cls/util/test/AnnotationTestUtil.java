/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.test;

import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


/**
 * @author marynows
 * 
 */
public class AnnotationTestUtil {

	public static void assertAnnotation(final Method method, final Class<? extends Annotation> annotationClass) {
		assertNotNull(method.getAnnotation(annotationClass));
	}

	public static void assertAnnotation(final Class<?> clazz, final Class<? extends Annotation> annotationClass) {
		assertNotNull(clazz.getAnnotation(annotationClass));
	}
}
