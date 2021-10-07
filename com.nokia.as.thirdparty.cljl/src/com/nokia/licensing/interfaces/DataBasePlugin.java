package com.nokia.licensing.interfaces;

import java.sql.Connection;


/**
 * Database plugin for storing the license details.
 */
public interface DataBasePlugin {

    Connection getConnection() throws LicenseException;
}
