package com.nokia.licensing.plugins;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import com.nokia.licensing.agnosticImpl.CLJLPreferencesImpl;
import com.nokia.licensing.agnosticImpl.CredentialAccessImpl;
import com.nokia.licensing.agnosticImpl.DataBasePluginImpl;
import com.nokia.licensing.interfaces.CLJLPreferences;
import com.nokia.licensing.interfaces.CredentialAccess;
import com.nokia.licensing.interfaces.DataBasePlugin;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.interfaces.LoggingPlugin;


/**
 *
 * @author twozniak
 */
public final class PluginRegistry {

	private static PluginRegistry registry;
	private final Map<Class<?>, Object> plugins = new HashMap<Class<?>, Object>();

	public static PluginRegistry getRegistry() throws LicenseException {
		if (registry == null) {
			registry = new PluginRegistry(null);
		}
		return registry;
	}

	public static boolean setCLJLPreferences(final CLJLPreferences preferences) throws LicenseException {
		if (registry == null) {
			registry = new PluginRegistry(preferences);
			return true;
		}
		return false;
	}

	private PluginRegistry(final CLJLPreferences preferences) throws LicenseException {
		registerLegacyPlugins(preferences == null ? new CLJLPreferencesImpl() : preferences);
	}

	public void registerPlugin(final Class<?> aClass, final Object aInstance) {
		if (!aClass.isInterface()) {
			throw new IllegalStateException(aClass.getName() + " is not an interface!");
		}
		this.plugins.put(aClass, aInstance);
	}

	@SuppressWarnings("unchecked")
	public <T> T getPlugin(final Class<T> aPluginInterface) {
		return (T) this.plugins.get(aPluginInterface);
	}

	private void registerLegacyPlugins(final CLJLPreferences cljlPreferences) throws LicenseException {
		registerPlugin(CLJLPreferences.class, cljlPreferences);

		try {
			registerCredentialAccess(cljlPreferences);
			registerDataBasePlugin(cljlPreferences);
			registerLoggingPlugin(cljlPreferences);

		} catch (final ClassNotFoundException e) {
			throw new LicenseException("Couldn't instantiate a class. Got a ClassNotFoundException: " + e.getMessage());
		} catch (final InstantiationException e) {
			throw new LicenseException("CLJL125",
					"Couldn't instantiate a class. Got an InstantiationException: " + e.getMessage());
		} catch (final IllegalAccessException e) {
			throw new LicenseException("CLJL126",
					"Couldn't instantiate a class. Got an IllegalAccessException: " + e.getMessage());
		} catch (final NoSuchMethodException e) {
			throw new LicenseException("Couldn't instantiate a class. Got an NoSuchMethodException: " + e.getMessage());
		}
	}

	private void registerDataBasePlugin(final CLJLPreferences cljlPreferences)
			throws LicenseException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		final Preferences preferences = cljlPreferences.getPreferencesSystemRoot();
		final String qualifiedClassName = preferences.node("dataStorage").get("dataStorageImpl",
				DataBasePluginImpl.class.getName());
		final DataBasePlugin plugin = (DataBasePlugin) Class.forName(qualifiedClassName).newInstance();
		registerPlugin(DataBasePlugin.class, plugin);
	}

	private void registerCredentialAccess(final CLJLPreferences cljlPreferences) throws LicenseException,
			InstantiationException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
		final Preferences preferences = cljlPreferences.getPreferencesSystemRoot();
		final String credImplClass = preferences.node("credentials").get("credAccImpl",
				CredentialAccessImpl.class.getName());
		final CredentialAccess credAcc = (CredentialAccess) Class.forName(credImplClass).newInstance();
		registerPlugin(CredentialAccess.class, credAcc);
	}

	private void registerLoggingPlugin(final CLJLPreferences cljlPreferences)
			throws LicenseException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		final Preferences preferences = cljlPreferences.getPreferencesSystemRoot();
		registerPluginByName(LoggingPlugin.class,
				preferences.node("logger").get("impl", "com.nokia.licensing.logging.JavaLoggingPlugin"));
	}

	private <X> void registerPluginByName(final Class<X> clazz, final String className)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		@SuppressWarnings("unchecked")
		final X instance = (X) Class.forName(className).newInstance();
		registerPlugin(clazz, instance);

	}
}
