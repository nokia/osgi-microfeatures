///* ========================================== */
///* Copyright (c) 2009 Nokia                   */
///*          All rights reserved.              */
///*          Company Confidential              */
///* ========================================== */
package com.nokia.licensing.interfaces;

import java.util.Date;
import java.util.List;

import com.nokia.licensing.dtos.License;


/**
 * License Access interface exposes methods which assist in fetching license details. There are variety of methods
 * available to get the License Information. This information will be used by Applications like License Management
 * Application to control Licenses. The implementation of this interfaces internally uses the Data Storage Interface
 * methods to access License Information from the Data Storage Repository
 *
 * @version 1.0
 */
public interface LicenseAccess {

    /**
     * Accesses License Information based on the Serial Number Returns the License Information wrapped in License
     * object. Interacts with the Storage Interfaces to fetch the License data The second argument tells whether data
     * integrity checks against data storage needs to be performed or not. TRUE -- Perform Integrity check, FALSE - Do
     * not Perform Integrity check
     *
     * @param serialNbr
     *            - License Serial Number
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return License - License Information
     * @throws LicenseException
     */
    License getLicenseBySerialNo(String serialNbr, boolean checkDataIntegrity) throws LicenseException;

    /**
     * Fetches a list of Licenses for a given Order ID The LicenseFileList object which is returned contains a list of
     * License objects. This method interacts with the DataStorage Interface to fetch a list of licenses. The second
     * argument tells whether data integrity checks against data storage needs to be performed or not. TRUE -- Perform
     * Integrity check, FALSE - Do not Perform Integrity check
     *
     * @param orderID
     *            -- Order Id
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return List<License> List of Licenses associated with the Order Id
     * @throws LicenseException
     */
    List<License> getLicensesForOrderID(String orderID, boolean checkDataIntegrity) throws LicenseException;

    /**
     * Fetches a list of Licenses for a given Customer ID The LicenseFileList object which is returned contains a list
     * of License objects This method interacts with the DataStorage Interface to fetch a list of licenses. The second
     * argument tells whether data integrity checks against data storage needs to be performed or not. TRUE -- Perform
     * Integrity check, FALSE - Do not Perform Integrity check
     *
     * @param customerID
     *            -- Customer ID of the License
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return List<License> -- List of Licenses associated with the Customer
     * @throws LicenseException
     */
    List<License> getLicensesForCustomerID(String customerID, boolean checkDataIntegrity) throws LicenseException;

    /**
     * Fetches a list of Licenses for a given Customer ID The List<License> object which is returned contains a list of
     * License objects. This method interacts with the DataStorage Interface to fetch a list of licenses. The second
     * argument tells whether data integrity checks against data storage needs to be performed or not. TRUE -- Perform
     * Integrity check, FALSE - Do not Perform Integrity check
     *
     * @param customerName
     *            -- Name of the Customer
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return List<License> -- List of Licenses associated with the Customer
     * @throws LicenseException
     */
    List<License> getLicensesForCustomerName(String customerName, boolean checkDataIntegrity) throws LicenseException;

    /**
     * Fetches a list of Licenses for a given Target The List<License> object which is returned contains a list of
     * License objects. This method interacts with the DataStorage Interface to fetch a list of licenses. The second
     * argument tells whether data integrity checks against data storage needs to be performed or not. TRUE -- Perform
     * Integrity check, FALSE - Do not Perform Integrity check
     *
     * @param targetID
     *            -- Target ID of the Licensing system
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return List<License> -- List of Licenses for a given Target ID
     * @throws LicenseException
     */
    List<License> getLicensesForTargetID(String targetID, boolean checkDataIntegrity) throws LicenseException;

    /**
     * Fetches a list of Licenses for a given License Type The List<License> object which is returned contains a list of
     * License objects This method interacts with the DataStorage Interface to fetch a list of licenses. The second
     * argument tells whether data integrity checks against data storage needs to be performed or not. TRUE -- Perform
     * Integrity check, FALSE - Do not Perform Integrity check
     *
     * @param licenseType
     *            -- License Type
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return List<License> -- List of Licenses for a given License Type
     * @throws LicenseException
     */
    List<License> getLicensesForLicenseType(String licenseType, boolean checkDataIntegrity) throws LicenseException;

    /**
     * Fetches a list of Licenses for a given Usage Type The List<License> object which is returned contains a list of
     * License objects This method interacts with the DataStorage Interface to fetch a list of licenses. The second
     * argument tells whether data integrity checks against data storage needs to be performed or not. TRUE -- Perform
     * Integrity check, FALSE - Do not Perform Integrity check
     *
     * @param usageType
     *            -- Usage Type. Purpose of License File
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return List<License> -- List of Licenses for a given Usage Type
     * @throws LicenseException
     */
    List<License> getLicensesForUsageType(String usageType, boolean checkDataIntegrity) throws LicenseException;

    /**
     * Fetches a list of Licenses for a given Feature Code The List<License> object which is returned contains a list of
     * License objects This method interacts with the DataStorage Interface to fetch a list of licenses. The second
     * argument tells whether data integrity checks against data storage needs to be performed or not. TRUE -- Perform
     * Integrity check, FALSE - Do not Perform Integrity check
     *
     * @param featureCode
     *            -- Feature Code associated with License
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return List<License> -- List of Licenses for a given Feature code
     * @throws LicenseException
     */
    List<License> getLicensesForFeatureCode(long featureCode, boolean checkDataIntegrity) throws LicenseException;

    /**
     * Fetches all the Licenses Available in the system. The List<License> object which is returned contains a list of
     * License objects which hold. This method interacts with the DataStorage Interface to fetch a list of licenses. The
     * checkDataIntegrity argument tells whether data integrity checks against data storage needs to be performed or
     * not. TRUE -- Perform Integrity check, FALSE - Do not Perform Integrity check
     *
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return List<License> -- List of Licenses available in the System
     * @throws LicenseException
     */
    List<License> getAllLicenses(boolean checkDataIntegrity) throws LicenseException;

    /**
     * Method gets list of Licenses that are currently valid on this system for the requested feature. The fourth
     * argument tells whether data integrity checks against data storage needs to be performed or not. TRUE -- Perform
     * Integrity check, FALSE - Do not Perform Integrity check
     *
     * @param featureCode
     *            -- Feature Code associated with License
     * @param targetId
     *            -- Target Id of the License File
     * @param time
     *            -- Time when the License file is valid
     * @return List of list of licenses that are currently (currentTime) valid on this system (targetId) for the
     *         requested feature (featureCode)
     * @throws LicenseException
     */
    List<License> getLicensesByFeatureCodeAndTargetIdAndTime(long featureCode, String targetId, Date time,
            boolean checkDataIntegrity) throws LicenseException;
}
