// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.tracer;

public interface Level {
  /**
   * The <code>LOG_ERR</code> level describes a non recoverable error.
   * It is similar to the syslog EMERG priority.
   */
  public final static int LOG_ERR = 3;
  
  /**
   * The <code>LOG_WARN</code> level describes an error that is not
   * really err, and that is recoverable by the application.
   * It is similar to the syslog WARN priority.
   */
  public final static int LOG_WARN = 4;
  
  /**
   * The <code>LOG_NOTICE</code> level describes a general info message. 
   * It is similar to the syslog NOTICE priority.
   */
  public final static int LOG_NOTICE = 5;
  
  /**
   * The <code>LOG_INFO</code> level describes an info message, and is
   * finer than <code>LOG_NOTICE</code> level.
   * It is similar to the syslog INFO priority.
   */
  public final static int LOG_INFO = 6;
  
  /**
   * The <code>LOG_DEBUG</code> level describes a debug message.
   * It is similar to the syslog DEBUG priority.
   */
  public final static int LOG_DEBUG = 7;
  
  /**
   * The <code>LOG_ALL</code> level describes a finer debug log level.
   * It is similar to the syslog ALL priority.
   *
   * @deprecated This constant was wrongly assuming that there was an existing
   * LOG_ALL (8) syslog priority, but actually syslog priorities goes up to 7 (LOG_DEBUG).
   * Please use LOG_DEBUG instead of LOG_ALL.
   */
  public final static int LOG_ALL = 7;
  
  /**
   * The constant <code>levelStr[]</code> gives a mapping between level 
   * numers and their corresponding symbolic names.
   *
   */
  public final static String levelStr[] = { "?", "?", "?", "err", "warn", "notice", "info", "debug", "all" };
}
