// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.impl;

import java.util.Date;
import java.util.List;

import com.nokia.licensing.dtos.License;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseAccess;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.storages.jdbc.LicenseDataStorageImpl;


/**
 * This class implements the LicenseAccess Interface. It provides the methods to provide info on the licenses.
 *
 *
 * @author Sheshagiri S Rao
 *
 */
public class LicenseAccessImpl implements LicenseAccess {
	LicenseCancelDataStorage cancelDataStorage;
	LicenseDataStorage dataStorage;

	public LicenseAccessImpl(final LicenseDataStorage dataStorage, final LicenseCancelDataStorage cancelDataStorage)
			throws NullPointerException {
		this.dataStorage = dataStorage;
		this.cancelDataStorage = cancelDataStorage;
	}

	/**
	 * This method retreives all the active licenses present in the database.
	 *
	 */
	@Override
	public List<License> getAllLicenses(final boolean checkDataIntegrity) throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getAllLicenses", "Entered");

		final LicenseAccessLogic accessLogic = new LicenseAccessLogic();
		final List<License> allLicense = accessLogic.getAllLicensesFromDb(true);

		LicenseLogger.getInstance().finest(this.getClass().getName(), "getAllLicenses", "Returning All Licenses : " + allLicense);

		return allLicense;
	}

	/**
	 * This method is used to get the License object for the given serial Number.
	 *
	 * @param serialNbr
	 *            -- The serial Number of the given License File. checkDataIntegrity --
	 */
	@Override
	public License getLicenseBySerialNo(final String serialNbr, final boolean checkDataIntegrity)
			throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicenseBySerialNo", "Entered");

		final LicenseDataStorageImpl dataStorageImpl = new LicenseDataStorageImpl();
		final StoredLicense storedLicense = dataStorageImpl.getLicenseBySerialNo(serialNbr, checkDataIntegrity);

		if (storedLicense == null) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicenseBySerialNo",
					"No license file exists for the given serial number");

			final LicenseException licenseException = new LicenseException(
					" The license data not available in database.");

			licenseException.setErrorCode("CLJL110");
			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicenseBySerialNo",
					"error code set to: " + licenseException.getErrorCode());

			throw licenseException;
		}

		// LicenseLogger.getInstance().info(this.getClass().getName(), "getLicenseBySerialNo", "Returning License : " + allLicense);
		return null;
	}

	public License getLicenseByFileName(final String licenseFileName, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<License> getLicensesByFeatureCodeAndTargetIdAndTime(final long featureCode, final String targetId,
			final Date time, final boolean checkDataIntegrity) throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<License> getLicensesForCustomerID(final String customerID, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<License> getLicensesForCustomerName(final String customerName, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<License> getLicensesForFeatureCode(final long featureCode, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<License> getLicensesForFeatureName(final String featureName, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<License> getLicensesForLicenseCode(final String licenseCode, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<License> getLicensesForLicenseType(final String licenseType, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<License> getLicensesForName(final String licenseName, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<License> getLicensesForOrderID(final String orderID, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<License> getLicensesForSWBaseRelease(final String swReleaseBase, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<License> getLicensesForSWReleaseRelation(final String swReleaseRelation,
			final boolean checkDataIntegrity) throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<License> getLicensesForState(final String licenseState, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<License> getLicensesForTargetID(final String targetID, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<License> getLicensesForTargetType(final String targetType, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<License> getLicensesForUsageType(final String usageType, final boolean checkDataIntegrity)
			throws LicenseException {
		// TODO Auto-generated method stub
		return null;
	}
}
