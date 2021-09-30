/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service;

import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.audit.AuditLog;
import com.nsn.ood.cls.core.audit.AuditLogType;
import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.model.LicenseFile;
import com.nsn.ood.cls.core.operation.DBLicenseRetrieveOperation;
import com.nsn.ood.cls.core.operation.FeatureLockOperation;
import com.nsn.ood.cls.core.operation.FeatureLockOperation.LockException;
import com.nsn.ood.cls.core.operation.LicenseDbCancelOperation;
import com.nsn.ood.cls.core.operation.LicenseDbInstallOperation;
import com.nsn.ood.cls.core.operation.LicenseFileCancelOperation;
import com.nsn.ood.cls.core.operation.LicenseFileExportOperation;
import com.nsn.ood.cls.core.operation.LicenseFileExportOperation.ExportException;
import com.nsn.ood.cls.core.operation.LicenseFileInstallOperation;
import com.nsn.ood.cls.core.operation.LicenseRetrieveOperation;
import com.nsn.ood.cls.core.operation.StoredLicenseRetrieveOperation;
import com.nsn.ood.cls.core.operation.exception.CancelException;
import com.nsn.ood.cls.core.operation.exception.InstallException;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.operation.util.LicenseErrorType;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.gen.licenses.DBLicense;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.StoredLicense;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = LicensesService.class)
@Loggable
public class LicensesService {
	@ServiceDependency
	private LicenseRetrieveOperation licenseRetrieveOperation;
	@ServiceDependency
	private DBLicenseRetrieveOperation dbLicenseRetrieveOperation;
	@ServiceDependency
	private StoredLicenseRetrieveOperation storedLicenseRetrieveOperation;
	@ServiceDependency
	private LicenseFileInstallOperation licenseFileInstallOperation;
	@ServiceDependency
	private LicenseFileCancelOperation licenseFileCancelOperation;
	@ServiceDependency
	private LicenseFileExportOperation licenseFileExportOperation;
	@ServiceDependency
	private LicenseDbInstallOperation licenseDbInstallOperation;
	@ServiceDependency
	private LicenseDbCancelOperation licenseDbCancelOperation;
	@ServiceDependency
	private FeatureLockOperation featureLockOperation;
	@ServiceDependency
	private ServiceExceptionFactory serviceExceptionFactory;

	public MetaDataList<License> getLicenses(final Conditions conditions) throws ServiceException {
		try {
			return this.licenseRetrieveOperation.getList(conditions);
		} catch (final RetrieveException e) {
			throw this.serviceExceptionFactory.violation(e, e.getError());
		}
	}

	public MetaDataList<DBLicense> getDBLicenses(final Conditions conditions) throws ServiceException {
		try {
			return this.dbLicenseRetrieveOperation.getList(conditions);
		} catch (final RetrieveException e) {
			throw this.serviceExceptionFactory.violation(e, e.getError());
		}
	}

	public MetaDataList<StoredLicense> getStoredLicenses(final Conditions conditions) throws ServiceException {
		try {
			return this.storedLicenseRetrieveOperation.getList(conditions);
		} catch (final RetrieveException e) {
			throw this.serviceExceptionFactory.violation(e, e.getError());
		}
	}

	public List<String> getStoredLicenseFilterValues(final String filterName) throws ServiceException {
		try {
			return this.storedLicenseRetrieveOperation.getFilterValues(filterName,//
					ConditionsBuilder.createAndSkipMetaData().build());
		} catch (final RetrieveException e) {
			throw this.serviceExceptionFactory.violation(e, e.getError());
		}
	}

	@AuditLog(AuditLogType.LICENSE_INSTALLATION)
	public License install(final LicenseFile licenseFile) throws ServiceException {
		try {
			final License license = this.licenseFileInstallOperation.install(licenseFile);
			this.featureLockOperation.lock(license.getFeatures().get(0).getFeatureCode());
			this.licenseDbInstallOperation.install(license);
			return license;
		} catch (final InstallException e) {
			throw this.serviceExceptionFactory.license(convertCode(e.getErrorType(), true), e, e.getCljlErrorCode(),
					createLicense(licenseFile.getFileName()));
		} catch (final LockException e) {
			throw this.serviceExceptionFactory.error(ErrorCode.CONCURRENT_ACTIONS_FAIL, e);
		}
	}

	private License createLicense(final String fileName) {
		return new License().withFileName(fileName).withTargets(null).withFeatures(null);
	}

	@AuditLog(AuditLogType.LICENSE_TERMINATION)
	public License cancel(final License license) throws ServiceException {
		try {
			final License canceledLicense = this.licenseFileCancelOperation.cancel(license);
			this.featureLockOperation.lock(canceledLicense.getFeatures().get(0).getFeatureCode());
			this.licenseDbCancelOperation.cancel(canceledLicense);
			return canceledLicense;
		} catch (final CancelException e) {
			throw this.serviceExceptionFactory.license(convertCode(e.getErrorType(), false), e, e.getCljlErrorCode(),
					license);
		} catch (final LockException e) {
			throw this.serviceExceptionFactory.error(ErrorCode.CONCURRENT_ACTIONS_FAIL, e);
		}
	}

	private ErrorCode convertCode(final LicenseErrorType type, final boolean install) {
		if (type == LicenseErrorType.VERIFICATION) {
			return ErrorCode.LICENSE_VERIFICATION_FAIL;
		} else if (type == LicenseErrorType.CLJL) {
			return install ? ErrorCode.CLJL_LICENSE_INSTALL_FAIL : ErrorCode.CLJL_LICENSE_CANCEL_FAIL;
		} else {
			return install ? ErrorCode.LICENSE_INSTALL_FAIL : ErrorCode.LICENSE_CANCEL_FAIL;
		}
	}

	public LicenseFile export(final License license) throws ServiceException {
		try {
			return this.licenseFileExportOperation.export(license);
		} catch (final ExportException e) {
			throw this.serviceExceptionFactory.license(ErrorCode.LICENSE_EXPORT_FAIL, e, null, license);
		}
	}
}
