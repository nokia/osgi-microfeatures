/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */

package com.nokia.licensing.factories;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.prefs.Preferences;

import com.nokia.licensing.interfaces.CLJLPreferences;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.interfaces.LicenseInstall;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.plugins.PluginRegistry;
import com.nokia.licensing.storages.file.LicenseCancelDataStorageFileImpl;
import com.nokia.licensing.storages.file.LicenseDataStorageFileImpl;
import com.nokia.licensing.storages.jdbc.LicenseCancelDataStorageImpl;
import com.nokia.licensing.storages.jdbc.LicenseDataStorageImpl;
import com.nokia.licensing.storages.jpa.LicenseCancelDataStorageJPAImpl;
import com.nokia.licensing.storages.jpa.LicenseDataStorageJPAImpl;


/**
 * This is the Factory class used to fetch the instance of LicenseInstall. Methods in the factory accepts Instance
 * DataStorage and CancelStorage Implementation and returns instance of LinceseInstall implementation It is a Singleton
 * implementation. Each install of LicenseInstall implementation will have only one Instance
 *
 * @version 1.0
 *
 */
public class LicenseInstallFactory {
	private static HashMap<String, LicenseInstall> instanceList = new HashMap<String, LicenseInstall>();

	/**
	 * Instantiates the LicenseInstall interface Implementation and returns an instance of the same. The methods
	 * instantiates the Object using the implementationClass (passed as argument) and then sets the DataStorage
	 * implementation on that object and returns the instance The second argument acts as key to preferences entry. The
	 * Preferences entry contains the name of the implementation class. This implementation class is then instantiated
	 * and returned. The third argument is an instance of cancelDataStorage which is used check if the License file in
	 * question has been Canceled or not Ex:
	 * <entry key="DefaultImpl" value="com.nokia.licensing.DefaultLicenseInstallImpl.class"/>
	 * 
	 * @param dataStorage
	 *            -- Instance of License Data Storage
	 * @param implementationClassKey
	 *            -- Key to fetch the License Install Implementation Class
	 * @param cancelDataStorage
	 *            -- Instance of CancelDataStorage interface
	 * @return Instance of LicenseInstall interface
	 * @throws LicenseException
	 */
	public static LicenseInstall getInstance(LicenseDataStorage dataStorage, final String implementationClassKey,
			LicenseCancelDataStorage cancelDataStorage) throws LicenseException {

		// String implementationClass = "com.nokia.licensing.LicenseInstallImpl.class";
		LicenseInstall invoker = null;

		try {

			// Check if the instance is available in the HashMap, if yes then return
			// otherwise create a instance put it into HashMap and return it.
			invoker = instanceList.get(implementationClassKey);

			if (invoker == null) {
				final CLJLPreferences cljlPreferences = PluginRegistry.getRegistry().getPlugin(CLJLPreferences.class);
				;
				final Preferences prefSystemRoot = cljlPreferences.getPreferencesSystemRoot();
				final String implementationClass = prefSystemRoot.node("licenseInstall").get(implementationClassKey,
						"com.nokia.licensing.impl.LicenseInstallImpl");
				final String implementationPluginKey = "impl";

				if ((cancelDataStorage == null) && (dataStorage == null)) {
					final String implValue = prefSystemRoot.node("ImplPlugin").get(implementationPluginKey, "SQL");

					if (implValue.equalsIgnoreCase("JPA")) {
						LicenseLogger.getInstance().finest(LicenseInstallFactory.class.getName(), "getInstance",
								"JPA Implementation.");
						dataStorage = new LicenseDataStorageJPAImpl();
						cancelDataStorage = new LicenseCancelDataStorageJPAImpl();
					} else if (implValue.equalsIgnoreCase("SQL")) {
						LicenseLogger.getInstance().finest(LicenseInstallFactory.class.getName(), "getInstance",
								"SQL Implementation.");
						dataStorage = new LicenseDataStorageImpl();
						cancelDataStorage = new LicenseCancelDataStorageImpl();
					} else if (implValue.equalsIgnoreCase("FILE")) {
						LicenseLogger.getInstance().finest(LicenseInstallFactory.class.getName(), "getInstance",
								"FILE Implementation.");
						dataStorage = new LicenseDataStorageFileImpl();
						cancelDataStorage = new LicenseCancelDataStorageFileImpl();
					}
				}

				final Constructor<?> constructor = Class.forName(implementationClass).getConstructor(new Class[] {
						LicenseDataStorage.class, LicenseCancelDataStorage.class });

				invoker = (LicenseInstall) (constructor.newInstance(new Object[] {
						dataStorage, cancelDataStorage }));

				// Add it to HashMap
				instanceList.put(implementationClassKey, invoker);
			}
		} catch (final ClassNotFoundException cnfe) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" ClassNotFound Exception.");

			ex.setErrorCode("CLJL123");
			LicenseLogger.getInstance().error(LicenseInstallFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final InstantiationException ie) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" Instantiation Exception.");

			ex.setErrorCode("CLJL125");
			LicenseLogger.getInstance().error(LicenseInstallFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final IllegalAccessException iae) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" IllegalAccess Exception.");

			ex.setErrorCode("CLJL126");
			LicenseLogger.getInstance().error(LicenseInstallFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final IllegalArgumentException iare) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" IllegalArgument Exception.");

			ex.setErrorCode("CLJL124");
			LicenseLogger.getInstance().error(LicenseInstallFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final InvocationTargetException ite) {

			// Log the information into log files
			ite.printStackTrace();
			final LicenseException ex = new LicenseException(" InvocationTarget Exception.");

			ex.setErrorCode("CLJL121");
			LicenseLogger.getInstance().error(LicenseInstallFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final NoSuchMethodException nme) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" NoSuchMethod Exception.");

			ex.setErrorCode("CLJL122");
			LicenseLogger.getInstance().error(LicenseInstallFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		}

		return invoker;
	}
}
