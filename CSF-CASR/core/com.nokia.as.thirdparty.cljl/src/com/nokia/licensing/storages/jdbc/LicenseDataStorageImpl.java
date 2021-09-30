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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nokia.licensing.dtos.FeatureInfo;
import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.dtos.TargetSystem;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.querycache.DataBaseUtil;
import com.nokia.licensing.querycache.QueryRetrieval;
import com.nokia.licensing.utils.LicenseConstants;


/**
 * This class is the implementation for LicenseDataStorage interface.
 *
 * @author Rama Manohar P
 * @version 1.0
 *
 */
public class LicenseDataStorageImpl extends JDBCBaseStorage implements LicenseDataStorage {
	private static final String FCODES_pattern = "#fcodes#";
	private static final String FEATURECODE_QUALIFIED_NAME = "fi.featurecode";
	private static final String ITERATE_RESULT_SET = "iterateResultSet";
	private static final String DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss";
	private static final int ORA_01795_MAXIMUM_NUMBER_OF_EXPRESSION_IN_LIST = 990;

	/**
	 * This method is for Making the connection to Data Base and then building the prepare statement and executing it to
	 * insert data into the stored license, feature info and target system tables.
	 *
	 * @param queryId
	 *            -- Contains the query id
	 * @param storedLicenseInsert
	 *            -- Containing License details
	 * @param temp
	 *            -- Containing Feature Info or Target System details or may be Null value
	 * @throws LicenseException
	 */
	protected void buildPrepareStatement(final String queryId, final StoredLicense storedLicenseInsert,
			final Object temp) throws LicenseException {
		String strquery = null;
		Connection connection = null;
		PreparedStatement preparedStatement = null;

		LicenseLogger.getInstance().finest(this.getClass().getName(), "buildPrepareStatement", "Building preparestatement...");
		strquery = QueryRetrieval.getSQLData(queryId);
		connection = getConnection();
		if (connection != null) {
			try {
				preparedStatement = connection.prepareStatement(strquery);

				if (temp == null) {
					preparedStatement = DataBaseUtil.setStoredLicenseToPrepareStmt(preparedStatement,
							storedLicenseInsert);
					LicenseGenericDataAccess.setEncryptKey(connection, storedLicenseInsert.getSerialNbr(),
							storedLicenseInsert.getKey());
				} else if (temp instanceof FeatureInfo) {
					preparedStatement = DataBaseUtil.setFeatureInfoToPrepareStmt(preparedStatement, (FeatureInfo) temp,
							storedLicenseInsert);
				} else if (temp instanceof TargetSystem) {
					preparedStatement = DataBaseUtil.setTargetSystemToPrepareStmt(preparedStatement,
							(TargetSystem) temp, storedLicenseInsert);
				}
				preparedStatement.executeUpdate();
				LicenseLogger.getInstance().finest(this.getClass().getName(), "buildPrepareStatement",
						"Build preparestatement executed.");
			} catch (final SQLException sqle) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "buildPrepareStatement",
						"Data insertion is failed." + sqle.getMessage());
				final LicenseException licenseException = new LicenseException(
						ERROR_MESSAGE + " : " + sqle.getMessage());
				licenseException.setErrorCode("CLJL112");
				LicenseLogger.getInstance().error(this.getClass().getName(), "buildPrepareStatement",
						"error code set to: " + licenseException.getErrorCode(), sqle);
				throw licenseException;
			} catch (final ClassNotFoundException cnfe) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "buildPrepareStatement",
						"Getting the conetion is failed." + cnfe.getMessage());
				final LicenseException licenseException = new LicenseException(
						ERROR_MESSAGE + " : " + cnfe.getMessage());
				licenseException.setErrorCode("CLJL109");
				LicenseLogger.getInstance().error(this.getClass().getName(), "buildPrepareStatement",
						"error code set to: " + licenseException.getErrorCode(), cnfe);
				throw licenseException;
			} finally {
				closeConnection("buildPrepareStatement", preparedStatement, connection);
			}
		}
	}

	/**
	 * This method is for making the connection to the Data Base and then building the prepare statement and executing
	 * the query to get the data from Data Base.
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
	 * @return Result set
	 * @throws LicenseException
	 */
	private List<StoredLicense> iterateResultSet(final String queryID, final Object data, final long data2,
			final String data3, final boolean checkDataIntegrity) throws LicenseException {
		ResultSet resultSet = null;
		String strquery = null;
		PreparedStatement preparedStatement = null;
		StoredLicense storedLicense = null;
		List<StoredLicense> resultListForStoredLicense = null;
		Connection connection = null;
		String featureName = null;
		long featureCode = 0l;
		String targetId = null;
		byte[] key = null;

		final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STRING);

		LicenseLogger.getInstance().finest(this.getClass().getName(), ITERATE_RESULT_SET,
				"Getting the connection,making the statement and executing the statement...");
		try {
			connection = getConnection();
			strquery = QueryRetrieval.getSQLData(queryID);

			if (connection != null) {
				preparedStatement = connection.prepareStatement(strquery);
			}
			if (data instanceof String) {
				preparedStatement.setString(1, (String) data);
			} else if (data instanceof Integer) {
				preparedStatement.setInt(1, (Integer) data);
			} else if (data instanceof Long) {
				preparedStatement.setLong(1, (Long) data);
			} else if (data instanceof Date) {
				final String dateFormat = sdf.format(data);
				preparedStatement.setString(1, dateFormat);
				preparedStatement.setString(2, dateFormat);
				preparedStatement.setLong(3, data2);
				preparedStatement.setString(4, data3);
			}
			resultSet = preparedStatement.executeQuery();
			LicenseLogger.getInstance().finest(this.getClass().getName(), ITERATE_RESULT_SET,
					"Retrieving Data from Data Base is completed.");

			LicenseLogger.getInstance().finest(this.getClass().getName(), "convertResultSetToList",
					"Setting data to List<StoredLicense> from result set...");

			// converting the result set to stored license object and populating
			// the complete stored license object

			resultListForStoredLicense = new ArrayList<StoredLicense>();
			while (resultSet.next()) {
				storedLicense = new StoredLicense();
				storedLicense = DataBaseUtil.convertResultSetToStoredLicense(resultSet);

				if ((queryID == "selectLicensesByFeatureName") || (queryID == "selectLicenseByFeatureCode")
						|| (queryID == "selectLicensesByTargetID")
						|| (queryID == "selectLicensesByFeatureCodeAndTargetIdAndTime")) {
					if (data instanceof String) {
						if (queryID == "selectLicensesByFeatureName") {
							featureName = (String) data;
						} else if (queryID == "selectLicensesByTargetID") {
							targetId = (String) data;
						}
					} else if (data instanceof Long) {
						featureCode = (Long) data;
					} else if (data instanceof java.util.Date) {
						featureCode = data2;
						targetId = data3;
					}
				}
				storedLicense = StoredLicenseUtil.populateCompletStoredLicense(storedLicense, connection, featureName,
						featureCode, targetId);
				try {
					key = LicenseGenericDataAccess.getEncryptKey(connection, storedLicense.getSerialNbr());
					LicenseLogger.getInstance().finest(this.getClass().getName(), "convertResultSetToList",
							"Getting License Key data from the Data Base is completed.");
				} catch (final Exception e) {
					LicenseLogger.getInstance().error(this.getClass().getName(), "convertResultSetToList",
							"Getting License Key data from the Data Base is failed." + e.getMessage());
					final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);
					licenseException.setErrorCode("CLJL111");
					LicenseLogger.getInstance().error(this.getClass().getName(), ITERATE_RESULT_SET,
							"error code set to: " + licenseException.getErrorCode(), e);
					throw licenseException;
				}
				storedLicense.setKey(key);
				resultListForStoredLicense.add(storedLicense);
			}
			LicenseLogger.getInstance().finest(this.getClass().getName(), "convertResultSetToList",
					"Setting data to List<StoredLicense> from result set is completed.");
		} catch (final SQLException sqle) {
			handleSQLException(sqle);
		} finally {
			closeConnection(ITERATE_RESULT_SET, preparedStatement, connection);
		}
		LicenseLogger.getInstance().finest(this.getClass().getName(), ITERATE_RESULT_SET,
				"Size of results for stored license=" + resultListForStoredLicense.size());
		return resultListForStoredLicense;
	}

	private void handleSQLException(final SQLException sqle) throws LicenseException {
		LicenseLogger.getInstance().error(this.getClass().getName(), ITERATE_RESULT_SET,
				"Retrieving Data from Data Base is failed." + sqle.getMessage());
		final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);
		licenseException.setErrorCode("CLJL111");
		LicenseLogger.getInstance().error(this.getClass().getName(), ITERATE_RESULT_SET,
				"error code set to: " + licenseException.getErrorCode(), sqle);
		throw licenseException;
	}

	private void closeConnection(final String methodName, final PreparedStatement preparedStatement,
			final Connection connection) throws LicenseException {
		try {
			ConnectionUtil.closeConnection(preparedStatement, connection);
			LicenseLogger.getInstance().finest(this.getClass().getName(), methodName, "Connection is closed.");
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(this.getClass().getName(), methodName,
					"Connection close is failed." + sqle.getMessage());
			final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);
			licenseException.setErrorCode("CLJL109");
			LicenseLogger.getInstance().error(this.getClass().getName(), methodName,
					"error code set to: " + licenseException.getErrorCode(), sqle);
			throw licenseException;
		}
	}

	/**
	 * This method is for making the connection to the Data Base and then creating the prepare statement and executing
	 * the query to delete the data from the Data Base.
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
		Connection connection = null;
		PreparedStatement preparedStatement = null, preparedStatement2 = null, preparedStatement3 = null;
		ResultSet resultSet = null;
		String strquery = null, strquery2 = null;
		int countDelSL = 0, countDelLK = 0;
		int countFI = 0;
		try {
			connection = getConnection();
			strquery = QueryRetrieval.getSQLData(queryID);
			if (connection != null) {
				preparedStatement = connection.prepareStatement(strquery);
			}
			if (featureCode != 0l) {
				strquery2 = QueryRetrieval.getSQLData(LicenseConstants.SELECTFEATUREINFOBYSERIALNUMBER);
				if (connection != null) {
					preparedStatement2 = connection.prepareStatement(strquery2);
				}
				preparedStatement2.setString(1, serialNbr);
				resultSet = preparedStatement2.executeQuery();

				while (resultSet.next()) {
					countFI++;
				}
				if (countFI == 1) {
					strquery = QueryRetrieval.getSQLData(LicenseConstants.DELETELICENSEBASEDONSERIALNUMBER);
					preparedStatement = connection.prepareStatement(strquery);
					// featureCode = 0l;
				} else {
					if (connection != null) {
						preparedStatement = connection.prepareStatement(strquery);
						preparedStatement.setLong(2, featureCode);
					}
				}
				countDelLK = 1;
			}
			preparedStatement.setString(1, serialNbr);
			preparedStatement.execute();
			countDelSL = preparedStatement.getUpdateCount();

			LicenseLogger.getInstance().finest(this.getClass().getName(), "deleteLicenseUtil",
					"License Data is deleted from the Data Base.");
			if (featureCode == 0l) {
				strquery = QueryRetrieval.getSQLData(LicenseConstants.DELETELICENSEKEYBYSERALNUMBER);
				if (connection != null) {
					preparedStatement3 = connection.prepareStatement(strquery);
				}
				preparedStatement3.setString(1, serialNbr);

				preparedStatement3.execute();
				countDelLK = preparedStatement3.getUpdateCount();
				LicenseLogger.getInstance().finest(this.getClass().getName(), "deleteLicenseUtil",
						"Data is deleted from LicenseKey Data Base based on serialnumber is completed.");
			}
		} catch (final Exception e) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseUtil",
					"Deleting Data from Data Base based on serialnumber is failed." + e.getMessage());
			final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);
			licenseException.setErrorCode("CLJL113");
			LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseUtil",
					"error code set to: " + licenseException.getErrorCode(), e);
			throw licenseException;
		} finally {
			try {
				try {
					ConnectionUtil.closeConnection(preparedStatement, null);
				} catch (final SQLException e) {
					LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseUtil",
							"Closing preparedStatement failed.");
				}

				try {
					ConnectionUtil.closeConnection(preparedStatement2, null);
				} catch (final SQLException e) {
					LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseUtil",
							"Closing preparedStatement2 failed.");
				}

				ConnectionUtil.closeConnection(preparedStatement3, connection);

				LicenseLogger.getInstance().finest(this.getClass().getName(), "deleteLicenseUtil", "Connection is closed.");
			} catch (final Exception e) {
				LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseUtil",
						"Connection close is failed." + e.getMessage());
				final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);
				licenseException.setErrorCode("CLJL109");
				LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseUtil",
						"error code set to: " + licenseException.getErrorCode(), e);
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
	 * @param List
	 *            <StoredLicense> List containing License details
	 * @throws LicenseException
	 */
	@Override
	public void insertLicenseInformation(final List<StoredLicense> licenseFileList) throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "insertLicenseInformation", "Inserting Data into Data Base...");

		StoredLicense storedLicenseInsert = null;
		FeatureInfo featureInfo = null;
		TargetSystem targetSystem = null;
		Iterator<StoredLicense> iterator = null;
		Iterator<FeatureInfo> iteratorForFeatureInfo = null;
		Iterator<TargetSystem> iteratorForTargetSystem = null;

		iterator = licenseFileList.iterator();
		while (iterator.hasNext()) {
			storedLicenseInsert = iterator.next();
			buildPrepareStatement(LicenseConstants.INSERTLICENSE, storedLicenseInsert, null);

			LicenseLogger.getInstance().finest(this.getClass().getName(), "insertLicenseInformation",
					"Data is inserted into StoredLicense Data Base is completed.");

			iteratorForFeatureInfo = storedLicenseInsert.getFeatureInfoList().iterator();
			while (iteratorForFeatureInfo.hasNext()) {
				featureInfo = iteratorForFeatureInfo.next();
				buildPrepareStatement(LicenseConstants.INSERTFEATUREINFO, storedLicenseInsert, featureInfo);
			}
			LicenseLogger.getInstance().finest(this.getClass().getName(), "insertLicenseInformation",
					"Data is inserted into FeatureInfo Data Base is completed.");
			if (!storedLicenseInsert.getTargetIds().isEmpty()) {
				iteratorForTargetSystem = storedLicenseInsert.getTargetIds().iterator();
				while (iteratorForTargetSystem.hasNext()) {
					targetSystem = iteratorForTargetSystem.next();
					buildPrepareStatement(LicenseConstants.INSERTTARGETSSYSTEM, storedLicenseInsert, targetSystem);
				}

				LicenseLogger.getInstance().finest(this.getClass().getName(), "insertLicenseInformation",
						"Data is inserted into TargetSystem Data Base is completed.");
			}
		}
	}

	/**
	 * Deletes the License data from the underlying repository based on the Serial Number. Serial Number being the
	 * Primary Key of the License will delete only one entry from the Repository.
	 *
	 * @param license
	 *            -- Serial Number of the License
	 * @return boolean -- returns true is deletion is successful otherwise false
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
			final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);
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
	 * @param featureCode
	 * @return boolean -- returns true is deletion is successful otherwise false
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
					"Data is deleted from Data Base based on serialnumber and featurecode is completed.");
		} else {
			LicenseLogger.getInstance().error(this.getClass().getName(), "deleteLicenseBySerialNumber",
					"Deleting Data from Data Base based on serialnumber and featurecode is failed.");
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
					"Retrieving Data from Data Base based on serialnumber is completed.");
		} catch (final LicenseException sqle) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicenseBySerialNo",
					"Retrieving Data from Data Base based on serialnumber is failed." + sqle.getMessage());
			final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);
			licenseException.setErrorCode("CLJL111");
			LicenseLogger.getInstance().error(this.getClass().getName(), "getLicenseBySerialNo",
					"error code set to: " + licenseException.getErrorCode(), sqle);
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

		// ResultSet resultSet = null;

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
			final Date time, final boolean checkDataIntegrity) throws LicenseException {
		final Set<Long> featureCodes = new HashSet<Long>();
		featureCodes.add(featureCode);
		List<StoredLicense> storedLicense = getLicensesByFeatureCodeAndTargetIdAndTime(featureCodes, targetId, time)
				.get(featureCode);
		if (storedLicense == null) {
			storedLicense = new ArrayList<StoredLicense>();
		}
		return storedLicense;
	}

	@Override
	public Map<Long, List<StoredLicense>> getLicensesByFeatureCodeAndTargetIdAndTime(final Set<Long> featureCodes,
			final String targetId, final Date time) throws LicenseException {
		return retriveLicenseDataFromDb(time, featureCodes, targetId);
	}

	private Map<Long, List<StoredLicense>> retriveLicenseDataFromDb(final Date time, final Set<Long> featureCodes,
			final String targetId) throws LicenseException {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		final Map<Long, List<StoredLicense>> storedLicenseMap = new HashMap<Long, List<StoredLicense>>();

		LicenseLogger.getInstance().finer(this.getClass().getName(), "retriveLicenseDataFromDb",
				"feature codes number: " + featureCodes.size());
		try {
			connection = getConnection();
			String strquery;
			if (targetId != null) {
				strquery = QueryRetrieval
						.getSQLData(LicenseConstants.SELECTLICENSESBY_MANY_FEATURECODEANDTARGETIDANDTIME_OPTIMIZED);
			} else {
				strquery = QueryRetrieval.getSQLData(
						LicenseConstants.SELECTLICENSESBY_MANY_FEATURECODEANDTARGETIDANDTIME_OPTIMIZED_WITHOUT_TARGET);
			}

			strquery = addFeatureCodesToSqlQuery(strquery, featureCodes);
			LicenseLogger.getInstance().finer(this.getClass().getName(), "retriveLicenseDataFromDb", "QUERY: " + strquery);
			preparedStatement = connection.prepareStatement(strquery);

			final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STRING);
			final String dateFormat = sdf.format(time);
			int index = 1;
			preparedStatement.setString(index++, dateFormat);
			preparedStatement.setString(index++, dateFormat);
			if ((featureCodes != null) && !featureCodes.isEmpty()) {
				for (final Long fc : featureCodes) {
					preparedStatement.setLong(index++, fc);
				}
			}
			if (targetId != null) {
				preparedStatement.setString(index++, targetId);
			}

			final ResultSet resultSet = preparedStatement.executeQuery();
			LicenseLogger.getInstance().finer(this.getClass().getName(), "retriveLicenseDataFromDb",
					"Retrieving Data from Data Base is completed.");

			while (resultSet.next()) {
				final StoredLicense storedLicense = getStoredLicense(resultSet);
				final Long fc = storedLicense.getFeatureInfoList().get(0).getFeatureCode();
				if (storedLicenseMap.containsKey(fc)) {
					storedLicenseMap.get(fc).add(storedLicense);
				} else {
					final List<StoredLicense> storedLicenseList = new ArrayList<StoredLicense>();
					storedLicenseList.add(storedLicense);
					storedLicenseMap.put(fc, storedLicenseList);
				}
			}
		} catch (final SQLException sqle) {
			handleSQLException(sqle);
		} finally {
			closeConnection("retriveLicenseDataFromDb", preparedStatement, connection);
		}

		return storedLicenseMap;
	}

	private String addFeatureCodesToSqlQuery(final String strquery, final Set<Long> featureCodes) {
		final StringBuilder fcCriteria = new StringBuilder();
		if ((featureCodes != null) && !featureCodes.isEmpty()) {
			fcCriteria.append(InQueryBuilder.buildQuery(FEATURECODE_QUALIFIED_NAME, featureCodes.size(),
					ORA_01795_MAXIMUM_NUMBER_OF_EXPRESSION_IN_LIST));
		}
		return strquery.replaceAll(FCODES_pattern, fcCriteria.toString());
	}

	private StoredLicense getStoredLicense(final ResultSet resultSet) throws SQLException {
		final StoredLicense storedLicense = DataBaseUtil.convertResultSetToStoredLicense(resultSet);
		addFeatureInfo(storedLicense, resultSet);
		addLicensKey(storedLicense, resultSet);
		addTargetSystem(storedLicense, resultSet);
		return storedLicense;
	}

	private void addFeatureInfo(final StoredLicense storedLicense, final ResultSet resultSet) throws SQLException {
		final FeatureInfo featureInfo = new FeatureInfo();
		featureInfo.setFeatureName(resultSet.getString(26));
		featureInfo.setFeatureCode(resultSet.getLong(27));
		featureInfo.setFeatureInfoSignature(resultSet.getBytes(28));
		featureInfo.setModifiedTime(resultSet.getTimestamp(29));
		final List<FeatureInfo> featureInfoList = new ArrayList<FeatureInfo>();
		featureInfoList.add(featureInfo);
		storedLicense.setFeatureInfoList(featureInfoList);
	}

	private void addLicensKey(final StoredLicense storedLicense, final ResultSet resultSet) throws SQLException {
		storedLicense.setKey(resultSet.getBytes(30));
	}

	private void addTargetSystem(final StoredLicense storedLicense, final ResultSet resultSet) throws SQLException {
		final TargetSystem targetSystem = new TargetSystem();
		targetSystem.setTargetId(resultSet.getString(31));
		targetSystem.setTargetSystemSignature(resultSet.getBytes(32));
		targetSystem.setModifiedTime(resultSet.getTimestamp(33));
		final List<TargetSystem> targetList = new ArrayList<TargetSystem>();
		targetList.add(targetSystem);
		storedLicense.setTargetIds(targetList);
	}

	@Override
	public List<StoredLicense> getLicenseChanges(final Date startTime, final Date endTime) throws LicenseException {
		return null;
	}

	@Override
	public List<StoredLicense> getExpiredLicenses(final Date startTime, final Date endTime) throws LicenseException {
		final List<StoredLicense> licenseList = iterateResultSet(LicenseConstants.SELECTLICENSEGETTINGEXPIRED,
				startTime, endTime);
		return licenseList;
	}

	private List<StoredLicense> iterateResultSet(final String queryID, final Date startTime, final Date endTime)
			throws LicenseException {
		ResultSet resultSet = null;
		String strquery = null;
		PreparedStatement preparedStatement = null;
		StoredLicense storedLicense = null;
		List<StoredLicense> resultListForStoredLicense = null;
		Connection connection = null;
		final String featureName = null;
		final long featureCode = 0l;
		final String targetId = null;
		byte[] key = null;

		final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STRING);

		LicenseLogger.getInstance().finest(this.getClass().getName(), ITERATE_RESULT_SET,
				"Getting the connection,making the statement and executing the statement...");
		try {
			connection = getConnection();
			strquery = QueryRetrieval.getSQLData(queryID);
			if (connection != null) {
				preparedStatement = connection.prepareStatement(strquery);
			}
			final String dateFormat1 = sdf.format(startTime);
			preparedStatement.setString(1, dateFormat1);
			final String dateFormat2 = sdf.format(endTime);
			preparedStatement.setString(2, dateFormat2);

			resultSet = preparedStatement.executeQuery();
			LicenseLogger.getInstance().finest(this.getClass().getName(), ITERATE_RESULT_SET,
					"Retrieving Data from Data Base is completed.");
			LicenseLogger.getInstance().finest(this.getClass().getName(), "convertResultSetToList",
					"Setting data to List<StoredLicense> from result set...");

			// converting the result set to stored license object and populating
			// the complete stored license object

			resultListForStoredLicense = new ArrayList<StoredLicense>();
			while (resultSet.next()) {
				storedLicense = new StoredLicense();
				storedLicense = DataBaseUtil.convertResultSetToStoredLicense(resultSet);
				storedLicense = StoredLicenseUtil.populateCompletStoredLicense(storedLicense, connection, featureName,
						featureCode, targetId);

				LicenseLogger.getInstance().finest(StoredLicenseUtil.class.getName(), "populateCompletStoredLicense",
						"targetSystem Data is Added to StoredLicense Object.");

				key = LicenseGenericDataAccess.getEncryptKey(connection, storedLicense.getSerialNbr());
				LicenseLogger.getInstance().finest(this.getClass().getName(), "convertResultSetToList",
						"Getting License Key data from the Data Base is completed.");
				storedLicense.setKey(key);
				resultListForStoredLicense.add(storedLicense);
			}
			LicenseLogger.getInstance().finest(this.getClass().getName(), "convertResultSetToList",
					"Setting data to List<StoredLicense> from result set is completed.");
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(this.getClass().getName(), ITERATE_RESULT_SET,
					"Retrieving Data from Data Base is failed." + sqle.getMessage());
			final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);
			licenseException.setErrorCode("CLJL111");
			LicenseLogger.getInstance().error(this.getClass().getName(), ITERATE_RESULT_SET,
					"error code set to: " + licenseException.getErrorCode(), sqle);
			throw licenseException;
		} catch (final ClassNotFoundException e) {
			LicenseLogger.getInstance().error(this.getClass().getName(), ITERATE_RESULT_SET, "Getting encrypted key is failed.", e);
		} finally {
			closeConnection(ITERATE_RESULT_SET, preparedStatement, connection);
		}
		return resultListForStoredLicense;
	}

	public List<StoredLicense> getActivatedLicenses(final Date startTime, final Date endTime) {
		return null;
	}

	public List<LicenseCancelInfo> getCanceledLicense(final Date startTime, final Date endTime)
			throws LicenseException {
		return null;
	}

	@Override
	public List<StoredLicense> getModifiedLicenses(final Date startTime, final Date endTime) throws LicenseException {
		return null;
	}
}
