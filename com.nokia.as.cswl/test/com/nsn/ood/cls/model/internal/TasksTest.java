/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonProperty;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonPropertyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class TasksTest {

	@Test
	public void testEmptyTasks() throws Exception {
		final Tasks tasks = new Tasks();
		assertNull(tasks.getReleaseCapacityForExpiredClients());
		assertNull(tasks.getReleaseCapacityForExpiredLicenses());
		assertNull(tasks.getUpdateLicensesState());
		assertNull(tasks.getSendExpiringLicensesEmail());
		assertNull(tasks.getSendCapacityThresholdEmail());
	}

	@Test
	public void testReleaseCapacityForExpiredClients() throws Exception {
		final TaskExpression task1 = new TaskExpression();
		final TaskExpression task2 = new TaskExpression();

		final Tasks tasks = new Tasks().withReleaseCapacityForExpiredClients(task1);
		assertSame(task1, tasks.getReleaseCapacityForExpiredClients());
		assertNotSame(task2, tasks.getReleaseCapacityForExpiredClients());
		assertNull(tasks.getReleaseCapacityForExpiredLicenses());
		assertNull(tasks.getUpdateLicensesState());
		assertNull(tasks.getSendExpiringLicensesEmail());
		assertNull(tasks.getSendCapacityThresholdEmail());

		tasks.setReleaseCapacityForExpiredClients(task2);
		assertNotSame(task1, tasks.getReleaseCapacityForExpiredClients());
		assertSame(task2, tasks.getReleaseCapacityForExpiredClients());
		assertNull(tasks.getReleaseCapacityForExpiredLicenses());
		assertNull(tasks.getUpdateLicensesState());
		assertNull(tasks.getSendExpiringLicensesEmail());
		assertNull(tasks.getSendCapacityThresholdEmail());
	}

	@Test
	public void testReleaseCapacityForExpiredLicenses() throws Exception {
		final TaskExpression task1 = new TaskExpression();
		final TaskExpression task2 = new TaskExpression();

		final Tasks tasks = new Tasks().withReleaseCapacityForExpiredLicenses(task1);
		assertNull(tasks.getReleaseCapacityForExpiredClients());
		assertSame(task1, tasks.getReleaseCapacityForExpiredLicenses());
		assertNotSame(task2, tasks.getReleaseCapacityForExpiredLicenses());
		assertNull(tasks.getUpdateLicensesState());
		assertNull(tasks.getSendExpiringLicensesEmail());
		assertNull(tasks.getSendCapacityThresholdEmail());

		tasks.setReleaseCapacityForExpiredLicenses(task2);
		assertNull(tasks.getReleaseCapacityForExpiredClients());
		assertNotSame(task1, tasks.getReleaseCapacityForExpiredLicenses());
		assertSame(task2, tasks.getReleaseCapacityForExpiredLicenses());
		assertNull(tasks.getUpdateLicensesState());
		assertNull(tasks.getSendExpiringLicensesEmail());
		assertNull(tasks.getSendCapacityThresholdEmail());
	}

	@Test
	public void testUpdateLicensesState() throws Exception {
		final TaskExpression task1 = new TaskExpression();
		final TaskExpression task2 = new TaskExpression();

		final Tasks tasks = new Tasks().withUpdateLicensesState(task1);
		assertNull(tasks.getReleaseCapacityForExpiredClients());
		assertNull(tasks.getReleaseCapacityForExpiredLicenses());
		assertSame(task1, tasks.getUpdateLicensesState());
		assertNotSame(task2, tasks.getUpdateLicensesState());
		assertNull(tasks.getSendExpiringLicensesEmail());
		assertNull(tasks.getSendCapacityThresholdEmail());

		tasks.setUpdateLicensesState(task2);
		assertNull(tasks.getReleaseCapacityForExpiredClients());
		assertNull(tasks.getReleaseCapacityForExpiredLicenses());
		assertNotSame(task1, tasks.getUpdateLicensesState());
		assertSame(task2, tasks.getUpdateLicensesState());
		assertNull(tasks.getSendExpiringLicensesEmail());
		assertNull(tasks.getSendCapacityThresholdEmail());
	}

	@Test
	public void testSendExpiringLicensesEmail() throws Exception {
		final TaskExpression task1 = new TaskExpression();
		final TaskExpression task2 = new TaskExpression();

		final Tasks tasks = new Tasks().withSendExpiringLicensesEmail(task1);
		assertNull(tasks.getReleaseCapacityForExpiredClients());
		assertNull(tasks.getReleaseCapacityForExpiredLicenses());
		assertNull(tasks.getUpdateLicensesState());
		assertSame(task1, tasks.getSendExpiringLicensesEmail());
		assertNotSame(task2, tasks.getSendExpiringLicensesEmail());
		assertNull(tasks.getSendCapacityThresholdEmail());

		tasks.setSendExpiringLicensesEmail(task2);
		assertNull(tasks.getReleaseCapacityForExpiredClients());
		assertNull(tasks.getReleaseCapacityForExpiredLicenses());
		assertNull(tasks.getUpdateLicensesState());
		assertNotSame(task1, tasks.getSendExpiringLicensesEmail());
		assertSame(task2, tasks.getSendExpiringLicensesEmail());
		assertNull(tasks.getSendCapacityThresholdEmail());
	}

	@Test
	public void testSendCapacityThresholdEmail() throws Exception {
		final TaskExpression task1 = new TaskExpression();
		final TaskExpression task2 = new TaskExpression();

		final Tasks tasks = new Tasks().withSendCapacityThresholdEmail(task1);
		assertNull(tasks.getReleaseCapacityForExpiredClients());
		assertNull(tasks.getReleaseCapacityForExpiredLicenses());
		assertNull(tasks.getUpdateLicensesState());
		assertNull(tasks.getSendExpiringLicensesEmail());
		assertSame(task1, tasks.getSendCapacityThresholdEmail());
		assertNotSame(task2, tasks.getSendCapacityThresholdEmail());

		tasks.setSendCapacityThresholdEmail(task2);
		assertNull(tasks.getReleaseCapacityForExpiredClients());
		assertNull(tasks.getReleaseCapacityForExpiredLicenses());
		assertNull(tasks.getUpdateLicensesState());
		assertNull(tasks.getSendExpiringLicensesEmail());
		assertNotSame(task1, tasks.getSendCapacityThresholdEmail());
		assertSame(task2, tasks.getSendCapacityThresholdEmail());
	}

	@Test
	public void testAnnotations() throws Exception {
		assertJsonPropertyOrder(Tasks.class, "releaseCapacityForExpiredClients", "releaseCapacityForExpiredLicenses",
				"updateLicensesState", "sendExpiringLicensesEmail", "sendCapacityThresholdEmail");
		assertJsonProperty(Tasks.class, "releaseCapacityForExpiredClients", "releaseCapacityForExpiredClients");
		assertJsonProperty(Tasks.class, "releaseCapacityForExpiredLicenses", "releaseCapacityForExpiredLicenses");
		assertJsonProperty(Tasks.class, "updateLicensesState", "updateLicensesState");
		assertJsonProperty(Tasks.class, "sendExpiringLicensesEmail", "sendExpiringLicensesEmail");
		assertJsonProperty(Tasks.class, "sendCapacityThresholdEmail", "sendCapacityThresholdEmail");
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(new Tasks().toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		final Tasks tasks = new Tasks().withReleaseCapacityForExpiredClients(new TaskExpression());

		assertFalse(tasks.equals(null));
		assertFalse(tasks.equals("test"));
		assertEquals(tasks, tasks);

		assertFalse(tasks.equals(new Tasks()));
		assertNotEquals(tasks.hashCode(), new Tasks().hashCode());

		final Tasks tasks2 = new Tasks().withReleaseCapacityForExpiredClients(new TaskExpression());
		assertEquals(tasks, tasks2);
		assertEquals(tasks.hashCode(), tasks2.hashCode());
	}
}
