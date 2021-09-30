/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.core.db.license.UpdateLicensesStateToActive;
import com.nsn.ood.cls.core.db.license.UpdateLicensesStateToExpired;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = LicenseStateUpdateOperation.class)
@Loggable
public class LicenseStateUpdateOperation {
	private static final Logger LOG = LoggerFactory.getLogger(LicenseStateUpdateOperation.class);

	@ServiceDependency(filter = "(name=update)")
	private StatementExecutor<Update> updateExecutor;
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DatetimeConverter;

	public void updateState(final DateTime date) throws UpdateException {
		try {
			this.updateExecutor.execute(new UpdateLicensesStateToActive(date, timestamp2DatetimeConverter));
			this.updateExecutor.execute(new UpdateLicensesStateToExpired(date, timestamp2DatetimeConverter));
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new UpdateException(e);
		}
	}
}
