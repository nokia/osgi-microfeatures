/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.storages.jpa;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import com.nokia.licensing.interfaces.CLJLPreferences;
import com.nokia.licensing.interfaces.CredentialAccess;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.plugins.PluginRegistry;


/**
 * This class is used to connect to the data base and to close the connection.
 *
 * @author Rama Manohar P
 * @version 1.0
 *
 */
public class ConnectionUtilJPA {

	private static final String defaultProvider = "org.hibernate.ejb.HibernatePersistence";
	private static Preferences pref = null;

	public static Preferences loadProperties() {
		if (pref == null) {
			try {
				final CLJLPreferences cljlPreferences = PluginRegistry.getRegistry().getPlugin(CLJLPreferences.class);
				pref = cljlPreferences.getPreferencesSystemRoot();
			} catch (final LicenseException e) {
				LicenseLogger.getInstance().error("com.nokia.licensing.utils.ConnectionUtil", "getPrefSystemRoot",
						"Failed to get preferences system node." + e.getMessage());
			}
		}

		return pref;
	}

	/**
	 * To get the Database information.Making the connection between the java to Data Base and gets the driver, URL,
	 * User Name and Password from the Persistence file
	 *
	 * @return EntityManager -- data base connection object
	 * @throws Exception
	 */
	public static EntityManager getConnection() throws LicenseException {
		LicenseLogger.getInstance().finest(ConnectionUtilJPA.class.getName(), "getConnection", "Connection is creating...");

		EntityManagerFactory entityManagerFactory = null;
		EntityManager entityManager = null;

		try {
			final String driver = getDriver();
			final String url = getUrl();
			final String userName = getUsername();
			final String passWd = getPassword();
			final String dialect = getDialect();
			final Map<String, String> map = new HashMap<String, String>();

			map.put("hibernate.connection.driver_class", driver);
			map.put("hibernate.connection.url", url);
			map.put("hibernate.connection.username", userName);
			map.put("hibernate.connection.password", passWd);
			map.put("hibernate.dialect", dialect);
			map.put("javax.persistence.provider", defaultProvider);

			// Connection is created here first it will get Map data for the connection
			// then rest of the connection details which are not overwrite by Map
			// are taken from the persistent unit named with 'license' in the persistence.XML file.
			entityManagerFactory = Persistence.createEntityManagerFactory("license", map);
			LicenseLogger.getInstance().finest(ConnectionUtilJPA.class.getName(), "getConnection",
					"The persistence-unit named with license is found.");
			entityManager = entityManagerFactory.createEntityManager();
			LicenseLogger.getInstance().finest(ConnectionUtilJPA.class.getName(), "getConnection",
					"The EntityManager factory is created.");
			LicenseLogger.getInstance().finest(ConnectionUtilJPA.class.getName(), "getConnection", "The connection is created.");
		} catch (final PersistenceException e) {
			LicenseLogger.getInstance().error(ConnectionUtilJPA.class.getName(), "getConnection",
					"Failed to creating the connection." + e.getMessage());
			throw e;
		}
		return entityManager;
	}

	/**
	 * Closing the connection between the java to Data Base
	 *
	 * @param pstmt
	 *            -- PreparedStatement
	 * @param conn
	 *            -- Connection
	 * @throws Exception
	 */
	public static void closeConnection(final EntityManager entityManager) throws Exception {
		try {
			if (null != entityManager) {
				entityManager.close();
				LicenseLogger.getInstance().finest(ConnectionUtilJPA.class.getName(), "closeConnection",
						"Data Base Connection is closed.");
			}
		} catch (final Exception e) {
			LicenseLogger.getInstance().error(ConnectionUtilJPA.class.getName(), "closeConnection",
					"Data Base Connection is not closed." + e.getMessage());

			throw e;
		}
	}

	/**
	 * Getting the Driver information form the Preference file to connect Data Base
	 *
	 * @return Sting -- Driver Information
	 */
	public static String getDriver() {
		final String driver = ConnectionUtilJPA.loadProperties().node("servicelocator").get("driverName",
				"oracle.jdbc.driver.OracleDriver");
		LicenseLogger.getInstance().finest(ConnectionUtilJPA.class.getName(), "getDriver",
				"Driver Name is readed from preferences file.");

		return driver;
	}

	/**
	 * Getting the URL information form the Preference file to connect Data Base
	 *
	 * @return Sting -- url Information
	 */
	public static String getUrl() {
		final String url = ConnectionUtilJPA.loadProperties().node("servicelocator").get("url",
				"jdbc:oracle:thin:@127.0.0.1:1521:OSS");
		LicenseLogger.getInstance().finest(ConnectionUtilJPA.class.getName(), "getUrl", "URL data is readed from preferences file.");
		return url;
	}

	/**
	 * Getting the User Name information form the Preference file to login into Data Base
	 *
	 * @return Sting -- User Name Information
	 * @throws IOException
	 * @throws LicenseException
	 */
	public static String getUsername() throws LicenseException {
		LicenseLogger.getInstance().finest(ConnectionUtilJPA.class.getName(), "getUsername", " Entered getUserName method");
		String licUserName = null;

		try {
			CredentialAccess credAcc = null;

			credAcc = PluginRegistry.getRegistry().getPlugin(CredentialAccess.class);

			final HashMap<String, String> credMap = credAcc.getCredentials();

			licUserName = credMap.get(CredentialAccess.USERNAME_KEY);
		} catch (final SecurityException e) {
			LicenseLogger.getInstance().error(ConnectionUtilJPA.class.getName(), "getUsername",
					"SecurityException:" + e.getMessage());
			final LicenseException ex = new LicenseException(" Security Exception.");
			ex.setErrorCode("CLJL127");
			LicenseLogger.getInstance().error(ConnectionUtilJPA.class.getName(), "getUsername",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		}

		LicenseLogger.getInstance().finest(ConnectionUtilJPA.class.getName(), "getUsername", " Returned getUserName method");

		return licUserName;
	}

	/**
	 * Getting the Password information form the Preference file to login into Data Base
	 *
	 * @return Sting -- Password Information
	 * @throws IOException
	 * @throws LicenseException
	 */
	public static String getPassword() throws LicenseException {
		LicenseLogger.getInstance().finest(ConnectionUtilJPA.class.getName(), "getpassword", " Entered getPassword method");

		String licPassword = null;

		try {
			final CredentialAccess credAcc = PluginRegistry.getRegistry().getPlugin(CredentialAccess.class);
			final HashMap<String, String> credMap = credAcc.getCredentials();

			licPassword = credMap.get(CredentialAccess.PASSWORD_KEY);
		} catch (final SecurityException e) {
			LicenseLogger.getInstance().error(ConnectionUtilJPA.class.getName(), "getpassword",
					"SecurityException:" + e.getMessage());
			throw new LicenseException("SecurityException");
		}
		LicenseLogger.getInstance().finest(ConnectionUtilJPA.class.getName(), "getPassword", " Returned getpassword method");

		return licPassword;
	}

	/**
	 * Getting the dialect information form the Preference file to connect Data Base
	 *
	 * @return Sting -- dialect Information
	 */
	public static String getDialect() {
		final String url = ConnectionUtilJPA.loadProperties().node("servicelocator").get("dialect",
				"org.hibernate.dialect.OracleDialect");
		LicenseLogger.getInstance().finest(ConnectionUtilJPA.class.getName(), "getDialect",
				"Dialect data is readed from preferences file.");

		return url;
	}
}
