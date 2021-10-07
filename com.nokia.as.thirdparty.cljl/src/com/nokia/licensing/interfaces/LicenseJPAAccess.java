///* ========================================== */
///* Copyright (c) 2009 Nokia                   */
///*          All rights reserved.              */
///*          Company Confidential              */
///* ========================================== */
package com.nokia.licensing.interfaces;

import java.util.List;

import com.nokia.licensing.dtos.License;


/**
 * This interface exposes method which could be used by the License Manager Application to interact with the database
 * directly. This interface is specially designed for License Management (LM) Application. LM Application will use this
 * interface directly.
 *
 */
public interface LicenseJPAAccess {

    /**
     * Method accepts the SQL Query as input from the License Management Application. It then internally uses the JPA
     * call to query against the database. The Results are then converted into StoredLicense objects and returned.
     *
     * @param query
     *            SQL Query to be executed against the database
     * @return List of License objects
     * @throws LicenseException
     */
    List<License> getLicenseData(String query) throws LicenseException;

    /**
     * Methods accepts JPQL (Java Persistence Query Language) query as input from the License Management Application. It
     * then internally uses the JPA call to query against the database. The Results are then converted into
     * StoredLicense objects and returned.
     *
     * @param query
     *            - JPQL Query which will be executed against the database
     * @return -- List of License Objects
     * @throws LicenseException
     */
    List<License> getLicenseDataUseJPQL(String query) throws LicenseException;
}
