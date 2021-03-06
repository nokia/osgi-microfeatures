// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

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
public class ReleaseCapacityForExpiredLicenses implements EventHandler {
	private static final Logger LOG = LoggerFactory.getLogger(TaskEventsHandler.class);
	
	private FeatureLockOperation featureLockOperation;
	private FeatureReleaseOperation featureReleaseOperation;
	private UpdateCapacityOperation updateCapacityOperation;
	
	@Override
	public void handleEvent(Event event) {
		try {
			handle();
		} catch (final LockException | ReleaseException | UpdateException e) {
			LOG.error("Cannot release reservations for expired licenses.", e);
		}
	}
	
	public void handle() throws LockException, ReleaseException, UpdateException {
		final DateTime now = DateTime.now();
		final List<Long> featureCodes = this.featureLockOperation.lockForExpiredLicenses(now);
		if (!featureCodes.isEmpty()) {
			this.featureReleaseOperation.releaseForExpiredLicenses(now);
			this.updateCapacityOperation.updateCapacity(featureCodes);
		}
	}

}
