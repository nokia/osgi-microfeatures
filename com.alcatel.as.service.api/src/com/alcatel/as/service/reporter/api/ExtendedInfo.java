// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.reporter.api ;

import com.alcatel_lucent.as.management.annotation.alarm.AlarmType ;
import com.alcatel_lucent.as.management.annotation.alarm.ProbableCause ;
import com.alcatel_lucent.as.management.annotation.alarm.DiscriminatingFields ;

import java.io.DataInputStream ;
import java.io.DataOutputStream ;
import java.io.Serializable ;
import java.io.ObjectInputStream ;
import java.io.ObjectOutputStream ;
import java.io.IOException ;
import java.io.ObjectStreamException ;

/**
 * Extended alarm information
 */
public class ExtendedInfo implements Serializable {

  /** Unique serial UID */
  private static final long serialVersionUID = 66507829730534110L ;

  /** Json tag: ExtendedInfo object */
  public static final String EXTENDED_INFO_TAG = "info" ;
  /** Json tag: Alarm type */
  public static final String ALARM_TYPE_TAG = "type" ;
  /** Json tag: Probable cause */
  public static final String PROBABLE_CAUSE_TAG = "cause" ;
  /** Json tag: User1 */
  public static final String USER1_TAG = "user1" ;
  /** Json tag: User2 */
  public static final String USER2_TAG = "user2" ;
  /** Json tag: Discriminating fields */
  public static final String DISCRIMINATING_FIELDS_TAG = "fields" ;

  /** Generic default instance */
  private static final ExtendedInfo defaultInfo = new ExtendedInfo() ;

  /**
   * Retrieve our default instance. Avoids endless instantiations whenever the extended
   * information is not used
   * @return Default instance
   */
  public static ExtendedInfo getDefault() {
    return defaultInfo ;
  }

  /**
   * Parse a string representation of discriminating fields
   * @param s String representation
   * @return Discriminating fields value
   */
  public static int getDiscriminatingFields (String s) {
    int discriminatingFields = 0 ;
    if (s == null) {
      throw new RuntimeException ("Unable to parse discriminating fields: Nothing to parse") ;
    }
    String[] arr = s.split (" ") ;
    if (arr.length == 1) {
      arr = s.split (":") ;
    }
    for (int i = 0; i < arr.length; ++i) {
      if (arr[i].equals ("HOST")) {
        discriminatingFields |= DiscriminatingFields.HOST ;
      } else if (arr[i].equals ("INSTANCE")) {
        discriminatingFields |= DiscriminatingFields.INSTANCE ;
      } else if (arr[i].equals ("USER1")) {
        discriminatingFields |= DiscriminatingFields.USER1 ;
      } else if (arr[i].equals ("USER2")) {
        discriminatingFields |= DiscriminatingFields.USER2 ;
      } else {
        throw new RuntimeException ("Unknown field " + arr[i] + " in discriminating fields") ;
      }
    }
    return discriminatingFields ;
  }

  /** Alarm type */
  private AlarmType alarmType ;
  /** Probable cause */
  private ProbableCause probableCause ;
  /** User field 1 */
  private String user1 ;
  /** User field 2 */
  private String user2 ;
  /** Discriminating fields */
  private int discriminatingFields ;

  /**
   * Construct an instance with default values
   */
  public ExtendedInfo() {
    alarmType = AlarmType.DEFAULT ;
    probableCause = ProbableCause.DEFAULT ;
    user1 = "" ;
    user2 = "" ;
    discriminatingFields = DiscriminatingFields.DEFAULT ;
  }

  /**
   * Construct an instance with specific information
   * @param alarmType Alarm type
   * @param probableCause Probable cause
   * @param user1 User information field 1
   * @param user2 User information field 2
   * @param discriminatingFields Discriminating fields to use
   */
  public ExtendedInfo (AlarmType alarmType, ProbableCause probableCause,
      String user1, String user2, int discriminatingFields) {
    this.alarmType = (alarmType == null) ? AlarmType.DEFAULT : alarmType ;
    this.probableCause = (probableCause == null) ? ProbableCause.DEFAULT : probableCause ;
    this.user1 = (user1 == null) ? "" : user1 ;
    this.user2 = (user2 == null) ? "" : user2 ;
    this.discriminatingFields = discriminatingFields ;
  }

  /**
   * Construct an instance from an encoded string
   * @param s Input string (alarmType;probableCause;user1;user2;discriminatingFields)
   */
  public ExtendedInfo (String s) {
    init (s) ;
  }

  /**
   * Initialize an instance from an encoded string
   * @param s Input string (alarmType;probableCause;user1;user2;discriminatingFields)
   */
  private void init (String s) {
    if (s != null) {
      String[] arr = s.split (":") ;
      try {
        alarmType = AlarmType.getFromValue (Integer.parseInt (arr[0])) ;
        probableCause = ProbableCause.getFromValue (Integer.parseInt (arr[1])) ;
        user1 = arr[2] ;
        user2 = arr[3] ;
        discriminatingFields = Integer.parseInt (arr[4]) ;
      } catch (Throwable t) {
        throw new RuntimeException ("Unable to parse extended info data (" + s + ")") ;
      }
    }
  }

  /**
   * Load this object from an input stream
   * @param in DataInputStream to load from
   */
  public ExtendedInfo (DataInputStream in) throws Exception {
    alarmType = AlarmType.getFromValue (in.readInt()) ;
    probableCause = ProbableCause.getFromValue (in.readInt()) ;
    user1 = in.readUTF() ;
    user2 = in.readUTF() ;
    discriminatingFields = in.readInt() ;
  }

  /**
   * Check if this object is the default extendedInfo object
   * @return True if this is the case, false otherwise
   */
  public boolean isDefault() {
    return (alarmType.equals (AlarmType.DEFAULT)
      && probableCause.equals (ProbableCause.DEFAULT)
      && user1.equals ("")
      && user2.equals ("")
      && discriminatingFields == DiscriminatingFields.DEFAULT) ;
  }

  /**
   * Retrieve the alarm type
   * @return Alarm type
   */
  public AlarmType getAlarmType() {
    return alarmType ;
  }

  /**
   * Set the alarm type
   * @param alarmType Alarm type
   */
  public void setAlarmType (AlarmType alarmType) {
    this.alarmType = alarmType ;
  }

  /**
   * Retrieve the probable cause
   * @return Probable cause
   */
  public ProbableCause getProbableCause() {
    return probableCause ;
  }

  /**
   * Set the probable cause
   * @param probableCause Probable cause
   */
  public void setProbableCause (ProbableCause probableCause) {
    this.probableCause = probableCause ;
  }

  /**
   * Retrieve the user field 1
   * @return User field 1
   */
  public String getUser1() {
    return user1 ;
  }

  /**
   * Set the user field 1
   * @param user1 User field 1
   */
  public void setUser1 (String user1) {
    this.user1 = (user1 == null) ? "" : user1 ;
  }

  /**
   * Retrieve the user field 2
   * @return User field 2
   */
  public String getUser2() {
    return user2 ;
  }

  /**
   * Set the user field 2
   * @param user2 User field 2
   */
  public void setUser2 (String user2) {
    this.user2 = (user2 == null) ? "" : user2 ;
  }

  /**
   * Retrieve the discriminating fields
   * @return Discriminating fields
   */
  public int getDiscriminatingFields() {
    return discriminatingFields ;
  }

  /**
   * Set the discriminating fields
   * @param discriminatingFields Discriminating fields
   */
  public void setDiscriminatingFields (int discriminatingFields) {
    this.discriminatingFields = discriminatingFields ;
  }

  /** Discriminating fields names */
  private String discriminatingFieldsNames ;

  /**
   * Retrieve the discriminating fields
   * @return Discriminating fields
   */
  public String getDiscriminatingFieldsNames() {
    if (discriminatingFieldsNames == null) {
      StringBuilder buf = new StringBuilder() ;
      if ((discriminatingFields & DiscriminatingFields.HOST) != 0) {
        buf.append ("HOST ") ;
      }
      if ((discriminatingFields & DiscriminatingFields.INSTANCE) != 0) {
        buf.append ("INSTANCE ") ;
      }
      if ((discriminatingFields & DiscriminatingFields.USER1) != 0) {
        buf.append ("USER1 ") ;
      }
      if ((discriminatingFields & DiscriminatingFields.USER2) != 0) {
        buf.append ("USER2 ") ;
      }
      if (buf.length() != 0) {
        buf.setLength (buf.length() - 1) ;
      }
      discriminatingFieldsNames = buf.toString() ;
    }
    return discriminatingFieldsNames ;
  }

  /**
   * Store this object in an output stream
   * @param out DataOutputStream to use
   */
  public void store (DataOutputStream out) throws Exception {
    out.writeInt (alarmType.getValue()) ;
    out.writeInt (probableCause.getValue()) ;
    out.writeUTF (user1) ;
    out.writeUTF (user2) ;
    out.writeInt (discriminatingFields) ;
  }

  /**
   * Encode this object
   * @return Encoded value
   */
  public String encodeUserData() {
    StringBuilder buf = new StringBuilder() ;
    buf.append (alarmType.getValue()).append (':') ;
    buf.append (probableCause.getValue()).append (':') ;
    buf.append (user1).append (':') ;
    buf.append (user2).append (':') ;
    buf.append (discriminatingFields) ;
    return buf.toString() ;
  }

  /**
   * Show detailed information
   * @return Extended information details
   */
  public String toString() {
    StringBuilder buf = new StringBuilder() ;
    buf.append ("    Alarm type:            ").append (alarmType.name()).append ('\n') ;
    buf.append ("    Probable cause:        ").append (probableCause.name()).append ('\n') ;
    buf.append ("    User 1:                ").append (user1).append ('\n') ;
    buf.append ("    User 2:                ").append (user2).append ('\n') ;
    buf.append ("    Discriminating fields: ").append (getDiscriminatingFieldsNames()) ;
    return buf.toString() ;
  }

  //
  // Serialization
  //

  /**
   * Write this object
   * @param out Object output stream to write to
   */
  private void writeObject (java.io.ObjectOutputStream out) throws IOException {
    try {
      out.writeUTF (encodeUserData()) ;
    } catch (Throwable t) {
      throw new IOException ("Unable to serialize ExtendedInfo object", t) ;
    }
  }

  /**
   * Load this object
   * @param in Object input stream to load from
   */
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    try {
      init (in.readUTF()) ;
    } catch (Throwable t) {
      throw new IOException ("Unable to load ExtendedInfo object", t) ;
    }
  }

  /**
   * Load this object under adverse conditions
   */
  private void readObjectNoData() throws ObjectStreamException {
    init (null) ;
  }
}
