///* ========================================== */
///* Copyright (c) 2009 Nokia                   */
///*          All rights reserved.              */
///*          Company Confidential              */
///* ========================================== */

package com.nokia.licensing.factories;

import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.interfaces.LicenseJPAAccess;


/**
 * This factory has getInstance method which returns a single instance of LicenseJPAAccess interface's implementation.
 * The LicenseJPAAccess Implementation class is hard-coded in the getInstance method, the reason is that this factory
 * will only be used by License Manager Application and that there will be only one implementation of the interface.
 *
 */
public class LicenseJPAFactory {
    private static LicenseJPAAccess licenseJPAAccess = null;

    /**
     * This method returns the instance of LicenseJPAAccess implementation. This method checks if instance of
     * LicenseJPAAccess is already available if yes it returns the same instance, otherwise instantiates and returns.
     * The first argument is instance of LicenseDataStorage implementation which can be plugged in. If default
     * implementation has to be used then pass NULL The second argument is instance of LicenseCancelDataStorage
     * implementation which can be plugged in. If default implementation has to be used then pass NULL
     * 
     * @param dataStorage:
     *            The implementation of the DataStorage is JPA based
     * @param cancelDataStorage
     *            The implementation of the CanceDataStorage is JPA based
     * @return instance of LicenseJPAAccess
     * @throws LicenseException
     */
    public static LicenseJPAAccess getInstance(final LicenseDataStorage dataStorage,
            final LicenseCancelDataStorage cancelDataStorage) throws LicenseException {
        if (licenseJPAAccess == null) {
            // Instantiate License JPA Access.
            // licenseJPAAccess = new LicenseJPAAccessImple(dataStorage,cancelDataStorage);
        }

        return licenseJPAAccess;
    }
}
