package alcatel.tess.hometop.gateways.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class <code>Config</code> manages a set of properties.
 * It is similar to the java.util package and adds some convenient
 * accessors. Proxy platform public and private properties are supported.
 *
 */
@SuppressWarnings({ "serial", "javadoc", "rawtypes", "unchecked" })
public class Config extends Properties {
  public Config() {
    this(new Properties(), "properties");
  }
  
  public Config(Properties prop, String resource) {
    super(prop);
    this.resource = resource;
  }
  
  public Config(String resource) throws ConfigException {
    super(new Properties());
    InputStream in = null;
    try {
      load(in = getResourceAsStream(resource));
      this.resource = resource;
    } catch (IOException e) {
      throw new ConfigException("failed to load resource: " + resource, e);
    } finally {
      close(in);
    }
  }
  
  /**
   * Set a property. By default, the property type is private and
   * will erase an eventual public property which name is equals to the
   * one you provide to this method.
   *
   * @param name The property name
   * @param value The property value
   */
  public Object setProperty(String name, String value) {
    return (setPrivateProperty(name, value));
  }
  
  /**
   * Remove a property. By default, the property type is private
   *
   * @param name The property name
   * @param value The property value
   */
  public String removeProperty(String name) throws ConfigException {
    return (removePrivateProperty(name));
  }
  
  /**
   * Set a table property. By default, the property type is private and
   * will erase an eventual public property which name is equals to the
   * one you provide to this method.
   *
   * @param name The property name
   * @param value A table represented as a Vector
   */
  public Object setTable(String name, Vector table) {
    return (setPrivateTable(name, table));
  }
  
  /**
   * Returns an enumeration of all the keys in this property list,
   * including the public/private keys. private keys will overide
   * any eventual public keys which have the same name.
   * 
   * @return  an enumeration of all the keys in this property list.
   * 
   */
  public Enumeration getKeys() {
    return (super.propertyNames());
  }
  
  /**
   * Returns an enumeration of all the keys in this property list,
   * including the public/private keys. private keys will overide
   * any eventual public keys which have the same name.<p>
   * A pattern must be provided in order to filter the listed keys.
   * 
   * @param	pattern  A pattern used to filter the enumeration. A star 
   *		may be placed at tbe beginning, in the midle, or at the
   *		end of the pattern. Only one star is supported.

   * @return	an enumeration of all the keys in this property list.
   * 
   */
  public Enumeration getKeys(String pattern) {
    pattern = pattern.trim();
    
    Enumeration e = super.propertyNames();
    Hashtable h = new Hashtable();
    boolean fromStart = false, fromEnd = false, fromMiddle = false;
    String patternStart = null, patternEnd = null;
    
    if (pattern.charAt(0) == '*') {
      pattern = pattern.substring(1);
      fromEnd = true;
    } else if (pattern.charAt(pattern.length() - 1) == '*') {
      fromStart = true;
      pattern = pattern.substring(0, pattern.length() - 1);
    } else if (pattern.indexOf("*") != -1) {
      fromMiddle = true;
      patternStart = pattern.substring(0, pattern.indexOf("*"));
      patternEnd = pattern.substring(pattern.indexOf("*") + 1);
    }
    
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      boolean matched = false;
      
      if (fromStart) {
        if (key.startsWith(pattern)) {
          matched = true;
        }
      } else if (fromEnd) {
        if (key.endsWith(pattern)) {
          matched = true;
        }
      } else if (fromMiddle) {
        if (key.startsWith(patternStart) && key.endsWith(patternEnd)) {
          matched = true;
        }
      } else {
        if (key.equals(pattern)) {
          matched = true;
        }
      }
      
      if (matched) {
        Object value = super.getProperty(key);
        if (value == null) {
          value = super.get(key);
        }
        if (value == null) {
          value = super.defaults.get(key);
        }
        if (value != null) {
          h.put(key, value);
        }
      }
    }
    
    return (h.keys());
  }
  
  /**
   * Return a string representation of a property value.
   *
   * @param key		The property name.
   * @return		The property value.
   *
   * @throws ConfigException if the property is not found.
   */
  public String getString(String key) throws ConfigException {
    String result = super.getProperty(key);
    
    if (result == null || result.length() == 0) {
      throw new ConfigException(getName(), key, "invalid null property:" + key);
    }
    
    return (result.trim());
  }
  
  /**
   * Return a string representation of a property value.
   * If the property value is not found, then a default value is
   * returned.
   *
   * @param key		The key name.
   * @param defVal	Returned if the key value is not found.
   * @return		The property value.
   *
   * @throws ConfigException if the property is not found.
   */
  public String getString(String key, String defVal) {
    try {
      return (getString(key));
    } catch (ConfigException e) {
      return (defVal);
    }
  }
  
  /**
   * Return an int representation of a property value.
   *
   * @param key		The property name.
   * @return		The property value.
   *
   * @throws ConfigException if the property is not found.
   */
  public int getInt(String key) throws ConfigException {
    try {
      return (Integer.parseInt(getString(key)));
    } catch (NumberFormatException e) {
      throw new ConfigException(getName(), key, " invalid number property");
    }
  }
  
  /**
   * Return an int representation of a property value.
   * If the property value is not found, then a default value is
   * returned.
   *
   * @param key		The key name.
   * @param defVal	Returned if the key value is not found.
   * @return		The property value.
   *
   * @throws ConfigException if the property is not found.
   */
  public int getInt(String key, int defVal) {
    try {
      return (getInt(key));
    } catch (ConfigException e) {
      return (defVal);
    }
  }
  
  /**
   * Return a boolean representation of a property value.
   *
   * @param key		The property name.
   * @return		The property value.
   *
   * @throws ConfigException if the property is not found.
   */
  public boolean getBoolean(String key) throws ConfigException {
    return (getString(key).equalsIgnoreCase("true"));
  }
  
  /**
   * Return a boolean representation of a property value.
   * If the property value is not found, then a default value is
   * returned.
   *
   * @param key		The key name.
   * @param defVal	Returned if the key value is not found.
   * @return		The property value.
   *
   * @throws ConfigException if the property is not found.
   */
  public boolean getBoolean(String key, boolean defVal) {
    try {
      return (getBoolean(key));
    } catch (ConfigException e) {
      return (defVal);
    }
  }
  
  /**
   * Return a long representation of a property value.
   *
   * @param key		The property name.
   * @return		The property value.
   *
   * @throws ConfigException if the property is not found.
   */
  public long getLong(String key) throws ConfigException {
    try {
      return (Long.parseLong(getString(key)));
    } catch (NumberFormatException e) {
      throw new ConfigException(getName(), key, "invalid long property");
    }
  }
  
  /**
   * Return a long representation of a property value.
   * If the property value is not found, then a default value is
   * returned.
   *
   * @param key		The key name.
   * @param defVal	Returned if the key value is not found.
   * @return		The property value.
   *
   * @throws ConfigException if the property is not found.
   */
  public long getLong(String key, long defVal) {
    try {
      return (getLong(key));
    } catch (ConfigException e) {
      return (defVal);
    } catch (NumberFormatException e) {
      return (defVal);
    }
  }
  
  /**
   * Return a vector of a tokenized string.
   *
   * @param key		The property name.
   * @param sep		The separator which is used to tokenize the string.
   * @return		The Vector of tokens.
   *
   * @throws ConfigException if the property is not found.
   */
  public Vector getStringVector(String key, String sep) throws ConfigException {
    String paramList = getString(key);
    StringTokenizer st = new StringTokenizer(paramList, sep);
    Vector list = new Vector();
    
    while (st.hasMoreTokens()) {
      list.addElement(st.nextToken().trim());
    }
    
    return list;
  }
  
  /**
   * Return a vector of a tokenized string.
   *
   * @param key		The property name.
   * @param sep		The separator which is used to tokenize the string.
   * @param def		The value returned if no value is found.
   * @return		The Vector of tokens.
   *
   * @throws ConfigException if the property is not found.
   */
  public Vector getStringVector(String key, String sep, Vector def) {
    try {
      String paramList = getString(key);
      StringTokenizer st = new StringTokenizer(paramList, sep);
      Vector list = new Vector();
      
      while (st.hasMoreTokens()) {
        list.addElement(st.nextToken().trim());
      }
      
      return list;
    } catch (ConfigException e) {
      return (def);
    }
  }
  
  /**
   * Return an array of a tokenized string.
   *
   * @param key		The property name.
   * @param sep		The separator which is used to tokenize the string.
   * @param def		The value returned if no value is found.
   * @return		An array containing all tokens.
   *
   * @throws ConfigException if the property is not found.
   */
  public String[] getStringArray(String key, String sep) throws ConfigException {
    return (getStringArray(key, sep, false));
  }
  
  /**
   * Return an array of a tokenized string.
   *
   * @param key		 The property name.
   * @param sep		 The separator which is used to tokenize the string.
   * @param nullOfNotSet true if null must be returned if no value is found.
   * @return		 An array containing all tokens.
   *
   * @throws ConfigException if the property is not found and if nullIfNotSet is false.
   */
  public String[] getStringArray(String key, String sep, boolean nullIfNotSet) throws ConfigException {
    String paramList = null;
    
    try {
      paramList = getString(key);
    } catch (ConfigException e) {
      if (nullIfNotSet) {
        return (null);
      }
    }
    
    return (getStringArrayFromString(paramList, sep));
  }
  
  /**
   * Tokenize a String and returns tokens in strings array.
   *
   * @param	key The property name.
   * @param	sep The separator which is used to tokenize the string.
   * @return	An array of all string tokens.
   *
   * @throws ConfigException if the property is not found.
   */
  public String[] getStringArrayFromString(String key, String sep) throws ConfigException {
    StringTokenizer st = new StringTokenizer(key, sep);
    String list[] = new String[st.countTokens()];
    
    int i = 0;
    
    while (st.hasMoreTokens()) {
      list[i++] = st.nextToken().trim();
    }
    
    return list;
  }
  
  /**
   * Return a property table.
   *
   * @param key		The table name.
   * @return		The table value.
   *
   * @throws ConfigException if the property is not found.
   */
  public Vector getTable(String key) throws ConfigException {
    Object val = super.get(key);
    Vector result = null;
    if (val == null) {
      result = (Vector) super.defaults.get(key);
    } else if (val instanceof Vector) {
      result = (Vector) super.get(key);
    } else if (val instanceof String)
      try {
        // json serialized table in blueprint mode:
        // each table entry is a JSONObject containing a single name/value
        // the name is the name of the row, 
        // the value is a JSONArray of actual values for the "row"
        // => warning, in the resulting vector, we must reconstruct the columns!
        // example: 
        // [
        //  {"Client_name": ["ModemClient"]},
        //  {"Start": ["False"]},
        //  {"Channel": ["Nextenso"]},
        //  {"Serial_port_id": ["/dev/ttyS0"]},
        //  {"Modem_baudrate": ["9600"]},
        //  {"SMSC_phone_number": [""]},
        //  {"Driver_log_level": ["0"]},
        //  {"GSM_modem_PIN_code": ["0"]},
        //  {"Databits": ["DATABITS_8"]},
        //  {"Parity": ["PARITY_NONE"]},
        //  {"Stopbits": ["STOPBITS_1"]},
        //  {"Maximum_parts_per_message": ["5"]},
        //  {"Default_parts_per_message": ["1"]},
        //  {"Connection_phone_number": [""]}
        // ]
        JSONArray json = new JSONArray((String) val);
        result = new Vector();
        // get the number of columns from the first entry
        // all entries must have the same length
        int nbCol = JSONObject.getNames(json.getJSONObject(0)).length;
        for (int i = 0; i < nbCol; i++) {
          Vector column = new Vector();
          for (int j = 0; j < json.length(); j++) {
            JSONObject entry = json.getJSONObject(j);
            JSONArray row = entry.getJSONArray(JSONObject.getNames(entry)[0]);
            column.add(row.getString(i));
          }
          result.add(column);
        }
      } catch (Exception e) {
        throw new ConfigException("invalid json table " + val, e);
      }
    
    if (result == null) {
      throw new ConfigException(getName(), key, "invalid null property");
    }
    
    //org.apache.log4j.Logger.getLogger("config").warn("returning table property "+result);
    
    return (result);
  }
  
  /**
   * Return a property table.
   *
   * @param key		The table name.
   * @return		The table value.
   *
   * @throws ConfigException if the property is not found.
   */
  public Vector getTable(String key, Vector def) {
    try {
      return (getTable(key));
    }
    
    catch (ConfigException e) {
      return def;
    }
  }
  
  /**
   * Load an url which name is stored in a property value.
   *
   * @param key		The key of the property which contains the url name.
   * @return		The URL described in the key value.
   *
   * @throws ConfigException if the key value is not found.
   */
  public URL getUrl(String key) throws ConfigException {
    try {
      String url = getString(resource);
      return (new URL(url));
    }
    
    catch (MalformedURLException e) {
      throw new ConfigException(resource, e);
    }
  }
  
  /**
   * Get a file name stored in a property value.
   *
   * @param key The property key.
   * @return	The filename stored in the property value. path separators are changed
   *		depending on the running platform ("/" on unix, "\\" on windows).
   *
   * @throws ConfigException if no property value is found.
   */
  public String getFileName(String key) throws ConfigException {
    return (getString(key).replace('/', File.separatorChar));
  }
  
  /**
   * Get The content of a file which name is stored in a property value.
   *
   * @param key The property key.
   * @return	The content of the file name stored in the property value. path separators are changed
   *		depending on the running platform ("/" on unix, "\\" on windows).
   *
   * @throws ConfigException if no property value is found.
   */
  public File getFile(String key) throws ConfigException {
    return (new File(getFileName(key)));
  }
  
  /**
   * Return the content of a file found from the classpath.
   *
   * @param fileName the name of the file to read from the classpath.
   * @return The file content.
   *
   * @throws ConfigException if the file is not found.
   */
  public String getContentFileFromCP(String key) throws ConfigException {
    String file = getString(key);
    InputStream in;
    
    try {
      in = getResourceAsStream(file);
    } catch (IOException e) {
      throw new ConfigException("failed to load resource: " + file, e);
    }
    
    BufferedReader bfr = null;
    
    try {
      bfr = new BufferedReader(new InputStreamReader(in));
      StringBuffer buffer = new StringBuffer();
      
      String line;
      
      while ((line = bfr.readLine()) != null) {
        buffer.append(line);
        buffer.append(Utils.LINE_SEPARATOR);
      }
      
      return (buffer.toString());
    }
    
    catch (IOException e) {
      throw new ConfigException(getName(), e);
    }
    
    finally {
      if (bfr != null) {
        try {
          bfr.close();
        } catch (IOException e) {
        }
      }
    }
  }
  
  /**
   * Check if a property value contains a specific string.
   * The property value is tokenized using a "sep" separator,
   * and each token is compared to the given "val" parameter.
   *
   * @param	key The key property.
   * @param	sep The separator used to tokenize the property 
   *		value.
   * @param	val The value compared to each tokens.
   * @return	true if any tokens match the val parameter, false if not.
   *
   * @throws ConfigException
   */
  public boolean isInList(String key, String sep, String val) throws ConfigException {
    return (isInList(key, sep, val, false));
  }
  
  /**
   * Check if a property value contains a specific string.
   * The property value is tokenized using a "sep" separator,
   * and each token is compared to the given "val" parameter.
   *
   * @param	key The key property.
   * @param	sep The separator used to tokenize the property 
   *		value.
   * @param	val The value compared to each tokens.
   * @param	ignoreCase true if the comparison is case isensitive.
   * @return	true if any tokens match the val parameter, false if not.
   *
   * @throws ConfigException
   */
  public boolean isInList(String key, String sep, String val, boolean ignoreCase) throws ConfigException {
    String paramList = null;
    
    try {
      paramList = getString(key);
    }
    
    catch (ConfigException e) {
      return (false);
    }
    
    StringTokenizer st = new StringTokenizer(paramList, sep);
    
    while (st.hasMoreTokens()) {
      String s = st.nextToken().trim();
      
      if (ignoreCase) {
        if (s.equalsIgnoreCase(val))
          return (true);
      } else {
        if (s.equals(val))
          return (true);
      }
    }
    
    return false;
  }
  
  /**
   * Return an array of classes specified in a property value.
   *
   * @param propName	The property key
   * @param sep		the character that separates the classes specified in the 
   *			property value.
   * @param nullIfNotSet	True if null must be returned in case no value is found.
   */
  public Object[] getClassArray(String propName, String sep, boolean nullIfNotSet) throws ConfigException {
    String hf[] = getStringArray(propName, sep, nullIfNotSet);
    
    if (hf == null) {
      return (null);
    }
    
    Object[] objs = new Object[hf.length];
    
    for (int i = 0; i < hf.length; i++) {
      objs[i] = loadClass(hf[i], this, propName);
    }
    
    return (objs);
  }
  
  public Object[] getClassArrayFromString(String str, String sep) throws ConfigException {
    String hf[] = getStringArrayFromString(str, sep);
    
    if (hf == null) {
      return (null);
    }
    
    Object[] objs = new Object[hf.length];
    
    for (int i = 0; i < hf.length; i++) {
      objs[i] = loadClass(hf[i], this, hf[i]);
    }
    
    return (objs);
  }
  
  /**
   * Instanciate a class name specified in a property.
   *
   * @param key the property name that contains the class name.
   * @param cnf The cnf parameter given to the class constructor (may be null
   *	if the class name do not take a Config param in its constructor)
   * @param nullIfNotPresent true if null must be returned in case no class
   *	name is found in property "key", or false if an exception must be 
   *	thrown. Notice that if the property has a "none" value, then it is
   *	considered as a null property.
   * @return The class instance corresponding to the property valueAg
   *
   * @throws ConfigException If the class could not be created
   */
  public Object getClass(String key, Object param, boolean nullIfNotPresent) throws ConfigException {
    String className = null;
    
    try {
      className = getString(key);
      if (className.equalsIgnoreCase(NONE)) {
        if (nullIfNotPresent)
          return (null);
        
        throw new ConfigException(getName(), key, "invalid null property");
      }
    }
    
    catch (ConfigException e) {
      if (nullIfNotPresent == true) {
        return (null);
      }
      
      throw e;
    }
    
    return (loadClass(className, param, key));
  }
  
  /**
   * Instanciate a class name specified in a property.
   *
   * @param key the property name that contains the class name.
   * @param cnf The cnf parameter given to the class constructor (may be null
   *	if the class name do not take a Config param in its constructor)
   * @param nullIfNotPresent true if null must be returned in case no class
   *	name is found in property "key", or false if an exception must be 
   *	thrown. Notice that if the property has a "none" value, then it is
   *	considered as a null property.
   * @return The class instance corresponding to the property value
   *
   * @throws ConfigException If the class could not be created
   */
  public Object loadClass(String className, Object param, String key) throws ConfigException {
    try {
      Class c = Class.forName(className);
      
      if (param != null) {
        Class paramClass = param.getClass();
        
        while (paramClass != null) {
          try {
            Constructor constr = c.getConstructor(new Class[] { paramClass });
            return (constr.newInstance(new Object[] { param }));
          }
          
          catch (NoSuchMethodException e) {
            paramClass = paramClass.getSuperclass();
          }
        }
        
        throw new ConfigException(getName(), key, "The class \"" + className + "\""
            + " do not have such a constructor.");
      } else {
        Constructor constr = c.getConstructor();
        return (constr.newInstance());
      }
    }
    
    catch (ClassNotFoundException e) {
      if (e.getException() != null) {
        throw new ConfigException(getName(), key, e.getException());
      }
      String msg = "class \"" + className + "\" not found";
      throw new ConfigException(getName(), key, msg);
    }
    
    catch (NoSuchMethodException e) {
      String msg = "the class \"" + className + "\" do not have such a constructor.";
      throw new ConfigException(getName(), key, msg, e);
    } catch (InstantiationException e) {
      String msg = "the class \"" + className + "\" is abstract or is an interface";
      throw new ConfigException(getName(), key, msg);
    } catch (IllegalAccessException e) {
      String msg = "cannot instantiate class \"" + className
          + "\" (not public or located in another package)";
      throw new ConfigException(getName(), key, msg);
    } catch (IllegalArgumentException e) {
      String msg = "the class \"" + className + "\" do not have such a constructor";
      throw new ConfigException(getName(), key, msg);
    } catch (InvocationTargetException e) {
      if (e.getTargetException() instanceof ConfigException) {
        throw (ConfigException) e.getTargetException();
      } else {
        throw new ConfigException(getName(), key, e.getTargetException());
      }
    }
  }
  
  /**
   * Returns the name for this config.
   * 
   * @return the name corresponding to this config instance.
   */
  public String getName() {
    return (resource);
  }
  
  /**
   * Load the property file from the classpath.
   *
   * @param resource	A file stored somewhere in the classpath.
   * @throws IOException if the resource could not be loaded.
   */
  public static InputStream getResourceAsStream(String resource) throws IOException {
    String file = resource;
    
    if (resource.charAt(0) != '/') {
      resource = '/' + resource;
    }
    
    InputStream in = Config.class.getResourceAsStream(resource);
    
    if (in == null) {
      in = Object.class.getResourceAsStream(resource);
    }
    
    if (in == null) {
      ClassLoader ccl = Thread.currentThread().getContextClassLoader();
      if (ccl != null) {
        in = ccl.getResourceAsStream(resource);
      }
    }
    
    if (in == null) {
      in = new FileInputStream(file);
    }
    
    if (in == null) {
      if (file.charAt(0) == '/') {
        file = file.substring(1);
      }
      in = ClassLoader.getSystemClassLoader().getResourceAsStream(file);
    }
    
    return in;
  }
  
  /**
   * Load the property file from the classpath.
   *
   * @param resource	A file stored somewhere in the classpath.
   * @throws IOException if the resource could not be loaded.
   */
  public static Reader getResourceAsReader(String resource) throws IOException {
    return (new InputStreamReader(getResourceAsStream(resource)));
  }
  
  /**
   * Load an url
   *
   * @param resource	The resource name.
   * @return		The URL
   *
   * @throws ConfigException if the key value is not found.
   */
  public static URL getResource(String resource) throws ConfigException {
    if (resource.charAt(0) != '/') {
      resource = '/' + resource;
    }
    
    URL url = Config.class.getResource(resource);
    
    if (url == null) {
      throw new ConfigException("resource not found", resource);
    }
    
    return url;
  }
  
  /**
   * Register a config listener on a regexp property name.
   * the listener will be notified about config changes only
   * if the property names match the regexp (the regexp may
   * contain a star only at begin OR end of the regexp and 
   * only one star is supported.
   */
  public void registerListener(ConfigListener cl, String regexp) {
    listenersByRegexp.add(regexp, cl);
  }
  
  /**
   * Remove a properties listener
   *
   * @param lst The listener
   * @param regexp The regexp used by the listener
   */
  public boolean unregisterListener(ConfigListener lst, String regexp) {
    return listenersByRegexp.delete(regexp, lst) != null;
  }
  
  /**
   * Clear Changes history. This method will erase the history of
   * all previous config modification via setProperty/removeProperty.
   */
  public void clearChangeHistory() {
    propertyChanges.clear();
  }
  
  /**
   * Notify config listeners about property changes.
   * @throws ConfigException if a listener gets some errors
   * while hanlding the property change event.
   */
  public void notifyListeners() throws ConfigException {
    //
    // For each changed properties, lookup relevant listeners
    // interested in.
    //
    Iterator it = propertyChanges.iterator();
    while (it.hasNext()) {
      String propName = (String) it.next();
      
      //
      // Get the list of listener for this property name.
      //
      Enumeration listeners = listenersByRegexp.match(propName);
      while (listeners.hasMoreElements()) {
        ConfigListener cl = (ConfigListener) listeners.nextElement();
        
        //
        // For that listener, find or build an array that will
        // contain modified property names.
        //
        ArrayList props = (ArrayList) listenersProps.get(cl);
        
        if (props == null) {
          props = new ArrayList();
          listenersProps.put(cl, props);
        }
        props.add(propName);
      }
    }
    
    //
    // Now, we have built for each listeners the relevant array of modified
    // property names. We now loop on each listener and call back them,
    // giving to them their property changed.
    //
    ConfigException cex = null;
    it = listenersProps.keySet().iterator();
    
    while (it.hasNext()) {
      ConfigListener cl = (ConfigListener) it.next();
      ArrayList props = (ArrayList) listenersProps.get(cl);
      String[] array = new String[props.size()];
      
      for (int i = 0; i < props.size(); i++) {
        array[i] = (String) props.get(i);
      }
      
      try {
        cl.propertyChanged(this, array);
      } catch (ConfigException e) {
        cex = e;
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
    
    listenersProps.clear();
    clearChangeHistory();
    
    if (cex != null) {
      throw cex;
    }
  }
  
  /**
   * Return a string representation of properties.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("[public:");
    buf.append(defaults.toString());
    buf.append(Utils.LINE_SEPARATOR);
    buf.append("private:");
    buf.append(super.toString());
    buf.append("]");
    return (buf.toString());
  }
  
  /**
   * Reload the properties. All listener will be notified via the
   * Watcher interface.
   */
  public void reload(String resource) throws ConfigException {
    this.resource = resource;
    InputStream in = null;
    try {
      load(in = getResourceAsStream(resource));
    } catch (IOException e) {
      throw new ConfigException("failed to load resource: " + resource, e);
    } finally {
      close(in);
    }
  }
  
  public void writeTo(OutputStream out) throws IOException, ConfigException {
    PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
    
    try {
      Enumeration e = getKeys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        String val = getString(key, null);
        if (val != null) {
          pw.println(key + "=" + val);
        }
      }
    }
    
    finally {
      pw.close();
    }
  }
  
  /**
   * Write the content of this config object into a stream.
   * The param prefixToRemove is used to remove a prefix from property
   * names.
   */
  public void writeTo(OutputStream out, String prefixToRemove) throws IOException, ConfigException {
    PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
    
    try {
      Enumeration e = getKeys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        String val = getString(key, null);
        
        if (key.startsWith(prefixToRemove)) {
          key = key.substring(prefixToRemove.length());
        }
        
        if (val != null) {
          pw.println(key + "=" + val);
        }
      }
    }
    
    finally {
      pw.close();
    }
  }
  
  /**
   * Modify a public property.
   * @deprecated use #setProperty instead
   */
  public Object setDefaultProperty(String name, String value) {
    return (setPublicProperty(name, value));
  }
  
  /**
   * Modify a public table 
   * @deprecated use #setTable instead
   */
  public Object setDefaultTable(String name, Vector value) {
    return (setPublicTable(name, value));
  }
  
  /**
   * Remove a public property
   * @deprecated use removeProperty
   */
  public String removeDefaultProperty(String name) throws ConfigException {
    return (removePublicProperty(name));
  }
  
  public Object setPublicProperty(String name, String value) {
    String old = (String) super.defaults.remove(name);
    super.defaults.put(name, value);
    // notify listeners only if no private vars exists.
    if (super.get(name) == null) {
      propertyChanges.add(name);
    }
    return (old);
  }
  
  public Object setPublicTable(String name, Vector value) {
    Object old = super.defaults.remove(name);
    super.defaults.put(name, value);
    // notify listeners only if no private vars exists.
    if (super.get(name) == null) {
      propertyChanges.add(name);
    }
    return (old);
  }
  
  public String removePrivateProperty(String name) throws ConfigException {
    String old = (String) super.remove(name);
    propertyChanges.add(name);
    return (old);
  }
  
  public String removePublicProperty(String name) throws ConfigException {
    String old = (String) super.defaults.remove(name);
    // notify listeners only if no private vars exists.
    if (super.get(name) == null) {
      propertyChanges.add(name);
    }
    return (old);
  }
  
  public Object setPrivateProperty(String name, String value) {
    String old = (String) super.getProperty(name);
    super.setProperty(name, value);
    propertyChanges.add(name);
    return (old);
  }
  
  public Object setPrivateTable(String name, Vector table) {
    Object old = super.getProperty(name);
    super.put(name, table);
    propertyChanges.add(name);
    return (old);
  }
  
  /**
   * Set a Table or String property value. By default, the property type is private and
   * will erase an eventual public property which name is equals to the
   * one you provide to this method.
   *
   * @param name The property name
   * @param value Either a String or a Vector.
   * @param isPrivate true if the parameter is private, false if public
   * @return either the old String, or the old Vector.
   */
  public Object setObject(String name, Object obj, boolean isPrivate) {
    Object oldPrivate = super.get(name);
    Object oldPublic = super.defaults.get(name);
    
    if (isPrivate) {
      super.put(name, obj);
      propertyChanges.add(name);
      return oldPrivate;
    } else {
      // Public ...
      super.defaults.put(name, obj);
      if (oldPrivate == null) {
        // notify listeners only if no private vars exists.
        propertyChanges.add(name);
      }
      return oldPublic;
    }
  }
  
  /**
   * Return a property value (either a String or a Vector)..
   *
   * @param key		The property name.
   * @return		The property value (either a String, or a Vector)
   */
  public Object getObject(String key) throws ConfigException {
    Object result = super.get(key);
    
    if (result == null) {
      result = super.defaults.get(key);
    }
    
    if (result == null) {
      throw new ConfigException(getName(), key, "invalid null property");
    }
    
    return (result);
  }
  
  /**
   * Return a property value (either a String or a Vector)..
   *
   * @param key		The property name.
   * @param def		The default value to be returned if the prop does not exist.
   * @return		The property value (a String, or a Vector)
   */
  public Object getObject(String key, Object def) {
    try {
      return (getObject(key));
    }
    
    catch (ConfigException e) {
      return def;
    }
  }
  
  /**
   * Remove a public/private property.
   *
   * @param name The property name
   * @param isPrivate true if the parameter is private, false if public
   * @return either a Vector, or a String.
   */
  public Object removeObject(String name, boolean isPrivate) throws ConfigException {
    Object old = null;
    if (isPrivate) {
      old = super.remove(name);
      propertyChanges.add(name);
    } else {
      old = super.defaults.remove(name);
      // notify listeners only if no private vars exists.
      if (super.get(name) == null) {
        propertyChanges.add(name);
      }
    }
    
    return (old);
  }
  
  private void close(InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException ignored) {
      }
    }
  }
  
  /**
   * The name of the resource from which this config has been
   * created.
   */
  private String resource;
  
  /**
   * This set is used to store the property changed for
   * each listeners. Key = listener, Value = ArrayList 
   * containing the list of property changed for the
   * corresponding listener.
   */
  private HashMap listenersProps = new HashMap();
  
  /**
   * List of listeners listening on a regexp.
   */
  private RegexpPool listenersByRegexp = new RegexpPool();
  
  /**
   * List of modified properties (using the setProperty method)
   */
  private HashSet propertyChanges = new HashSet();
  
  /**
   * Constants used to check if no class name if present in a property
   */
  private final static String NONE = "none";
}
