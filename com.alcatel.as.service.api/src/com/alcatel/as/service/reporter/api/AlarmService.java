package com.alcatel.as.service.reporter.api ;

/**
 * Methods provided by the reporter to let the application manage alarms
 */
public interface AlarmService {

  /**
   * Send a new alarm. The severity, alarm type, probable cause and discriminating fields are taken from
   * the alarm definition defined in the @Alarm annotation.
   * @param sourceId Sub-component identifier, null or empty string for alarms generated by
   * the core instance itself. This identifier represents the "path" to the sub-component, 
   * such as "container/application/servlet". For instance: "http/MyApp/MyProxylet".
   * @param code Alarm code
   * @param message Optional alarm message, overriding the one defined by default in the annotation, or null.
   */
  public void sendAlarm (String sourceId, int code, String message) ;

  /**
   * Send a new alarm. The severity, alarm type, probable cause and discriminating fields are taken from
   * the alarm definition defined in the @Alarm annotation.
   * @param sourceId Sub-component identifier, null or empty string for alarms generated by
   * the core instance itself. This identifier represents the "path" to the sub-component, 
   * such as "container/application/servlet". For instance: "http/MyApp/MyProxylet".
   * @param code Alarm code
   * @param message Optional alarm message, overriding the one defined by default in the annotation, or null.
   * @param user1 User 1 additional field
   */
  public void sendAlarm (String sourceId, int code, String message, String user1) ;

  /**
   * Send a new alarm. The severity, alarm type, probable cause and discriminating fields are taken from
   * the alarm definition defined in the @Alarm annotation.
   * @param sourceId Sub-component identifier, null or empty string for alarms generated by
   * the core instance itself. This identifier represents the "path" to the sub-component, 
   * such as "container/application/servlet". For instance: "http/MyApp/MyProxylet".
   * @param code Alarm code
   * @param message Optional alarm message, overriding the one defined by default in the annotation, or null.
   * @param user1 User 1 additional field
   * @param user2 User 2 additional field
   */
  public void sendAlarm (String sourceId, int code, String message, String user1, String user2) ;

  /**
   * Send a new alarm using the extended alarm format
   * The severity and default message are taken from the alarm definition described in the @Alarm annotation.
   * @param sourceId Sub-component identifier, null or empty string for alarms generated by
   * the core instance itself. This identifier represents the "path" to the sub-component, 
   * such as "container/application/servlet". For instance: "http/MyApp/MyProxylet".
   * @param code Alarm code
   * @param message Optional alarm message, overriding the one defined by default in the annotation, or null.
   * @param extendedInfo Extended information
   */
  public void sendAlarm (String sourceId, int code, String message, ExtendedInfo extendedInfo) ;

  /**
   * Clear an alarm
   * @param sourceId Sub-component identifier. Must be the one initially provided when generating
   * the alarm to clear. This identifier represents the "path" to the sub-component, 
   * such as "container/application/servlet". For instance: "http/MyApp/MyProxylet".
   * @param code Alarm code
   * @param message Clear message
   */
  public void clearAlarm (String sourceId, int code, String message) ;

  /**
   * Clear an alarm
   * @param sourceId Sub-component identifier. Must be the one initially provided when generating
   * the alarm to clear. This identifier represents the "path" to the sub-component, 
   * such as "container/application/servlet". For instance: "http/MyApp/MyProxylet".
   * @param code Alarm code
   * @param message Clear message
   * @param user1 User field 1
   */
  public void clearAlarm (String sourceId, int code, String message, String user1) ;

  /**
   * Clear an alarm
   * @param sourceId Sub-component identifier. Must be the one initially provided when generating
   * the alarm to clear. This identifier represents the "path" to the sub-component, 
   * such as "container/application/servlet". For instance: "http/MyApp/MyProxylet".
   * @param code Alarm code
   * @param message Clear message
   * @param user1 User field 1
   * @param user2 User field 2
   */
  public void clearAlarm (String sourceId, int code, String message, String user1, String user2) ;

  /**
   * Clear an extended alarm
   * @param sourceId Sub-component identifier. Must be the one initially provided when generating
   * the alarm to clear. This identifier represents the "path" to the sub-component, 
   * such as "container/application/servlet". For instance: "http/MyApp/MyProxylet".
   * @param code Alarm code
   * @param message Clear message
   * @param extendedInfo Extended information
   */
  public void clearAlarm (String sourceId, int code, String message, ExtendedInfo extendedInfo) ;

  /**
   * Add a comment to an alarm
   * @param sourceId Sub-component identifier. Must be the one initially provided when generating
   * the alarm
   * @param code Alarm code
   * @param message Comment message
   */
  public void commentAlarm (String sourceId, int code, String message) ;

  /**
   * Add a comment to an alarm
   * @param sourceId Sub-component identifier. Must be the one initially provided when generating
   * the alarm
   * @param code Alarm code
   * @param message Comment message
   * @param user1 User field 1
   */
  public void commentAlarm (String sourceId, int code, String message, String user1) ;

  /**
   * Add a comment to an alarm
   * @param sourceId Sub-component identifier. Must be the one initially provided when generating
   * the alarm
   * @param code Alarm code
   * @param message Comment message
   * @param user1 User field 1
   * @param user2 User field 2
   */
  public void commentAlarm (String sourceId, int code, String message, String user1, String user2) ;

  /**
   * Add a comment to an extended alarm
   * @param sourceId Sub-component identifier. Must be the one initially provided when generating
   * the alarm
   * @param code Alarm code
   * @param extendedInfo Extended information
   */
  public void commentAlarm (String sourceId, int code, String message, ExtendedInfo extendedInfo) ;
}