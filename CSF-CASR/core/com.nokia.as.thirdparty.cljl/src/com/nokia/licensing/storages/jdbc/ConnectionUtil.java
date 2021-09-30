/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/* All rights reserved. */
/* Company Confidential */
/* ========================================== */
package com.nokia.licensing.storages.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nokia.licensing.interfaces.DataBasePlugin;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.plugins.PluginRegistry;


/**
 * This class is used to get the connection plug-in.
 *
 * @author ushan
 */
public class ConnectionUtil {

    private static final String className = ConnectionUtil.class.toString();

    /**
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static Connection getConnection() throws SQLException, ClassNotFoundException, LicenseException {

        Connection connection = null;
        final DataBasePlugin plugin = PluginRegistry.getRegistry().getPlugin(DataBasePlugin.class);
        connection = plugin.getConnection();

        LicenseLogger.getInstance().finest(className, "getConnection", "Connection is created.");

        return connection;
    }

    /**
     * Closing the connection between the java to Data Base
     *
     * @param pstmt
     *            -- PreparedStatement
     * @param conn
     *            -- Connection
     * @throws LicenseException
     */
    public static void closeConnection(final PreparedStatement statement, final Connection connection)
            throws SQLException {
        try {
            if (statement != null) {
                statement.close();
            }

            if (connection != null) {
                connection.close();
            }

            LicenseLogger.getInstance().finest(className, "closeConnection", "Connection is closed.");
        } catch (final SQLException sqle) {
            LicenseLogger.getInstance().error(className, "closeConnection", "Connection is not closed." + sqle);

            throw sqle;
        }
    }
}
