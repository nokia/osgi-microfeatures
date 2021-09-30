package com.alcatel.as.util.config;

import java.io.ByteArrayInputStream ;
import java.io.File ;
import java.io.FileInputStream ;
import java.io.FileOutputStream ;
import java.nio.channels.FileLock ;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.alcatel.as.util.osgi.MapToDictionary;

/**
 * Helper class used to parse configuration from Dictionaries
 */
public class ConfigHelper {

  /** Our logger */
  static final Logger _logger = Logger.getLogger(ConfigHelper.class);

  /**
   * Get a configuration from a Dictionary.
   * @param dic the Dictionary 
   * @param key the configuration key
   * @return the configuration value
   */
  @SuppressWarnings("unchecked")
  public static String getString(Dictionary dic, String key) {
    return getString(dic, key, null, false);
  }

  /**
   * Get a configuration from a Dictionary.
   * @param dic the Dictionary 
   * @param key the configuration key
   * @param checkEmptyString if true, then empty strings are considered to null values
   * @return the configuration value (null of the value is an empty string)
   */
  @SuppressWarnings("unchecked")
  public static String getString(Dictionary dic, String key, boolean checkEmptyString) {
    return getString(dic, key, null, checkEmptyString);
  }

  /**
   * Get a configuration from a Dictionary.
   * @param dic the Dictionary 
   * @param key the configuration key
   * @param defaultValue the value returned if the configuration does not exist
   * @return the configuration value or te defaultValue
   */
  @SuppressWarnings("unchecked")
  public static String getString(Dictionary dic, String key, String defaultValue) {
    return getString(dic, key, defaultValue, false);
  }

  /**
   * Get a configuration from a Dictionary.
   * 
   * @param dic the Dictionary 
   * @param key the configuratio key
   * @param defaultValue the value returned if the configuration does not exist
   * @param checkEmptyString
   * @return the value of the key, or the default value if no configuration is found, or null 
   * emptyString is true and the configuration value is an empty string.
   */
  @SuppressWarnings("unchecked")
  public static String getString(Dictionary dic, String key, String defaultValue, boolean checkEmptyString) {
    if (dic == null) {
      return defaultValue;
    }
    Object res = dic.get(key);
    if (res == null) {
      return defaultValue;
    }
    if (checkEmptyString && "".equals(res.toString())) {
      return null;
    }
    return res.toString();
  }

  /**
   * Get a configuration, and split it using string separators
   * @param dic the dictionary
   * @param key the configuration key 
   * @param separators the separators
   * @param defaultValue the default values if no configuration value is present in the dictionary
   * @return the configuration value splitted with the separators, or the defaultValue
   */
  @SuppressWarnings("unchecked")
  public static String[] getStrings(Dictionary dic, String key, String separators, String[] defaultValue) {
    if (dic == null) {
      return defaultValue;
    }
    String res = (String) dic.get(key);
    if (res == null) {
      return defaultValue;
    }
    return res.split("[" + separators + "]");
  }

  /**
   * Get a dictionary value in the form of an Integer.
   * @param dic the dictionary
   * @param key the configuration key 
   * @return the int value of the configuration, or -1 if the value is not an integer
   */
  @SuppressWarnings("unchecked")
  public static int getInt(Dictionary dic, String key) {
    return getInt(dic, key, -1);
  }

  /**
   * Get a dictionary value in the form of an Integer.
   * @param dic the dictionary
   * @param key the configuration key 
   * @param defaultValue the default int to return if the configuration value is not present
   * @return the int value of the configuration, or the defaultValue
   */
  @SuppressWarnings("unchecked")
  public static int getInt(Dictionary dic, String key, int defaultValue) {
    if (dic == null) {
      return defaultValue;
    }
    Object res = dic.get(key);
    if (res == null) {
      return defaultValue;
    }
    if (res instanceof Number) {
      return ((Number) res).intValue();
    }
    try {
      return Integer.parseInt(res.toString());
    } catch (NumberFormatException e) {
      _logger.warn("Unexpected value " + res + " for key " + key + ": returning default:" + defaultValue, e);
      return defaultValue;
    }
  }

  /**
   * Get a dictionary value in the form of a Long.
   * @param dic the dictionary
   * @param key the configuration key 
   * @return the long value of the configuration value, or -1 if the value is not a Long
   */
  @SuppressWarnings("unchecked")
  public static long getLong(Dictionary dic, String key) {
    return getLong(dic, key, -1);
  }

  /**
   * Get a dictionary value in the form of a Long.
   * @param dic the dictionary
   * @param key the configuration key 
   * @param defaultValue the default long to return if the configuration value is not present
   * @return the long value of the configuration, or the defaultValue
   */
  @SuppressWarnings("unchecked")
  public static long getLong(Dictionary dic, String key, long defaultValue) {
    if (dic == null) {
      return defaultValue;
    }
    Object res = dic.get(key);
    if (res == null) {
      return defaultValue;
    }
    if (res instanceof Number) {
      return ((Number) res).longValue();
    }
    try {
      return Long.parseLong(res.toString());
    } catch (NumberFormatException e) {
      _logger.warn("Unexpected value " + res + " for key " + key + ": returning default:" + defaultValue, e);
      return defaultValue;
    }
  }

  /**
   * Get a dictionary value in the form of a boolean.
   * @param dic the dictionary
   * @param key the configuration key 
   * @return the boolean value of the configuration value, or false if the value is not a boolean ("true"/"false")
   */
  @SuppressWarnings("unchecked")
  public static boolean getBoolean(Dictionary dic, String key) {
    return getBoolean(dic, key, false);
  }

  /**
   * Get a dictionary value in the form of a boolean.
   * @param dic the dictionary
   * @param key the configuration key 
   * @param defaultValue the default boolean to return if the configuration value is not present
   * @return the boolean value of the configuration, or the defaultValue
   */
  @SuppressWarnings("unchecked")
  public static boolean getBoolean(Dictionary dic, String key, boolean defaultValue) {
    if (dic == null) {
      return defaultValue;
    }
    Object res = dic.get(key);
    if (res == null) {
      return defaultValue;
    }
    if (res instanceof Boolean) {
      return ((Boolean) res).booleanValue();
    }
    return Boolean.parseBoolean(res.toString());
  }

  // Same methods as above, but using Map instead of Dictionary ...
  
  /**
   * Get a configuration from a Dictionary.
   * @param dic the Dictionary 
   * @param key the configuration key
   * @return the configuration value
   */
  @SuppressWarnings("unchecked")
  public static String getString(Map<String, ?> dic, String key) {
    return getString(toDictionary(dic), key);
  }

  /**
   * Get a configuration from a Dictionary.
   * @param dic the Dictionary 
   * @param key the configuration key
   * @param checkEmptyString if true, then empty strings are considered to null values
   * @return the configuration value (null of the value is an empty string)
   */
  @SuppressWarnings("unchecked")
  public static String getString(Map<String, ?> dic, String key, boolean checkEmptyString) {
    return getString(toDictionary(dic), key, checkEmptyString);
  }

  /**
   * Get a configuration from a Dictionary.
   * @param dic the Dictionary 
   * @param key the configuration key
   * @param defaultValue the value returned if the configuration does not exist
   * @return the configuration value or te defaultValue
   */
  @SuppressWarnings("unchecked")
  public static String getString(Map<String, ?> dic, String key, String defaultValue) {
    return getString(toDictionary(dic), key, defaultValue);
  }

  /**
   * Get a configuration from a Dictionary.
   * 
   * @param dic the Dictionary 
   * @param key the configuratio key
   * @param defaultValue the value returned if the configuration does not exist
   * @param checkEmptyString
   * @return the value of the key, or the default value if no configuration is found, or null 
   * emptyString is true and the configuration value is an empty string.
   */
  @SuppressWarnings("unchecked")
  public static String getString(Map<String, ?> dic, String key, String defaultValue, boolean checkEmptyString) {
    return getString(toDictionary(dic), key, defaultValue, checkEmptyString);
  }

  /**
   * Get a configuration, and split it using string separators
   * @param dic the dictionary
   * @param key the configuration key 
   * @param separators the separators
   * @param defaultValue the default values if no configuration value is present in the dictionary
   * @return the configuration value splitted with the separators, or the defaultValue
   */
  @SuppressWarnings("unchecked")
  public static String[] getStrings(Map<String, ?> dic, String key, String separators, String[] defaultValue) {
    return getStrings(toDictionary(dic), key, separators, defaultValue);
  }

  /**
   * Get a dictionary value in the form of an Integer.
   * @param dic the dictionary
   * @param key the configuration key 
   * @return the int value of the configuration, or -1 if the value is not an integer
   */
  @SuppressWarnings("unchecked")
  public static int getInt(Map<String, ?> dic, String key) {
    return getInt(toDictionary(dic), key);
  }

  /**
   * Get a dictionary value in the form of an Integer.
   * @param dic the dictionary
   * @param key the configuration key 
   * @param defaultValue the default int to return if the configuration value is not present
   * @return the int value of the configuration, or the defaultValue
   */
  @SuppressWarnings("unchecked")
  public static int getInt(Map<String, ?> dic, String key, int defaultValue) {
    return getInt(toDictionary(dic), key, defaultValue);
  }

  /**
   * Get a dictionary value in the form of a Long.
   * @param dic the dictionary
   * @param key the configuration key 
   * @return the long value of the configuration value, or -1 if the value is not a Long
   */
  @SuppressWarnings("unchecked")
  public static long getLong(Map<String, ?> dic, String key) {
    return getLong(toDictionary(dic), key);
  }

  /**
   * Get a dictionary value in the form of a Long.
   * @param dic the dictionary
   * @param key the configuration key 
   * @param defaultValue the default long to return if the configuration value is not present
   * @return the long value of the configuration, or the defaultValue
   */
  @SuppressWarnings("unchecked")
  public static long getLong(Map<String, ?> dic, String key, long defaultValue) {
    return getLong(toDictionary(dic), key, defaultValue);
  }

  /**
   * Get a dictionary value in the form of a boolean.
   * @param dic the dictionary
   * @param key the configuration key 
   * @return the boolean value of the configuration value, or false if the value is not a boolean ("true"/"false")
   */
  @SuppressWarnings("unchecked")
  public static boolean getBoolean(Map<String, ?> dic, String key) {
    return getBoolean(toDictionary(dic), key);
  }

  /**
   * Get a dictionary value in the form of a boolean.
   * @param dic the dictionary
   * @param key the configuration key 
   * @param defaultValue the default boolean to return if the configuration value is not present
   * @return the boolean value of the configuration, or the defaultValue
   */
  @SuppressWarnings("unchecked")
  public static boolean getBoolean(Map<String, ?> dic, String key, boolean defaultValue) {
    return getBoolean(toDictionary(dic), key, defaultValue);
  }  
  
  /**
   * Convert a map to a dictionary
   * @param map the map to convert into a dictionary
   * @return the converted dictionary or null if the original map was null
   */
  public static Dictionary toDictionary(Map<String, ?> map) {
    return map == null ? null : new MapToDictionary(map);
  }

  /** Global configuration directory */
  private static final String CONF_DIR = getIntallDir() + "/var/global" ;
  /** Global configuration file */
  private static final String CONF_FILE = CONF_DIR + "/system.cfg" ;
  /** Lock file for shared configuration file access */
  private static final String LOCK_FILE = CONF_DIR + "/.lock" ;
  static {
    try {
      File dir = new File (CONF_DIR) ;
      if (dir.exists()) {
        if ( ! dir.isDirectory()) {
          _logger.error ("Global configuration directory " + CONF_DIR + " exists and is not a directory") ;
        }
      } else if ( ! dir.mkdirs()) {
        _logger.error ("Unable to create global configuration directory " + CONF_DIR) ;
      }
    } catch (Throwable t) {
      _logger.error ("Unable to create global configuration directory " + CONF_DIR) ;
    }
  }
  /** Gogo host name */
  private static String gogoHost ;
  /** Gogo port number */
  private static int gogoPort ;
  
  /**
   * Returns the installation directory.
   */
  public static String getIntallDir() {
	  return System.getProperty(ConfigConstants.INSTALL_DIR, System.getenv(ConfigConstants.INSTALL_DIR));
  }

  /**
   * Handle the Gogo configuration from the provided Felix configuration
   * @param heading Heading to use in property names
   * @param value Property value
   * @return True if successful
   */
  public static boolean handleGogoConfiguration (String heading, String value) {
    if (getGogoConfiguration (value)) {
      setGlobalProperties (heading + ".GOGO.HOST", gogoHost, heading + ".GOGO.PORT", Integer.toString (gogoPort)) ;
      return true ;
    } else {
      removeGlobalProperties (heading + ".GOGO.HOST", heading + "JMX.GOGO.PORT") ;
      return false ;
    }
  }

  /**
   * Handle the Gogo configuration from the provided Felix configuration
   * @param heading Heading to use in property names
   * @param value Property value
   * @return True if successful
   */
  public static boolean getGogoConfiguration (String value) {
    String propValue = null ;
    try (ByteArrayInputStream in = new ByteArrayInputStream (value.getBytes())) {
      Properties props = new Properties() ;
      props.load (in) ;
      propValue = props.getProperty ("gosh.args") ;
      String s = propValue.substring (propValue.indexOf (" -i ") + 4) ;
      String[] arr = s.split (" ") ;
      gogoHost = arr[0] ;
      if (gogoHost.startsWith ("${")) {
        s = gogoHost.substring (2, gogoHost.length() - 1) ;
        if ((gogoHost = System.getenv (s)) == null) {
          gogoHost = System.getProperty (s) ;
        }
        gogoHost.length() ; // To get an exception if not found
      }
      gogoPort = Integer.parseInt (arr[2]) ;
      return true ;
    } catch (Throwable t) {
      _logger.warn ("Gogo configuration is not defined or invalid") ;
      return false ;
    }
  }

  /**
   * Retrieve our gogo host name
   * @return Gogo host name
   */
  public static String getGogoHost() {
    return gogoHost ;
  }

  /**
   * Retrieve our gogo port number
   * @return Gogo port number
   */
  public static int getGogoPort() {
    return gogoPort ;
  }

  /**
   * Remove the specified global properties
   * @param keys Properties to remove
   * @return True if the update was successful, false otherwise
   */
  public static boolean removeGlobalProperties (String... keys) {
    if (keys.length == 0) {
      return true ;
    }
    synchronized (CONF_DIR) {
      try (FileOutputStream lockFp = new FileOutputStream (LOCK_FILE);
          FileLock lock = lockFp.getChannel().lock()) {
        Properties props = loadProperties() ;
        for (String key : keys) {
          props.remove (key) ;
        }
        storeProperties (props) ;
        return true ;
      } catch (Throwable t) {
        return false ;
      }
    }
  }

  /**
   * Set the value of global properties
   * @param properties Properties to create or update
   * @param properties Properties to update
   * @return True if the update was successful, false otherwise
   */
  public static boolean setGlobalProperties (String... properties) {
    if (properties.length == 0) {
      return true ;
    }
    if ((properties.length & 1) != 0) {
      _logger.error ("setGlobalProperties: arguments are not key/value pairs") ;
      return false ;
    }
    synchronized (CONF_DIR) {
      try (FileOutputStream lockFp = new FileOutputStream (LOCK_FILE);
          FileLock lock = lockFp.getChannel().lock()) {
        Properties props = loadProperties() ;
        for (int i = 0; i < properties.length; i += 2) {
          props.setProperty (properties[i], properties[i + 1]) ;
        }
        storeProperties (props) ;
        return true ;
      } catch (Throwable t) {
        return false ;
      }
    }
  }

  /**
   * Load our global properties
   * @return Global properties
   * @throws RuntimeException if unable to store the property
   */
  private static Properties loadProperties() {
    Properties props = new Properties() ;
    try (FileInputStream in = new FileInputStream (CONF_FILE)) {
      props.load (in) ;
    } catch (Throwable t) {
    }
    return props ;
  }

  /**
   * Store our global properties
   * @param props Properties to store
   */
  private static void storeProperties (Properties props) {
    try (FileOutputStream out = new FileOutputStream (CONF_FILE)) {
      props.store (out, "Automatically generated. Do not edit this file") ;
    } catch (Throwable t) {
      _logger.error ("Unable to write properties to " + CONF_FILE, t) ;
      throw new RuntimeException ("Unable to write properties") ;
    }
  }
}
