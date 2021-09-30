/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.internal;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.powermock.reflect.Whitebox;

import com.nsn.ood.cls.core.platform.PlatformPreferences;
import com.nsn.ood.cls.core.service.task.TaskName2EventConverter;
import com.nsn.ood.cls.core.startup.InitSchedules;
import com.nsn.ood.cls.core.util.ApiVersionChooser;
import com.nsn.ood.cls.core.util.ApiVersionChooser.API_VERSION;
import com.nsn.ood.cls.model.internal.Tasks;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 *
 */
public class TestServiceTest {

	@Test
	public void testReloadTasks() throws Exception {
		final InitSchedules initSchedulesMock = createMock(InitSchedules.class);
		final Tasks tasks = new Tasks();

		initSchedulesMock.reload(tasks);

		replayAll();
		final TestService service = new TestService();
		setInternalState(service, initSchedulesMock);
		service.reloadTasks(tasks);
		verifyAll();
	}

	@Test
	public void testReloadTargetId() throws Exception {
		final PlatformPreferences platformPreferencesMock = createMock(PlatformPreferences.class);

		expect(platformPreferencesMock.reloadTargetId()).andReturn("ttt");

		replayAll();
		final TestService service = new TestService();
		setInternalState(service, platformPreferencesMock);
		assertEquals("ttt", service.reloadTargetId());
		verifyAll();
	}

	@Test
	public void testRunTask() throws Exception {
		final EventAdmin schedulerMock = createMock(EventAdmin.class);
		final Converter<String, Event> converterMock = createMock(TaskName2EventConverter.class);

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		final Event event = new Event("test", properties);
		expect(converterMock.convertTo("taskName")).andReturn(event);
		schedulerMock.postEvent(event);

		replayAll();
		final TestService service = new TestService();
		setInternalState(service, schedulerMock, converterMock);
		service.runTask("taskName");
		verifyAll();
	}

	@Test
	public void testSetAPIVersion() throws Exception {
		final ApiVersionChooser apiMock = createMock(ApiVersionChooser.class);
		apiMock.setCurrentVersion(API_VERSION.VERSION_1_0);

		final TestService service = new TestService();
		Whitebox.setInternalState(service, apiMock);

		replayAll();
		service.setApiVersion(API_VERSION.VERSION_1_0);

		verifyAll();

	}

}
