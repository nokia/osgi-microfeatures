/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.interfaces;

import java.util.Date;
import java.util.List;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;


/**
 * This interface exposes method to fetch information about canceled licenses. It also has methods to check if a given
 * license is canceled or not. This interface is used by LicenseInstall and LicenseAccess interface implementation to
 * check if a given License is canceled or not. The LicenseCancelDataStorage must be implemented such that the only way
 * to modify the underlying persistent storage is through the Licensing Java API. The LicenseCancelDataStorage API
 * should be exposed and used by only Licensing interfaces (LicenseAccess and LicenseInstall) and there should not be a
 * possibility to access these interfaces directly by an outsider (say an hacker)
 *
 * remove, modify or add cancel entries
 *
 * @version 1.0
 */
public interface LicenseCancelDataStorage {

    /**
     * Methods cancels the License based on the Information provided in the LicenseCancelInfo object and then moves the
     * License to Cancel Storage
     *
     * @param cancelInfo
     *            -- Information about the License to be canceled
     * @throws LicenseException
     */
    void insertCancelInformation(LicenseCancelInfo cancelInfo) throws LicenseException;

    /**
     * Method gets the Cancel Information based on Serial Number. The second argument tells whether data integrity
     * checks against data storage needs to be performed or not.
     *
     * @param serialNumber
     *            -- License Serial Number
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return -- Cancel Information fetched for a given Serial Number
     * @throws LicenseException
     */
    List<LicenseCancelInfo> getCancelInfoBySerialNumber(String serialNumber, boolean checkDataIntegrity)
            throws LicenseException;

    /**
     * Methods gets a List of all Canceled Licenses The checkDataIntegrity argument tells whether data integrity checks
     * against data storage needs to be performed or not.
     *
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return -- List if all Canceled Licenses
     * @throws LicenseException
     */
    List<LicenseCancelInfo> getAllCancelInfos(boolean checkDataIntegrity) throws LicenseException;

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

    public List<LicenseCancelInfo> getCanceledLicense(Date startTime, Date endTime) throws LicenseException;
}
