/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.interfaces;

import java.util.List;
import java.util.Map;

import com.nokia.licensing.dtos.FeatureStatus;


/**
 * This interfaces exposes method to check the License. The methods in the interface check for License Validity and then
 * return based on the response return the License Information back to the caller
 *
 * @author Nokia
 * @version 1.0
 */
public interface LicenseCheck {

    /**
     * Method to obtain license information from the licensing system. The License Information is fetched based on the
     * feature code. The second argument tells whether data integrity checks against data storage needs to be performed
     * or not. TRUE -- Perform Integrity check, FALSE - Do not Perform Integrity check
     *
     * @param featureCode
     *            License Feature code
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @return FeatureStatus object specifying the status of license. This method never returns null.
     * @throws LicenseException
     */
    FeatureStatus getFeatureStatus(final long featureCode, boolean checkDataIntegrity) throws LicenseException;

    /**
     * Method to obtain license information from the licensing system. The License Information is fetched based on the
     * feature code. The second argument tells whether data integrity checks against data storage needs to be performed
     * or not. TRUE -- Perform Integrity check, FALSE - Do not Perform Integrity check
     *
     * @param featureCode
     *            License Feature code
     * @param checkDataIntegrity
     *            -- Check for the integrity of the data being modified
     * @param targetNeType
     *            --It specifies the target NE Type. In case of application licenses it is NAC, NAT, NAR and so on.Note
     *            : But as now this parameter is ignored.
     * @return FeatureStatus object specifying the status of license. This method never returns null.
     * @throws LicenseException
     */
    FeatureStatus getFeatureStatus(final long featureCode, boolean checkDataIntegrity, String targetNeType)
            throws LicenseException;

    /**
     *
     * @param featureCode
     * @param cachedDigest
     * @return
     * @throws LicenseException
     */
    FeatureStatus getFeatureStatus(final long featureCode, String cachedDigest) throws LicenseException;

    /**
     *
     * @param featureCodes
     * @return
     * @throws LicenseException
     */
    List<FeatureStatus> getFeatureStatus(Map<Long, String> featureCodes) throws LicenseException;
}
