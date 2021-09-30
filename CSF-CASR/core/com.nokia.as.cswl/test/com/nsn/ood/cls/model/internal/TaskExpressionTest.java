/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonProperty;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonPropertyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.joda.time.DateTime;
import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class TaskExpressionTest {
	private static final DateTime START = new DateTime(2015, 6, 1, 11, 2);
	private static final DateTime END = new DateTime(2016, 1, 6, 2, 11);

	@Test
	public void testEmptyTask() throws Exception {
		assertTask(new TaskExpression(), "*", "*", "*", "*", "0", "0", "0", "", null, null);
	}

	@Test
	public void testTask() throws Exception {
		assertTask(new TaskExpression().withDayOfMonth("dom"), "*", "*", "dom", "*", "0", "0", "0", "", null, null);
		assertTask(new TaskExpression().withDayOfWeek("dow"), "*", "*", "*", "dow", "0", "0", "0", "", null, null);
		assertTask(new TaskExpression().withEnd(END), "*", "*", "*", "*", "0", "0", "0", "", null, END);
		assertTask(new TaskExpression().withHour("h"), "*", "*", "*", "*", "h", "0", "0", "", null, null);
		assertTask(new TaskExpression().withMinute("m"), "*", "*", "*", "*", "0", "m", "0", "", null, null);
		assertTask(new TaskExpression().withMonth("m"), "*", "m", "*", "*", "0", "0", "0", "", null, null);
		assertTask(new TaskExpression().withSecond("s"), "*", "*", "*", "*", "0", "0", "s", "", null, null);
		assertTask(new TaskExpression().withStart(START), "*", "*", "*", "*", "0", "0", "0", "", START, null);
		assertTask(new TaskExpression().withTimezone("tz"), "*", "*", "*", "*", "0", "0", "0", "tz", null, null);
		assertTask(new TaskExpression().withYear("y"), "y", "*", "*", "*", "0", "0", "0", "", null, null);

		assertTask(new TaskExpression().withDayOfMonth("dom").withDayOfWeek("dow").withEnd(END).withHour("h")
				.withMinute("m").withMonth("M").withSecond("s").withStart(START).withTimezone("tz").withYear("y"),//
				"y", "M", "dom", "dow", "h", "m", "s", "tz", START, END);
	}

	@Test
	public void testTaskSetters() throws Exception {
		final TaskExpression task = new TaskExpression();
		task.setDayOfMonth("dom");
		task.setDayOfWeek("dow");
		task.setEnd(END);
		task.setHour("h");
		task.setMinute("m");
		task.setMonth("M");
		task.setSecond("s");
		task.setStart(START);
		task.setTimezone("tz");
		task.setYear("y");

		assertTask(task, "y", "M", "dom", "dow", "h", "m", "s", "tz", START, END);
	}

	private void assertTask(final TaskExpression task, final String year, final String month, final String dayOfMonth,
			final String dayOfWeek, final String hour, final String minute, final String second, final String timezone,
			final DateTime start, final DateTime end) {
		assertEquals(dayOfMonth, task.getDayOfMonth());
		assertEquals(dayOfWeek, task.getDayOfWeek());
		assertEquals(end, task.getEnd());
		assertEquals(hour, task.getHour());
		assertEquals(minute, task.getMinute());
		assertEquals(month, task.getMonth());
		assertEquals(second, task.getSecond());
		assertEquals(start, task.getStart());
		assertEquals(timezone, task.getTimezone());
		assertEquals(year, task.getYear());
	}

	@Test
	public void testAnnotations() throws Exception {
		assertJsonPropertyOrder(TaskExpression.class, "second", "minute", "hour", "dayOfWeek", "dayOfMonth", "month",
				"year", "start", "end", "timezone");
		assertJsonProperty(TaskExpression.class, "dayOfMonth", "dayOfMonth");
		assertJsonProperty(TaskExpression.class, "dayOfWeek", "dayOfWeek");
		assertJsonProperty(TaskExpression.class, "end", "end");
		assertJsonProperty(TaskExpression.class, "hour", "hour");
		assertJsonProperty(TaskExpression.class, "minute", "minute");
		assertJsonProperty(TaskExpression.class, "month", "month");
		assertJsonProperty(TaskExpression.class, "second", "second");
		assertJsonProperty(TaskExpression.class, "start", "start");
		assertJsonProperty(TaskExpression.class, "timezone", "timezone");
		assertJsonProperty(TaskExpression.class, "year", "year");
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(new TaskExpression().toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		final TaskExpression expression = new TaskExpression().withHour("2");

		assertFalse(expression.equals(null));
		assertFalse(expression.equals("test"));
		assertEquals(expression, expression);

		assertFalse(expression.equals(new TaskExpression()));
		assertNotEquals(expression.hashCode(), new TaskExpression().hashCode());

		final TaskExpression expression2 = new TaskExpression().withHour("2");
		assertEquals(expression, expression2);
		assertEquals(expression.hashCode(), expression2.hashCode());
	}
}
