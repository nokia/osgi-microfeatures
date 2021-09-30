/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.interfaces;

import com.nokia.licensing.dtos.CapacityFeatureStatus;


/**
 * This interface has the methods which help in storing and accessing Capacity reservation information into the data
 * storage.
 *
 * @version 1.0
 */
public interface LicenseExtendedStorage {

    /**
     * This method reserves the capacity for a given feature code. This method updates the underlying data storage.
     *
     * @param featureCode
     *            -- Feature code of the License to be Reserved
     * @param capacityFeatureStatus.
     *            Object containing information about the capacity to be reserved
     * @return boolean. Returns true if the capacity reservation has been done, otherwise returns false
     * @throws LicenseException
     */
    public boolean reserveCapacityByFeatureCode(CapacityFeatureStatus capacityFeatureStatus) throws LicenseException;

    /**
     * Releases the capacity for a given Reservation ID. The corresponding implementation checks if reservation token is
     * valid and if is then releases the capacity. This methods updates the underlying data storage so as to release the
     * capacity
     *
     * @param reservationID
     *            -- Reservation ID of the Token to be released
     * @return boolean. True if the operation is successful, otherwise false
     * @throws LicenseException
     */
    public boolean releaseCapacity(String reservationID) throws LicenseException;
}
