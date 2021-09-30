/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.interfaces;

import java.util.List;

import com.nokia.licensing.dtos.CapacityReservation;


/**
 * This class has methods to Reserve and Release a License Capacity from the Pool. Once a Capacity is reserved then a
 * Reservation Token is generated and stored in the Extended Capacity Reservation Database.
 *
 * @version 1.0
 */
public interface LicenseCapacityReservation {

    /**
     * This method reserves the capacity for a given feature code. The corresponding implementation checks whether
     * capacity is available and then makes a reservation by generating reservation token. The returned
     * CapacityReservation instance has information about the reserved capacity. There are flags in the
     * CapacityReservation instance to suggest the following a. No Capacity Left b. Not enough capacity left
     *
     * @param featureCode
     *            -- Feature code of the License
     * @param capacity
     *            -- Capacity to be reserved
     * @return CapacityFeatureStatus object containing information about the Reservation information
     * @throws LicenseException
     */
    public CapacityReservation reserveCapacityByFeatureCode(String featureCode, int capacity) throws LicenseException;

    /**
     * Releases the capacity for a given Reservation ID. The corresponding implementation checks if reservation token is
     * valid and if is then releases the capacity
     *
     * @param reservationID
     *            -- Reservation ID of the token which needs to be released
     * @return boolean. True if the operation is successful, otherwise false
     * @throws LicenseException
     */
    public boolean releaseCapacity(String reservationID) throws LicenseException;

    /**
     * Fetches a list of reserved capacities for a given feature code. It access the information from the
     * LicenseDataStorage and performs validation to check if the License is valid or has expired. If the License is
     * valid then makes this information as part of the List which is then returned
     *
     * @param featureCode
     *            -- Feature code for which Capacities has to be fetched
     * @return -- List of CapacityReservation objects
     * @throws LicenseException
     */
    public List<CapacityReservation> getReservedCapacitiesByFeatureCode(long featureCode) throws LicenseException;

    /**
     * Fetches all the Reserved Capacities from the repository It access the information from the LicenseDataStorage and
     * performs validation to check if the License is valid or has expired. If the License is valid then makes this
     * information as part of the List which is then returned
     *
     * @return List of CapacityReservation objects
     * @throws LicenseException
     */
    public List<CapacityReservation> getReservedCapacities() throws LicenseException;
}
