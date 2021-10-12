// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.annotation.alarm ;

/**
 * X.733 alarm types
 */
public enum AlarmType {
  communicationAlarm(1),
  qualityOfServiceAlarm(2),
  processingErrorAlarm(3),
  equipmentAlarm(4),
  environmentalAlarm(5) ;

  /** Default alarm type */
  public static final AlarmType DEFAULT = AlarmType.processingErrorAlarm ;

  /**
   * Get an instance from a string. The string is either a name or an integer
   * @param s String to decode
   * @return Object instance, null if not found
   */
  public static AlarmType get (String s) {
    try {
      return getFromValue (Integer.parseInt (s)) ;
    } catch (Throwable t) {
    }
    for (AlarmType object : values()) {
      if (object.name().equals (s)) {
        return object ;
      }
    }
    return null ;
  }

  /**
   * Get an instance from a value
   * @param value Value to lookup
   * @return Object instance, null if not found
   */
  public static AlarmType getFromValue (int value) {
    for (AlarmType object : values()) {
      if (object.getValue() == value) {
        return object ;
      }
    }
    return null ;
  }

  /** Associated value */
  private int value ;

  /**
   * Constructor from value
   * @param value Value
   */
  private AlarmType (int value) {
    this.value = value ;
  }

  /**
   * Retrieve the alarm type
   * @return Alarm type
   */
  public int getValue() {
    return value ;
  }
}
