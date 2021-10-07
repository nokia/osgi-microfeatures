/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */

package com.nokia.licensing.storages.jpa;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.nokia.licensing.dao.LicenseCancelInfoJPA;
import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.querycache.DataBaseUtil;
import com.nokia.licensing.querycache.QueryRetrieval;
import com.nokia.licensing.utils.LicenseConstants;


/**
 * This class is the JPA implementation of LicenseCancelDataStorage Interface
 *
 * @author Rama Manohar P
 * @version 1.0
 *
 */
public class LicenseCancelDataStorageJPAImpl extends JPABaseStorage implements LicenseCancelDataStorage {

	/**
	 * Methods cancels the License based on the Information provided in the LicenseCancelInfo object and then moves the
	 * License to Cancel Storage
	 *
	 * @param cancelInfo
	 *            -- Information about the License to be canceled
	 * @throws LicenseException
	 */
	@Override
	public void insertCancelInformation(final LicenseCancelInfo licenseCancelInfo) throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "insertCancelInformation",
				"insert cancel info to cancellist table");

		EntityManager entityManager = null;
		LicenseCancelInfoJPA licenseCancelInfoJPA = null;

		try {
			entityManager = getConnection();
			entityManager.getTransaction().begin();
			licenseCancelInfoJPA = DataBaseUtil.convertLicenseCancelInfoToLicenseCancelInfoJPA(licenseCancelInfo);
			entityManager.persist(licenseCancelInfoJPA);
			entityManager.getTransaction().commit();
			LicenseLogger.getInstance().finest(this.getClass().getName(), "insertCancelInformation",
					"Data is inserted into CancelList Data Base is completed");
		} catch (final Exception e) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
					"Data insertion into the database is failed." + e.getMessage());

			final LicenseException licenseException = new LicenseException(
					" Data insertion into the database is failed.");

			licenseException.setErrorCode("CLJL112");
			LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
					"error code set to: " + licenseException.getErrorCode());

			throw licenseException;
		} finally {
			try {
				ConnectionUtilJPA.closeConnection(entityManager);
				LicenseLogger.getInstance().finest(this.getClass().getName(), "insertCancelInformation", "Connection is closed.");
			} catch (final Exception e) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
						"Unable to connect/disconnect to the database." + e.getMessage());

				final LicenseException licenseException = new LicenseException(
						" Unable to connect/disconnect to the database.");

				licenseException.setErrorCode("CLJL109");
				LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
						"error code set to: " + licenseException.getErrorCode());

				throw licenseException;
			}
		}
	}

	/**
	 * Method gets the Cancel Information based on Serial Number. The second argument tells whether data integrity
	 * checks against data storage needs to be performed or not.
	 *
	 * @param serialNumber
	 *            -- License Serial Number
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return -- Cancel Information fetched for a given Serial Number
	 * @throws LicenseException
	 */
	@Override
	public List<LicenseCancelInfo> getCancelInfoBySerialNumber(final String serialNumber,
			final boolean checkDataIntegrity) throws LicenseException {
		LicenseCancelInfo licenseCancelInfo = null;
		List<LicenseCancelInfo> listForLicenseCancelInfo = null;
		EntityManager entityManager = null;
		LicenseCancelInfoJPA licenseCancelInfoJPA = null;
		List<LicenseCancelInfoJPA> listForLicenseCancelInfoJPA = null;
		Iterator<LicenseCancelInfoJPA> iteratorForLicenseCancelInfoJPA = null;
		String strquery = null;
		Query query = null;

		LicenseLogger.getInstance().finest(this.getClass().getName(), "getCancelInfoBySerialNumber",
				"get cancel info by serialnumber");

		try {
			entityManager = getConnection();
			strquery = QueryRetrieval.getSQLData(LicenseConstants.SELECTCANCELINFOBYSERIALNUMBER);
			entityManager.getTransaction().begin();
			query = entityManager.createNativeQuery(strquery, LicenseCancelInfoJPA.class);
			query.setParameter(1, serialNumber);
			listForLicenseCancelInfoJPA = new ArrayList<LicenseCancelInfoJPA>();
			listForLicenseCancelInfoJPA = query.getResultList();
			iteratorForLicenseCancelInfoJPA = listForLicenseCancelInfoJPA.iterator();
			listForLicenseCancelInfo = new ArrayList<LicenseCancelInfo>();

			while (iteratorForLicenseCancelInfoJPA.hasNext()) {
				licenseCancelInfo = new LicenseCancelInfo();
				licenseCancelInfoJPA = iteratorForLicenseCancelInfoJPA.next();
				licenseCancelInfo = DataBaseUtil.convertLicenseCancelInfoJPAToLicenseCancelInfo(licenseCancelInfoJPA);
				listForLicenseCancelInfo.add(licenseCancelInfo);
			}

			LicenseLogger.getInstance().finest(this.getClass().getName(), "getCancelInfoBySerialNumber",
					"Retrieving CancelList Data from Data Base based on serilanumber is completed.");
		} catch (final Exception e) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getCancelInfoBySerialNumber",
					"Retrieving CancelList Data from Data Base based on serialnumber is failed." + e.getMessage());

			final LicenseException licenseException = new LicenseException(
					" Data retrieval is not successful from database.");

			licenseException.setErrorCode("CLJL111");
			LicenseLogger.getInstance().error(this.getClass().getName(), "getCancelInfoBySerialNumber",
					"error code set to: " + licenseException.getErrorCode());

			throw licenseException;
		} finally {
			try {
				ConnectionUtilJPA.closeConnection(entityManager);
				LicenseLogger.getInstance().finest(this.getClass().getName(), "getCancelInfoBySerialNumber", "Connection is closed.");
			} catch (final Exception e) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "getCancelInfoBySerialNumber",
						"Connection close is failed." + e.getMessage());

				final LicenseException licenseException = new LicenseException(
						" Unable to connect/disconnect to the database.");

				licenseException.setErrorCode("CLJL109");
				LicenseLogger.getInstance().error(this.getClass().getName(), "getCancelInfoBySerialNumber",
						"error code set to: " + licenseException.getErrorCode());

				throw licenseException;
			}
		}

		return listForLicenseCancelInfo;
	}

	/**
	 * Methods gets a List of all Canceled Licenses The checkDataIntegrity argument tells whether data integrity checks
	 * against data storage needs to be performed or not.
	 *
	 * @param checkDataIntegrity
	 *            -- Check for the integrity of the data being modified
	 * @return -- List if all Canceled Licenses
	 * @throws LicenseException
	 */
	@Override
	public List<LicenseCancelInfo> getAllCancelInfos(final boolean checkDataIntegrity) throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getAllCancelInfos", "get all cancel information");

		LicenseCancelInfo licenseCancelInfo = null;
		List<LicenseCancelInfo> listForLicenseCancelInfo = null;
		EntityManager entityManager = null;
		LicenseCancelInfoJPA licenseCancelInfoJPA = null;
		List<LicenseCancelInfoJPA> listForLicenseCancelInfoJPA = null;
		Iterator<LicenseCancelInfoJPA> iteratorForLicenseCancelInfoJPA = null;
		String strquery = null;
		Query query = null;

		try {
			entityManager = getConnection();
			strquery = QueryRetrieval.getSQLData(LicenseConstants.SELECTALLCANCELINFOS);
			entityManager.getTransaction().begin();
			query = entityManager.createNativeQuery(strquery, LicenseCancelInfoJPA.class);
			listForLicenseCancelInfoJPA = new ArrayList<LicenseCancelInfoJPA>();
			listForLicenseCancelInfoJPA = query.getResultList();
			iteratorForLicenseCancelInfoJPA = listForLicenseCancelInfoJPA.iterator();
			listForLicenseCancelInfo = new ArrayList<LicenseCancelInfo>();

			while (iteratorForLicenseCancelInfoJPA.hasNext()) {
				licenseCancelInfo = new LicenseCancelInfo();
				licenseCancelInfoJPA = iteratorForLicenseCancelInfoJPA.next();
				licenseCancelInfo = DataBaseUtil.convertLicenseCancelInfoJPAToLicenseCancelInfo(licenseCancelInfoJPA);
				listForLicenseCancelInfo.add(licenseCancelInfo);
			}

			LicenseLogger.getInstance().finest(this.getClass().getName(), "getAllCancelInfos",
					"Retrieving CancelLiis Data from Data Base is completed.");
		} catch (final Exception e) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getAllCancelInfos",
					"Data retrieval is not successful from database." + e.getMessage());

			final LicenseException licenseException = new LicenseException(
					" Data retrieval is not successful from database.");

			licenseException.setErrorCode("CLJL111");
			LicenseLogger.getInstance().error(this.getClass().getName(), "getAllCancelInfos",
					"error code set to: " + licenseException.getErrorCode());

			throw licenseException;
		} finally {
			try {
				ConnectionUtilJPA.closeConnection(entityManager);
				LicenseLogger.getInstance().finest(this.getClass().getName(), "getAllCancelInfos", "Connection is closed.");
			} catch (final Exception e) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "getAllCancelInfos",
						"Connection close is failed." + e.getMessage());

				final LicenseException licenseException = new LicenseException(
						" Unable to connect/disconnect to the database.");

				licenseException.setErrorCode("CLJL109");
				LicenseLogger.getInstance().error(this.getClass().getName(), "getAllCancelInfos",
						"error code set to: " + licenseException.getErrorCode());

				throw licenseException;
			}
		}

		return listForLicenseCancelInfo;
	}

	@Override
	public List<StoredLicense> getLicenseChanges(final Date startTime, final Date endTime) throws LicenseException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<LicenseCancelInfo> getCanceledLicense(final Date startTime, final Date endTime)
			throws LicenseException {
		String strquery = "";
		Query query = null;
		List<LicenseCancelInfoJPA> licenseCancelInfoJPAList = null;
		List<LicenseCancelInfo> licenseCancelInfoList = null;
		EntityManager entityManager = null;

		LicenseLogger.getInstance().finest(this.getClass().getName(), "getCanceledLicense",
				"Getting the connection,making the query and executing the query...");

		try {
			entityManager = getConnection();
			strquery = QueryRetrieval.getSQLData(LicenseConstants.SELECTALLCANCELINFOSBYCANCELDATE);
			entityManager.getTransaction().begin();
			query = entityManager.createNativeQuery(strquery, LicenseCancelInfoJPA.class);

			final SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy hh.mm.ss");
			final String dateFormat1 = sdf.format(startTime);

			query.setParameter(1, dateFormat1);

			final String dateFormat2 = sdf.format(endTime);

			query.setParameter(2, dateFormat2);
			licenseCancelInfoList = new ArrayList<LicenseCancelInfo>();
			licenseCancelInfoJPAList = new ArrayList<LicenseCancelInfoJPA>();
			licenseCancelInfoJPAList = query.getResultList();

			for (final LicenseCancelInfoJPA licenseCancelInfoJPA : licenseCancelInfoJPAList) {
				final LicenseCancelInfo licenseCancelInfo = DataBaseUtil
						.convertLicenseCancelInfoJPAToLicenseCancelInfo(licenseCancelInfoJPA);

				licenseCancelInfoList.add(licenseCancelInfo);
			}
		} catch (final Exception e) {
			final LicenseException licenseException = new LicenseException(
					" Unable to connect/disconnect to the database.");

			licenseException.setErrorCode("CLJL109");
			LicenseLogger.getInstance().error(this.getClass().getName(), "getCanceledLicense",
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
				LicenseLogger.getInstance().error(this.getClass().getName(), "getCanceledLicense",
						"error code set to: " + licenseException.getErrorCode());

				throw licenseException;
			}
		}

		return licenseCancelInfoList;
	}
}
