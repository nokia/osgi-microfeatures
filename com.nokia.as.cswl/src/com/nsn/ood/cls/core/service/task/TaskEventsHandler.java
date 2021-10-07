/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.task;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.nsn.ood.cls.core.operation.EmailSendOperation;
import com.nsn.ood.cls.core.operation.FeatureLockOperation;
import com.nsn.ood.cls.core.operation.FeatureReleaseOperation;
import com.nsn.ood.cls.core.operation.LicenseStateUpdateOperation;
import com.nsn.ood.cls.core.operation.UpdateCapacityOperation;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component
@Loggable
public class TaskEventsHandler {
	@Inject
	private DependencyManager dm;
	
	@Start
	public void createEventHandlers() {
		createReleaseCapacityForExpiredClients();
		createReleaseCapacityForExpiredLicenses();
		createSendCapacityThresholdEmail();
		createSendExpiringLicensesEmail();
		createUpdateLicensesState();
	}
	
	private void createReleaseCapacityForExpiredClients() {
		Dictionary<String, Object> properties = new Hashtable<>();
		
		String[] topics = new String[] {
				"com/nsn/ood/cls/core/event/releaseCapacityForExpiredClients"
	    };
		properties.put(EventConstants.EVENT_TOPIC, topics);
		
		dm.add(dm.createComponent()
		  .setInterface(EventHandler.class, properties)
		  .setImplementation(ReleaseCapacityForExpiredClients.class)
		  .add(dm.createServiceDependency().setService(FeatureLockOperation.class).setRequired(true))
		  .add(dm.createServiceDependency().setService(FeatureReleaseOperation.class).setRequired(true))
		  .add(dm.createServiceDependency().setService(UpdateCapacityOperation.class).setRequired(true)));
	}
	
	private void createReleaseCapacityForExpiredLicenses() {
		Dictionary<String, Object> properties = new Hashtable<>();
		
		String[] topics = new String[] {
				"com/nsn/ood/cls/core/event/releaseCapacityForExpiredLicenses"
	    };
		properties.put(EventConstants.EVENT_TOPIC, topics);
		
		dm.add(dm.createComponent()
		  .setInterface(EventHandler.class, properties)
		  .setImplementation(ReleaseCapacityForExpiredLicenses.class)
		  .add(dm.createServiceDependency().setService(FeatureLockOperation.class).setRequired(true))
		  .add(dm.createServiceDependency().setService(FeatureReleaseOperation.class).setRequired(true))
		  .add(dm.createServiceDependency().setService(UpdateCapacityOperation.class).setRequired(true)));
	}
	
	private void createSendCapacityThresholdEmail() {
		Dictionary<String, Object> properties = new Hashtable<>();
		
		String[] topics = new String[] {
				"com/nsn/ood/cls/core/event/sendCapacityThresholdEmail"
	    };
		properties.put(EventConstants.EVENT_TOPIC, topics);
		
		dm.add(dm.createComponent()
		  .setInterface(EventHandler.class, properties)
		  .setImplementation(SendCapacityThresholdEmail.class)
		  .add(dm.createServiceDependency().setService(EmailSendOperation.class).setRequired(true)));
	}
	
	private void createSendExpiringLicensesEmail() {
		Dictionary<String, Object> properties = new Hashtable<>();
		
		String[] topics = new String[] {
				"com/nsn/ood/cls/core/event/sendExpiringLicensesEmail"
	    };
		properties.put(EventConstants.EVENT_TOPIC, topics);
		
		dm.add(dm.createComponent()
		  .setInterface(EventHandler.class, properties)
		  .setImplementation(SendExpiringLicensesEmail.class)
		  .add(dm.createServiceDependency().setService(EmailSendOperation.class).setRequired(true)));
	}
	
	private void createUpdateLicensesState() {
		Dictionary<String, Object> properties = new Hashtable<>();
		
		String[] topics = new String[] {
				"com/nsn/ood/cls/core/event/updateLicensesState"
	    };
		properties.put(EventConstants.EVENT_TOPIC, topics);
		
		dm.add(dm.createComponent()
		  .setInterface(EventHandler.class, properties)
		  .setImplementation(UpdateLicensesState.class)
		  .add(dm.createServiceDependency().setService(FeatureLockOperation.class).setRequired(true))
		  .add(dm.createServiceDependency().setService(LicenseStateUpdateOperation.class).setRequired(true))
		  .add(dm.createServiceDependency().setService(UpdateCapacityOperation.class).setRequired(true)));
	}
}
