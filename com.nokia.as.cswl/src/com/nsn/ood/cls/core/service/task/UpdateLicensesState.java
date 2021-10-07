package com.nsn.ood.cls.core.service.task;

import java.util.List;

import org.joda.time.DateTime;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.operation.FeatureLockOperation;
import com.nsn.ood.cls.core.operation.FeatureLockOperation.LockException;
import com.nsn.ood.cls.core.operation.LicenseStateUpdateOperation;
import com.nsn.ood.cls.core.operation.UpdateCapacityOperation;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.util.log.Loggable;

@Loggable
public class UpdateLicensesState implements EventHandler {
	
private static final Logger LOG = LoggerFactory.getLogger(TaskEventsHandler.class);
	
	private FeatureLockOperation featureLockOperation;
	private LicenseStateUpdateOperation licenseStateUpdateOperation;
	private UpdateCapacityOperation updateCapacityOperation;

	@Override
	public void handleEvent(Event event) {
		try {
			handle();
		} catch (LockException | UpdateException e) {
			LOG.error("Cannot update licenses state.", e);
		}
	}

	private void handle() throws LockException, UpdateException {
		final DateTime now = DateTime.now();
		final List<Long> featureCodes = this.featureLockOperation.lockForLicensesState(now);
		if (!featureCodes.isEmpty()) {
			this.licenseStateUpdateOperation.updateState(now);
			this.updateCapacityOperation.updateCapacity(featureCodes);
		}
	}
}
