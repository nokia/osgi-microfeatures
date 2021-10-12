// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.agnosticImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import com.nokia.licensing.interfaces.CLJLPreferences;
import com.nokia.licensing.interfaces.CredentialAccess;
import com.nokia.licensing.interfaces.DataBasePlugin;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.plugins.PluginRegistry;


/**
 * This class uses JDBC driver manager to connect to the database.
 */
public class DataBasePluginImpl implements DataBasePlugin {

    private Preferences pref = null;
    private final Logger logger = Logger.getLogger(DataBasePluginImpl.class.getPackage().toString());
    private final String className = DataBasePluginImpl.class.toString();

    private Preferences loadProperties() {
        if (this.pref == null) {
            try {
                final CLJLPreferences cljlPreferences = PluginRegistry.getRegistry().getPlugin(CLJLPreferences.class);
                this.pref = cljlPreferences.getPreferencesSystemRoot();
            } catch (final LicenseException e) {
                this.logger.log(Level.SEVERE,
                        this.className + ".getPrefSystemRoot :: Failed to get preferences system node. " + e);
            }
        }
        return this.pref;
    }

    /**
     * Making the connection between the java to Data Base and gets the driver, URL, User Name and Password from the
     * Preference file
     *
     * @return Connection -- data base connection object
     * @throws LicenseException
     */
    @Override
    public Connection getConnection() throws LicenseException {
        this.logger.log(Level.INFO, this.className + ".getConnection :: Connection is creating...");
        final String driver = getDriver();
        final String url = getUrl();
        final String userName = getUsername();
        final String passWd = getPassword();
        try {
            Class.forName(driver);
            this.logger.log(Level.INFO, this.className + ".getConnection :: Opened the ORACLE driver class.");
        } catch (final ClassNotFoundException cnfe) {
            this.logger.log(Level.INFO,
                    this.className + ".getConnection :: Unable to open the ORACLE driver class. " + cnfe);
            throw new LicenseException(cnfe);
        }
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, userName, passWd);
            this.logger.log(Level.INFO, this.className + ".getConnection :: The connection is created.");
        } catch (final SQLException sqle) {
            this.logger.log(Level.INFO, this.className + ".getConnection :: Failed to creating the connection." + sqle);
            throw new LicenseException(sqle);
        }
        this.logger.log(Level.INFO, this.className + ".getConnection :: Connection is created.");
        return conn;
    }

    /**
     * Getting the Driver information form the Preference file to connect Data Base
     *
     * @return Sting -- Driver Information
     */
    private String getDriver() {
        final String driver = loadProperties().node("servicelocator").get("driverName",
                "oracle.jdbc.driver.OracleDriver");
        this.logger.log(Level.INFO, this.className + ".getDriver :: Driver Name is readed from preferences file.");
        return driver;
    }

    /**
     * Getting the URL information form the Preference file to connect Data Base
     *
     * @return Sting -- url Information
     */
    private String getUrl() {
        final String url = loadProperties().node("servicelocator").get("url", "jdbc:oracle:thin:@127.0.0.1:1521:OSS");
        this.logger.log(Level.INFO, this.className + ".getUrl :: URL data is readed from preferences file.");
        return url;
    }

    /**
     * Getting the User Name information form the Preference file to login into Data Base
     *
     * @return Sting -- User Name Information
     */
    private String getUsername() {
        this.logger.log(Level.INFO, this.className + ".getUsername :: Entered getUserName method");

        String licUserName = null;
        try {
            final CredentialAccess credAcc = PluginRegistry.getRegistry().getPlugin(CredentialAccess.class);

            final HashMap<String, String> credMap = credAcc.getCredentials();

            licUserName = credMap.get(CredentialAccess.USERNAME_KEY);
        } catch (final SecurityException e) {
            this.logger.log(Level.INFO, this.className + ".getUsername :: SecurityException:" + e.getMessage());
        } catch (final LicenseException e) {
            this.logger.log(Level.INFO, this.className + ".getUsername :: LicenseException:" + e.getMessage());
        }
        this.logger.log(Level.INFO, this.className + ".getUsername :: Returned getUserName method");

        return licUserName;
    }

    /**
     * Getting the Password information form the Preference file to login into Data Base
     *
     * @return Sting -- Password Information
     */
    private String getPassword() {

        this.logger.log(Level.INFO, this.className + ".getpassword :: Entered getpassword method");

        String licPassword = null;

        try {
            final CredentialAccess credAcc = PluginRegistry.getRegistry().getPlugin(CredentialAccess.class);

            final HashMap<String, String> credMap = credAcc.getCredentials();

            licPassword = credMap.get(CredentialAccess.PASSWORD_KEY);
        } catch (final SecurityException e) {
            this.logger.log(Level.INFO, this.className + ".getpassword :: SecurityException:" + e.getMessage());
        } catch (final LicenseException e) {
            this.logger.log(Level.INFO, this.className + ".getpassword :: LicenseException:" + e.getMessage());
        }

        this.logger.log(Level.INFO, this.className + ".getpassword :: Returned getpassword method");

        return licPassword;
    }
}
