/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.factories;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import com.nokia.licensing.dtos.TargetSystem;
import com.nokia.licensing.interfaces.CLJLPreferences;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseCheck;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.plugins.PluginRegistry;
import com.nokia.licensing.storages.file.LicenseCancelDataStorageFileImpl;
import com.nokia.licensing.storages.file.LicenseDataStorageFileImpl;
import com.nokia.licensing.storages.jdbc.LicenseCancelDataStorageImpl;
import com.nokia.licensing.storages.jdbc.LicenseDataStorageImpl;
import com.nokia.licensing.storages.jpa.LicenseCancelDataStorageJPAImpl;
import com.nokia.licensing.storages.jpa.LicenseDataStorageJPAImpl;


/**
 * This is a Factory class used to get the instance of LicenseCheck Implementation. Methods in the factory accept
 * DataStorage implementation and test responder configuration It is a Singleton implementation
 *
 * @version 1.0
 */
public class LicenseCheckFactory {

	private static Map<String, LicenseCheck> LICENSE_CHECK_MAP = new HashMap<String, LicenseCheck>();

	/**
	 * Instantiates the LicenseCheck Implementation and returns an instance of the same. LicenseCheck is instantiated
	 * with DataStorage Implementation,TestResponder configuration and TargetID.
	 * <p>
	 * The TestResponder configuration holds the TestResponder URL. If this URL is set then License File is used for
	 * Testing Purposes otherwise LicenseCheck will be on a commercial License File.
	 * <p>
	 * TargetID specifies the target system against which License checking needs to be done License Check is carried out
	 * on licenses specified for the system specified through target ID.
	 *
	 * @param dataStorage
	 *            -- Data Storage Implementation instance
	 * @param cancelDataStorage
	 *            -- Instance of CancelDataStorage interface
	 * @param testResponder
	 *            -- Test Responder Configuration
	 * @param targetID
	 *            -- Target ID of the System
	 * @return Instance of LicenseCheck interface
	 * @throws LicenseException
	 */
	public static LicenseCheck getInstance(LicenseDataStorage dataStorage, LicenseCancelDataStorage cancelDataStorage,
			final String targetID) throws LicenseException {
		final String implementationClassKey = "licenseCheckImpl";
		final String implementationPluginKey = "impl";

		try {
			if (LICENSE_CHECK_MAP.get(targetID) == null) {
				final CLJLPreferences cljlPreferences = PluginRegistry.getRegistry().getPlugin(CLJLPreferences.class);
				final Preferences pref = cljlPreferences.getPreferencesSystemRoot();
				final String implementationClass = pref.node("licenseCheck").get(implementationClassKey,
						"com.nokia.licensing.impl.LicenseCheckImpl");

				// TODO: After JPA implementation we will change the default to JPA implementation
				if ((cancelDataStorage == null) && (dataStorage == null)) {
					final String implValue = pref.node("ImplPlugin").get(implementationPluginKey, "SQL");

					if (implValue.equalsIgnoreCase("JPA")) {
						LicenseLogger.getInstance().finest(LicenseCheckFactory.class.getName(), "getInstance", "JPA Implementation.");
						dataStorage = new LicenseDataStorageJPAImpl();
						cancelDataStorage = new LicenseCancelDataStorageJPAImpl();
					} else if (implValue.equalsIgnoreCase("SQL")) {
						LicenseLogger.getInstance().finest(LicenseCheckFactory.class.getName(), "getInstance", "SQL Implementation.");
						dataStorage = new LicenseDataStorageImpl();
						cancelDataStorage = new LicenseCancelDataStorageImpl();
					} else if (implValue.equalsIgnoreCase("FILE")) {
						LicenseLogger.getInstance().finest(LicenseInstallFactory.class.getName(), "getInstance",
								"FILE Implementation.");
						dataStorage = new LicenseDataStorageFileImpl();
						cancelDataStorage = new LicenseCancelDataStorageFileImpl();
					}
				}

				@SuppressWarnings("rawtypes")
				final Constructor constructor = Class.forName(implementationClass).getConstructor(new Class[] {
						LicenseDataStorage.class, LicenseCancelDataStorage.class, TargetSystem.class });
				final TargetSystem targetSystem = new TargetSystem();

				targetSystem.setTargetId(targetID);
				final LicenseCheck licenseCheck = (LicenseCheck) (constructor.newInstance(new Object[] {
						dataStorage, cancelDataStorage, targetSystem }));
				LICENSE_CHECK_MAP.put(targetID, licenseCheck);
			}
		} catch (final ClassNotFoundException cnfe) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" ClassNotFound Exception.");

			ex.setErrorCode("CLJL123");
			LicenseLogger.getInstance().error(LicenseCheckFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final InstantiationException ie) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" Instantiation Exception.");

			ex.setErrorCode("CLJL125");
			LicenseLogger.getInstance().error(LicenseCheckFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final IllegalAccessException iae) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" IllegalAccess Exception.");

			ex.setErrorCode("CLJL126");
			LicenseLogger.getInstance().error(LicenseCheckFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final IllegalArgumentException iae) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" IllegalArgument Exception.");

			ex.setErrorCode("CLJL124");
			LicenseLogger.getInstance().error(LicenseCheckFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final InvocationTargetException ite) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" InvocationTarget Exception.");

			ex.setErrorCode("CLJL121");
			LicenseLogger.getInstance().error(LicenseCheckFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final NoSuchMethodException nme) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" NoSuchMethod Exception.");

			ex.setErrorCode("CLJL122");
			LicenseLogger.getInstance().error(LicenseCheckFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		}

		return LICENSE_CHECK_MAP.get(targetID);
	}
}
