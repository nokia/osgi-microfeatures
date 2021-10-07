/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl;

import java.io.File;
import java.io.InputStream;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.interfaces.LicenseInstall;


/**
 * @author marynows
 *
 */
public class LicenseInstallSpy implements LicenseInstall {
	public static final StoredLicense STORED_LICENSE = new StoredLicense();

	private InputStream licenseFileStream;
	private String licenseFileName;
	private Boolean forceInstall;
	private String targetId;
	private String username;
	private LicenseCancelInfo licenseCancelInfo;

	public InputStream getLicenseFileStream() {
		return this.licenseFileStream;
	}

	public String getLicenseFileName() {
		return this.licenseFileName;
	}

	public Boolean getForceInstall() {
		return this.forceInstall;
	}

	public String getTargetId() {
		return this.targetId;
	}

	public String getUsername() {
		return this.username;
	}

	public LicenseCancelInfo getLicenseCancelInfo() {
		return this.licenseCancelInfo;
	}

	@Override
	public StoredLicense parseAndValidate(final InputStream licenseFileStream, final String licenseFileName)
			throws LicenseException {
		return null;
	}

	@Override
	public StoredLicense installLicense(final File licenseFileName, final boolean forceInstall)
			throws LicenseException {
		return null;
	}

	@Override
	public StoredLicense installLicense(final InputStream licenseFileStream, final boolean forceInstall)
			throws LicenseException {
		return null;
	}

	@Override
	public StoredLicense installLicense(final InputStream licenseFileStream, final String licenseFileName,
			final boolean forceInstall, final String targetId) throws LicenseException {
		return null;
	}

	@Override
	public StoredLicense installLicense(final InputStream licenseFileStream, final String licenseFileName,
			final boolean forceInstall, final String targetId, final String username) throws LicenseException {
		this.licenseFileStream = licenseFileStream;
		this.licenseFileName = licenseFileName;
		this.forceInstall = forceInstall;
		this.targetId = targetId;
		this.username = username;

		return STORED_LICENSE;
	}

	@Override
	public StoredLicense deleteLicenseWithFeedback(final String serialNbr) throws LicenseException {
		return null;
	}

	@Override
	public void deleteLicenseBySerialNumber(final String serialNbr) throws LicenseException {
	}

	@Override
	public StoredLicense cancelLicenseWithFeedback(final LicenseCancelInfo license) throws LicenseException {
		this.licenseCancelInfo = license;

		return STORED_LICENSE;
	}

	@Override
	public void cancelLicense(final LicenseCancelInfo license) throws LicenseException {
	}

	@Override
	public void validate(final String serialNbr) throws LicenseException {
	}
}
