/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nokia.licensing.storages.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;


/**
 *
 * @author twozniak
 */
public class JDBCBaseStorage {

	protected static final String ERROR_MESSAGE = "License system error: performed operation is failed.";

	/**
	 * Making the connection to the database.
	 *
	 * @return Connection -- data base connection object
	 * @throws LicenseException
	 */
	protected Connection getConnection() throws LicenseException {
		Connection connection = null;
		try {
			connection = ConnectionUtil.getConnection();
		} catch (final SQLException sqle) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "connectionhelp",
					"Failed to creating the connection." + sqle.getMessage());
			final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);
			licenseException.setErrorCode("CLJL109");
			LicenseLogger.getInstance().error(this.getClass().getName(), "connectionhelp",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		} catch (final ClassNotFoundException cnfe) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "connectionhelp",
					"Unable to open the ORACLE driver clas." + cnfe.getMessage());
			final LicenseException licenseException = new LicenseException(ERROR_MESSAGE);
			licenseException.setErrorCode("CLJL109");
			LicenseLogger.getInstance().error(this.getClass().getName(), "connectionhelp",
					"error code set to: " + licenseException.getErrorCode());
			throw licenseException;
		}
		return connection;
	}
}
