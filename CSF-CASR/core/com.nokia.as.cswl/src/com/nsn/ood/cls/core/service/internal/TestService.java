/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.internal;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.nsn.ood.cls.core.platform.PlatformPreferences;
import com.nsn.ood.cls.core.startup.InitSchedules;
import com.nsn.ood.cls.core.util.ApiVersionChooser;
import com.nsn.ood.cls.core.util.ApiVersionChooser.API_VERSION;
import com.nsn.ood.cls.model.internal.Tasks;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;
import com.nsn.ood.cls.util.log.Loggable.Level;


/**
 * @author marynows
 *
 */
@Component(provides = TestService.class)
@Loggable(Level.WARNING)
public class TestService {
	@ServiceDependency
	private InitSchedules initSchedules;
	@ServiceDependency
	private PlatformPreferences platformPreferences;
	@ServiceDependency(filter = "(&(from=taskName)(to=event))")
	private Converter<String, Event> taskName2EventConverter;
	@ServiceDependency
	private ApiVersionChooser apiVersionChooser;
	@ServiceDependency
	private EventAdmin admin;

	public void reloadTasks(final Tasks tasks) {
		this.initSchedules.reload(tasks);
	}

	public String reloadTargetId() {
		return this.platformPreferences.reloadTargetId();
	}

	public void runTask(final String taskName) {
		this.admin.postEvent(taskName2EventConverter.convertTo(taskName));
	}

	public void setApiVersion(final API_VERSION apiVersion) {
		this.apiVersionChooser.setCurrentVersion(apiVersion);
	}

}
