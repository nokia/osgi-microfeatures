package com.nsn.ood.cls.core.service.task;

import java.util.List;

import org.joda.time.DateTime;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.operation.FeatureLockOperation;
import com.nsn.ood.cls.core.operation.FeatureLockOperation.LockException;
import com.nsn.ood.cls.core.operation.FeatureReleaseOperation;
import com.nsn.ood.cls.core.operation.FeatureReleaseOperation.ReleaseException;
import com.nsn.ood.cls.core.operation.UpdateCapacityOperation;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.util.log.Loggable;

@Loggable
public class ReleaseCapacityForExpiredClients implements EventHandler {
	private static final Logger LOG = LoggerFactory.getLogger(TaskEventsHandler.class);
	
	private FeatureLockOperation featureLockOperation;
	private FeatureReleaseOperation featureReleaseOperation;
	private UpdateCapacityOperation updateCapacityOperation;

	@Override
	
	public void handleEvent(Event event) {
		try {
			handle();
		} catch (final LockException | ReleaseException | UpdateException e) {
			LOG.error("Cannot release reservations for expired clients.", e);
		}
	}
	
	private void handle() throws LockException, ReleaseException, UpdateException {
		final DateTime now = DateTime.now();
		final List<Long> featureCodes = this.featureLockOperation.lockForExpiredClients(now);
		if (!featureCodes.isEmpty()) {
			this.featureReleaseOperation.releaseForExpiredClients(now);
			this.updateCapacityOperation.updateCapacity(featureCodes);
		} 
	}

}
