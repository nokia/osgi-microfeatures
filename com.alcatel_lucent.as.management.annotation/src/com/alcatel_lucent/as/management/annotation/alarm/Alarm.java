// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.annotation.alarm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a String class field which defines an alarm type. This alarm type can then be
 * used to fire snmp alarms.
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>1 - JMX</h4>
 * 
 * <p> Here is a sample showing how a component may define an alarm type for sending alarms:
 * <blockquote>
 * <pre>
 * public class MyServlet
 * {
 *     &#64;Alarm(code = 100,
 *            severity = AlarmSeverity.MAJOR, 
 *            message = "Critical Memory Level",
 *            generatedBy = "MyServlet",
 *            description = "This alarm occurs when the system starts getting out of memory.",
 *            clearCondition = "This alarm autoclears when memory level drops below 75%",
 *            correctiveAction = "Abnormal buffering due to slow network or application memory leak.")
 *     static final String OUT_OF_MEMORY = "OUT_OF_MEMORY";
 * 
 *     &#64;Alarm(code = 100,
 *            severity = AlarmSeverity.NORMAL, 
 *            message = "Memory level back to normal",
 *            generatedBy = "MyServlet",
 *            description = "This alarm clears the OutOfMemory alarm")
 *     static final String CLEAR_OUT_OF_MEMORY = "CLEAR_OUT_OF_MEMORY";
 * 
 *     public void sendAlarm()
 *     {
 *       if (outOfMemory)
 *       {
 *         getServletContext()
 *           .getAttribute("javax.management.modelmbean." + myServletName)
 *           .sendNotification(new Notification(OUT_OF_MEMORY, "MyServlet", 1, "optional message"));
 *       }
 *       else 
 *         getServletContext()
 *           .getAttribute("javax.management.modelmbean." + myServletName)
 *           .sendNotification(new Notification(CLEAR_OUT_OF_MEMORY, "MyServlet", 1, "optional message"));
 *     }
 *     ...
 * </pre>
 * 
 * <h4>2 - AlarmService</h4>
 * 
 * <p> Here is a sample showing how a component may define an alarm type for sending alarms:
 * <blockquote>
 * <pre>
 * public class MyClass
 * {
 *     AlarmService alarmService; //injected
 * 
 *     &#64;Alarm(name = "OUT_OF_MEMORY",
 *            severity = AlarmSeverity.MAJOR, 
 *            message = "Critical Memory Level",
 *            generatedBy = "MyComponent",
 *            description = "This alarm occurs when the system starts getting out of memory.",
 *            clearCondition = "This alarm autoclears when memory level drops below 75%",
 *            correctiveAction = "Abnormal buffering due to slow network or application memory leak.")
 *     static final int ALARM_CODE = 100;
 * 
 *     public void sendAlarm()
 *     {
 *       if (outOfMemory)
 *         alarmService.sendAlarm("MySourceId", ALARM_CODE, "optional message");
 *       else
 *         alarmService.clearAlarm("MySourceId", ALARM_CODE, "optional message");
 *     }
 *     ...
 * </pre>
 * </blockquote>
 *
 * <h3>Note on alarms severity:<h3>
 * Jmx defines 6 severity levels currently mapped to 4 levels in the ASR:
 * <ul>
 * <li> NON_RECOVERABLE, CRITICAL, MAJOR: all map to an ASR ALARM
 * <li> MINOR maps to an ASR EVENT
 * <li> WARNING maps to an ASR NOTIFICATION
 * <li> NORMAL maps to an ASR CLEAR
 * <ul>
 *
 * <pre>
 * See http://docs.oracle.com/javase/1.5.0/docs/api/javax/management/modelmbean/ModelMBeanNotificationInfo.html
 * severity : 0-6 where 
 *         0: unknown; 
 *         1: non-recoverable; 2: critical, failure; 3: major, severe;
 *         4: minor, marginal, error; 5: warning;
 *         6: normal, cleared, informative
 * </pre>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ java.lang.annotation.ElementType.FIELD })
public @interface Alarm {
  /**
   * Alarm code used when sending alarm notification. Must be >= 100 for component which are 
   * not part of the core containers.
   * Not required if the annotation is used on an Int defining the alarm code.
   * @return the alarm code (myst be >= 100 for components which are not part of the containers)
   */
  int code() default -1;
  
  /**
   * Alarm name used in the documentation.
   * Not required if the annotation is used on a String defining the alarm name.
   * @return the alarm name
   */
  String name() default "";
  
  /**
   * The Alarm severity. Note that when using the AlarmService API, NORMAL severity should not be 
   * used since the API has a specific clearAlarm method.
   * @return the alarm severity.
   */
  AlarmSeverity severity();

  /**
   * Alarm type
   * @return The X.733 alarm type
   */
  AlarmType alarmType() default AlarmType.processingErrorAlarm ;

  /**
   * Probable cause
   * @return The X.733 probable cause
   */
  ProbableCause probableCause() default ProbableCause.applicationSubsystemFailure ;

  /**
   * Discriminating fields
   * @return The alarm discriminating fields
   */
  int discriminatingFields() default DiscriminatingFields.DEFAULT ;

  /**
   * Alarm message
   * @return the Alarm message
   */
  String message() default "Not defined";
  
  /**
   * Alarm description.
   * @return the Alarm description
   */
  String description();

  /** 
   * the logical name of the component generating this alarm
   */
  String generatedBy() default "Not defined";

  /**
   * Alarm clear condition
   * @return the condition required for this alarm to be cleared automatically
   */
  String clearCondition() default "Not defined";
  
  /**
   * Alarm corrective action.
   * @return the message indicating how to correct the problem notified by this alarm.
   */
  String correctiveAction() default "Not defined";
}
