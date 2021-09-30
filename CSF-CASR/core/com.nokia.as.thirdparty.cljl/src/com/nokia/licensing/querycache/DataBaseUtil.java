/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.querycache;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.nokia.licensing.dao.FeatureInfoJPA;
import com.nokia.licensing.dao.LicenseCancelInfoJPA;
import com.nokia.licensing.dao.StoredLicenseJPA;
import com.nokia.licensing.dao.TargetSystemJPA;
import com.nokia.licensing.dtos.FeatureInfo;
import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.dtos.TargetSystem;
import com.nokia.licensing.logging.LicenseLogger;


/**
 * This class is for converting the ResultSet into StoredLicense and from LicenseCancelInfo and setting values to the
 * PrepareStatements of StoredLicens,FeatureInfo,TragetSystem and LicenseCancelInfo to insert data into the tables.
 *
 * @author Rama Manohar P
 * @version 1.0
 *
 */
public class DataBaseUtil {
	private static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss";

	/**
	 * Setting values to the StoredLicense POJO class from the result set
	 *
	 * @param resultSet
	 *            -- License Data Information
	 * @return StoredLicense -- License Data Information
	 * @throws SQLException
	 */
	public static StoredLicense convertResultSetToStoredLicense(final ResultSet resultSet) throws SQLException {
		final StoredLicense storedLicense = new StoredLicense();

		try {
			storedLicense.setSerialNbr(resultSet.getString(1));
			storedLicense.setOrderId(resultSet.getString(2));
			storedLicense.setLicenseCode(resultSet.getString(3));
			storedLicense.setLicenseName(resultSet.getString(4));
			storedLicense.setCustomerName(resultSet.getString(5));
			storedLicense.setCustomerId(resultSet.getString(6));

			final int licenseModeValue = resultSet.getInt(7);
			final int licenseTypeValue = resultSet.getInt(8);

			storedLicense.setLicenseMode(StoredLicense.LicenseMode.returnEnumValue(licenseModeValue));
			storedLicense.setLicenseType(StoredLicense.LicenseType.returnEnumValue(licenseTypeValue));
			storedLicense.setLicenseFileImportTime(resultSet.getTimestamp(9));
			storedLicense.setLicenseFileImportUser(resultSet.getString(10));
			storedLicense.setMaxValue(resultSet.getLong(11));
			storedLicense.setStartTime(resultSet.getTimestamp(12));
			storedLicense.setEndTime(resultSet.getTimestamp(13));
			storedLicense.setCapacityUnit(resultSet.getString(14));
			storedLicense.setAdditionalInfo(resultSet.getString(15));
			storedLicense.setOriginOMC(resultSet.getString(16));
			storedLicense.setPool(resultSet.getString(17));
			storedLicense.setLicenseFileName(resultSet.getString(18));
			storedLicense.setLicenseFilePath(resultSet.getString(19));
			storedLicense.setSwReleaseBase(resultSet.getString(20));
			storedLicense.setSwReleaseRelation(resultSet.getString(21));
			storedLicense.setTargetNEType(resultSet.getString(22));
			storedLicense.setUsageType(resultSet.getString(23));
			storedLicense.setIsValid(resultSet.getString(24));
			storedLicense.setStoredLicenseSignature(resultSet.getBytes(25));
			LicenseLogger.getInstance().finer(DataBaseUtil.class.getName(), "convertResultSetToStoredLicense",
					"License converted=" + storedLicense.getLicenseFileName());
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(DataBaseUtil.class.getName(), "convertResultSetToStoredLicense",
					"Failed to get the data from result set." + sqle.getMessage());

			throw sqle;
		}

		return storedLicense;
	}

	/**
	 * Setting the values to the prepare statement from the Stored License object
	 *
	 * @param prepareStatement
	 *            -- Contains the query statement to insert data in to Stored License Table
	 * @param storedLicenseInsert
	 *            -- Contains the License Data to be insert into Data Base
	 * @return PreparedStatement -- Holds the Stored License Data
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public static PreparedStatement setStoredLicenseToPrepareStmt(final PreparedStatement prepareStatement,
			final StoredLicense storedLicenseInsert) throws SQLException, ClassNotFoundException {
		Timestamp sDate = null;
		Timestamp eDate = null;
		Timestamp importTime = null;
		Date date = null;
		final PreparedStatement prepareStatementSet = prepareStatement;
		final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);

		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "setStoredLicenseToPrepareStmt",
				"Setting data from StoredLicense object into prepare statement...");

		try {
			prepareStatementSet.setString(1, storedLicenseInsert.getSerialNbr());
			prepareStatementSet.setString(2, storedLicenseInsert.getOrderId());
			prepareStatementSet.setString(3, storedLicenseInsert.getLicenseCode());
			prepareStatementSet.setString(4, storedLicenseInsert.getLicenseName());
			prepareStatementSet.setString(5, storedLicenseInsert.getCustomerName());
			prepareStatementSet.setString(6, storedLicenseInsert.getCustomerId());
			prepareStatementSet.setInt(7, storedLicenseInsert.getLicenseMode().returnIntValue());
			prepareStatementSet.setInt(8, storedLicenseInsert.getLicenseType().returnIntValue());

			// converting to 24 hour format while inserting
			String timeString = dateFormat.format(storedLicenseInsert.getLicenseFileImportTime());

			date = dateFormat.parse(timeString);
			importTime = new Timestamp(date.getTime());
			prepareStatementSet.setTimestamp(9, importTime);
			prepareStatementSet.setString(10, storedLicenseInsert.getLicenseFileImportUser());
			prepareStatementSet.setLong(11, storedLicenseInsert.getMaxValue());

			// converting to 24 hour format while inserting
			timeString = dateFormat.format(storedLicenseInsert.getStartTime());
			date = dateFormat.parse(timeString);
			sDate = new Timestamp(date.getTime());
			prepareStatementSet.setTimestamp(12, sDate);

			// converting to 24 hour format while inserting
			if (storedLicenseInsert.getEndTime() != null) {
				timeString = dateFormat.format(storedLicenseInsert.getEndTime());
				date = dateFormat.parse(timeString);

				/* if (date != null){ */
				eDate = new Timestamp(date.getTime());

				/* } */
			}

			prepareStatementSet.setTimestamp(13, eDate);
			prepareStatementSet.setString(14, storedLicenseInsert.getCapacityUnit());
			prepareStatementSet.setString(15, storedLicenseInsert.getAdditionalInfo());
			prepareStatementSet.setString(16, storedLicenseInsert.getOriginOMC());
			prepareStatementSet.setString(17, storedLicenseInsert.getPool());
			prepareStatementSet.setString(18, storedLicenseInsert.getLicenseFileName());
			prepareStatementSet.setString(19, storedLicenseInsert.getLicenseFilePath());
			prepareStatementSet.setString(20, storedLicenseInsert.getSwReleaseBase());
			prepareStatementSet.setString(21, storedLicenseInsert.getSwReleaseRelation());
			prepareStatementSet.setString(22, storedLicenseInsert.getTargetNEType());
			prepareStatementSet.setString(23, storedLicenseInsert.getUsageType());
			prepareStatementSet.setString(24, storedLicenseInsert.getIsValid());
			prepareStatementSet.setBytes(25, storedLicenseInsert.getStoredLicenseSignature());
			LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "setStoredLicenseToPrepareStmt",
					"Setting data from StoredLicense object into prepare statement is completed.");
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(DataBaseUtil.class.getName(), "setStoredLicenseToPrepareStmt",
					"Failed to put the data into prepare statement." + sqle.getMessage());

			throw sqle;
		} catch (final ParseException e) {
			LicenseLogger.getInstance().error(DataBaseUtil.class.getName(), "setStoredLicenseToPrepareStmt",
					"Failed to parse date format." + e.getMessage());
		}

		return prepareStatementSet;
	}

	/**
	 * Setting the values to the prepare statement from the Feature Info object and Serial Number from Stored License
	 * object
	 *
	 * @param prepareStatement
	 *            -- Contains the query statement to insert data in to Feature Info Table
	 * @param featureInfo
	 *            -- Contains the License Feature Data to be insert into Data Base
	 * @param storedLicenseInsert
	 *            -- Contains the License Data to get Serial number
	 * @return PreparedStatement -- Holds the Feature Info Data
	 * @throws SQLException
	 */
	public static PreparedStatement setFeatureInfoToPrepareStmt(final PreparedStatement prepareStatement,
			final FeatureInfo featureInfo, final StoredLicense storedLicenseInsert) throws SQLException {
		Timestamp modifiedTimeFeatureInfo = null;
		final PreparedStatement prepareStatementSet = prepareStatement;
		Date date = null;
		final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);

		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "setFeatureInfoToPrepareStmt",
				"Setting data from FeatureInfo object into prepare statement...");

		try {
			prepareStatementSet.setString(1, storedLicenseInsert.getSerialNbr());
			prepareStatementSet.setString(2, featureInfo.getFeatureName());
			prepareStatementSet.setLong(3, featureInfo.getFeatureCode());
			prepareStatementSet.setBytes(4, featureInfo.getFeatureInfoSignature());

			final String timeString = dateFormat.format(featureInfo.getModifiedTime());

			date = dateFormat.parse(timeString);
			modifiedTimeFeatureInfo = new Timestamp(date.getTime());
			prepareStatementSet.setTimestamp(5, modifiedTimeFeatureInfo);
			LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "setFeatureInfoToPrepareStmt",
					"Setting data from FeatureInfo object into prepare statement is completed.");
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(DataBaseUtil.class.getName(), "setFeatureInfoToPrepareStmt",
					"Failed to put the data into prepare statement." + sqle.getMessage());

			throw sqle;
		} catch (final ParseException e) {
			LicenseLogger.getInstance().error(DataBaseUtil.class.getName(), "setFeatureInfoToPrepareStmt",
					"Failed to parse date format." + e.getMessage());
		}

		return prepareStatementSet;
	}

	/**
	 * Setting the values to the prepare statement from the Target System object
	 *
	 * @param prepareStatement
	 *            -- Contains the query statement to insert data in to Target System Table
	 * @param targetSystem
	 *            -- Contains the License Target Id Data to be insert into Data Base
	 * @param storedLicenseInsert
	 *            -- Contains the License Data to get Serial number
	 * @return PreparedStatement -- Holds the Target System Data
	 * @throws SQLException
	 */
	public static PreparedStatement setTargetSystemToPrepareStmt(final PreparedStatement prepareStatement,
			final TargetSystem targetSystem, final StoredLicense storedLicenseInsert) throws SQLException {
		Timestamp modifiedTimeTargetSystem = null;
		final PreparedStatement prepareStatementSet = prepareStatement;
		Date date = null;
		final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);

		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "setTargetSystemToPrepareStmt",
				"Setting data from TargetSystem object into prepare statement...");

		try {
			prepareStatementSet.setString(1, storedLicenseInsert.getSerialNbr());
			prepareStatementSet.setString(2, targetSystem.getTargetId());
			prepareStatementSet.setBytes(3, targetSystem.getTargetSystemSignature());

			final String timeString = dateFormat.format(targetSystem.getModifiedTime());

			date = dateFormat.parse(timeString);
			modifiedTimeTargetSystem = new Timestamp(date.getTime());
			prepareStatementSet.setTimestamp(4, modifiedTimeTargetSystem);
			LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "setTargetSystemToPrepareStmt",
					"Setting data from TargetSystem object into prepare statement is completed.");
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(DataBaseUtil.class.getName(), "setTargetSystemToPrepareStmt",
					"Failed to put the data into prepare statement." + sqle.getMessage());

			throw sqle;
		} catch (final ParseException e) {
			LicenseLogger.getInstance().error(DataBaseUtil.class.getName(), "setTargetSystemToPrepareStmt",
					"Failed to parse date format." + e.getMessage());
		}

		return prepareStatementSet;
	}

	/**
	 * Setting values to the StoredLicense POJO class from the result set
	 *
	 * @param resultSet
	 *            -- License Cancel Information
	 * @return LicenseCancelInfo -- License Cancel Information
	 * @throws SQLException
	 */
	public static LicenseCancelInfo convertResultSetToLicenseCancelInfo(final ResultSet resultSet) throws SQLException {
		final LicenseCancelInfo licenseCancelInfo = new LicenseCancelInfo();

		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "convertResultSetToLicenseCancelInfo",
				"Setting data to CancelInfo object from result set...");

		try {
			licenseCancelInfo.setSerialNbr(resultSet.getString(1));
			licenseCancelInfo.setFeaturecode(resultSet.getLong(2));
			licenseCancelInfo.setCancelDate(resultSet.getDate(3));
			licenseCancelInfo.setCancelReason(resultSet.getString(4));
			licenseCancelInfo.setUserName(resultSet.getString(5));
			licenseCancelInfo.setLicenseFileName(resultSet.getString(6));
			licenseCancelInfo.setCancelListSignature(resultSet.getBytes(7));
			LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "convertResultSetToLicenseCancelInfo",
					"Setting data to CancelInfo object from result set is completed");
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(DataBaseUtil.class.getName(), "convertResultSetToLicenseCancelInfo",
					"Failed to get the data from result set." + sqle.getMessage());

			throw sqle;
		}

		return licenseCancelInfo;
	}

	/**
	 * Setting the values to the prepare statement from the License Cancel Info object
	 *
	 * @param prepareStatement
	 *            -- Contains the query statement to insert data in to License Cancel Info Table
	 * @param licenseCancelInfo
	 *            -- Contains the License Cancel Data to be insert into Data Base
	 * @return PreparedStatement -- Holds the License Cancel Data
	 * @throws SQLException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 *
	 */
	public static PreparedStatement setLicenseCancelInfoToPrepareStmt(final PreparedStatement prepareStatement,
			final LicenseCancelInfo licenseCancelInfo) throws SQLException, ClassNotFoundException, IOException {
		Timestamp sqlDate = null;
		final PreparedStatement prepareStatementSet = prepareStatement;
		Date date = null;
		final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);

		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "setLicenseCancelInfoToPrepareStmt",
				"Setting data from CancelInfo object into prepare statement...");

		try {
			prepareStatementSet.setString(1, licenseCancelInfo.getSerialNbr());
			prepareStatementSet.setLong(2, licenseCancelInfo.getFeaturecode());

			final String timeString = dateFormat.format(licenseCancelInfo.getCancelDate());

			date = dateFormat.parse(timeString);
			sqlDate = new Timestamp(date.getTime());
			prepareStatementSet.setTimestamp(3, sqlDate);
			prepareStatementSet.setString(4, licenseCancelInfo.getCancelReason());
			prepareStatementSet.setString(5, licenseCancelInfo.getUserName());
			prepareStatementSet.setString(6, licenseCancelInfo.getLicenseFileName());
			prepareStatementSet.setBytes(7, licenseCancelInfo.getCancelListSignature());
			LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "setLicenseCancelInfoToPrepareStmt",
					"Setting data from CancelInfo object into prepare statement is completed.");
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(DataBaseUtil.class.getName(), "setLicenseCancelInfoToPrepareStmt",
					"Failed to put the data into prepare statement." + sqle.getMessage());

			throw sqle;
		} catch (final ParseException e) {
			LicenseLogger.getInstance().error(DataBaseUtil.class.getName(), "setLicenseCancelInfoToPrepareStmt",
					"Failed to parse date format." + e.getMessage());
		}

		return prepareStatementSet;
	}

	/**
	 * Setting values to the StoredLicense POJO class from the StoredLicenseJPA annotated POJO class
	 *
	 * @param storedLicenseJPA
	 * @return StoredLicense -- License Data Information
	 */
	public static StoredLicense convertStoredLicenseJPAToStoredLicense(final StoredLicenseJPA storedLicenseJPA) {
		StoredLicense storedLicense = null;
		List<FeatureInfo> listForFeatureInfo = null;
		FeatureInfo featureInfo = null;
		List<TargetSystem> listForTargetSystem = null;
		TargetSystem targetSystem = null;

		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "convertStoredLicenseJPAToStoredLicense",
				"Setting data to StoredLicense object from StoredLicenseJPA object...");
		storedLicense = new StoredLicense();
		storedLicense.setSerialNbr(storedLicenseJPA.getSerialNbr());
		storedLicense.setOrderId(storedLicenseJPA.getOrderId());
		storedLicense.setLicenseCode(storedLicenseJPA.getLicenseCode());
		storedLicense.setLicenseName(storedLicenseJPA.getLicenseName());
		storedLicense.setCustomerName(storedLicenseJPA.getCustomerName());
		storedLicense.setCustomerId(storedLicenseJPA.getCustomerId());
		storedLicense.setLicenseType(StoredLicense.LicenseType.returnEnumValue(storedLicenseJPA.getLicenseType()));
		storedLicense.setMaxValue(storedLicenseJPA.getMaxValue());
		storedLicense.setStartTime(storedLicenseJPA.getStartTime());
		storedLicense.setEndTime(storedLicenseJPA.getEndTime());
		storedLicense.setCapacityUnit(storedLicenseJPA.getCapacityUnit());
		storedLicense.setAdditionalInfo(storedLicenseJPA.getAdditionalInfo());
		storedLicense.setLicenseFileName(storedLicenseJPA.getLicenseFileName());
		storedLicense.setLicenseFilePath(storedLicenseJPA.getLicenseFilePath());
		storedLicense.setSwReleaseBase(storedLicenseJPA.getSwReleaseBase());
		storedLicense.setSwReleaseRelation(storedLicenseJPA.getSwReleaseRelation());
		storedLicense.setTargetNEType(storedLicenseJPA.getTargetNEType());
		storedLicense.setUsageType(storedLicenseJPA.getUsageType());
		storedLicense.setIsValid(storedLicenseJPA.getIsValid());

		final Iterator<FeatureInfoJPA> iteratorForFeatureInfoJPA = storedLicenseJPA.getFeatureInfoList().iterator();
		FeatureInfoJPA featureInfoJPA = new FeatureInfoJPA();

		listForFeatureInfo = new ArrayList<FeatureInfo>();

		while (iteratorForFeatureInfoJPA.hasNext()) {
			featureInfo = new FeatureInfo();
			featureInfoJPA = iteratorForFeatureInfoJPA.next();
			featureInfo.setFeatureName(featureInfoJPA.getFeatureName());
			featureInfo.setFeatureCode(featureInfoJPA.getFeatureCode());
			featureInfo.setFeatureInfoSignature(featureInfoJPA.getFeatureInfoSignature());
			featureInfo.setModifiedTime(featureInfoJPA.getModifiedTime());
			listForFeatureInfo.add(featureInfo);
		}

		storedLicense.setFeatureInfoList(listForFeatureInfo);

		final Iterator<TargetSystemJPA> iteratorForTargetSystemJPA = storedLicenseJPA.getTargetIds().iterator();
		TargetSystemJPA targetSystemJPA = new TargetSystemJPA();

		listForTargetSystem = new ArrayList<TargetSystem>();

		while (iteratorForTargetSystemJPA.hasNext()) {
			targetSystem = new TargetSystem();
			targetSystemJPA = iteratorForTargetSystemJPA.next();
			targetSystem.setTargetId(targetSystemJPA.getTargetId());
			targetSystem.setTargetSystemSignature(targetSystemJPA.getTargetSystemSignature());
			targetSystem.setModifiedTime(targetSystemJPA.getModifiedTime());
			listForTargetSystem.add(targetSystem);
		}

		storedLicense.setTargetIds(listForTargetSystem);
		storedLicense.setStoredLicenseSignature(storedLicenseJPA.getStoredLicenseSignature());

		// Added
		storedLicense.setLicenseMode(StoredLicense.LicenseMode.returnEnumValue(storedLicenseJPA.getLicenseMode()));
		storedLicense.setLicenseFileImportTime(storedLicenseJPA.getLicenseFileImportTime());
		storedLicense.setLicenseFileImportUser(storedLicenseJPA.getLicenseFileImportUser());
		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "convertResultSetToStoredLicense",
				"Setting data to StoredLicense object from StoredLicenseJPA object is completed");

		return storedLicense;
	}

	/**
	 * Setting values to the StoredLicenseJPA annotated POJO class from the StoredLicense POJO class
	 *
	 * @param storedLicense
	 * @return StoredLicenseJPA -- License Data Information
	 */
	public static StoredLicenseJPA convertStoredLicenseToStoredLicenseJPA(final StoredLicense storedLicense) {
		FeatureInfo featureInfo = null;
		TargetSystem targetSystem = null;
		FeatureInfoJPA featureInfoJPA = null;
		TargetSystemJPA targetSystemJPA = null;
		Iterator<FeatureInfo> iteratorForFeatureInfo = null;
		Iterator<TargetSystem> iteratorForTargetSystem = null;
		List<FeatureInfoJPA> listForFeatureInfoJPA = null;
		List<TargetSystemJPA> listForTargetSystemJPA = null;
		final StoredLicenseJPA storedLicenseJPA = new StoredLicenseJPA();

		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "convertStoredLicenseToStoredLicenseJPA",
				"Setting data to StoredLicenseJPA object from StoredLicense object...");
		storedLicenseJPA.setSerialNbr(storedLicense.getSerialNbr());
		storedLicenseJPA.setOrderId(storedLicense.getOrderId());
		storedLicenseJPA.setLicenseCode(storedLicense.getLicenseCode());
		storedLicenseJPA.setLicenseName(storedLicense.getLicenseName());
		storedLicenseJPA.setCustomerName(storedLicense.getCustomerName());
		storedLicenseJPA.setCustomerId(storedLicense.getCustomerId());
		storedLicenseJPA.setLicenseType(storedLicense.getLicenseType().returnIntValue());
		storedLicenseJPA.setMaxValue(storedLicense.getMaxValue());
		storedLicenseJPA.setStartTime(storedLicense.getStartTime());
		storedLicenseJPA.setEndTime(storedLicense.getEndTime());
		storedLicenseJPA.setCapacityUnit(storedLicense.getCapacityUnit());
		storedLicenseJPA.setAdditionalInfo(storedLicense.getAdditionalInfo());
		storedLicenseJPA.setLicenseFileName(storedLicense.getLicenseFileName());
		storedLicenseJPA.setLicenseFilePath(storedLicense.getLicenseFilePath());
		storedLicenseJPA.setSwReleaseBase(storedLicense.getSwReleaseBase());
		storedLicenseJPA.setSwReleaseRelation(storedLicense.getSwReleaseRelation());
		storedLicenseJPA.setTargetNEType(storedLicense.getTargetNEType());
		storedLicenseJPA.setUsageType(storedLicense.getUsageType());
		storedLicenseJPA.setIsValid(storedLicense.getIsValid());
		storedLicenseJPA.setStoredLicenseSignature(storedLicense.getStoredLicenseSignature());

		// Added
		storedLicenseJPA.setLicenseMode(storedLicense.getLicenseMode().returnIntValue());
		storedLicenseJPA.setLicenseFileImportTime(storedLicense.getLicenseFileImportTime());
		storedLicenseJPA.setLicenseFileImportUser(storedLicense.getLicenseFileImportUser());
		iteratorForFeatureInfo = storedLicense.getFeatureInfoList().iterator();
		listForFeatureInfoJPA = new ArrayList<FeatureInfoJPA>();

		while (iteratorForFeatureInfo.hasNext()) {
			featureInfoJPA = new FeatureInfoJPA();
			featureInfo = iteratorForFeatureInfo.next();
			featureInfoJPA.setFeatureCode(featureInfo.getFeatureCode());
			featureInfoJPA.setFeatureName(featureInfo.getFeatureName());
			featureInfoJPA.setFeatureInfoSignature(featureInfo.getFeatureInfoSignature());
			featureInfoJPA.setSerialNbr(storedLicense.getSerialNbr());
			featureInfoJPA.setModifiedTime(featureInfo.getModifiedTime());
			listForFeatureInfoJPA.add(featureInfoJPA);
		}

		storedLicenseJPA.setFeatureInfoList(listForFeatureInfoJPA);
		listForTargetSystemJPA = new ArrayList<TargetSystemJPA>();

		if (!storedLicense.getTargetIds().isEmpty()) {
			iteratorForTargetSystem = storedLicense.getTargetIds().iterator();

			while (iteratorForTargetSystem.hasNext()) {
				targetSystemJPA = new TargetSystemJPA();
				targetSystem = iteratorForTargetSystem.next();
				targetSystemJPA.setTargetId(targetSystem.getTargetId());
				targetSystemJPA.setTargetSystemSignature(targetSystem.getTargetSystemSignature());
				targetSystemJPA.setSerialNbr(storedLicense.getSerialNbr());
				targetSystemJPA.setModifiedTime(targetSystem.getModifiedTime());
				listForTargetSystemJPA.add(targetSystemJPA);
			}
		}

		storedLicenseJPA.setTargetIds(listForTargetSystemJPA);
		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "convertStoredLicenseToStoredLicenseJPA",
				"Setting data to StoredLicenseJPA object from StoredLicense object is completed");

		return storedLicenseJPA;
	}

	public static LicenseCancelInfo convertLicenseCancelInfoJPAToLicenseCancelInfo(
			final LicenseCancelInfoJPA licenseCancelInfoJPA) {
		final LicenseCancelInfo licenseCancelInfo = new LicenseCancelInfo();

		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "setLicenseCancelInfoToPrepareStmt",
				"Setting data to LicenseCancelInfo object from LicenseCancelInfoJPA object...");
		licenseCancelInfo.setSerialNbr(licenseCancelInfoJPA.getSerialNbr());
		licenseCancelInfo.setFeaturecode(licenseCancelInfoJPA.getFeaturecode());
		licenseCancelInfo.setCancelDate(licenseCancelInfoJPA.getCancelDate());
		licenseCancelInfo.setCancelReason(licenseCancelInfoJPA.getCancelReason());
		licenseCancelInfo.setUserName(licenseCancelInfoJPA.getUserName());
		licenseCancelInfo.setLicenseFileName(licenseCancelInfoJPA.getLicenseFileName());
		licenseCancelInfo.setCancelListSignature(licenseCancelInfoJPA.getCancelListSignature());
		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "setLicenseCancelInfoToPrepareStmt",
				"Setting data to LicenseCancelInfo object from LicenseCancelInfoJPA object is completed.");

		return licenseCancelInfo;
	}

	public static LicenseCancelInfoJPA convertLicenseCancelInfoToLicenseCancelInfoJPA(
			final LicenseCancelInfo licenseCancelInfo) {
		final LicenseCancelInfoJPA licenseCancelInfoJPA = new LicenseCancelInfoJPA();

		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "setLicenseCancelInfoToPrepareStmt",
				"Setting data to LicenseCancelInfoJPA object from LicenseCancelInfo object...");
		licenseCancelInfoJPA.setSerialNbr(licenseCancelInfo.getSerialNbr());
		licenseCancelInfoJPA.setFeaturecode(licenseCancelInfo.getFeaturecode());
		licenseCancelInfoJPA.setCancelDate(licenseCancelInfo.getCancelDate());
		licenseCancelInfoJPA.setCancelReason(licenseCancelInfo.getCancelReason());
		licenseCancelInfoJPA.setUserName(licenseCancelInfo.getUserName());
		licenseCancelInfoJPA.setLicenseFileName(licenseCancelInfo.getLicenseFileName());
		licenseCancelInfoJPA.setCancelListSignature(licenseCancelInfo.getCancelListSignature());
		LicenseLogger.getInstance().finest(DataBaseUtil.class.getName(), "setLicenseCancelInfoToPrepareStmt",
				"Setting data to LicenseCancelInfoJPA object from LicenseCancelInfo object is completed.");

		return licenseCancelInfoJPA;
	}
}
