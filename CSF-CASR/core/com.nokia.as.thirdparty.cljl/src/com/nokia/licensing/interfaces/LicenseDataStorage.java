/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.interfaces;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nokia.licensing.dtos.StoredLicense;


/**
 * This interface has methods which help in storing and accessing the License data from/to the Storage Repository. It
 * has methods to insert, modify delete information from the underlying repository. LicenseAccess and LicenseInstall
 * interfaces expose API which interact with the LicenseDataStorage to carry license operations like
 * fetching/modifications/deletion. The LicenseDataStorage must be implemented such that the only way to modify the
 * underlying persistent storage is through the Licensing Java API. The DataStorage API should be exposed and used by
 * only Licensing interfaces (LicenseAccess and LicenseInstall) and there should not be a possibility to access these
 * interfaces directly by an outsider (say an hacker)
 *
 * @version 1.0
 */
public interface LicenseDataStorage {

    /**
     * Inserts License information into a corresponding Data Storage. License Management Interface uses this method to
     * insert data into the relevant Data Storage
     *
     * @param List<StoredLicense>
     *            List containing License details
     * @throws LicenseException
     */
    void insertLicenseInformation(List<StoredLicense> licenseFileList) throws LicenseException;

    /**
     * Deletes the License data from the underlying repository based on the Serial Number. Serial Number being the
     * Primary Key of the License will delete only one entry from the Repository.
     *
     * @param license
     *            -- Serial Number of the License
     * @return boolean -- returns true is deletion is successful otherwise false
     * @throws LicenseException
     */
    boolean deleteLicenseBySerialNumber(String serialNbr) throws LicenseException;

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
    boolean deleteLicenseBySerialNumberAndFeatureCode(String serialNbr, long featureCode) throws LicenseException;

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
    StoredLicense getLicenseBySerialNo(String serialNbr, boolean checkDataIntegrity) throws LicenseException;

    /**
     * Accesses License Information based on the License File Name Returns the License Information wrapped in License
     * object. The second argument tells whether data integrity checks against data storage needs to be performed or
     * not.
     *
     * @param licenseFileName
     *            - Name of the License File
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return List<StoredLicense> -- List of Licenses associated with the Name
     * @throws LicenseException
     */
    List<StoredLicense> getLicenseByFileName(String licenseFileName, boolean checkDataIntegrity)
            throws LicenseException;

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
    List<StoredLicense> getLicensesForLicenseCode(String licenseCode, boolean checkDataIntegrity)
            throws LicenseException;

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
    List<StoredLicense> getLicensesForName(String licenseName, boolean checkDataIntegrity) throws LicenseException;

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
    List<StoredLicense> getLicensesForOrderID(String orderID, boolean checkDataIntegrity) throws LicenseException;

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
    List<StoredLicense> getLicensesForCustomerID(String customerID, boolean checkDataIntegrity) throws LicenseException;

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
    List<StoredLicense> getLicensesForCustomerName(String customerName, boolean checkDataIntegrity)
            throws LicenseException;

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
    List<StoredLicense> getLicensesForSWBaseRelease(String swReleaseBase, boolean checkDataIntegrity)
            throws LicenseException;

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
    List<StoredLicense> getLicensesForSWReleaseRelation(String swReleaseRelation, boolean checkDataIntegrity)
            throws LicenseException;

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
    List<StoredLicense> getLicensesForTargetType(String targetType, boolean checkDataIntegrity) throws LicenseException;

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
    List<StoredLicense> getLicensesForTargetID(String targetID, boolean checkDataIntegrity) throws LicenseException;

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
    List<StoredLicense> getLicensesForState(String licenseState, boolean checkDataIntegrity) throws LicenseException;

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
    List<StoredLicense> getLicensesForLicenseType(int licenseType, boolean checkDataIntegrity) throws LicenseException;

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
    List<StoredLicense> getLicensesForUsageType(String usageType, boolean checkDataIntegrity) throws LicenseException;

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
    List<StoredLicense> getLicensesForFeatureName(String featureName, boolean checkDataIntegrity)
            throws LicenseException;

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
    List<StoredLicense> getLicensesForFeatureCode(long featureCode, boolean checkDataIntegrity) throws LicenseException;

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
    List<StoredLicense> getAllLicenses(boolean checkDataIntegrity) throws LicenseException;

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
    List<StoredLicense> getLicensesByFeatureCodeAndTargetIdAndTime(long featureCode, String targetId, Date time,
            boolean checkDataIntegrity) throws LicenseException;

    /**
     * Method gets list of Licenses that are currently valid on this system for the requested features.
     *
     *
     * @param featureCodes
     * @param targetId
     * @param time
     * @return
     */
    Map<Long, List<StoredLicense>> getLicensesByFeatureCodeAndTargetIdAndTime(Set<Long> featureCodes, String targetId,
            Date time) throws LicenseException;

    /**
     * This method checks each license from the time "startTime" till "endTime". It queries for License information
     * between "startTime" till "endTime". The modifiedDate column in database for License table will be used for
     * querying. This modifiedDate column will be updated whenever there is any changes to the License information. For
     * all the Licenses retrieved it checks the following 1. Whether one of the featureCode of a license has been
     * cancelled between "startTime" and "endTime" 2. Checks whether one of the featureCode of a license has been
     * installed between "startTime" and "endTime". 3. Checks whether License has been activated between "startTime" and
     * "endTime" 4. Checks whether License has expired between "startTime" and "endTime"
     *
     * @param startTime
     *            -- Represents the start time.
     * @param endTime
     *            -- Represents the end time
     * @return List<StoredLicense> -- List of StoredLicense objects
     * @throws LicenseException
     */
    List<StoredLicense> getLicenseChanges(Date startTime, Date endTime) throws LicenseException;

    /**
     * This method fetches all licenses which will expire between startTime and endTime.
     *
     * @param startTime
     *            -- Represents the start time.
     * @param endTime
     *            -- Represents the end time.
     * @return -- Returns a list of StoredLicense objects.
     * @throws LicenseException
     */
    List<StoredLicense> getExpiredLicenses(Date startTime, Date endTime) throws LicenseException;

    List<StoredLicense> getModifiedLicenses(Date startTime, Date endTime) throws LicenseException;
}
