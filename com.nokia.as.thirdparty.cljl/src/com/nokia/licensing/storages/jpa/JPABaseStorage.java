// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.storages.jpa;

import javax.persistence.EntityManager;

import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;


/**
 *
 * @author twozniak
 */
public class JPABaseStorage {

	/**
	 * Making the connection to the database.
	 *
	 * @return EntityManager -- data base connection object
	 * @throws LicenseException
	 */
	protected EntityManager getConnection() throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), "connectionhelp", "Creating the connection...");

		EntityManager entityManager = null;

		try {
			entityManager = ConnectionUtilJPA.getConnection();
			LicenseLogger.getInstance().finest(this.getClass().getName(), "connectionhelp", "The connection is created.");
		} catch (final Exception e) {
			LicenseLogger.getInstance().error(this.getClass().getName(), "connectionhelp",
					"Failed to creating the connection." + e.getMessage());

			final LicenseException licenseException = new LicenseException(
					" Unable to connect/disconnect to the database.");

			licenseException.setErrorCode("CLJL109");
			LicenseLogger.getInstance().error(this.getClass().getName(), "connectionhelp",
					"error code set to: " + licenseException.getErrorCode());

			throw licenseException;
		}

		return entityManager;
	}
}
