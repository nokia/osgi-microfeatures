/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.storages.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.nokia.licensing.dtos.FeatureInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.dtos.TargetSystem;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.querycache.QueryRetrieval;
import com.nokia.licensing.utils.LicenseConstants;


/**
 * This class is for getting the appended sting of stored license attributes and populating the complete stored license
 * by adding Feature Info and Target System List.
 *
 * @author Rama Manohar P
 * @version 1.0
 *
 */
public class StoredLicenseUtil {

	/**
	 * This method will add all the attributes of Stored License
	 *
	 * @param storedLicense
	 *            -- Containing License details
	 * @return String -- Contain appended string formed after adding all the stored license attributes.
	 */
	public static String getAppendedString(final StoredLicense storedLicense) {
		Timestamp sDate = null;
		Timestamp eDate = null;
		Timestamp importTime = null;

		sDate = new Timestamp(storedLicense.getStartTime().getTime());

		if (null != storedLicense.getEndTime()) {
			eDate = new Timestamp(storedLicense.getEndTime().getTime());
		} else {
			eDate = null;
		}

		importTime = new Timestamp(storedLicense.getLicenseFileImportTime().getTime());

		final String str = storedLicense.getSerialNbr() + storedLicense.getOrderId() + storedLicense.getLicenseCode()
				+ storedLicense.getLicenseName() + storedLicense.getCustomerName() + storedLicense.getCustomerId()
				+ storedLicense.getLicenseMode().returnIntValue() + storedLicense.getLicenseType().returnIntValue()
				+ importTime + storedLicense.getLicenseFileImportUser() + storedLicense.getMaxValue() + sDate + eDate
				+ storedLicense.getCapacityUnit() + storedLicense.getAdditionalInfo() + storedLicense.getOriginOMC()
				+ storedLicense.getPool() + storedLicense.getLicenseFileName() + storedLicense.getLicenseFilePath()
				+ storedLicense.getSwReleaseBase() + storedLicense.getSwReleaseRelation()
				+ storedLicense.getTargetNEType() + storedLicense.getUsageType() + storedLicense.getIsValid();

		return str;
	}

	/**
	 * This method is for connecting to the data base and getting Feature Info List and Target System List for the
	 * corresponding Feature Name,Feature Code,Target Id and Serial Number. This method uses Plain Sql statement to
	 * create query and execute it for getting data.
	 *
	 * @param storedLicense
	 *            -- Containing License details without Feature Info & Target System List
	 * @param connection
	 *            -- connection object to get the data from Data Base
	 * @param featureName
	 *            -- Feature Name
	 * @param FeatureCode
	 *            -- Feature Code
	 * @param TargetId
	 *            -- Target ID of the Licensing system
	 * @return StoredLicense -- Containing License details with Feature Info & Target System List
	 * @throws SQLException
	 * @throws LicenseException
	 */
	public static StoredLicense populateCompletStoredLicense(final StoredLicense storedLicense,
			final Connection connection, final String featureName, final long featureCode, final String targetId)
					throws SQLException, LicenseException {
		ResultSet resultSet = null;
		String strquery = null;
		PreparedStatement preparedStatement = null;
		PreparedStatement preparedStatement2 = null;
		FeatureInfo featureInfo = null;
		List<FeatureInfo> featureInfoList = null;
		TargetSystem targetSystem = null;
		List<TargetSystem> targetSystemList = null;

		LicenseLogger.getInstance().finest(StoredLicenseUtil.class.getName(), "populateCompletStoredLicense",
				"Getting the connection,making the statement and executing the statement to get Complete data for storedlicense so it will contain FeaureInfo and TargetSystem...");

		try {
			if ((featureName == null) && (featureCode == 0l)) {
				strquery = QueryRetrieval.getSQLData(LicenseConstants.SELECTFEATUREINFOBYSERIALNUMBER);
			} else if (featureName != null) {
				strquery = QueryRetrieval.getSQLData(LicenseConstants.SELECTFEATUREINFOBYSERIALNUMBERANDFEATURENAME);
			} else if (featureCode != 0l) {
				strquery = QueryRetrieval.getSQLData(LicenseConstants.SELECTFEATUREINFOBYSERIALNUMBERANDFEATURECODE);
			}

			if (connection != null) {
				preparedStatement = connection.prepareStatement(strquery);
			}

			preparedStatement.setString(1, storedLicense.getSerialNbr());

			if (featureName != null) {
				preparedStatement.setString(2, featureName);
			} else if (featureCode != 0l) {
				preparedStatement.setLong(2, featureCode);
			}

			resultSet = preparedStatement.executeQuery();
			featureInfoList = new ArrayList<FeatureInfo>();

			while (resultSet.next()) {
				featureInfo = new FeatureInfo();
				featureInfo.setFeatureName(resultSet.getString(1));
				featureInfo.setFeatureCode(resultSet.getLong(2));
				featureInfo.setFeatureInfoSignature(resultSet.getBytes(3));
				featureInfo.setModifiedTime(resultSet.getTimestamp(4));
				featureInfoList.add(featureInfo);
			}

			storedLicense.setFeatureInfoList(featureInfoList);
			LicenseLogger.getInstance().finest(StoredLicenseUtil.class.getName(), "populateCompletStoredLicense",
					"FeatureInfo Data is Added to StoredLicense Object.");

			if (targetId == null) {
				strquery = QueryRetrieval.getSQLData(LicenseConstants.SELECTTARGETSYSTEMBYSERIALNUMBER);
			} else {
				strquery = QueryRetrieval.getSQLData(LicenseConstants.SELECTTARGETSYSTEMBYSERIALNUMBERANDTARGETID);
			}

			if (connection != null) {
				preparedStatement2 = connection.prepareStatement(strquery);
			}

			preparedStatement2.setString(1, storedLicense.getSerialNbr());

			if (targetId != null) {
				preparedStatement2.setString(2, targetId);
			}

			resultSet = preparedStatement2.executeQuery();
			targetSystemList = new ArrayList<TargetSystem>();

			while (resultSet.next()) {
				targetSystem = new TargetSystem();
				targetSystem.setTargetId(resultSet.getString(1));
				targetSystem.setTargetSystemSignature(resultSet.getBytes(2));
				targetSystem.setModifiedTime(resultSet.getTimestamp(3));
				targetSystemList.add(targetSystem);
			}

			storedLicense.setTargetIds(targetSystemList);
			LicenseLogger.getInstance().finest(StoredLicenseUtil.class.getName(), "populateCompletStoredLicense",
					"targetSystem Data is Added to StoredLicense Object.");
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(StoredLicenseUtil.class.getName(), "populateCompletStoredLicense",
					"Retrieving Data from Data Base is failed." + sqle.getMessage());

			throw sqle;
		} finally {
			try {
				ConnectionUtil.closeConnection(preparedStatement, null);
			} catch (final SQLException sqle) {
				LicenseLogger.getInstance().error(StoredLicenseUtil.class.getName(), "populateCompletStoredLicense",
						"Closing prepared statement failed." + sqle.getMessage());
			}

			ConnectionUtil.closeConnection(preparedStatement2, null);
		}

		return storedLicense;
	}
}
