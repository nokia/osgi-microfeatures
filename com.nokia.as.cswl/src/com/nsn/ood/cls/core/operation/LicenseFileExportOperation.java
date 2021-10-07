/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.license.QueryLicenseFile;
import com.nsn.ood.cls.core.model.LicenseFile;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.Strings;
import com.nsn.ood.cls.util.exception.CLSException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = LicenseFileExportOperation.class)
@Loggable
public class LicenseFileExportOperation {
	private static final Logger LOG = LoggerFactory.getLogger(LicenseFileExportOperation.class);

	@ServiceDependency(filter = "(name=query)")
	private StatementExecutor<Query> queryExecutor;

	public LicenseFile export(final License license) throws ExportException {
		final LicenseFile licenseFile = retrieveLicenseFile(license);
		verifyLicenseFile(licenseFile);
		loadContent(licenseFile);
		return licenseFile;
	}

	private LicenseFile retrieveLicenseFile(final License license) {
		final QueryLicenseFile query = createQueryLicenseFile(license.getSerialNumber());
		try {
			this.queryExecutor.execute(query);
		} catch (final SQLException e) {
			LOG.error(LogMessage.QUERY_FAIL, e);
		}
		return query.getValue();
	}

	protected QueryLicenseFile createQueryLicenseFile(final String serialNumber) {
		return new QueryLicenseFile(serialNumber);
	}

	private void verifyLicenseFile(final LicenseFile licenseFile) throws ExportException {
		if (licenseFile == null) {
			throw new ExportException("License does not exist");
		}
	}

	private void loadContent(final LicenseFile licenseFile) throws ExportException {
		try {
			final File file = new File(Strings.nullToEmpty(licenseFile.getContent()));
			licenseFile.setContent(FileUtils.readFileToString(file));
		} catch (final IOException e) {
			throw new ExportException(e);
		}
	}

	public static final class ExportException extends CLSException {
		private static final long serialVersionUID = 1205770805441585780L;

		private ExportException(final Throwable cause) {
			super(cause.getMessage(), cause);
		}

		private ExportException(final String message) {
			super(message);
		}
	}
}
