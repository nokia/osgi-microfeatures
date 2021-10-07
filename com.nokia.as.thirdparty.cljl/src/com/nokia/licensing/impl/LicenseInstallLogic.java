/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */

package com.nokia.licensing.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.nokia.licensing.dtos.FeatureInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.dtos.TargetSystem;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.storages.jdbc.StoredLicenseUtil;
import com.nokia.licensing.utils.LicenseEncrypt;
import com.nokia.licensing.utils.LicensesFilesRepository;
import com.nokia.licensing.utils.LicensesFilesRepositoryImpl;


/**
 * This class is responsible for installing the licenses. It delegates the calls to the LicenseDataStorage
 *
 * @author Sheshagiri S Rao
 *
 */
public class LicenseInstallLogic {
	LicenseCancelDataStorage cancelDataStorage;
	LicenseDataStorage dataStorage;
	LicensesFilesRepository fileRepository;

	StoredLicenseValiator licenseValidator;

	public LicenseInstallLogic() {
		// TODO Auto-generated constructor stub
	}

	// This value flows from the LicenseInstall Factory
	public LicenseInstallLogic(final LicenseDataStorage dataStorage, final LicenseCancelDataStorage cancelDataStorage) {
		this.dataStorage = dataStorage;
		this.cancelDataStorage = cancelDataStorage;
		this.fileRepository = LicensesFilesRepositoryImpl.getInstance();
		this.licenseValidator = new StoredLicenseValiator(this.cancelDataStorage, this.fileRepository);
	}

	public LicenseInstallLogic(final LicenseDataStorage dataStorage, final LicenseCancelDataStorage cancelDataStorage,
			final LicensesFilesRepository fileRepository) {
		this.dataStorage = dataStorage;
		this.cancelDataStorage = cancelDataStorage;
		this.fileRepository = fileRepository;
		this.licenseValidator = new StoredLicenseValiator(this.cancelDataStorage, this.fileRepository);
	}

	/**
	 * This method allows you install a license file.
	 *
	 * @param storelicense
	 *
	 * @throws LicenseException
	 */
	public void install(final StoredLicense storelicense) throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "install", "Stored License Value : " + storelicense);
		LicenseLogger.getInstance().finest(this.getClass().getName(), "install",
				"storelicense.getLicenseName : " + storelicense.getLicenseFileName());

		this.licenseValidator.handleExpiredLicense(storelicense);

		this.licenseValidator.handleCanceledLicense(storelicense);

		final List<StoredLicense> listSLWithSignature = prepareLicenseToSave(storelicense);

		this.dataStorage.insertLicenseInformation(listSLWithSignature);		
		LicenseLogger.getInstance().finest(this.getClass().getName(), "install", "Installation succesfull.....");
	}

	private List<StoredLicense> prepareLicenseToSave(final StoredLicense storedLicense) {
		StoredLicense storedLicenseTemp = null;
		FeatureInfo featureInfoTemp = null;
		TargetSystem targetSystemTemp = null;
		Iterator<FeatureInfo> iteratorForFeatureInfo = null;
		Iterator<TargetSystem> iteratorForTargetSystem = null;
		final List<StoredLicense> listSLWithSignature = new ArrayList<StoredLicense>();
		;
		List<FeatureInfo> listFIWithSignature = new ArrayList<FeatureInfo>();
		;
		List<TargetSystem> listTSWithSignature = new ArrayList<TargetSystem>();
		;
		String str = null;

		storedLicenseTemp = storedLicense;
		str = StoredLicenseUtil.getAppendedString(storedLicenseTemp);
		LicenseLogger.getInstance().finest(this.getClass().getName(), "install", "Appended all the data into one string.");

		final Iterator<byte[]> iteratorLicenseKey = LicenseEncrypt.encryptData(str).iterator();
		final byte[] storedLicenseSignature = iteratorLicenseKey.next();
		final byte[] encryptKey = iteratorLicenseKey.next();

		storedLicenseTemp.setStoredLicenseSignature(storedLicenseSignature);
		LicenseLogger.getInstance().finest(this.getClass().getName(), "install", "Appended data is encrypted.");
		storedLicenseTemp.setKey(encryptKey);
		iteratorForFeatureInfo = storedLicenseTemp.getFeatureInfoList().iterator();
		listFIWithSignature = new ArrayList<FeatureInfo>();

		while (iteratorForFeatureInfo.hasNext()) {
			featureInfoTemp = new FeatureInfo();
			featureInfoTemp = iteratorForFeatureInfo.next();
			featureInfoTemp.setModifiedTime(storedLicenseTemp.getLicenseFileImportTime());
			str = storedLicenseTemp.getSerialNbr() + featureInfoTemp.getFeatureCode() + featureInfoTemp.getFeatureName()
					+ featureInfoTemp.getModifiedTime();

			final byte[] featureInfoSignature = LicenseEncrypt.encryptData(str, encryptKey);

			featureInfoTemp.setFeatureInfoSignature(featureInfoSignature);
			listFIWithSignature.add(featureInfoTemp);
		}

		storedLicenseTemp.setFeatureInfoList(listFIWithSignature);

		if (!storedLicenseTemp.getTargetIds().isEmpty()) {
			iteratorForTargetSystem = storedLicenseTemp.getTargetIds().iterator();
			listTSWithSignature = new ArrayList<TargetSystem>();

			while (iteratorForTargetSystem.hasNext()) {
				targetSystemTemp = new TargetSystem();
				targetSystemTemp = iteratorForTargetSystem.next();
				targetSystemTemp.setModifiedTime(storedLicenseTemp.getLicenseFileImportTime());
				str = storedLicenseTemp.getSerialNbr() + targetSystemTemp.getTargetId()
						+ targetSystemTemp.getModifiedTime();

				final byte[] targetSystemSignature = LicenseEncrypt.encryptData(str, encryptKey);

				targetSystemTemp.setTargetSystemSignature(targetSystemSignature);
				listTSWithSignature.add(targetSystemTemp);
			}

			storedLicenseTemp.setTargetIds(listTSWithSignature);
		}

		listSLWithSignature.add(storedLicenseTemp);
		return listSLWithSignature;
	}

}
