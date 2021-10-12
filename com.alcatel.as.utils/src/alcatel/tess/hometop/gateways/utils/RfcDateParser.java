// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

/**
 * Class used to parse rfc dates.
 */
public class RfcDateParser {
  /**
   * Constructor.
   */
  public RfcDateParser() {
  }
  
  @SuppressWarnings("static-access")
  public static void main(String args[]) throws Exception {
    RfcDateParser p = new RfcDateParser();
    
    if (args.length == 0) {
      System.out.println(p.parse("Thu, 07 Sep 2000 14:28:24 GMT"));
    } else {
      System.out.println(p.parse(args[0]));
    }
  }
  
  /**
   * Formats a Date into a date/time string.
   * 
   * @param date the time value to be formatted into a time string.
   * @return the formatted time string.
   */
  public static synchronized String format(Date date) {
    return (gmtStandardFormats[0].format(date));
  }
  
  /**
   * Parse an rfc date.
   */
  public static Date parse(String s) {
    boolean isGMT = false;
    String dateString = s.trim();
    
    if (dateString.indexOf(GMT) != -1) {
      isGMT = true;
    }
    
    int i = isGMT ? gmtStandardFormats.length : standardFormats.length;
    
    for (int j = 0; j < i; j++) {
      Date date = null;
      
      if (isGMT) {
        date = tryParsing(dateString, gmtStandardFormats[j]);
      } else {
        date = tryParsing(dateString, standardFormats[j]);
      }
      
      if (date != null) {
        if (DEBUG) {
          if (j > 1) {
            System.err.println("RfcDateParser: parser no " + j + " matched the date");
          }
        }
        return date;
      }
    }
    
    return null;
  }
  
  private static synchronized Date tryParsing(String dateString, SimpleDateFormat sdf) {
    try {
      return sdf.parse(dateString);
    } catch (Exception e) {
      return null;
    }
  }
  
  private static SimpleDateFormat standardFormats[];
  private static SimpleDateFormat gmtStandardFormats[];
  private final static String GMT = "GMT";
  private final static boolean DEBUG = (System.getProperty("RfcDateParser.debug", "false").equals("true"));
  
  static {
    // Load date patterns from a property file.
    
    BufferedReader reader = null;
    try {
      URL url = Utils.getResource("utils/RfcDateParser.properties",
                                  new ClassLoader[] { ClassLoader.getSystemClassLoader(),
                                      RfcDateParser.class.getClassLoader() });
      
      if (url == null) {
        throw new ExceptionInInitializerError("Could not find META-INF/RfcDateParser.properties");
      }
      reader = new BufferedReader(new InputStreamReader(url.openStream()));
      String pattern = null;
      
      Vector gmtPatterns = new Vector();
      Vector patterns = new Vector();
      
      while ((pattern = reader.readLine()) != null) {
        pattern = pattern.trim();
        if (pattern.startsWith("#")) {
          continue;
        }
        
        if (pattern.indexOf(GMT) != -1) {
          gmtPatterns.add(pattern);
        } else {
          patterns.add(pattern);
        }
      }
      
      // Initialize our SimpleDateFormat parsers
      
      gmtStandardFormats = new SimpleDateFormat[gmtPatterns.size()];
      for (int i = 0; i < gmtPatterns.size(); i++) {
        gmtStandardFormats[i] = new SimpleDateFormat((String) gmtPatterns.get(i));
        gmtStandardFormats[i].setTimeZone(TimeZone.getTimeZone(GMT));
      }
      
      RfcDateParser.standardFormats = new SimpleDateFormat[patterns.size()];
      for (int i = 0; i < patterns.size(); i++) {
        standardFormats[i] = new SimpleDateFormat((String) patterns.get(i));
      }
    }
    
    catch (Throwable t) {
      throw new ExceptionInInitializerError(t);
    }
    
    finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (Throwable t) {
        }
      }
    }
  }
}
