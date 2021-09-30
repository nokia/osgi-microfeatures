/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.startup;

import java.util.Dictionary;
import java.util.Hashtable;

import org.amdatu.scheduling.Job;
import org.amdatu.scheduling.constants.Constants;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.model.internal.TaskExpression;
import com.nsn.ood.cls.model.internal.Tasks;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;
import com.nsn.ood.cls.util.log.Loggable.Level;


/**
 * @author marynows
 *
 */
@Component(provides = InitSchedules.class)
//@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class InitSchedules {
	private static final String EXPR_ANY_ANY_0 = "0 * * * * *";
	private static final String EXPR_ANY_ANY_30 = "30 * * * * *";
	private static final String EXPR_0_0_0 = "0 0 0 * * *";
	private static final String EXPR_0_1_0 = "0 1 0 * * *";

	@ServiceDependency(filter = "(&(from=task)(to=cronString))")
	private Converter<TaskExpression, String> task2CronStringConverter;
	
	@Inject
	private DependencyManager dm;
	
	private org.apache.felix.dm.Component releaseCapacityForExpiredClientsTask = null;
	private org.apache.felix.dm.Component releaseCapacityForExpiredLicensesTask = null;
	private org.apache.felix.dm.Component updateLicensesStateTask = null;
	private org.apache.felix.dm.Component sendExpiringLicensesEmailTask = null;
	private org.apache.felix.dm.Component sendCapacityThresholdEmailTask = null;
	
	@Start
	public void init() {
		log().info("Initialize schedules...");

		initReleaseCapacityForExpiredClientsTask(EXPR_ANY_ANY_0);
		initReleaseCapacityForExpiredLicensesTask(EXPR_0_0_0);
		initUpdateLicensesStateTask(EXPR_0_0_0);
		initSendExpiringLicensesEmailTask(EXPR_0_1_0);
		initSendCapacityThresholdEmailTask(EXPR_ANY_ANY_30);
	}

	//@Lock(LockType.WRITE)
	@Loggable(Level.WARNING)
	public void reload(final Tasks tasks) {
		if (tasks.getReleaseCapacityForExpiredClients() != null) {
			initReleaseCapacityForExpiredClientsTask(convert(tasks.getReleaseCapacityForExpiredClients()));
		}
		if (tasks.getReleaseCapacityForExpiredLicenses() != null) {
			initReleaseCapacityForExpiredLicensesTask(convert(tasks.getReleaseCapacityForExpiredLicenses()));
		}
		if (tasks.getUpdateLicensesState() != null) {
			initUpdateLicensesStateTask(convert(tasks.getUpdateLicensesState()));
		}
		if (tasks.getSendExpiringLicensesEmail() != null) {
			initSendExpiringLicensesEmailTask(convert(tasks.getSendExpiringLicensesEmail()));
		}
		if (tasks.getSendCapacityThresholdEmail() != null) {
			initSendCapacityThresholdEmailTask(convert(tasks.getSendCapacityThresholdEmail()));
		}
	}

	private String convert(final TaskExpression task) {
		return task2CronStringConverter.convertTo(task);
	}

	private void initReleaseCapacityForExpiredClientsTask(final String expression) {
		String name = "release capacity for expired clients";
		Dictionary<String, Object> properties = new Hashtable<String, Object>();		
		properties.put(Constants.DESCRIPTION, name);
		properties.put(Constants.CRON, expression);
		releaseCapacityForExpiredClientsTask = dm.createComponent()
				.setInterface(Job.class, properties)
				.setImplementation(ReleaseCapacityForExpiredClientsTask.class)
				.add(dm.createServiceDependency().setService(EventAdmin.class).setRequired(true));
		initTask(name, releaseCapacityForExpiredClientsTask);
	}

	private void initReleaseCapacityForExpiredLicensesTask(final String expression) {
		String name = "release capacity for expired licenses";
		Dictionary<String, Object> properties = new Hashtable<String, Object>();		
		properties.put(Constants.DESCRIPTION, name);
		properties.put(Constants.CRON, expression);
		releaseCapacityForExpiredLicensesTask = dm.createComponent()
				.setInterface(Job.class, properties)
				.setImplementation(ReleaseCapacityForExpiredLicensesTask.class)
				.add(dm.createServiceDependency().setService(EventAdmin.class).setRequired(true));
		initTask(name, releaseCapacityForExpiredLicensesTask);
	}

	private void initUpdateLicensesStateTask(final String expression) {
		String name = "update licenses state";
		Dictionary<String, Object> properties = new Hashtable<String, Object>();		
		properties.put(Constants.DESCRIPTION, name);
		properties.put(Constants.CRON, expression);
		updateLicensesStateTask = dm.createComponent()
				.setInterface(Job.class, properties)
				.setImplementation(UpdateLicensesStateTask.class)
				.add(dm.createServiceDependency().setService(EventAdmin.class).setRequired(true));
		initTask(name, updateLicensesStateTask);
	}

	private void initSendExpiringLicensesEmailTask(final String expression) {
		String name = "send expiring licenses email";
		Dictionary<String, Object> properties = new Hashtable<String, Object>();		
		properties.put(Constants.DESCRIPTION, name);
		properties.put(Constants.CRON, expression);
		sendExpiringLicensesEmailTask = dm.createComponent()
				.setInterface(Job.class, properties)
				.setImplementation(SendExpiringLicensesEmailTask.class)
				.add(dm.createServiceDependency().setService(EventAdmin.class).setRequired(true));
		initTask(name, sendExpiringLicensesEmailTask);
	}

	private void initSendCapacityThresholdEmailTask(final String expression) {
		String name = "send capacity threshold email";
		Dictionary<String, Object> properties = new Hashtable<String, Object>();		
		properties.put(Constants.DESCRIPTION, name);
		properties.put(Constants.CRON, expression);
		sendCapacityThresholdEmailTask = dm.createComponent()
				.setInterface(Job.class, properties)
				.setImplementation(SendCapacityThresholdEmailTask.class)
				.add(dm.createServiceDependency().setService(EventAdmin.class).setRequired(true));
		initTask(name, sendCapacityThresholdEmailTask);
	}

	private void initTask(final String name, org.apache.felix.dm.Component component) {
		log().info("Starting task: {}", name);
		cancelTask(component);
		startTask(component);
	}

	private void startTask(org.apache.felix.dm.Component component) {
		log().debug("Add new task");
		try {
			dm.add(component);
		} catch (final Exception e) {
			log().error("Exception during starting task", e);
		}
	}

	private void cancelTask(org.apache.felix.dm.Component component) {
		if (component != null) {
			log().debug("Cancel existing task");
			try {
				dm.remove(component);
			} catch (final Exception e) {
				log().error("Exception during canceling task", e);
			}
		}
	}

	private Logger log() {
		return LoggerFactory.getLogger(InitSchedules.class);
	}
}
