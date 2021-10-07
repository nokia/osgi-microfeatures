package com.nsn.ood.cls.core.startup;

import java.util.Dictionary;
import java.util.Hashtable;

import org.amdatu.scheduling.Job;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class ReleaseCapacityForExpiredLicensesTask implements Job {
	
	private EventAdmin admin;
	
	@Override
	public void execute() {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
        admin.postEvent(new Event("com/nsn/ood/cls/core/event/releaseCapacityForExpiredLicenses", properties));
	}
}
