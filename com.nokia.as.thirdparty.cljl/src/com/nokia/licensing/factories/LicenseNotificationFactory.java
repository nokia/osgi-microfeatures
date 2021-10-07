package com.nokia.licensing.factories;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.prefs.Preferences;

import com.nokia.licensing.interfaces.CLJLPreferences;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.interfaces.LicenseNotification;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.plugins.PluginRegistry;
import com.nokia.licensing.storages.jdbc.LicenseCancelDataStorageImpl;
import com.nokia.licensing.storages.jdbc.LicenseDataStorageImpl;
import com.nokia.licensing.storages.jpa.LicenseCancelDataStorageJPAImpl;
import com.nokia.licensing.storages.jpa.LicenseDataStorageJPAImpl;


public class LicenseNotificationFactory {
	private static HashMap<String, LicenseNotification> instanceList = new HashMap<String, LicenseNotification>();

	/**
	 * Instantiates the LicenseNotification interface Implementation and returns an instance of the same. The methods
	 * instantiates the Object using the implementationClass (passed as argument) and then sets the DataStorage
	 * implementation on that object and returns the instance The second argument acts as key to preferences entry. The
	 * Preferences entry contains the name of the implementation class. This implementation class is then instantiated
	 * and returned.
	 *
	 * @param dataStorage
	 *            -- Instance of License Data Storage
	 * @param cancelDateStorage
	 *            -- Instance of LicenseCancelDataStorage
	 * @param implementationClassKey
	 *            -- Key to fetch the License Install Implementation Class
	 * @return Instance of LicenseNotification interface
	 * @throws LicenseException
	 */
	public static LicenseNotification getInstance(LicenseDataStorage dataStorage,
			LicenseCancelDataStorage licenseCancelDataStorage, final String implementationClassKey)
					throws LicenseException {
		LicenseNotification licenseNotificationObject = null;

		try {

			// Check if the instance is available in the HashMap, if yes then return
			// otherwise create a instance put it into HashMap and return it.
			licenseNotificationObject = instanceList.get(licenseNotificationObject);

			if (licenseNotificationObject == null) {
				final CLJLPreferences cljlPreferences = PluginRegistry.getRegistry().getPlugin(CLJLPreferences.class);
				;
				final Preferences prefSystemRoot = cljlPreferences.getPreferencesSystemRoot();
				final String implementationClass = prefSystemRoot.node("licenseNotification")
						.get(implementationClassKey, "com.nokia.licensing.impl.LicenseNotificationImpl");
				final String implementationPluginKey = "impl";

				if (dataStorage == null) {
					final String implValue = prefSystemRoot.node("ImplPlugin").get(implementationPluginKey, "JPA");

					if (implValue.equalsIgnoreCase("JPA")) {
						LicenseLogger.getInstance().finest(LicenseNotificationFactory.class.getName(), "getInstance",
								"JPA Implementation.");
						dataStorage = new LicenseDataStorageJPAImpl();
						licenseCancelDataStorage = new LicenseCancelDataStorageJPAImpl();
					} else if (implValue.equalsIgnoreCase("SQL")) {
						LicenseLogger.getInstance().finest(LicenseInstallFactory.class.getName(), "getInstance",
								"SQL Implementation.");
						dataStorage = new LicenseDataStorageImpl();
						licenseCancelDataStorage = new LicenseCancelDataStorageImpl();
					}
				}

				final Constructor<?> constructor = Class.forName(implementationClass)
						.getConstructor(LicenseDataStorage.class, LicenseCancelDataStorage.class);

				licenseNotificationObject = (LicenseNotification) (constructor.newInstance(dataStorage,
						licenseCancelDataStorage));

				// Add it to HashMap
				instanceList.put(implementationClassKey, licenseNotificationObject);
			}
		} catch (final ClassNotFoundException cnfe) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" ClassNotFound Exception.");

			ex.setErrorCode("CLJL123");
			LicenseLogger.getInstance().error(LicenseNotificationFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final InstantiationException ie) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" Instantiation Exception.");

			ex.setErrorCode("CLJL125");
			LicenseLogger.getInstance().error(LicenseNotificationFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final IllegalAccessException iae) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" IllegalAccess Exception.");

			ex.setErrorCode("CLJL126");
			LicenseLogger.getInstance().error(LicenseNotificationFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final IllegalArgumentException iare) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" IllegalArgument Exception.");

			ex.setErrorCode("CLJL124");
			LicenseLogger.getInstance().error(LicenseNotificationFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final InvocationTargetException ite) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" InvocationTarget Exception.");

			ex.setErrorCode("CLJL121");
			LicenseLogger.getInstance().error(LicenseNotificationFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		} catch (final NoSuchMethodException nme) {

			// Log the information into log files
			final LicenseException ex = new LicenseException(" NoSuchMethod Exception.");

			ex.setErrorCode("CLJL122");
			LicenseLogger.getInstance().error(LicenseNotificationFactory.class.getName(), "getInstance",
					"error code set to: " + ex.getErrorCode());

			throw ex;
		}

		return licenseNotificationObject;
	}
}
