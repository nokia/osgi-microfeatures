// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.bnd;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alcatel_lucent.as.management.annotation.stat.ConsolidationMode;

import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.Descriptors.TypeRef;

public class Utils {
  // Pattern used to check if a field type is a String
  final static Pattern TSTRING = Pattern.compile("Ljava/lang/String;");
  
  // Pattern used to check if a field type is a Integer
  final static Pattern TINTEGER = Pattern.compile("I");

  // Pattern used to parse classes from class descriptors;
  public final static Pattern CLASS = Pattern.compile("L([^;]+);");
  
  // Pattern used to parse class names;
  //public final static Pattern CLASSNAME = Pattern.compile("([a-zA-Z_0-9]*\\.)*[a-zA-Z_0-9]*");
  
  // Pattern used to check if counter method returns ant int and takes no parameters
  public final static Pattern COUNTER_METHOD = Pattern.compile("\\(\\)I");
  
  /**
   * Checks if a field descriptor matches a given pattern.
   * @param annotation 
   * @param field the field whose type descriptor is checked
   * @param descriptor the field descriptor to be checked
   * @param descriptor2 
   * @param pattern the pattern to use
   * @param errmsg additional error message to display in case the field has not the correct type.
   * @throws IllegalArgumentException if the method signature descriptor does not match the given pattern.
   */
  static boolean checkDescriptor(String descriptor, Pattern pattern)
  
  {
    Matcher matcher = pattern.matcher(descriptor);
    return matcher.matches();
  }
  
  /**
   * Parses a class.
   * @param clazz the class to be parsed (the package is "/" separated).
   * @param pattern the pattern used to match the class.
   * @param group the pattern group index where the class can be retrieved.
   * @return the parsed class.
   */
  private static String parseClass(String clazz, Pattern pattern, int group) {
    Matcher matcher = pattern.matcher(clazz);
    if (matcher.matches()) {
      return matcher.group(group).replace("/", ".");
    } else {
      throw new IllegalArgumentException("Invalid class descriptor: " + clazz);
    }
  }
  
  /**
   * Writes the specified params in a print writer
   */
  static void print(PrintWriter pw, Object ... params) {
    for (Object p : params) {
      pw.write(p.toString());
    }
  }
  
  /**
   * Get an annotation attribute, and return a default value if its not present.
   * @param <T> the type of the variable which is assigned to the return value of this method.
   * @param properties The annotation we are parsing
   * @param name the attribute name to get from the annotation
   * @param defaultValue the default value to return if the attribute is not found in the annotation
   * @return the annotation attribute value, or the defaultValue if not found
   */
  @SuppressWarnings("unchecked")
  static <T> T get(Annotation properties, String name, T defaultValue) {
    T value = (T) properties.get(name);
    return value != null ? value : defaultValue;
  }
  
  /**
   * Returns an oid array as a string.
   */
  public static String getOid(Integer[] oid) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < oid.length; i++) {
      sb.append(oid[i]);
      if (i < oid.length - 1) {
        sb.append(".");
      }
    }
    return sb.toString();
  }
  
  /**
   * Convert an consolidation mode to a string
   * A null mode should corresponds to the default mode ( i.e the sum ) to be applied
   * A 'none' value returned means the consolidation do not be applied on counter/gauge
   * @param mode enumeration
   * @return mode as string or null when sum is wanted
   */
  public static String consolidationModeToString(ConsolidationMode mode) {
	  	if( mode == null)
	  		return null;
	    switch (mode) {
	    default:
	    	// Walk through
	    case SUM:
	      return null;
	    case AVERAGE:
	      return "average";
	    case MIN:
	      return "min";
	    case MAX:
	      return "max";
	    case NONE:	    
	      return "none";
	    }
 }
  

  /**
   * Get the annotation name (FQN).
   */
  public static String getName(Annotation annot) {
      return annot.getName().getFQN();
  }
  
  /**
   * Parse the value of a given annotation attribute (which is of type 'class').
   * This method is compatible with bndtools 2.4.1 (where the annotation.get() method returns a String of the form "Lfull/class/name;"),
   * and with bndtools 3.x.x (where the annotation.get() method returns a TypeRef).
   * 
   * @param annot the annotation which contains the given attribute
   * @param attr the attribute name (of 'class' type).
   * @return the annotation class attribute value
   */
  public static String parseClassAttrValue(Object value) {
      if (value instanceof String)
      {
          return parseClass((String) value, CLASS, 1);
      }
      else if (value instanceof TypeRef) 
      {
          return ((TypeRef) value).getFQN();
      } 
      else if (value == null) {
          return null;
      }
      else {
          throw new IllegalStateException("can't parse class attribute value from " + value);
      }
  }
}
