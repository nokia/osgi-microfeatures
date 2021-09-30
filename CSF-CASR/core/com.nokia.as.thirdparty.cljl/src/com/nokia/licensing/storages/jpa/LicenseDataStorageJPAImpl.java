/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.storages.jpa;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import com.nokia.licensing.dao.FeatureInfoJPA;
import com.nokia.licensing.dao.StoredLicenseJPA;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.querycache.DataBaseUtil;
import com.nokia.licensing.querycache.QueryRetrieval;
import com.nokia.licensing.utils.LicenseConstants;


/**
 * This class is the JPA implementation for LicenseDataStorage interface.
 *
 * @author Rama Manohar P
 * @version 1.0
 *
 */
public class LicenseDataStorageJPAImpl extends JPABaseStorage implements LicenseDataStorage {

	/**
	 * This method is for making the connection to the Data Base and then creating the query and executing the query to
	 * get the data from Data Base.
	 *
	 * @param queryID
	 *            -- Contains the query id
	 * @param data
	 *            -- Data based on the value get the License data from Data Base
	 * @param data2
	 *            -- Feature Code associated with License or null
	 * @param data3
	 *            -- Target Id of the License File or nulls
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses associated with the Data
	 * @throws LicenseException
	 */
	protected List<StoredLicense> iterateResultSet(final String queryID, final Object data, final long data2,
			final String data3, final boolean checkDataIntegrity) throws LicenseException {
		StoredLicenseJPA storedLicenseJPA = null;
		StoredLicense storedLicense = null;
		String strquery = null;
		Query query = null;
		List<StoredLicenseJPA> listForStoredLicenseJPA = null;
		Iterator<StoredLicenseJPA> iteratorForStoredLicenseJPA = null;
		List<StoredLicense> listForStoredLicense = null;
		EntityManager entityManager = null;
		String featureName = null;
		long featureCode = 0l;
		String targetId = null;
		byte[] key = null;

		LicenseLogger.getInstance().finest(this.getClass().getName(), "iterateResultSet",
				"Getting the connection,making the query and executing the query...");
		try {
			entityManager = getConnection();
			strquery = QueryRetrieval.getSQLData(queryID);

			if (null != entityManager) {
				entityManager.getTransaction().begin();
				query = entityManager.createNativeQuery(strquery, StoredLicenseJPA.class);

				// Based on the values passed and queryId it will gets corresponding query and
				// sets the values to the query and execute the query to get the license data
				// based on the values passed on the method calling.
				if (data instanceof String) {
					query.setParameter(1, data);
				} else if (data instanceof Integer) {
					query.setParameter(1, data);
				} else if (data instanceof Long) {
					query.setParameter(1, data);
				} else if (data instanceof java.util.Date) {
					final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					final String dateFormat = sdf.format(data);
					query.setParameter(1, dateFormat);
					query.setParameter(2, dateFormat);
					query.setParameter(3, data2);
					if (data3 != null) {
						query.setParameter(4, data3);
					}
				}
				listForStoredLicenseJPA = query.getResultList();
				LicenseLogger.getInstance().finest(this.getClass().getName(), "iterateResultSet",
						"Retrieving Data from Data Base is completed.");

				LicenseLogger.getInstance().finest(this.getClass().getName(), "iterateResultSet",
						"Setting data to List<StoredLicense> from result list(StoredLicenseJPA)...");

				listForStoredLicense = new ArrayList<StoredLicense>();
				iteratorForStoredLicenseJPA = listForStoredLicenseJPA.iterator();
				while (iteratorForStoredLicenseJPA.hasNext()) {
					storedLicenseJPA = iteratorForStoredLicenseJPA.next();
					storedLicense = new StoredLicense();
					storedLicense = DataBaseUtil.convertStoredLicenseJPAToStoredLicense(storedLicenseJPA);
					if (queryID.equals("selectLicensesByFeatureName") || queryID.equals("selectLicenseByFeatureCode")
							|| queryID.equals("selectLicensesByTargetID")
							|| queryID.equals("selectLicensesByFeatureCodeAndTargetIdAndTime")) {
						if (data instanceof String) {
							if (queryID.equals("selectLicensesByFeatureName")) {
								featureName = (String) data;
							} else if (queryID.equals("selectLicensesByTargetID")) {
								targetId = (String) data;
							}
						} else if (data instanceof Long) {
							featureCode = (Long) data;
						} else if (data instanceof java.util.Date) {
							featureCode = data2;
							targetId = data3;
						}
					}
					storedLicense = StoredLicenseUtil.populateCompletStoredLicense(storedLicense, entityManager,
							featureName, featureCode, targetId);
					try {
						key = LicenseGenericDataAccessJPA.getEncryptKey(entityManager, storedLicense.getSerialNbr());
						LicenseLogger.getInstance().finest(this.getClass().getName(), "iterateResultSet",
								"Getting License Key data from the Data Base is completed..");
					} catch (final Exception e) {
						LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
								"Getting License Key data from the Data Base is failed." + e.getMessage());
						final LicenseException licenseException = new LicenseException(
								" Data retrieval is not successful from database.");
						licenseException.setErrorCode("CLJL111");
						LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
								"error code set to: " + licenseException.getErrorCode());
						throw licenseException;
					}
					storedLicense.setKey(key);
					listForStoredLicense.add(storedLicense);
				}
			}
		} catch (final Exception e) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
					"Retrieving Data from Data Base is failed." + e.getMessage());
			final LicenseException licenseException = new LicenseException(
					" Data retrieval is not successful from database.");
			licenseException.setErrorCode("CLJL111");
			LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		} finally {
			try {
				ConnectionUtilJPA.closeConnection(entityManager);
				LicenseLogger.getInstance().finest(this.getClass().getName(), "iterateResultSet", "Connection is closed.");
			} catch (final Exception e) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
						"Connection close is failed." + e.getMessage());
				final LicenseException licenseException = new LicenseException(
						" Unable to connect/disconnect to the database.");
				licenseException.setErrorCode("CLJL109");
				LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
						"error code set to: " + licenseException.getErrorCode());
				throw licenseException;
			}
		}
		return listForStoredLicense;
	}

	/**
	 * This method will get the corresponding License key for the License data.
	 *
	 * @param storedLicense
	 * @param entityManager
	 * @return StoredLicense -- License Data
	 * @throws LicenseException
	 */
	protected StoredLicense populateLicenseKey(final StoredLicense storedLicense, final EntityManager entityManager)
			throws LicenseException {
		byte[] key = null;
		try {
			key = LicenseGenericDataAccessJPA.getEncryptKey(entityManager, storedLicense.getSerialNbr());
			LicenseLogger.getInstance().finest(this.getClass().getName(), "iterateResultSet",
					"Getting License Key data from the Data Base is completed..");
		} catch (final Exception e) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
					"Getting License Key data from the Data Base is failed." + e.getMessage());
			final LicenseException licenseException = new LicenseException(
					" Data retrieval is not successful from database.");
			licenseException.setErrorCode("CLJL111");
			LicenseLogger.getInstance().error(this.getClass().getName(), "populateLicenseKey",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		}
		storedLicense.setKey(key);
		return storedLicense;
	}

	/**
	 * This method is for making the connection to the Data Base and then creating the query and executing the query to
	 * delete the data from the Data Base.
	 *
	 * @param queryID
	 *            -- Contains the query id
	 * @param serialNbr
	 *            -- Serial Number of the License
	 * @param featureCode
	 *            -- Feature Code associated with License
	 * @return boolean -- returns true if deletion is successful otherwise false
	 * @throws LicenseException
	 */
	private boolean deleteLicenseUtil(final String queryID, final String serialNbr, final long featureCode)
			throws LicenseException {
		boolean status = false;
		EntityManager entityManager = null;
		String strquery = null, featureListSql = null;
		Query query = null, featureList = null;
		int countDelSL = 0, countDelLK = 0;

		final String sourceMethod = "deleteLicenseUtil";
		LicenseLogger.getInstance().finest(this.getClass().getName(), sourceMethod, "License Data is deleting from the Data Base...");
		try {
			entityManager = getConnection();
			strquery = QueryRetrieval.getSQLData(queryID);

			entityManager.getTransaction().begin();
			query = entityManager.createNativeQuery(strquery);

			if (0l != featureCode) {
				List<FeatureInfoJPA> listForFeatureInfoJPA = null;
				featureListSql = QueryRetrieval.getSQLData(LicenseConstants.SELECTFEATUREINFOBYSERIALNUMBER);
				featureList = entityManager.createNativeQuery(featureListSql);

				featureList.setParameter(1, serialNbr);
				listForFeatureInfoJPA = new ArrayList<FeatureInfoJPA>();
				listForFeatureInfoJPA = featureList.getResultList();
				if (listForFeatureInfoJPA.size() == 1) {
					strquery = QueryRetrieval.getSQLData(LicenseConstants.DELETELICENSEBASEDONSERIALNUMBER);
					query = entityManager.createNativeQuery(strquery);
				} else {
					if (entityManager != null) {
						query = entityManager.createNativeQuery(strquery);
						query.setParameter(2, featureCode);
					}
				}
				countDelLK = 1;
			}

			query.setParameter(1, serialNbr);
			countDelSL = query.executeUpdate();
			LicenseLogger.getInstance().finest(this.getClass().getName(), sourceMethod,
					"License Data is deleted from the Data Base.");

			if (0l == featureCode) {
				strquery = QueryRetrieval.getSQLData(LicenseConstants.DELETELICENSEKEYBYSERALNUMBER);
				if (null != entityManager) {
					query = entityManager.createNativeQuery(strquery);
				}
				query.setParameter(1, serialNbr);
				countDelLK = query.executeUpdate();
				LicenseLogger.getInstance().finest(this.getClass().getName(), sourceMethod,
						"Data is deleted from LicenseKey Data Base based on serialnumber is completed.");
			}
			entityManager.getTransaction().commit();
		} catch (final Exception e) {
			LicenseLogger.getInstance().error(this.getClass().getName(), sourceMethod,
					"Deleting Data from Data Base based on serialnumber is failed.", e);
			final LicenseException licenseException = new LicenseException(
					" Deleting the data from database not successful.");
			licenseException.setErrorCode("CLJL113");
			LicenseLogger.getInstance().error(this.getClass().getName(), sourceMethod,
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		} finally {
			try {
				ConnectionUtilJPA.closeConnection(entityManager);
				LicenseLogger.getInstance().finest(this.getClass().getName(), sourceMethod, "Connection is closed.");
			} catch (final Exception e) {
				LicenseLogger.getInstance().error(this.getClass().getName(), sourceMethod,
						"Connection close is failed." + e.getMessage());
				final LicenseException licenseException = new LicenseException(
						" Unable to connect/disconnect to the database.");
				licenseException.setErrorCode("CLJL109");
				LicenseLogger.getInstance().error(this.getClass().getName(), sourceMethod,
						"error code set to: " + licenseException.getErrorCode());
				throw licenseException;
			}
		}
		if ((countDelSL == 0) || (countDelLK == 0)) {
			status = false;
		} else {
			status = true;
		}
		return status;
	}

	/**
	 * Inserts License information into a corresponding Data Storage. License Management Interface uses this method to
	 * insert data into the relevant Data Storage
	 *
	 * @param List<StoredLicense>
	 *            List containing License details
	 * @throws LicenseException
	 */
	@Override
	public void insertLicenseInformation(final List<StoredLicense> licenseFileList) throws LicenseException {
		EntityManager entityManager = null;
		EntityTransaction entityTransaction = null;
		StoredLicense storedLicenseInsert = null;
		StoredLicenseJPA storedLicenseJPAInsert = null;
		Iterator<StoredLicense> iteratorForStoredLicense = null;

		LicenseLogger.getInstance().finest(this.getClass().getName(), "insertLicenseInformation", "Inserting Data into Data Base...");

		entityManager = getConnection();

		iteratorForStoredLicense = licenseFileList.iterator();
		while (iteratorForStoredLicense.hasNext()) {
			try {

				entityTransaction = entityManager.getTransaction();
				entityTransaction.begin();

				storedLicenseInsert = iteratorForStoredLicense.next();
				storedLicenseJPAInsert = DataBaseUtil.convertStoredLicenseToStoredLicenseJPA(storedLicenseInsert);

				entityManager.persist(storedLicenseJPAInsert);
				entityManager.flush();
				LicenseGenericDataAccessJPA.setEncryptKey(entityManager, storedLicenseInsert.getSerialNbr(),
						storedLicenseInsert.getKey());
				LicenseLogger.getInstance().finest(LicenseGenericDataAccessJPA.class.getName(), "insertLicenseInformation",
						"Setting License data to entitymanager is completed..");
				entityTransaction.commit();
				LicenseLogger.getInstance().finest(this.getClass().getName(), "insertLicenseInformation",
						"Data inserting into Data Base is completed.");
			} catch (final Exception e) {
				// Implement the audit logger to display the license which are not installed.
				LicenseLogger.getInstance().error(this.getClass().getName(), "insertLicenseInformation", "Data insertion is failed.");

				if ((entityTransaction != null) && entityTransaction.isActive()) {
					entityTransaction.rollback();
				}
				final LicenseException licenseException = new LicenseException(
						" Data insertion into the database is failed. : " + e.getMessage());
				licenseException.setErrorCode("CLJL112");
				LicenseLogger.getInstance().error(this.getClass().getName(), "insertLicenseInformation",
						"error code set to: " + licenseException.getErrorCode() + " : " + e.getMessage());
				throw licenseException;
			}
		}
		try {
			ConnectionUtilJPA.closeConnection(entityManager);
			LicenseLogger.getInstance().finest(this.getClass().getName(), "insertLicenseInformation", "Connection is closed.");
		} catch (final Exception e) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "insertLicenseInformation",
					"Connection close is failed." + e.getMessage());
			final LicenseException licenseException = new LicenseException(
					" Unable to connect/disconnect to the database.");
			licenseException.setErrorCode("CLJL109");
			LicenseLogger.getInstance().error(this.getClass().getName(), "insertLicenseInformation",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		}

	}

	/**
	 * Deletes the License data from the underlying repository based on the Serial Number. Serial Number being the
	 * Primary Key of the License will delete only one entry from the Repository.
	 *
	 * @param license
	 *            -- Serial Number of the License
	 * @return boolean -- returns true if deletion is successful otherwise false
	 * @throws LicenseException
	 */
	@Override
	public boolean deleteLicenseBySerialNumber(final String serialNbr) throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "deleteLicenseBySerialNumber",
				"Deleting Data from Data Base based on serialnumber...");

		final boolean status = deleteLicenseUtil(LicenseConstants.DELETELICENSEBASEDONSERIALNUMBER, serialNbr, 0l);

		if (status == true) {
			LicenseLogger.getInstance().finest(this.getClass().getName(), "deleteLicenseBySerialNumber",
					"Data is deleted from LicenseKey Data Base based on serialnumber is completed.");
		} else {
			LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseBySerialNumber",
					"Deleting Data from Data Base based on serialnumber is failed.");
			final LicenseException licenseException = new LicenseException(
					" Deleting the data from database not successful");
			licenseException.setErrorCode("CLJL113");
			LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseBySerialNumber",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		}
		return status;
	}

	/**
	 * Deletes the License data from the Feature Info table based on the Serial Number and Feature Code.For the
	 * corresponding Serial Number and Feature Code there will be only one entry in Feature Info, so this method will
	 * delete only one entry from the Repository.
	 *
	 * @param serialNbr
	 *            -- Serial Number of the License
	 * @param featureCode
	 *            -- Feature Code associated with License
	 * @return boolean -- returns true if deletion is successful otherwise false
	 * @throws LicenseException
	 */
	@Override
	public boolean deleteLicenseBySerialNumberAndFeatureCode(final String serialNbr, final long featureCode)
			throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "deleteLicenseBySerialNumberAndFeatureCode",
				"Deleting Data from Data Base based on serialnumber and featurecode...");

		final boolean status = deleteLicenseUtil(LicenseConstants.DELETELICENSEBASEDONSERIALNUMBERFEATURECODE,
				serialNbr, featureCode);

		if (status == true) {
			LicenseLogger.getInstance().finest(this.getClass().getName(), "deleteLicenseBySerialNumber",
					"Data is deleted from LicenseKey Data Base based on serialnumber is completed.");
		} else {
			LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseBySerialNumber",
					"Deleting Data from Data Base based on serialnumber is failed.");
		}

		return status;
	}

	/**
	 * Accesses License Information based on the Serial Number Returns the License Information wrapped in License
	 * object. The second argument tells whether data integrity checks against data storage needs to be performed or
	 * not.
	 *
	 * @param serialNbr
	 *            - License Serial Number
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return StoredLicense - License Information
	 * @throws LicenseException
	 */
	@Override
	public StoredLicense getLicenseBySerialNo(final String serialNbr, final boolean checkDataIntegrity)
			throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicenseBySerialNo",
				"Retrieving Data from Data Base based on serialnumber...");

		StoredLicense storedLicense = null;
		try {
			final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSEBYSERIALNUMBER,
					serialNbr, 0l, null, checkDataIntegrity);
			if (licenseList.size() > 0) {
				storedLicense = licenseList.get(0);
			}

			LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicenseBySerialNo",
					"Retrieving Data from Data Base based on serilanumber is completed.");
		} catch (final LicenseException sqle) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicenseBySerialNo",
					"Retrieving Data from Data Base based on serialnumber is failed." + sqle.getMessage());
			final LicenseException licenseException = new LicenseException(
					" Data retrieval is not successful from database.");
			licenseException.setErrorCode("CLJL111");
			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicenseBySerialNo",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		}
		return storedLicense;
	}

	/**
	 * Accesses License Information based on the License File Name Returns the License Information wrapped in License
	 * object. The second argument tells whether data integrity checks against data storage needs to be performed or
	 * not.
	 *
	 * @param licenseFileName
	 *            - Name of the License File
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return List<StoredLicense> -- List of Licenses associated with the File Name
	 * @throws LicenseException
	 */
	@Override
	public List<StoredLicense> getLicenseByFileName(final String licenseFileName, final boolean checkDataIntegrity)
			throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicenseByFileName",
				"Retrieving Data from Data Base based on licensefilename...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYFILENAME,
				licenseFileName, 0l, null, checkDataIntegrity);

		return licenseList;
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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForLicenseCode",
				"Retrieving Data from Data Base based on licensecode...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYLICENSECODE,
				licenseCode, 0l, null, checkDataIntegrity);

		return licenseList;
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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForName",
				"Retrieving Data from Data Base based on licensename...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYNAME, licenseName, 0l,
				null, checkDataIntegrity);

		return licenseList;
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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForOrderID",
				"Retrieving Data from Data Base based on orderid...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYORDERID, orderID, 0l,
				null, checkDataIntegrity);

		return licenseList;
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
				"Retrieving Data from Data Base based on customerid...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYCUSTOMERID,
				customerID, 0l, null, checkDataIntegrity);

		return licenseList;
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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForCustomerName",
				"Retrieving Data from Data Base based on customername...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYCUSTOMERNAME,
				customerName, 0l, null, checkDataIntegrity);

		return licenseList;
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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForesultSetWBaseRelease",
				"Retrieving Data from Data Base based on swreleasebase...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYSWBASERELEASE,
				swReleaseBase, 0l, null, checkDataIntegrity);

		return licenseList;
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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForesultSetWReleaseRelation",
				"Retrieving Data from Data Base based on swreleaserelation...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYSWRELEASERELATION,
				swReleaseRelation, 0l, null, checkDataIntegrity);

		return licenseList;
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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForTargetType",
				"Retrieving Data from Data Base based on targettype...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYTARGETTYPE,
				targetType, 0l, null, checkDataIntegrity);

		return licenseList;
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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForTargetID",
				"Retrieving Data from Data Base based on targetid...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYTARGETID, targetID,
				0l, null, checkDataIntegrity);

		return licenseList;
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
		throw new UnsupportedOperationException();
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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForLicenseType",
				"Retrieving Data from Data Base based on licensetype...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYLICENSETYPE,
				licenseType, 0l, null, checkDataIntegrity);

		return licenseList;
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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForUsageType",
				"Retrieving Data from Data Base based on usagetype...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYUSAGETYPE, usageType,
				0l, null, checkDataIntegrity);

		return licenseList;
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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getLicensesForFeatureName",
				"Retrieving Data from Data Base based on featurename...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSESBYFEATURENAME,
				featureName, 0l, null, checkDataIntegrity);

		return licenseList;
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
				"Retrieving Data from Data Base based on featurecode...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSEBYFEATURECODE,
				featureCode, 0l, null, checkDataIntegrity);

		return licenseList;
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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getAllLicenses", "Retrieving all Data from Data Base...");

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTALLLICENSES, null, 0l, null,
				checkDataIntegrity);

		return licenseList;
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
			final java.util.Date time, final boolean checkDataIntegrity) throws LicenseException {
		LicenseLogger.getInstance().fine(this.getClass().getName(), "getLicensesByFeatureCodeAndTargetIdAndTime",
				"Retrieving Data from Data Base based on FEATURECODE,TARGETID and DATE...");
		String query;
		if (targetId == null) {
			query = LicenseConstants.SELECTLICENSESBYFEATURECODEANDTARGETIDANDTIME_NO_TARGETID;
		} else {
			query = LicenseConstants.SELECTLICENSESBYFEATURECODEANDTARGETIDANDTIME;
		}
		final List<StoredLicense> licenseList = iterateResultSet(query, time, featureCode, targetId,
				checkDataIntegrity);

		return licenseList;
	}

	@Override
	public Map<Long, List<StoredLicense>> getLicensesByFeatureCodeAndTargetIdAndTime(final Set<Long> featureCodes,
			final String targetId, final java.util.Date time) throws LicenseException {
		final Map<Long, List<StoredLicense>> licenses = new LinkedHashMap<Long, List<StoredLicense>>();
		for (final Long fc : featureCodes) {
			licenses.put(fc, getLicensesByFeatureCodeAndTargetIdAndTime(fc, targetId, time, false));
		}
		return licenses;
	}

	@Override
	public List<StoredLicense> getLicenseChanges(final Date startTime, final Date endTime) throws LicenseException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<StoredLicense> getExpiredLicenses(final Date startTime, final Date endTime) throws LicenseException {

		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSEGETTINGEXPIRED,
				startTime, endTime);
		return licenseList;
	}

	private List<StoredLicense> iterateResultSet(final String queryID, final Date startTime, final Date endTime)
			throws LicenseException {

		StoredLicenseJPA storedLicenseJPA = null;
		StoredLicense storedLicense = null;
		String strquery = "";
		Query query = null;
		List<StoredLicenseJPA> listForStoredLicenseJPA = null;
		Iterator<StoredLicenseJPA> iteratorForStoredLicenseJPA = null;
		List<StoredLicense> listForStoredLicense = null;
		EntityManager entityManager = null;
		byte[] key = null;

		LicenseLogger.getInstance().finest(this.getClass().getName(), "iterateResultSet",
				"Getting the connection,making the query and executing the query...");
		try {
			entityManager = getConnection();
			strquery = QueryRetrieval.getSQLData(queryID);

			entityManager.getTransaction().begin();
			query = entityManager.createNativeQuery(strquery, StoredLicenseJPA.class);
			final SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy hh.mm.ss");
			final String dateFormat1 = sdf.format(startTime);
			query.setParameter(1, dateFormat1);
			final String dateFormat2 = sdf.format(endTime);
			query.setParameter(2, dateFormat2);

			listForStoredLicenseJPA = new ArrayList<StoredLicenseJPA>();
			listForStoredLicenseJPA = query.getResultList();
			listForStoredLicense = new ArrayList<StoredLicense>();
			iteratorForStoredLicenseJPA = listForStoredLicenseJPA.iterator();
			while (iteratorForStoredLicenseJPA.hasNext()) {
				storedLicenseJPA = iteratorForStoredLicenseJPA.next();
				storedLicense = new StoredLicense();
				storedLicense = DataBaseUtil.convertStoredLicenseJPAToStoredLicense(storedLicenseJPA);
				try {
					key = LicenseGenericDataAccessJPA.getEncryptKey(entityManager, storedLicense.getSerialNbr());
					LicenseLogger.getInstance().finest(this.getClass().getName(), "iterateResultSet",
							"Getting License Key data from the Data Base is completed..");
				} catch (final Exception e) {
					LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
							"Getting License Key data from the Data Base is failed." + e.getMessage());
					final LicenseException licenseException = new LicenseException(
							" Data retrieval is not successful from database.");
					licenseException.setErrorCode("CLJL111");
					LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
							"error code set to: " + licenseException.getErrorCode());
					throw licenseException;
				}
				storedLicense.setKey(key);
				listForStoredLicense.add(storedLicense);
			}
		} catch (final Exception e) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
					"Retrieving Data from Data Base is failed." + e.getMessage());
			final LicenseException licenseException = new LicenseException(
					" Data retrieval is not successful from database.");
			licenseException.setErrorCode("CLJL111");
			LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		} finally {
			try {
				ConnectionUtilJPA.closeConnection(entityManager);
				LicenseLogger.getInstance().finest(this.getClass().getName(), "iterateResultSet", "Connection is closed.");
			} catch (final Exception e) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
						"Connection close is failed." + e.getMessage());
				final LicenseException licenseException = new LicenseException(
						" Unable to connect/disconnect to the database.");
				licenseException.setErrorCode("CLJL109");
				LicenseLogger.getInstance().error(this.getClass().getName(), "iterateResultSet",
						"error code set to: " + licenseException.getErrorCode());
				throw licenseException;
			}
		}
		return listForStoredLicense;
	}

	@Override
	public List<StoredLicense> getModifiedLicenses(final Date startTime, final Date endTime) throws LicenseException {
		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSEMODIFIED, startTime,
				endTime);
		return licenseList;
	}
}