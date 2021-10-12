// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.reporter.api ;

import java.io.DataInputStream ;
import java.io.DataOutputStream ;
import java.io.Serializable ;
import java.io.ObjectInputStream ;
import java.io.ObjectOutputStream ;
import java.io.IOException ;
import java.io.ObjectStreamException ;

/**
 * Instance information object
 */
public class InstanceInfo {

  /** Instance info session attribute name */
  public static final String INST_INFO = "instinfo" ;

  /** Instance state */
  private int state = ReporterSession.STARTING ;
  /** Message */
  private String message ;

  /**
   * Default constructor
   */
  public InstanceInfo() {
  }

  /**
   * Set the instance state
   * @param state Instance state
   */
  public void setState (int state) {
    if (state < ReporterSession.ACTIVE || state > ReporterSession.STARTING) {
      state = ReporterSession.FAILED ;
    }
    this.state = state ;
  }

  /**
   * Set the instance message
   * @param message Message
   */
  public void setMessage (String message) {
    this.message = (message == null) ? "" : message.trim() ;
  }

  /**
   * Show detailed information
   * @return Extended information details
   */
  public String toString() {
    String s ;
    switch (state) {
      case ReporterSession.ACTIVE:
        s = "active" ;
        break ;
      case ReporterSession.STANDBY:
        s = "standby" ;
        break ;
      case ReporterSession.STARTING:
        s = "starting" ;
        break ;
      default:
        s = "failed" ;
        break ;
    }
    return s + "-" + ((message == null) ? "false" : message) ;
  }
}
