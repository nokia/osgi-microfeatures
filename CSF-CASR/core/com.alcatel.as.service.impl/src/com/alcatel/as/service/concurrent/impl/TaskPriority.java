package com.alcatel.as.service.concurrent.impl;

/**
 * Defines the supported PlatformExecutor schedule priorities. 
 */
public enum TaskPriority {
  /**
   * The task must be scheduled on one executor, using the DEFAULT priority.
   * DEFAULT priority tasks are executed after all HIGH priority tasks.
   */
  DEFAULT,
  
  /**
   * The task must be scheduled in one executor, using the HIGH priority.
   * HIGH priority tasks are executed before any DEFAULT priority tasks.
   */
  HIGH,
}
