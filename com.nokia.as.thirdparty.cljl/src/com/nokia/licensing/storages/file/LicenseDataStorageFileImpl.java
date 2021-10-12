// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.storages.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.CLJLPreferences;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.plugins.PluginRegistry;
import com.nokia.licensing.utils.Constants;


public class LicenseDataStorageFileImpl implements LicenseDataStorage {

	CLJLPreferences cljlPreferences;
	Preferences prefSystemRoot;

	public LicenseDataStorageFileImpl() throws LicenseException {
		this.cljlPreferences = PluginRegistry.getRegistry().getPlugin(CLJLPreferences.class);
		this.prefSystemRoot = this.cljlPreferences.getPreferencesSystemRoot();
	}

	public LicenseDataStorageFileImpl(final CLJLPreferences cljlPreferences) throws LicenseException {
		this.cljlPreferences = cljlPreferences;
		this.prefSystemRoot = cljlPreferences.getPreferencesSystemRoot();
	}

	private List<String> getLicenseFileNameFrmFeatureCode(final long featureCode) {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicenseFileNameFrmFeatureCode",
				"Index file search for filenames for given Feature Code");
		final String xpath = "//*[@FeatureCode='" + featureCode + "']";
		final List<String> fileNames = getFileNames(xpath);
		return fileNames;
	}

	private List<String> getLicenseFileNameFrmCustomerID(final String customerID) {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicenseFileNameFrmCustomerID",
				"Index file search for filenames for given Customer ID");
		final String xpath = "//*[customerID='" + customerID + "']";
		final List<String> fileNames = getFileNames(xpath);
		return fileNames;
	}

	private List<String> getLicenseFileNameFrmSerialNo(final String serialNbr) {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicenseFileNameFrmSerialNo",
				"Index file search for filenames for given serial number");
		final String xpath = "//*[serialNumber='" + serialNbr + "']";
		final List<String> fileNames = getFileNames(xpath);
		return fileNames;
	}

	private List<String> getLicenseFileNameFrmSerialNoAndFeatureCode(final String serialNbr, final long featureCode) {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicenseFileNameFrmSerialNoAndFeatureCode",
				"Index file search for filenames for given serial number and feature code");
		final String xpath = "//*[@FeatureCode='" + featureCode + "' and serialNumber='" + serialNbr + "']";
		final List<String> fileNames = getFileNames(xpath);
		return fileNames;
	}

	@Override
	public boolean deleteLicenseBySerialNumber(final String serialNbr) throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "deleteLicenseBySerialNumber",
				"Moving the Active license with the given serial number to the Deleted directory");

		boolean status = false;
		List<String> fileNameList = null;
		final String deleteLicFolderLoc = this.prefSystemRoot.node("directory").get(Constants.DELETE_LICENSES, null);
		final String activeLicenseFolderLocation = this.prefSystemRoot.node("directory").get(Constants.ACTIVE_LICENSES,
				null);

		final File delLicenseFolder = new File(deleteLicFolderLoc);

		Iterator<String> iterator = null;
		String delete_FileName;

		if (!delLicenseFolder.exists()) {
			delLicenseFolder.mkdir();
		}

		fileNameList = getLicenseFileNameFrmSerialNo(serialNbr);
		iterator = fileNameList.iterator();

		while (iterator.hasNext()) {

			delete_FileName = iterator.next();

			// File (or directory) to be moved
			final File file = new File(activeLicenseFolderLocation + delete_FileName);

			// Destination directory
			final File dir = new File(deleteLicFolderLoc);

			// Move file to new directory
			status = file.renameTo(new File(dir, file.getName()));
			if (!status) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseBySerialNumber",
						"Delete License File was not sucessfully moved to the Deleted directory");
			} else {
				LicenseLogger.getInstance().finest(this.getClass().getName(), "deleteLicenseBySerialNumber",
						"Delete License File is sucessfully moved to the Deleted directory");

			}

		}
		return status;
	}

	@Override
	public boolean deleteLicenseBySerialNumberAndFeatureCode(final String serialNbr, final long featureCode)
			throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "deleteLicenseBySerialNumberAndFeatureCode",
				"Moving the Active license with the given serial number and feature code to the Deleted directory");

		boolean status = false;
		List<String> fileNameList = null;
		final String deleteLicFolderLoc = this.prefSystemRoot.node("directory").get(Constants.DELETE_LICENSES, null);
		final String activeLicenseFolderLocation = this.prefSystemRoot.node("directory").get(Constants.ACTIVE_LICENSES,
				null);

		final File delLicenseFolder = new File(deleteLicFolderLoc);

		Iterator<String> iterator = null;
		String delete_FileName;

		if (!delLicenseFolder.exists()) {
			delLicenseFolder.mkdir();
		}

		fileNameList = getLicenseFileNameFrmSerialNoAndFeatureCode(serialNbr, featureCode);
		iterator = fileNameList.iterator();

		while (iterator.hasNext()) {

			delete_FileName = iterator.next();

			// File (or directory) to be moved
			final File file = new File(activeLicenseFolderLocation + delete_FileName);

			// Destination directory
			final File dir = new File(deleteLicFolderLoc);

			// Move file to new directory
			status = file.renameTo(new File(dir, file.getName()));
			if (!status) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseBySerialNumber",
						"Delete License File was not sucessfully moved to the Deleted directory");
			} else {
				LicenseLogger.getInstance().finest(this.getClass().getName(), "deleteLicenseBySerialNumber",
						"Delete License File is sucessfully moved to the Deleted directory");

			}

		}
		return status;

	}

	@Override
	public StoredLicense getLicenseBySerialNo(final String serialNbr, final boolean checkDataIntegrity)
			throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicenseBySerialNo",
				"Getting the stored license object for the given serial number");
		List<String> fileNameList = null;

		StoredLicense storedLicObj = null;
		String activeLicenseFolderLocation;
		String fileName;
		String xpath;
		String status;

		FileInputStream licenseFileInputStream = null;
		ObjectInputStream licenseObjectInputStream = null;
		Iterator<String> iterator = null;

		try {
			fileNameList = getLicenseFileNameFrmSerialNo(serialNbr);
			if (!fileNameList.isEmpty()) {
				iterator = fileNameList.iterator();
				fileName = iterator.next();
				xpath = "//*[fileName='" + fileName + "']";
				status = getStatus(xpath);

				activeLicenseFolderLocation = this.prefSystemRoot.node("directory").get(Constants.ACTIVE_LICENSES,
						null);

				// if(new File(activeLicenseFolderLocation+"\\"+fileName).exists()){
				licenseFileInputStream = new FileInputStream(activeLicenseFolderLocation + fileName);
				licenseObjectInputStream = new ObjectInputStream(licenseFileInputStream);

				LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicenseBySerialNo",
						"Reading the Stored License for the given serial number");

				storedLicObj = (StoredLicense) licenseObjectInputStream.readObject();
				licenseObjectInputStream.close();
				licenseFileInputStream.close();

			} else {
				return null;
			}

		} catch (final IOException ioExpObj) {

			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicenseBySerialNo",
					"An IO Exception has been thrown" + ioExpObj.getMessage());

		} catch (final ClassNotFoundException cnfExpObj) {

			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicenseBySerialNo",
					"Class not found exception has been thrown" + cnfExpObj.getMessage());

		}
		return storedLicObj;

	}

	@Override
	public List<StoredLicense> getLicenseByFileName(final String licenseFileName, final boolean checkDataIntegrity)
			throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicenseByFileName",
				"Getting the list of stored license objects for the given File name");
		List<StoredLicense> licenseObjects = null;
		String activeLicenseFolderLocation;
		StoredLicense storedLicObj = null;
		licenseObjects = new ArrayList<StoredLicense>();

		String xpath;
		String status;

		FileInputStream licenseFileInputStream = null;
		ObjectInputStream licenseObjectInputStream = null;
		try {
			xpath = "//*[fileName='" + licenseFileName + "']";
			status = getStatus(xpath);

			activeLicenseFolderLocation = this.prefSystemRoot.node("directory").get(Constants.ACTIVE_LICENSES, null);

			licenseFileInputStream = new FileInputStream(activeLicenseFolderLocation + licenseFileName);
			licenseObjectInputStream = new ObjectInputStream(licenseFileInputStream);

			LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicenseByFileName",
					"Reading the Stored License for the given File Name");

			storedLicObj = (StoredLicense) licenseObjectInputStream.readObject();
			licenseObjects.add(storedLicObj);
			licenseObjectInputStream.close();
			licenseFileInputStream.close();

		} catch (final IOException ioExpObj) {

			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicenseByFileName",
					"Class not found exception has been thrown" + ioExpObj.getMessage());

		} catch (final ClassNotFoundException cnfExpObj) {

			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicenseByFileName",
					"Class not found exception has been thrown" + cnfExpObj.getMessage());
		}
		return licenseObjects;

	}

	/**
	 * Fetches a list of Licenses for a given License Code The List<StoredLicense> object which is returned contains a
	 * list of License objects. The second argument tells whether data integrity checks against data storage needs to be
	 * performed or not.
	 *
	 * @param licenseCode
	 *            - License Code
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses associated with the Code
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForLicenseCode(final String licenseCode, final boolean checkDataIntegrity)
			throws LicenseException {

		return null;
	}

	/**
	 * Fetches a list of Licenses for a given License Name The List<StoredLicense> object which is returned contains a
	 * list of License objects. The second argument tells whether data integrity checks against data storage needs to be
	 * performed or not.
	 *
	 * @param licenseName
	 *            -- License Name
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses associated with the Name
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForName(final String licenseName, final boolean checkDataIntegrity)
			throws LicenseException {
		return null;

	}

	/**
	 * Fetches a list of Licenses for a given Order ID The List<StoredLicense> object which is returned contains a list
	 * of License objects. The second argument tells whether data integrity checks against data storage needs to be
	 * performed or not.
	 *
	 * @param orderID
	 *            -- Order Id
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> List of Licenses associated with the Order Id
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForOrderID(final String orderID, final boolean checkDataIntegrity)
			throws LicenseException {
		return null;

	}

	/**
	 * Fetches a list of Licenses for a given Customer ID The List<StoredLicense> object which is returned contains a
	 * list of License objects The second argument tells whether data integrity checks against data storage needs to be
	 * performed or not.
	 *
	 * @param customerID
	 *            -- Customer ID of the License
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses associated with the Customer
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForCustomerID(final String customerID, final boolean checkDataIntegrity)
			throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForCustomerID",
				"Getting the list of stored license objects for the given Customer ID");
		List<String> fileNameList = null;
		String fileName;
		String xpath;
		String status;
		List<StoredLicense> licenseObjects = null;
		String activeLicenseFolderLocation = null;
		licenseObjects = new ArrayList<StoredLicense>();
		Iterator<String> iterator = null;
		FileInputStream licenseFileInputStream = null;
		ObjectInputStream licenseObjectInputStream = null;

		try {
			fileNameList = getLicenseFileNameFrmCustomerID(customerID);
			iterator = fileNameList.iterator();

			while (iterator.hasNext()) {

				fileName = iterator.next();

				xpath = "//*[fileName='" + fileName + "']";
				status = getStatus(xpath);

				activeLicenseFolderLocation = this.prefSystemRoot.node("directory").get(Constants.ACTIVE_LICENSES,
						null);

				// activeLicenseFolderLocation =prefSystemRoot.node("directory").get(Constants.ACTIVE_LICENSES, null);
				licenseFileInputStream = new FileInputStream(activeLicenseFolderLocation + fileName);
				licenseObjectInputStream = new ObjectInputStream(licenseFileInputStream);

				LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForCustomerID",
						"Reading the list of Stored License Objects for the given Customer ID");

				final StoredLicense storedLicObj = (StoredLicense) licenseObjectInputStream.readObject();
				licenseObjects.add(storedLicObj);
				licenseObjectInputStream.close();
				licenseFileInputStream.close();
			}

		} catch (final IOException ioExpObj) {

			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicensesForCustomerID",
					"IO exception has been thrown" + ioExpObj.getMessage());

		} catch (final ClassNotFoundException cnfExpObj) {

			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicensesForCustomerID",
					"Class not found exception has been thrown" + cnfExpObj.getMessage());
		}
		return licenseObjects;
	}

	/**
	 * Fetches a list of Licenses for a given Customer ID The List<StoredLicense> object which is returned contains a
	 * list of License objects. The second argument tells whether data integrity checks against data storage needs to be
	 * performed or not.
	 *
	 * @param customerName
	 *            -- Name of the Customer
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses associated with the Customer
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForCustomerName(final String customerName, final boolean checkDataIntegrity)
			throws LicenseException {
		return null;

	}

	/**
	 * Fetches a list of Licenses for a given Software Release base The List<StoredLicense> object which is returned
	 * contains a list of License objects. The second argument tells whether data integrity checks against data storage
	 * needs to be performed or not.
	 *
	 * @param swReleaseBase
	 *            -- Software Release Base
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses for a given Software Release Base
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForSWBaseRelease(final String swReleaseBase, final boolean checkDataIntegrity)
			throws LicenseException {
		return null;

	}

	/**
	 * Fetches a list of Licenses for a given Software Release Relation as argument. The List<StoredLicense> object
	 * which is returned contains a list of License objects which hold. The second argument tells whether data integrity
	 * checks against data storage needs to be performed or not.
	 *
	 * @param swReleaseRelation
	 *            -- Software Release Relation
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses for a given Software Release Relation
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForSWReleaseRelation(final String swReleaseRelation,
			final boolean checkDataIntegrity) throws LicenseException {
		return null;

	}

	/**
	 * Fetches a list of Licenses for a given Target Type as argument. The List<StoredLicense> object which is returned
	 * contains a list of License objects which hold. The second argument tells whether data integrity checks against
	 * data storage needs to be performed or not.
	 *
	 * @param targetType
	 *            -- Target Type on which License acts
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses for a given Target Type
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForTargetType(final String targetType, final boolean checkDataIntegrity)
			throws LicenseException {

		return null;
	}

	/**
	 * Fetches a list of Licenses for a given Target The List<StoredLicense> object which is returned contains a list of
	 * License objects. The second argument tells whether data integrity checks against data storage needs to be
	 * performed or not.
	 *
	 * @param targetID
	 *            -- Target ID of the Licensing system
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses for a given Target ID
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForTargetID(final String targetID, final boolean checkDataIntegrity)
			throws LicenseException {
		return null;

	}

	/**
	 * Fetches a list of Licenses for a given License State The List<StoredLicense> object which is returned contains a
	 * list of License objects. The second argument tells whether data integrity checks against data storage needs to be
	 * performed or not.
	 *
	 * @param licenseState
	 *            -- License State
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses for a given License State
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForState(final String licenseState, final boolean checkDataIntegrity)
			throws LicenseException {
		return null;

	}

	/**
	 * Fetches a list of Licenses for a given License Type The List<StoredLicense> object which is returned contains a
	 * list of License objects. The second argument tells whether data integrity checks against data storage needs to be
	 * performed or not.
	 *
	 * @param licenseType
	 *            -- License Type
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses for a given License Type
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForLicenseType(final int licenseType, final boolean checkDataIntegrity)
			throws LicenseException {
		return null;

	}

	/**
	 * Fetches a list of Licenses for a given Usage Type The List<StoredLicense> object which is returned contains a
	 * list of License objects. The second argument tells whether data integrity checks against data storage needs to be
	 * performed or not.
	 *
	 * @param usageType
	 *            -- Usage Type. Purpose of License File
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses for a given Usage Type
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForUsageType(final String usageType, final boolean checkDataIntegrity)
			throws LicenseException {
		return null;

	}

	/**
	 * Fetches a list of Licenses for a given Feature Name The List<License> object which is returned contains a list of
	 * License objects. The second argument tells whether data integrity checks against data storage needs to be
	 * performed or not.
	 *
	 * @param featureName
	 *            -- Feature Name
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses for a given Feature Name
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForFeatureName(final String featureName, final boolean checkDataIntegrity)
			throws LicenseException {

		return null;
	}

	/**
	 * Fetches a list of Licenses for a given Feature Code The List<StoredLicense> object which is returned contains a
	 * list of License objects. The second argument tells whether data integrity checks against data storage needs to be
	 * performed or not.
	 *
	 * @param featureCode
	 *            -- Feature Code associated with License
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses for a given Feature code
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesForFeatureCode(final long featureCode, final boolean checkDataIntegrity)
			throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForFeatureCode",
				"Getting the list of stored license objects for the given Feature Code");
		List<String> fileNameList = null;
		List<StoredLicense> licenseObjects = null;
		final Preferences prefSystemRoot = this.cljlPreferences.getPreferencesSystemRoot();
		String activeLicenseFolderLocation;

		String fileName;
		String xpath;
		String status;
		licenseObjects = new ArrayList<StoredLicense>();

		Iterator<String> iterator = null;
		FileInputStream licenseFileInputStream = null;
		ObjectInputStream licenseObjectInputStream = null;

		try {
			fileNameList = getLicenseFileNameFrmFeatureCode(featureCode);
			iterator = fileNameList.iterator();

			while (iterator.hasNext()) {

				fileName = iterator.next();

				xpath = "//*[fileName='" + fileName + "']";
				status = getStatus(xpath);

				activeLicenseFolderLocation = prefSystemRoot.node("directory").get(Constants.ACTIVE_LICENSES, null);

				licenseFileInputStream = new FileInputStream(activeLicenseFolderLocation + fileName);
				licenseObjectInputStream = new ObjectInputStream(licenseFileInputStream);

				LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForFeatureCode",
						"Reading the list of Stored License Objects for the given  Feature Code");

				final StoredLicense storedLicObj = (StoredLicense) licenseObjectInputStream.readObject();
				licenseObjects.add(storedLicObj);
				licenseObjectInputStream.close();
				licenseFileInputStream.close();
			}

		} catch (final IOException ioExpObj) {

			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicensesForFeatureCode",
					"Class not found exception has been thrown" + ioExpObj.getMessage());

		} catch (final ClassNotFoundException cnfExpObj) {

			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicensesForFeatureCode",
					"Class not found exception has been thrown" + cnfExpObj.getMessage());
		}
		return licenseObjects;
	}

	/**
	 * Fetches all the Licenses Available in the system. The List<StoredLicense> object which is returned contains a
	 * list of License objects which hold. The checkDataIntegrity argument tells whether data integrity checks against
	 * data storage needs to be performed or not.
	 *
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses available in the System
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getAllLicenses(final boolean checkDataIntegrity) throws LicenseException {

		return null;
	}

	/**
	 * Method gets list of Licenses that are currently valid on this system for the requested feature. The fourth
	 * argument tells whether data integrity checks against data storage needs to be performed or not.
	 *
	 * @param featureCode
	 *            -- Feature Code associated with License
	 * @param targetId
	 *            -- Target Id of the License File
	 * @param time
	 *            -- Time when the License file is valid
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List of list of licenses that are currently (currentTime) valid on this system (targetId) for the
	 *         requested feature (featureCode)
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicensesByFeatureCodeAndTargetIdAndTime(final long featureCode, final String targetId,
			final Date time, final boolean checkDataIntegrity) throws LicenseException {
		return null;
	}

	@Override
	public Map<Long, List<StoredLicense>> getLicensesByFeatureCodeAndTargetIdAndTime(final Set<Long> featureCodes,
			final String targetId, final java.util.Date time) throws LicenseException {
		return null;
	}

	/**
	 * This method checks each license from the time "startTime" till "endTime". It queries for License information
	 * between "startTime" till "endTime". The modifiedDate column in database for License table will be used for
	 * querying. This modifiedDate column will be updated whenever there is any changes to the License information. For
	 * all the Licenses retrieved it checks the following 1. Whether one of the featureCode of a license has been
	 * cancelled between "startTime" and "endTime" 2. Checks whether one of the featureCode of a license has been
	 * installed between "startTime" and "endTime". 3. Checks whether License has been activated between "startTime" and
	 * "endTime" 4. Checks whether License has expired between "startTime" and "endTime"
	 *
	 * @param startTime
	 *            -- Represents the start time.
	 * @param endTime
	 *            -- Represents the end time
	 * @return List<StoredLicense> -- List of StoredLicense objects
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicenseChanges(final Date startTime, final Date endTime) throws LicenseException {
		return null;

	}

	/**
	 * This method fetches all licenses which will expire between startTime and endTime.
	 *
	 * @param startTime
	 *            -- Represents the start time.
	 * @param endTime
	 *            -- Represents the end time.
	 * @return -- Returns a list of StoredLicense objects.
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getExpiredLicenses(final Date startTime, final Date endTime) throws LicenseException {
		return null;

	}

	@Override
	public List<StoredLicense> getModifiedLicenses(final Date startTime, final Date endTime) throws LicenseException {

		return null;
	}

	@Override
	public synchronized void insertLicenseInformation(final List<StoredLicense> licenseFileList)
			throws LicenseException {

		LicenseLogger.getInstance().finest(this.getClass().getName(), "insertLicenseInformation",
				"Inserting the license file in Active Folder");
		final Preferences prefSystemRoot = this.cljlPreferences.getPreferencesSystemRoot();
		final String indexFileAbsolutePath = prefSystemRoot.node("directory").get(Constants.INDEX_FILE_PATH, null);
		final File indexFile = new File(indexFileAbsolutePath);
		StoredLicense storedLicenseObj = null;
		Iterator<StoredLicense> iterator = null;
		iterator = licenseFileList.iterator();

		final IndexFileHandler fileCreator = new IndexFileHandler(indexFileAbsolutePath);
		if (!indexFile.exists()) {
			fileCreator.loadData(licenseFileList);
			fileCreator.createFile();
		} else {
			fileCreator.addElementsInFile(licenseFileList);
		}

		while (iterator.hasNext()) {

			storedLicenseObj = iterator.next();
			final String fileName = storedLicenseObj.getLicenseFileName();
			final String successLicenseFolderLocation = prefSystemRoot.node("directory").get(Constants.ACTIVE_LICENSES,
					null);
			final File successLicenseFolder = new File(successLicenseFolderLocation);
			if (!successLicenseFolder.exists()) {
				successLicenseFolder.mkdir();
			}
			FileOutputStream fileOutputStream;
			try {
				fileOutputStream = new FileOutputStream(successLicenseFolderLocation + fileName);
				final ObjectOutputStream objOutputStream = new ObjectOutputStream(fileOutputStream);
				LicenseLogger.getInstance().finest(this.getClass().getName(), "insertLicenseInformation",
						"Writing the license file in Active Folder");
				objOutputStream.writeObject(storedLicenseObj);
				objOutputStream.close();
				fileOutputStream.close();

			} catch (final FileNotFoundException fileNFEExp) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "insertLicenseInformation",
						"File not found" + fileNFEExp.getMessage());
			} catch (final IOException ioExp) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "insertLicenseInformation",
						"Class not found exception has been thrown" + ioExp.getMessage());
			}
		}
	}

	private List<String> getFileNames(final String xpath) {
		List<String> filenames = null;
		String fileName;
		try {
			filenames = new ArrayList<String>();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();

			final Preferences prefSystemRoot = this.cljlPreferences.getPreferencesSystemRoot();
			final String indexFileLocation = prefSystemRoot.node("directory").get(Constants.INDEX_FILE_PATH, null);
			final File indexFile = new File(indexFileLocation);
			if (indexFile.exists()) {
				final Document dom = db.parse(indexFile);

				final XPath theXPath = XPathFactory.newInstance().newXPath();
				final NodeList nodelist = (NodeList) theXPath.evaluate(xpath, dom, XPathConstants.NODESET);

				for (int i = 0; i < nodelist.getLength(); i++) {
					// Get element
					final Element elem = (Element) nodelist.item(i);

					fileName = elem.getElementsByTagName("fileName").item(0).getFirstChild().getNodeValue();
					filenames.add(fileName);
				}
			}
		} catch (final ParserConfigurationException parserConExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getFileNames",
					"Failed to configure the parser" + parserConExpObj.getMessage());
		} catch (final SAXException saxExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getFileNames",
					"Failed to parse the file" + saxExpObj.getMessage());
		} catch (final IOException ioExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getFileNames",
					"An I/O exception has been thrown" + ioExpObj.getMessage());
		} catch (final XPathExpressionException transExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getFileNames",
					"Transformer exception has been thrown" + transExpObj.getMessage());
		} catch (final LicenseException licExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getFileNames",
					"LicenseException has been thrown" + licExpObj.getMessage());
		}
		return filenames;
	}

	private String getStatus(final String xpath) {
		String status = null;
		try {

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();

			final Preferences prefSystemRoot = this.cljlPreferences.getPreferencesSystemRoot();
			final String indexFileLocation = prefSystemRoot.node("directory").get(Constants.INDEX_FILE_PATH, null);
			// String indexFileName="LicenseIndexFile.xml";
			final File indexFile = new File(indexFileLocation);
			if (indexFile.exists()) {
				final Document dom = db.parse(indexFile);

				final XPath theXPath = XPathFactory.newInstance().newXPath();
				final NodeList nodelist = (NodeList) theXPath.evaluate(xpath, dom, XPathConstants.NODESET);
				// Get element
				final Element elem = (Element) nodelist.item(0);
				status = elem.getElementsByTagName("status").item(0).getFirstChild().getNodeValue();

			}
		} catch (final ParserConfigurationException parserConExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getStatus",
					"Failed to configure the parser" + parserConExpObj.getMessage());
		} catch (final SAXException saxExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getStatus",
					"Failed to parse the file" + saxExpObj.getMessage());
		} catch (final IOException ioExpObj) {
			ioExpObj.printStackTrace();
		} catch (final XPathExpressionException transExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getStatus",
					"Transformer exception has been thrown" + transExpObj.getMessage());
		} catch (final LicenseException licExpObj) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getStatus",
					"LicenseException has been thrown" + licExpObj.getMessage());
		}
		return status;
	}
}
