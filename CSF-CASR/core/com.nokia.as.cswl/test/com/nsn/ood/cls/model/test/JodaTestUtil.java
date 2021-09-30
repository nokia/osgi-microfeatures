/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.test;

import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;
import org.joda.time.Seconds;


/**
 * @author marynows
 * 
 */
public class JodaTestUtil {

	public static void assertNow(final ReadableInstant time) {
		assertTrue(Seconds.secondsBetween(time, DateTime.now()).getSeconds() <= 1);
	}

	public static void assertNow(final ReadablePartial time) {
		assertTrue(Seconds.secondsBetween(time, LocalDateTime.now()).getSeconds() <= 1);
	}
}
