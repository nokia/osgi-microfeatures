///* ========================================== */
///* Copyright (c) 2009 Nokia                   */
///*          All rights reserved.              */
///*          Company Confidential              */
///* ========================================== */
package com.nokia.licensing.interfaces;

import java.util.Date;
import java.util.List;

import com.nokia.licensing.dtos.LicenseChange;
import com.nokia.licensing.dtos.StoredLicense;


/**
 * This interface specifies a contract which can be to get information on the License state. This information will be
 * used by the Notification Engine to notify the client about License State
 */
public interface LicenseNotification {

    /**
     * This method checks each license from the time "startTime" till "endTime". It queries for License information
     * between "startTime" till "endTime". The modifiedDate column in database for License table will be used for
     * querying. This modifiedDate column will be updated whenever there is any changes to the License information. For
     * all the Licenses retrieved it checks the following<br/>
     * <ol>
     * <li>Whether one of the featureCode of a license has been cancelled between "startTime" and "endTime"</li>
     * <li>Checks whether one of the featureCode of a license has been installed between "startTime" and "endTime".</li>
     * <li>Checks whether License has been activated between "startTime" and "endTime"</li>
     * <li>Checks whether License has expired between "startTime" and "endTime"</li>
     * </ol>
     *
     * The changes are wrapped into the LicenseChange object and a List of LicenseChange is returned
     *
     * @param startTime
     *            -- Represents the start time.
     * @param endTime
     *            -- Represents the end time
     * @return List<LicenseChange> -- List of changes in License between startTime and endTime
     * @throws LicenseException
     */
    List<LicenseChange> getLicenseChanges(Date startTime, Date endTime) throws LicenseException;

    /**
     * This method fetches all licenses which will expire in x days from now. This x days is passed as argument to the
     * method. It returns a list of license objects which will expire in x days
     *
     * @param days
     *            -- The days after which license expires
     * @return -- Returns a list of licenses which are about to expire
     * @throws LicenseException
     */
    List<StoredLicense> getExpiredLicenses(int days) throws LicenseException;
}
