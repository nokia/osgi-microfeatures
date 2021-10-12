// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.Test;


public class AnnotationsTest {

	@Test
	public void testGetAnnotationFromClass() throws Exception {
		final Method metodClass = DummyWithClass.class.getMethod("method");
		final Dummy classAnnotation = Annotations.getAnnotation(metodClass, Dummy.class);
		assertEquals("klazzz", classAnnotation.message());
	}

	@Test
	public void testGetAnnotationFromMethod() throws Exception {
		final Method metodMethod = DummyWithMethod.class.getMethod("method");
		final Dummy metodAnnotation = Annotations.getAnnotation(metodMethod, Dummy.class);
		assertEquals("mthd", metodAnnotation.message());
	}

	@Test
	public void testGetAnnotationFromNowhere() throws Exception {
		final Method without = WithoutAnnotations.class.getMethod("method");
		assertNull(Annotations.getAnnotation(without, Dummy.class));
	}

	@Retention(value = RetentionPolicy.RUNTIME)
	@Target({
			ElementType.METHOD, ElementType.TYPE })
	private static @interface Dummy {
		String message();
	}

	@Dummy(message = "klazzz")
	private static class DummyWithClass {
		@SuppressWarnings("unused")
		public void method() {
		}
	}

	private static class DummyWithMethod {
		@Dummy(message = "mthd")
		public void method() {
		}
	}

	private static class WithoutAnnotations {
		@SuppressWarnings("unused")
		public void method() {
		}
	}
}
