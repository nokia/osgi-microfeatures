/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */

package com.nokia.licensing.factories;

import com.nokia.licensing.interfaces.CapacityReservationStorage;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseCapacityReservation;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;


/**
 * This is the Factory class which instantiates an instance of the LicenseCapacityReservation interface and returns the
 * instance. There will be only one implementation of the capacity reservation supported. If LicenseExtendedStorage
 * instance is passed as null then default implementation is used It is a Singleton implementation.
 *
 * @version 1.0
 */
public class LicenseCapacityReservationFactory {
    private static LicenseCapacityReservation licenseCapacity = null;

    /**
     * This methods returns the instance of LicenseCapacityReservation. It checks if LicenseExtendedStorage has been
     * passed if not it gets the default implementation of LicenseExtendedStorage and assigns it to
     * LicenseCapacityReservation implementation
     * 
     * @param capacityReservationStorage
     *            - Instance if LicenseExtendedStorage. Null will be passed if default implementation is expected
     * @param licenseDataStorage
     *            -- Used to access license data storage
     * @param cancelStorage
     *            -- Used to access license cancel storage
     * @return -- Instance of LicenseCapacityReservation
     * @throws LicenseException
     */
    public static LicenseCapacityReservation getInstance(final CapacityReservationStorage capacityReservationStorage,
            final LicenseDataStorage licenseDataStorage, final LicenseCancelDataStorage cancelStorage)
                    throws LicenseException {
        if (capacityReservationStorage == null) {
            // TODO: Instantiate the default LicenseExtendedStorage instance by reading information from the Preferences
            // file
        }

        if (licenseCapacity == null) {
            // TODO: The LicenseCapacityReservation implementation is instantiated and returned. The instance of
            // LicenseExtendedStorage s
        }

        return licenseCapacity;
    }
}
