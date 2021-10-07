/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.storages.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.querycache.DataBaseUtil;
import com.nokia.licensing.querycache.QueryRetrieval;
import com.nokia.licensing.utils.LicenseConstants;


/**
 * This class is the implementation of LicenseCancelDataStorage Interface
 *
 * @author pratap
 * @version 1.0
 *
 */
public class LicenseCancelDataStorageImpl extends JDBCBaseStorage implements LicenseCancelDataStorage {

	/**
	 * Methods cancels the License based on the Information provided in the LicenseCancelInfo object and then moves the
	 * License to Cancel Storage
	 *
	 * @param cancelInfo
	 *            -- Information about the License to be canceled
	 * @throws LicenseException
	 */
	@Override
	public void insertCancelInformation(final LicenseCancelInfo cancelInfo) throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "insertCancelInformation",
				"insert cancel info to cancellist table");

		Connection connection = null;
		PreparedStatement prepareStatement = null;

		try {
			connection = getConnection();

			final String query = QueryRetrieval.getSQLData(LicenseConstants.INSERTCANCELINFORMATION);

			if (connection != null) {
				prepareStatement = connection.prepareStatement(query);
			}

			prepareStatement = DataBaseUtil.setLicenseCancelInfoToPrepareStmt(prepareStatement, cancelInfo);
			prepareStatement.executeUpdate();
			LicenseLogger.getInstance().finest(this.getClass().getName(), "insertCancelInformation",
					"Data is inserted into CancelList Data Base is completed");
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
					"Data insertion is failed." + sqle.getMessage());

			final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);

			licenseException.setErrorCode("CLJL112");
			LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
					"error code set to: " + licenseException.getErrorCode());

			throw licenseException;
		} catch (final ClassNotFoundException cnfe) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
					"Getting the conetion is failed." + cnfe.getMessage());

			final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);

			licenseException.setErrorCode("CLJL109");
			LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		} catch (final IOException ioe) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
					"Failed to load ORACLE Information." + ioe.getMessage());
			final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);
			licenseException.setErrorCode("CLJL109");
			LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
					"error code set to: " + licenseException.getErrorCode());

			throw licenseException;
		} finally {
			try {
				ConnectionUtil.closeConnection(prepareStatement, connection);
				LicenseLogger.getInstance().finest(this.getClass().getName(), "insertCancelInformation", "Connection is closed.");
			} catch (final SQLException sqle) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "insertCancelInformation",
						"Connection close is failed." + sqle.getMessage());

				final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);

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
		LicenseLogger.getInstance().finest(this.getClass().getName(), "getCancelInfoBySerialNumber",
				"get cancel info by serialnumber");

		Connection connection = null;
		PreparedStatement prepareStatement = null;
		LicenseCancelInfo licenseCancelInfo = null;
		List<LicenseCancelInfo> listForLicenseCancelInfo = null;
		ResultSet resultSet = null;

		try {
			connection = getConnection();

			final String query = QueryRetrieval.getSQLData(LicenseConstants.SELECTCANCELINFOBYSERIALNUMBER);

			if (connection != null) {
				prepareStatement = connection.prepareStatement(query);
			}

			prepareStatement.setString(1, serialNumber);
			resultSet = prepareStatement.executeQuery();
			listForLicenseCancelInfo = new ArrayList<LicenseCancelInfo>();

			while (resultSet.next()) {
				licenseCancelInfo = new LicenseCancelInfo();
				licenseCancelInfo = DataBaseUtil.convertResultSetToLicenseCancelInfo(resultSet);
				listForLicenseCancelInfo.add(licenseCancelInfo);
			}

			LicenseLogger.getInstance().finest(this.getClass().getName(), "getCancelInfoBySerialNumber",
					"Retrieving CancelList Data from Data Base based on serialnumber is completed.");
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getCancelInfoBySerialNumber",
					"Retrieving CancelList Data from Data Base based on serialnumber is failed." + sqle.getMessage());
			final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);
			licenseException.setErrorCode("CLJL111");
			LicenseLogger.getInstance().error(this.getClass().getName(), "getCancelInfoBySerialNumber",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		} finally {
			try {
				ConnectionUtil.closeConnection(prepareStatement, connection);
				LicenseLogger.getInstance().finest(this.getClass().getName(), "getCancelInfoBySerialNumber", "Connection is closed.");
			} catch (final SQLException sqle) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "getCancelInfoBySerialNumber",
						"Connection close is failed." + sqle.getMessage());
				final LicenseException licenseException = new LicenseException(" Connection close is failed.");
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
		Connection connection = null;
		PreparedStatement prepareStatement = null;
		ResultSet resultSet = null;
		final List<LicenseCancelInfo> list = new ArrayList<LicenseCancelInfo>();

		try {
			connection = getConnection();

			final String query = QueryRetrieval.getSQLData(LicenseConstants.SELECTALLCANCELINFOS);

			if (connection != null) {
				prepareStatement = connection.prepareStatement(query);
			}

			resultSet = prepareStatement.executeQuery();

			while (resultSet.next()) {
				licenseCancelInfo = new LicenseCancelInfo();
				licenseCancelInfo = DataBaseUtil.convertResultSetToLicenseCancelInfo(resultSet);
				list.add(licenseCancelInfo);
			}

			LicenseLogger.getInstance().finest(this.getClass().getName(), "getAllCancelInfos",
					"Retrieving CancelLiis Data from Data Base based on serial number is completed.");
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getAllCancelInfos",
					"Retrieving Data from Data base is failed." + sqle.getMessage());
			final LicenseException licenseException = new LicenseException(
					" Retrieving Data from Data Base based on serial number is failed.");
			licenseException.setErrorCode("CLJL111");
			LicenseLogger.getInstance().error(this.getClass().getName(), "getAllCancelInfos",
					"error code set to: " + licenseException.getErrorCode());

			throw licenseException;
		} finally {
			try {
				ConnectionUtil.closeConnection(prepareStatement, connection);
				LicenseLogger.getInstance().finest(this.getClass().getName(), "getAllCancelInfos", "Connection is closed.");
			} catch (final SQLException sqle) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "getAllCancelInfos",
						"Connection close is failed." + sqle.getMessage());
				final LicenseException licenseException = new LicenseException(" Connection close is failed.");
				licenseException.setErrorCode("CLJL109");
				LicenseLogger.getInstance().error(this.getClass().getName(), "getAllCancelInfos",
						"error code set to: " + licenseException.getErrorCode());
				throw licenseException;
			}
		}
		return list;
	}

	@Override
	public List<StoredLicense> getLicenseChanges(final Date startTime, final Date endTime) throws LicenseException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<LicenseCancelInfo> getCanceledLicense(final Date startTime, final Date endTime)
			throws LicenseException {
		throw new UnsupportedOperationException();
	}
}
