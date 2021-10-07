/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */

package com.nokia.licensing.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nokia.licensing.dtos.FeatureStatus;
import com.nokia.licensing.dtos.TargetSystem;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseCheck;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;


public class LicenseCheckImpl implements LicenseCheck {
	LicenseCancelDataStorage cancelDataStorage;
	LicenseDataStorage dataStorage;
	String targetId;
	LicenseCheckLogic licenseCheckLogic;

	public LicenseCheckImpl(final LicenseDataStorage dataStorage, final LicenseCancelDataStorage cancelDataStorage,
			final TargetSystem targetSystem) {
		this.dataStorage = dataStorage;
		this.cancelDataStorage = cancelDataStorage;
		this.targetId = targetSystem.getTargetId();
		this.licenseCheckLogic = new LicenseCheckLogic(dataStorage, cancelDataStorage, this.targetId);
	}

	@Override
	public FeatureStatus getFeatureStatus(final long featureCode, final boolean checkDataIntegrity)
			throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getFeatureStatus", "Checking FC: " + featureCode);

		FeatureStatus featureStatus;
		try {
			featureStatus = this.licenseCheckLogic.getFeatureStatus(featureCode, checkDataIntegrity);
		} catch (final SQLException e) {
			throw new LicenseException("unexpected SQLException: " + e, e);
		} catch (final ClassNotFoundException e) {
			throw new LicenseException("unexpected ClassNotFoundException: " + e, e);
		} catch (final IOException e) {
			throw new LicenseException("unexpected IOException: " + e, e);
		}

		return featureStatus;
	}

	/**
	 * As of now the TargetNeType is ignored.
	 */
	@Override
	public FeatureStatus getFeatureStatus(final long featureCode, final boolean checkDataIntegrity,
			final String targetNeType) throws LicenseException {
		final FeatureStatus featureStatus = getFeatureStatus(featureCode, checkDataIntegrity);

		return featureStatus;
	}

	/**
	 * 
	 * @param featureCode
	 * @param cachedDigest
	 * @return
	 * @throws LicenseException
	 */
	@Override
	public FeatureStatus getFeatureStatus(final long featureCode, final String cachedDigest) throws LicenseException {
		FeatureStatus featureStatus;
		try {
			featureStatus = this.licenseCheckLogic.getFeatureStatus(featureCode, cachedDigest);
		} catch (final SQLException e) {
			throw new LicenseException("unexpected SQLException: " + e, e);
		} catch (final ClassNotFoundException e) {
			throw new LicenseException("unexpected ClassNotFoundException: " + e, e);
		} catch (final IOException e) {
			throw new LicenseException("unexpected IOException: " + e, e);
		}

		return featureStatus;
	}

	/**
	 * 
	 * @param featureCodes
	 * @return
	 * @throws LicenseException
	 */
	@Override
	public List<FeatureStatus> getFeatureStatus(final Map<Long, String> featureCodes) throws LicenseException {
		List<FeatureStatus> featureStatusList = new ArrayList<FeatureStatus>();
		try {
			featureStatusList = this.licenseCheckLogic.getFeatureStatus(featureCodes);
		} catch (final SQLException e) {
			throw new LicenseException("unexpected SQLException: " + e, e);
		} catch (final ClassNotFoundException e) {
			throw new LicenseException("unexpected ClassNotFoundException: " + e, e);
		} catch (final IOException e) {
			throw new LicenseException("unexpected IOException: " + e, e);
		}
		return featureStatusList;
	}

}
