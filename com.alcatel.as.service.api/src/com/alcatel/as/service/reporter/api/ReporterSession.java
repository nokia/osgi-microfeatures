package com.alcatel.as.service.reporter.api ;

import java.io.Serializable;

/**
 * Method allowing the application to remotely set an instance session attribute
 */
public interface ReporterSession {

  /** Instance state: Fully active */
  public static final int ACTIVE = 0 ;
  /** Instance state: Standby */
  public static final int STANDBY = 1 ;
  /** Instance state: Failed */
  public static final int FAILED = 2 ;
  /** Instance state: Starting */
  public static final int STARTING = 3 ;

  /**
   * Set a session attribute to a specific value
   * @param attributeName Attribute name
   * @param attributeValue Attribute value
   */
  public void setAttribute (String attributeName, Serializable attributeValue) ;

  /**
   * Set the instance state
   * @param state Instance state. The implementation does not enforce the use of the ACTIVE, 
   * STANDBY, or FAILED states, which allows extension of this mechanism to other state values
   */
  public void setState (int state) ;

  /**
   * Set the instance message
   * @param message Instance warning message, null for none
   */
  public void setMessage (String message) ;
}
