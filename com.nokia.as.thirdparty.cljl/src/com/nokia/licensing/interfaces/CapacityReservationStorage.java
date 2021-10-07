/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.interfaces;

import java.util.List;

import com.nokia.licensing.dtos.CapacityReservation;


/**
 * This interface has the methods which help in storing and accessing Capacity reservation information into the data
 * storage.
 *
 * @version 1.0
 */
public interface CapacityReservationStorage {

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
    public boolean insertCapacityReservation(CapacityReservation capacityFeatureStatus) throws LicenseException;

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
    public boolean deleteCapacityReservation(String reservationID) throws LicenseException;

    /**
     * Method fetches all the Capacities which are reserved from the data storage repository for a given feature code.
     * It the packs the information into a list containing CapacityReservation objects and returns the object.
     *
     * @param featureCode
     *            -- Featurecode for which Capacities can be reserved
     * @return -- List of CapacityReservation object instances
     * @throws LicenseException
     */
    public List<CapacityReservation> getReservedCapacitiesByFeatureCode(long featureCode) throws LicenseException;

    /**
     * Method fetches all the Capacities which are reserved from the data storage repository. It the packs the
     * information into a list containing CapacityReservation objects and returns the object.
     *
     * @return -- List of CapacityReservation object instances
     * @throws LicenseException
     */
    public List<CapacityReservation> getReservedCapacities() throws LicenseException;
}
