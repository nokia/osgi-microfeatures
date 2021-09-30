/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import com.nokia.licensing.dtos.FeatureInfo;
import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.dtos.TargetSystem;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.interfaces.LicenseInstall;
import com.nokia.licensing.interfaces.TargetPlatformCapabilities;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.plugins.PluginRegistry;
import com.nokia.licensing.utils.LicenseEncrypt;
import com.nokia.licensing.utils.LicenseValidator;
import com.nokia.licensing.utils.LicenseXMLParser;
import com.nokia.licensing.utils.LicensesFilesRepository;
import com.nokia.licensing.utils.LicensesFilesRepositoryImpl;
import com.nokia.licensing.utils.LicensesFilesTemporaryRepositoryImpl;


/**
 * This is the implementation class of LicenseInstall interface.
 *
 * @author Sheshagiri S Rao
 * @version 1.0
 *
 */
public class LicenseInstallImpl implements LicenseInstall {

	LicenseDataStorage dataStorage;
	LicenseCancelDataStorage cancelDataStorage;
	LicensesFilesRepository filesRepository;
	LicensesFilesTemporaryRepositoryImpl temporaryFilesRepository;

	LicenseValidator licenseValidator;
	LicenseInstallLogic installLogic;
	StoredLicenseValiator temporaryStoredLicenseValiator;

	LicenseIntegrityValidator licenseIntegrityValidator;

	public LicenseInstallImpl(final LicenseDataStorage dataStorage, final LicenseCancelDataStorage cancelDataStorage)
			throws NullPointerException {
		this.dataStorage = dataStorage;
		this.cancelDataStorage = cancelDataStorage;
		this.filesRepository = LicensesFilesRepositoryImpl.getInstance();
		this.temporaryFilesRepository = LicensesFilesTemporaryRepositoryImpl.getInstance();
		this.licenseValidator = new LicenseValidator();
		this.installLogic = new LicenseInstallLogic(dataStorage, cancelDataStorage);
		this.temporaryStoredLicenseValiator = new StoredLicenseValiator(cancelDataStorage,
				this.temporaryFilesRepository);
		this.licenseIntegrityValidator = new LicenseIntegrityValidator(dataStorage, this.filesRepository);
	}

	/**
	 * This method allows you to install a License File.
	 *
	 * @param licenseFileStream
	 *            - Input Stream of the File licenseFileName - The License File Name which you need to install - Please
	 *            dont specify the absolute path here. forceInstall - if this attribute is FALSE then you are not
	 *            allowed to re-install this License File - if this attribute is TRUE then you are allowed to re-install
	 *            this License File targetId - target system identifier, if not null license key targets are verified
	 *
	 * @throws LicenseException
	 *             - If the License File is not able to install, LicenseException is thrown.
	 */
	// Test only this and parse and validate
	@Override
	public StoredLicense installLicense(final InputStream licenseFileStream, final String licenseFileName,
			final boolean forceInstall, final String targetId) throws LicenseException {
		return install(licenseFileStream, licenseFileName, forceInstall, targetId, null);
	}

	/**
	 * This method allows you to install a License File.
	 *
	 * @param licenseFileStream
	 *            - Input Stream of the File licenseFileName - The License File Name which you need to install - Please
	 *            dont specify the absolute path here. forceInstall - if this attribute is FALSE then you are not
	 *            allowed to re-install this License File - if this attribute is TRUE then you are allowed to re-install
	 *            this License File targetId - target system identifier, if not null license key targets are verified
	 *            username - user who is installing license
	 *
	 * @throws LicenseException
	 *             - If the License File is not able to install, LicenseException is thrown.
	 */
	// Test only this and parse and validate
	@Override
	public StoredLicense installLicense(final InputStream licenseFileStream, final String licenseFileName,
			final boolean forceInstall, final String targetId, final String username) throws LicenseException {
		return install(licenseFileStream, licenseFileName, forceInstall, targetId, username);
	}

	private StoredLicense install(final InputStream licenseFileStream, final String licenseFileName,
			final boolean forceInstall, final String targetId, final String username) throws LicenseException {
		LicenseLogger.getInstance().info(this.getClass().getName(), "installLicense", String.format(
				"License installation: for Target Id: %s, user: %s, force: %b", targetId, username, forceInstall));

		this.temporaryFilesRepository.copyLicenseIntoFileRepo(licenseFileStream, licenseFileName);
		StoredLicense storedLicense = this.temporaryFilesRepository.readStoredLicense(licenseFileName);
		// Validate license filename formats against NSN Standard LKG in 13.3/8.3 format

		this.temporaryStoredLicenseValiator.handleChekFileNameValidity(storedLicense);
		if (targetId != null) {
			checkTargetIdValidity(storedLicense, targetId);
		}

		final boolean status = this.temporaryFilesRepository.isValid(storedLicense, true, targetId != null);

		this.temporaryStoredLicenseValiator.handleExpiredLicense(storedLicense);
		this.temporaryStoredLicenseValiator.handleCanceledLicense(storedLicense);

		final Date licenseFileImportTime = getCurrentDate(licenseFileName);

		// check test responder status
		if ((status == true) && (storedLicense != null)) {
			LicenseLogger.getInstance().finest(this.getClass().getName(), "installLicense",
					"The status is true and stored license is not null...");
			this.temporaryFilesRepository.moveLicenseToRepository(this.filesRepository, licenseFileName);
			storedLicense = this.filesRepository.readStoredLicense(licenseFileName);
			storedLicense.setLicenseFileImportTime(licenseFileImportTime);
			if (username != null) {
				storedLicense.setLicenseFileImportUser(username);
				LicenseLogger.getInstance().finest(this.getClass().getName(), "installLicense", "StoredLicense.username="
						+ storedLicense.getLicenseFileImportUser() + " | username=" + username);
			}
			// Code for force Install installation.
			handlingForceInstallParameter(storedLicense, forceInstall);

		}
		if (status == false) {
			this.temporaryFilesRepository.deleteLicenseFromFileRepo(licenseFileName);
			final LicenseException licenseException = new LicenseException(
					"License " + licenseFileName + " is corrupted - signature validation failed");
			licenseException.setErrorCode("CLJL101");
			LicenseLogger.getInstance().error(this.getClass().getName(), "installLicense",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		}

		return storedLicense;
	}

	private Date getCurrentDate(final String licenseFileName) throws LicenseException {
		try {
			final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			final Calendar cal = Calendar.getInstance();
			final Date currentdate = cal.getTime();
			final String formattedCurrentDate = df.format(currentdate);
			return df.parse(formattedCurrentDate);
		} catch (final ParseException e) {
			this.temporaryFilesRepository.deleteLicenseFromFileRepo(licenseFileName);
			final LicenseException ex = new LicenseException(" Parse Exception.");
			ex.setErrorCode("CLJL119");
			LicenseLogger.getInstance().error(this.getClass().getName(), "installLicense", "error code set to: " + ex.getErrorCode());
			throw ex;
		}
	}

	/**
	 * Checks whether provided target id matches the one in license file. If not {@link LicenseException} is thrown
	 *
	 * @param storedLicense
	 * @param targetId
	 * @throws LicenseException
	 */
	private void checkTargetIdValidity(final StoredLicense storedLicense, final String targetId)
			throws LicenseException {
		final TargetSystem targetSystem = new TargetSystem();
		targetSystem.setTargetId(targetId);
		if (isProductionPlatform()
				&& ((storedLicense.getTargetIds() == null) || !storedLicense.getTargetIds().contains(targetSystem))) {
			final LicenseException licenseException = new LicenseException(" Target id verification Exception.");
			licenseException.setErrorCode("CLJL130");
			throw licenseException;
		}

	}

	private void handlingForceInstallParameter(final StoredLicense storedLicense, final boolean forceInstall)
			throws LicenseException {

		final StoredLicense dbStoLic = this.dataStorage.getLicenseBySerialNo(storedLicense.getSerialNbr(), true);

		if (forceInstall == false) {

			if (null == dbStoLic) {
				this.installLogic.install(storedLicense);
			} else {
				LicenseLogger.getInstance().error(this.getClass().getName(), "installLicense", " The force install "
						+ "parameter is set to false. Hence updating a license file is not allowed.");
				final LicenseException licenseException = new LicenseException(
						"The license file is already installed. The force install parameter "
								+ "is set to false. Hence updating a license file is not allowed.");
				licenseException.setErrorCode("CLJL105");
				LicenseLogger.getInstance().error(this.getClass().getName(), "handlingForceInstallParameter",
						"error code set to: " + licenseException.getErrorCode());
				throw licenseException;
			}
		} else {

			if (null == dbStoLic) {
				LicenseLogger.getInstance().finest(this.getClass().getName(), "installLicense",
						" The force install " + "parameter is set to true. Hence updating a license file is allowed.");
				this.installLogic.install(storedLicense);
			} else {								
				this.dataStorage.deleteLicenseBySerialNumber(storedLicense.getSerialNbr());
				this.installLogic.install(storedLicense);
			}
		}
	}

	/**
	 * This functions parses the license file and performs the validation of the same.
	 *
	 * @param licenseFileStream
	 *
	 * @param licenseFileName
	 *            -- We need to send only the name of the file. No need to send the entire path.
	 * @return StoredLicense
	 *
	 * @throws LicenseException
	 */
	@Override
	public StoredLicense parseAndValidate(final InputStream licenseFileStream, final String licenseFileName)
			throws LicenseException {
		// TODO: We need to check somehow that only the fileName is sent.
		if (licenseFileStream == null) {

			final LicenseException licenseException = new LicenseException(" License File/Stream input invalid");
			licenseException.setErrorCode("CLJL101");
			LicenseLogger.getInstance().error(this.getClass().getName(), "parseAndValidate",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		}

		StoredLicense storedLicense = null;
		try {

			final File tempLicFile = this.temporaryFilesRepository.copyLicenseIntoFileRepo(licenseFileStream,
					licenseFileName);

			final LicenseXMLParser parser = new LicenseXMLParser();

			final InputStream parsingInputStream = new FileInputStream(tempLicFile);
			final InputStream validatingInputStream = new FileInputStream(tempLicFile);

			try {
				storedLicense = parser.parse(parsingInputStream);
				storedLicense.setLicenseFileName(licenseFileName);

				// Digital Signature Validation
				boolean status = false;
				try {
					status = this.licenseValidator.validate(validatingInputStream);
				} catch (final LicenseException e) {
					LicenseLogger.getInstance().error(this.getClass().getName(), "parseAndValidate",
							"Signature error code set to: " + e.getErrorCode());
				}

				// check test responder status
				if ((status == true) && (storedLicense != null)) {
					LicenseLogger.getInstance().finest(this.getClass().getName(), "installLicense",
							"The status is true and stored license is not null...");
					// TODO: Set License File path to the stored license object.
					return storedLicense;
				}

				if (status == false) {
					final LicenseException licenseException = new LicenseException(
							" The LIcense file : " + licenseFileName + " is invalid.");
					licenseException.setErrorCode("CLJL101");
					LicenseLogger.getInstance().error(this.getClass().getName(), "parseAndValidate",
							"error code set to: " + licenseException.getErrorCode());
					throw licenseException;
				}
			} finally {
				parsingInputStream.close();
				validatingInputStream.close();
				this.temporaryFilesRepository.deleteLicenseFromFileRepo(licenseFileName);
			}

		} catch (final IOException ioe) {
			// Log the information into log files
			final LicenseException ex = new LicenseException(" IO Exception.");
			ex.setErrorCode("CLJL116");
			LicenseLogger.getInstance().error(this.getClass().getName(), "parseAndValidate",
					"error code set to: " + ex.getErrorCode());
			throw ex;
		} catch (final DOMException domexe) {
			// Log the information into log files
			final LicenseException ex = new LicenseException(" DOM Exception.");
			ex.setErrorCode("CLJL117");
			LicenseLogger.getInstance().error(this.getClass().getName(), "parseAndValidate",
					"error code set to: " + ex.getErrorCode());
			throw ex;
		} catch (final SAXException saxex) {
			// Log the information into log files
			final LicenseException ex = new LicenseException(" SAX Exception.");
			ex.setErrorCode("CLJL118");
			LicenseLogger.getInstance().error(this.getClass().getName(), "parseAndValidate",
					"error code set to: " + ex.getErrorCode());
			throw ex;
		}
		return null;

	}

	/**
	 * This method allows you to install a License File.
	 *
	 * @param licenseFileName
	 *            - The License File which you need to install forceInstall - if this attribute is FALSE then you are
	 *            not allowed to re-install this License File - if this attribute is TRUE then you are allowed to
	 *            re-install this License File
	 *
	 * @throws LicenseException
	 *             - If the License File is not able to install, LicenseException is thrown.
	 */
	@Override
	public StoredLicense installLicense(final File licenseFileName, final boolean forceInstall)
			throws LicenseException {
		try {
			final InputStream inputStream = new FileInputStream(licenseFileName);
			return installLicense(inputStream, licenseFileName.getName(), true, null);

		} catch (final FileNotFoundException fne) {
			// Log the information into log files
			final LicenseException ex = new LicenseException(" FileNotFound Exception.");
			ex.setErrorCode("CLJL120");
			LicenseLogger.getInstance().error(this.getClass().getName(), "installLicense", "error code set to: " + ex.getErrorCode());
			throw ex;
		}
	}

	/**
	 * This method allows you to cancel a file. It moves the License File from dataStorage to cancelDataStorage. Also
	 * the license file is moved from success folder to cancel folder.
	 *
	 * @param LicenseCancelInfo
	 */
	@Override
	public void cancelLicense(final LicenseCancelInfo license) throws LicenseException {
		cancelLicenseWithFeedback(license);
	}

	/**
	 * This method allows you to cancel a file. It moves the License File from dataStorage to cancelDataStorage. Also
	 * the license file is moved from success folder to cancel folder.
	 *
	 * @param LicenseCancelInfo
	 * @return canceled license information
	 */
	@Override
	public StoredLicense cancelLicenseWithFeedback(final LicenseCancelInfo license) throws LicenseException {
		byte[] encryptKey;
		boolean status = false;

		LicenseLogger.getInstance().info(this.getClass().getName(), "cancelLicense",
				String.format("License with serial: %s canelation", license.getSerialNbr()));

		final StoredLicense storedLicense = this.dataStorage.getLicenseBySerialNo(license.getSerialNbr(), true);
		if ((storedLicense != null) && storedLicense.getLicenseFileName().equals(license.getLicenseFileName())) {
			encryptKey = storedLicense.getKey();
			if (license.getFeaturecode() >= 0) {
				status = this.dataStorage.deleteLicenseBySerialNumberAndFeatureCode(storedLicense.getSerialNbr(),
						license.getFeaturecode());
				if (status) {
					insertCancellInformation(license.getFeaturecode(), license, encryptKey);
					if (storedLicense.getFeatureInfoList().size() == 1) {
						this.filesRepository.deleteLicenseFromFileRepo(storedLicense.getLicenseFileName());
					}
				}
			} else {
				status = this.dataStorage.deleteLicenseBySerialNumber(storedLicense.getSerialNbr());
				if (status) {
					final List<FeatureInfo> featureInfoList = storedLicense.getFeatureInfoList();
					for (final FeatureInfo featureInfo : featureInfoList) {
						final long featurecode = featureInfo.getFeatureCode();

						license.setFeaturecode(featurecode);
						insertCancellInformation(featurecode, license, encryptKey);
					}
					this.filesRepository.deleteLicenseFromFileRepo(storedLicense.getLicenseFileName());

				}

			}

		} else {
			LicenseLogger.getInstance().error(this.getClass().getName(), "cancelLicense",
					" The License File you are trying to cancel does not exist");
			final LicenseException licenseException = new LicenseException(
					" The license data not available in database");
			licenseException.setErrorCode("CLJL110");
			LicenseLogger.getInstance().error(this.getClass().getName(), "cancelLicense",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		}
		return storedLicense;
	}

	/**
	 * Inserts LicenseCancelIfo to the cancel data storage.
	 *
	 * @param featureCode
	 * @param license
	 * @param encryptKey
	 * @throws LicenseException
	 */
	private void insertCancellInformation(final long featureCode, final LicenseCancelInfo license,
			final byte[] encryptKey) throws LicenseException {
		final Timestamp sqlDate = new Timestamp(license.getCancelDate().getTime());
		final String str = license.getSerialNbr() + featureCode + sqlDate + license.getCancelReason()
				+ license.getUserName() + license.getLicenseFileName();

		final byte[] cancelListSignature = LicenseEncrypt.encryptData(str, encryptKey);
		license.setCancelListSignature(cancelListSignature);
		this.cancelDataStorage.insertCancelInformation(license);
	}

	/**
	 * This method allows you to delete a License file from Data Storage. Also it deletes from the "success" folder and
	 * moves it to the "deleted" folder.
	 *
	 * @param serial
	 *            Number - The serial number of the license file you need to delete.
	 * @return deleted license information
	 */
	@Override
	public void deleteLicenseBySerialNumber(final String serialNbr) throws LicenseException {
		deleteLicenseWithFeedback(serialNbr);
	}

	/**
	 * This method allows you to delete a License file from Data Storage. Also it deletes from the "success" folder and
	 * moves it to the "deleted" folder.
	 *
	 * @param serial
	 *            Number - The serial number of the license file you need to delete.
	 * @return deleted license information
	 */
	@Override
	public StoredLicense deleteLicenseWithFeedback(final String serialNbr) throws LicenseException {

		LicenseLogger.getInstance().info(this.getClass().getName(), "deleteLicense",
				String.format("Delete license with serial: %s", serialNbr));

		final StoredLicense storedLicenseData = this.dataStorage.getLicenseBySerialNo(serialNbr, true);

		if (storedLicenseData != null) {

			final boolean status = this.dataStorage.deleteLicenseBySerialNumber(serialNbr);
			if (status) {
				LicenseLogger.getInstance().finest(this.getClass().getName(), "installLicense", "Deletion file is successfull...");
				this.filesRepository.deleteLicenseFromFileRepo(storedLicenseData.getLicenseFileName());
			}
		} else {
			LicenseLogger.getInstance().error(this.getClass().getName(), "delete license",
					" The License File you are trying to delete does not exist");
			final LicenseException licenseException = new LicenseException(
					" The license data not available in database");
			licenseException.setErrorCode("CLJL110");
			LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseBySerialNumber",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		}
		return storedLicenseData;
	}

	@Override
	public StoredLicense installLicense(final InputStream licenseFileStream, final boolean forceInstall)
			throws LicenseException {
		throw new LicenseException(" This method is not supported...");
	}

	@Override
	public void validate(final String serialNbr) throws LicenseException {
		this.licenseIntegrityValidator.validate(serialNbr);
	}

	private boolean isProductionPlatform() throws LicenseException {
		final TargetPlatformCapabilities tpf = PluginRegistry.getRegistry().getPlugin(TargetPlatformCapabilities.class);
		return (tpf == null) ? true : tpf.isProductionPlatform();
	}
}
