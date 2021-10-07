package com.alcatel.as.service.reporter.api ;

/**
 * Known alarm levels
 * @internal
 * @exclude
 */
public interface AlarmLevels {
  /** Alarm severity: Notification */
  public static final int NOTIFICATION = 0 ;
  /** Alarm severity: Event */
  public static final int EVENT = 1 ;
  /** Alarm severity: Alarm */
  public static final int ALARM = 2 ;
  /** Alarm severity: Clear */
  public static final int CLEAR = 3 ;
  /** Alarm severity: Comment */
  public static final int COMMENT = 4 ;
  /** Alarm severity: Acknowledge */
  public static final int ACK = 5 ;
}
