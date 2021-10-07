/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.dtos;

/**
 * Holds information about the License. This class derives from StoredLicense. It has information about the License File
 * State and License Cancel Information
 *
 * @version 1.0
 * @see StoredLicense
 */
public class License extends StoredLicense {
    private static final long serialVersionUID = 1L;
    /**
     * Holds information about the Canceled License
     */
    private LicenseCancelInfo licenseCancelInfo;

    /**
     * LicenseFileState Enumeration holds all the possible License File States. The states are described as below:<br/>
     * INSTALLED -- The license has been downloaded into the License library storage but is not yet active (e.g. license
     * start date is in future)<br/>
     * ACTIVE -- The license is active and ready to be used<br/>
     * EXPIRED -- The license is expired. Possiblity is that end time has been reached<br/>
     * INVALID -- The downloaded license is invalid (e.g. license file is not compliant to defined schema or signature
     * of the license is not correct)<br/>
     * CANCELLED -- The license is cancelled, by means of the license is officially deleted and can not be installed any
     * more.<br/>
     * The license information is stored in cancellation list.
     */
    private enum LicenseFileState {

        INSTALLED, ACTIVE, EXPIRED, INVALID, CANCELLED
    };

    /**
     * License File state is calculated based on following parameters startTime, endTime and
     * licenseCancelInfo.isCanceled. This information is calculated online and returned. License States which are
     * returned are as below:<br/>
     * INSTALLED -- The license has been downloaded into the License library storage but is not yet active (e.g. license
     * start date is in future)<br/>
     * ACTIVE -- The license is active and ready to be used<br/>
     * EXPIRED -- The license is expired. Possiblity is that end time has been reached<br/>
     * INVALID -- The downloaded license is invalid (e.g. license file is not compliant to defined schema or signature
     * of the license is not correct)<br/>
     * CANCELLED -- The license is cancelled, by means of the license is officially deleted and can not be installed any
     * more.<br/>
     * The license information is stored in cancellation list.
     *
     * @return LicenceFileState -- LicenseFileState enumeration is returned
     */
    public LicenseFileState getLicenceFileState() {

        // The logic here will check for the state of the License File based on the StartTime, EndTime and
        // licenseInfo.isCanceled Based on the out come one of the states defined in the enum is returned as a String

        // TODO: The return type needs to be changed based on the result of the computation
        return LicenseFileState.ACTIVE;
    }

    /**
     * Gets the License Cancel Information
     *
     * @return License Cancel Information
     */
    public LicenseCancelInfo getLicenseCancelInfo() {
        return this.licenseCancelInfo;
    }

    /**
     * Sets the License Cancel Information
     *
     * @param licenseCancelInfo
     *            License Cancel information to be set
     */
    public void setLicenseCancelInfo(final LicenseCancelInfo licenseCancelInfo) {
        this.licenseCancelInfo = licenseCancelInfo;
    }
}
