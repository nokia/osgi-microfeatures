package com.nokia.licensing.impl;

import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.utils.LicensesFilesRepository;


public class StoredLicenseValiator {
	private final LicenseCancelDataStorage cancelDataStorage;
	private final LicensesFilesRepository fileRepository;
	private final Pattern licensePattern;
	private final Pattern nmsLicensePattern;

	public StoredLicenseValiator(final LicenseCancelDataStorage cancelDataStorage,
			final LicensesFilesRepository fileRepository) {
		this.cancelDataStorage = cancelDataStorage;
		this.fileRepository = fileRepository;
		this.licensePattern = Pattern.compile("[A-Z][0-9]{7}([0-9]{5})?");
		this.nmsLicensePattern = Pattern.compile("[A-Z][A-Z][0-9]{6}");
	}

	public void handleCanceledLicense(final StoredLicense storelicense) throws LicenseException {
		if (isLicenseCancelled(storelicense)) {
			this.fileRepository.deleteLicenseFromFileRepo(storelicense.getLicenseFileName());
			throw prepareCanceledLicenseException(storelicense);
		}
	}

	public void handleChekFileNameValidity(final StoredLicense storedLicense) throws LicenseException {
		checkFileNameValidity(storedLicense);
	}

	private boolean checkFileNameValidity(final StoredLicense storedLicense)
			throws NumberFormatException, LicenseException {

		final StringTokenizer licName = new StringTokenizer(storedLicense.getLicenseFileName(), ".");
		boolean nameValidity = false;
		boolean extnValidity = false;
		boolean licenseNameValidity = false;
		final String licenseFileName = storedLicense.getLicenseFileName();
		if (licName.countTokens() == 2) {
			final String name = licName.nextToken();
			final String extn = licName.nextToken();
			// validate RHS of file format
			if (extn.trim().equals("XML")) {
				extnValidity = true;
			}
			// validate LHS of file format

			if ((storedLicense.getOriginOMC() != null) && (storedLicense.getPool() != null)) {
				final Matcher nmsLicenseMatcher = this.nmsLicensePattern.matcher(name);
				if (nmsLicenseMatcher.matches()) {
					nameValidity = true;
				}
			} else {
				final Matcher licenseMatcher = this.licensePattern.matcher(name);
				if (licenseMatcher.matches()) {
					nameValidity = true;
				}
			}
			if (nameValidity && extnValidity) {
				licenseNameValidity = true;
			}
		}
		// Throw an exception if the file format is invalid.
		if (licenseNameValidity == false) {
			this.fileRepository.deleteLicenseFromFileRepo(licenseFileName);
			final LicenseException licenseException = new LicenseException("The license " + licenseFileName
					+ " cannot be installed." + "\nLicense file name format is invalid."
					+ "\nLicense file name format should be adhering to NSN"
					+ " Standard License Key Generator in 13.3/8.3 format.");
			licenseException.setErrorCode("CLJL102");
			LicenseLogger.getInstance().error(this.getClass().getName(), "installLicense",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		}
		return licenseNameValidity;
	}

	private LicenseException prepareCanceledLicenseException(final StoredLicense storelicense) {
		LicenseLogger.getInstance().error(this.getClass().getName(), "install",
				"The license file you are trying to " + "install is cancelled." + storelicense.getLicenseFileName());

		final LicenseException licenseException = new LicenseException(
				"Operation failed: The license file is cancelled.");

		licenseException.setErrorCode("CLJL103");
		LicenseLogger.getInstance().error(this.getClass().getName(), "install",
				"error code set to: " + licenseException.getErrorCode());

		return licenseException;
	}

	private boolean isLicenseCancelled(final StoredLicense storelicense) throws LicenseException {
		List<LicenseCancelInfo> listForLicenseCancelInfo;
		listForLicenseCancelInfo = this.cancelDataStorage.getCancelInfoBySerialNumber(storelicense.getSerialNbr(),
				true);

		for (final LicenseCancelInfo cancelInfo : listForLicenseCancelInfo) {
			LicenseLogger.getInstance().finest(this.getClass().getName(), "install", "Cancel Info :" + cancelInfo);
			if (storelicense.getSerialNbr().equals(cancelInfo.getSerialNbr())) {
				return true;
			}
		}
		return false;
	}

	public void handleExpiredLicense(final StoredLicense license) throws LicenseException {
		if (isLicenseExpired(license)) {
			this.fileRepository.deleteLicenseFromFileRepo(license.getLicenseFileName());
			throw prepareExpiredLicenseException(license);
		}
	}

	private LicenseException prepareExpiredLicenseException(final StoredLicense license) {
		LicenseLogger.getInstance().error(this.getClass().getName(), "install",
				"License file has been Expired...." + license.getLicenseFileName());
		final LicenseException licenseException = new LicenseException(
				"Operation failed: The license file is expired.");

		licenseException.setErrorCode("CLJL104");
		LicenseLogger.getInstance().error(this.getClass().getName(), "install",
				"error code set to: " + licenseException.getErrorCode());
		return licenseException;
	}

	private boolean isLicenseExpired(final StoredLicense license) {
		final Date currentDateTime = new Date();
		final Date endDateTime = license.getEndTime();
		if ((endDateTime != null) && endDateTime.before(currentDateTime)) {
			return true;
		}
		return false;
	}

}