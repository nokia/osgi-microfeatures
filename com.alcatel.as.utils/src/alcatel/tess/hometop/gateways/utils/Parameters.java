// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Observable;
import java.util.Properties;

/**
 * Class used to manage public (default) and private (override default) properties.
 */
public class Parameters extends Observable {
  /** Types used by the valueOf methods ... */
  public enum Type {
    STRING, INT, LONG, BOOLEAN, DOUBLE, FLOAT,
  };
  
  /** 
   * Constructor. 
   */
  public Parameters() {
  }
  
  public Parameters(String[] args) {
    load(args, false);
  }
  
  public Parameters(InputStream in) throws IOException {
    load(in, false);
  }
  
  /** 
   * Return a property.
   * @return the prop value, or null if the property has not been found.
   */
  public synchronized Object get(Object key) {
    Object result = _properties.get(key);
    if (result == null) {
      result = _defaultProperties.get(key);
    }
    return (result);
  }
  
  /** 
   * Return a property.
   * @return the prop value, or def if the property has not been found.
   */
  public synchronized Object get(Object key, Object def) {
    Object result = _properties.get(key);
    if (result == null) {
      result = _defaultProperties.get(key);
    }
    return (result != null ? result : def);
  }
  
  /**
   * Return the value of a property. The value type is specifieed by the Type enum.
   */
  public synchronized Object valueOf(Object key, Type t) throws IllegalArgumentException {
    Object val = get(key);
    if (val == null) {
      return null;
    }
    
    Object ret = null;
    
    switch (t) {
    case STRING:
      ret = val.toString();
      break;
    
    case INT:
      ret = Integer.valueOf(val.toString());
      break;
    
    case BOOLEAN:
      ret = Boolean.valueOf(val.toString());
      break;
    
    case LONG:
      ret = Long.valueOf(val.toString());
      break;
    
    case DOUBLE:
      ret = Double.valueOf(val.toString());
      break;
    
    case FLOAT:
      ret = Float.valueOf(val.toString());
      break;
    }
    
    return ret;
  }
  
  /**
   * Return the value of a property. The value type is specifieed by the Type enum.
   * If the value could not be convertet, def is returned.
   */
  public synchronized Object valueOf(Object key, Type t, Object def) {
    try {
      Object val = valueOf(key, t);
      if (val == null) {
        val = def;
      }
      return val;
    }
    
    catch (IllegalArgumentException e) {
      return def;
    }
  }
  
  /**
   * Set a property.
   */
  public synchronized Object set(Object name, Object value, boolean isDefault) {
    if (value instanceof String) {
      value = ((String) value).trim();
    }
    
    Object old = _properties.get(name);
    Object oldDefault = _defaultProperties.get(name);
    
    if (isDefault) {
      _defaultProperties.put(name, value);
      if (old == null) {
        // notify listeners only if no properties exists.
        _propertiesChanged.add(name);
        super.setChanged();
      }
      return oldDefault;
    } else {
      _properties.put(name, value);
      _propertiesChanged.add(name);
      super.setChanged();
      return old;
    }
  }
  
  /**
   * Return all property names.
   * @param a pattern (only one "*" is supported).
   */
  public synchronized Enumeration getKeys(String pattern) {
    // Get the list of property names.
    Hashtable allProperties = new Hashtable();
    for (Enumeration e = _defaultProperties.keys(); e.hasMoreElements();) {
      Object key = e.nextElement();
      allProperties.put(key, _defaultProperties.get(key));
    }
    for (Enumeration e = _properties.keys(); e.hasMoreElements();) {
      Object key = e.nextElement();
      allProperties.put(key, _properties.get(key));
    }
    Enumeration all = allProperties.keys();
    
    // Filter the property names.
    
    Hashtable filteredPropeties = new Hashtable();
    while (all.hasMoreElements()) {
      String key = all.nextElement().toString();
      if (match(key, pattern)) {
        Object value = get(key);
        filteredPropeties.put(key, value);
      }
    }
    
    return (filteredPropeties.keys());
  }
  
  /**
   * Remove a property.
   */
  public synchronized Object remove(Object name, boolean isDefault) {
    Object old = null;
    if (isDefault) {
      old = _defaultProperties.remove(name);
      // notify listeners only if no properties vars exists.
      if (_properties.get(name) == null) {
        _propertiesChanged.add(name);
        super.setChanged();
      }
    } else {
      old = _properties.remove(name);
      _propertiesChanged.add(name);
      super.setChanged();
    }
    
    return (old);
  }
  
  /**
   * Clear all properties.
   */
  public void clear() {
    synchronized (this) {
      Enumeration e = getKeys("*");
      while (e.hasMoreElements()) {
        _propertiesChanged.add(e.nextElement());
        super.setChanged();
      }
      _defaultProperties.clear();
      _properties.clear();
    }
  }
  
  public void load(String[] args, boolean isDefault) {
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-")) {
        String key = args[i].substring(1);
        if (key.startsWith("-")) {
          key = key.substring(1);
        }
        String val = "";
        
        if (i < args.length - 1 && !args[i + 1].startsWith("-")) {
          val = args[i + 1];
          i++;
        }
        
        set(key, val, isDefault);
      }
    }
  }
  
  /**
   * Load some properties from an inputstream.
   */
  public void load(InputStream in, boolean isDefault) throws IOException {
    Properties p = new Properties();
    p.load(in);
    load(p, isDefault);
  }
  
  /**
   * Reload a property file.
   */
  public void reload(InputStream in, boolean isDefault) throws IOException {
    Parameters newParams = new Parameters();
    
    synchronized (this) {
      newParams.load(in, isDefault);
      
      // Check for removed properties ...
      Enumeration e = getKeys("*");
      while (e.hasMoreElements()) {
        Object oldName = e.nextElement();
        if (newParams.get(oldName) == null) {
          remove(oldName, isDefault);
        }
      }
      
      // Check for modified / added properties
      e = newParams.getKeys("*");
      while (e.hasMoreElements()) {
        Object newName = e.nextElement();
        Object newValue = newParams.get(newName);
        
        Object oldValue = get(newName);
        if (oldValue == null || !(oldValue.equals(newValue))) {
          set(newName, newValue, isDefault);
        }
      }
    }
  }
  
  public synchronized void load(File f, boolean isDefault) throws IOException {
    _lastModifiedFile = f.lastModified();
    FileInputStream in = new FileInputStream(f);
    try {
      load(in, isDefault);
    } finally {
      in.close();
    }
  }
  
  public synchronized void reload(File f, boolean isDefault) throws IOException {
    if (f.lastModified() > _lastModifiedFile) {
      _lastModifiedFile = f.lastModified();
      FileInputStream in = new FileInputStream(f);
      try {
        reload(in, isDefault);
      } finally {
        in.close();
      }
    }
  }
  
  /**
   * Load some properties from a Properties object.
   */
  public void load(Properties p, boolean isDefault) {
    Enumeration e = p.propertyNames();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      String val = p.getProperty(key);
      set(key, val, isDefault);
    }
  }
  
  /**
   * Copy our properties into a Properties object.
   */
  public synchronized Properties toProperties() {
    Properties p = new Properties();
    
    Enumeration e = _defaultProperties.keys();
    while (e.hasMoreElements()) {
      Object key = e.nextElement();
      Object val = _defaultProperties.get(key);
      p.put(key, val);
    }
    
    e = _properties.keys();
    while (e.hasMoreElements()) {
      Object key = e.nextElement();
      Object val = _properties.get(key);
      p.put(key, val);
    }
    
    return p;
  }
  
  public synchronized void writeTo(OutputStream out) throws IOException {
    PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
    
    try {
      Enumeration e = getKeys("*");
      while (e.hasMoreElements()) {
        Object key = e.nextElement();
        if (!(key instanceof String)) {
          continue;
        }
        Object val = get(key);
        pw.println(key + "=" + val);
      }
    }
    
    finally {
      pw.close();
    }
  }
  
  /**
   * Displays our properties.
   */
  public synchronized String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("DEFAULT ");
    buf.append(_defaultProperties.toString());
    buf.append(" NORMAL ");
    buf.append(_properties.toString());
    return (buf.toString());
  }
  
  /**
   * Reset the property change history, so no observers will be notified the next time the notifyObservers()
   * method is called.
   */
  public synchronized void clearModificationHistory() {
    _propertiesChanged.clear();
    super.clearChanged();
  }
  
  // ------------------ Viewable methods -------------------------------------*/
  
  public void notifyObservers() {
    String[] propertiesChanged = null;
    
    synchronized (this) {
      if (!super.hasChanged()) {
        return;
      }
      propertiesChanged = new String[_propertiesChanged.size()];
      Iterator it = _propertiesChanged.iterator();
      int i = 0;
      while (it.hasNext()) {
        propertiesChanged[i++] = it.next().toString();
      }
      _propertiesChanged.clear();
    }
    
    // Notify observers outside our lock, in order to avoid dead lock ...
    super.notifyObservers(propertiesChanged);
  }
  
  /**
   * Tells if a string match a simple pattern.
   * The pattern may contains a "*" (and only one).
   */
  private static boolean match(String str, String pattern) {
    boolean fromStart = false, fromEnd = false, fromMiddle = false;
    String patternStart = null, patternEnd = null;
    pattern = pattern.trim();
    
    if (pattern.charAt(0) == '*') {
      pattern = pattern.substring(1);
      fromEnd = true;
    } else if (pattern.charAt(pattern.length() - 1) == '*') {
      fromStart = true;
      pattern = pattern.substring(0, pattern.length() - 2);
    } else if (pattern.indexOf("*") != -1) {
      fromMiddle = true;
      patternStart = pattern.substring(0, pattern.indexOf("*"));
      patternEnd = pattern.substring(pattern.indexOf("*") + 1);
    }
    
    if (fromStart) {
      if (str.startsWith(pattern)) {
        return true;
      }
    } else if (fromEnd) {
      if (str.endsWith(pattern)) {
        return true;
      }
    } else if (fromMiddle) {
      if (str.startsWith(patternStart) && str.endsWith(patternEnd)) {
        return true;
      }
    } else {
      if (str.equals(pattern)) {
        return true;
      }
    }
    
    return false;
  }
  
  // ------------------ Private methods -------------------------------------*/
  
  /** The default (public) properties. */
  private Hashtable _defaultProperties = new Hashtable();
  
  /** The properties (private) which override default (public) properties. */
  private Hashtable _properties = new Hashtable();
  
  /** The list of modified property names. */
  private HashSet _propertiesChanged = new HashSet();
  
  /** Last time a prop file has been modified. */
  private long _lastModifiedFile;
}
