package com.alcatel_lucent.as.management.annotation.alarm;

/**
 * This enum is used by the Alarm annotation, in order to define the Alarm JMX severity.
 */
public enum AlarmSeverity {
  /* non-recoverable alarm */
  NON_RECOVERABLE,
  /* critical, failure */
  CRITICAL,
  /* Major, severe */
  MAJOR,
  /* Minor, marginal error */
  MINOR,
  /* Warning error */
  WARNING,
  /* normal, cleared, informative */
  NORMAL
}
