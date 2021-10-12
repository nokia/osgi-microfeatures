// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.tracer;

import alcatel.tess.hometop.gateways.utils.DateFormatter;
import alcatel.tess.hometop.gateways.utils.Utils;

/**
 * Class declaration
 *
 *
 * @author
 */
public class AccessLogEvent extends LogEvent {
  public AccessLogEvent() {
  }
  
  /**
   * Method declaration
   * @return
   */
  static AccessLogEvent acquire(Tracer tracer, int level, String userAddr, String logName, String userAuth,
                                String method, String url, int returnCode, int sizeSent, String referer,
                                String browser) {
    AccessLogEvent le = new AccessLogEvent();
    
    le.tracer = tracer;
    le.level = level;
    le.date = System.currentTimeMillis();
    le.userAddr = userAddr;
    le.logName = logName;
    le.userAuth = userAuth;
    le.method = method;
    le.url = url;
    le.returnCode = returnCode;
    le.sizeSent = sizeSent;
    le.referer = referer;
    le.browser = browser;
    
    return (le);
  }
  
  /**
   * Method declaration
   */
  void release() {
  }
  
  Tracer getTracer() {
    return (this.tracer);
  }
  
  int getLevel() {
    return (this.level);
  }
  
  public String toString() {
    if (this.access == null) {
      StringBuffer sb = new StringBuffer();
      
      makeCombinedLog(sb, userAddr);
      makeCombinedLog(sb, logName);
      makeCombinedLog(sb, userAuth);
      df.format(System.currentTimeMillis(), sb);
      sb.append(' ');
      sb.append(SPACE);
      sb.append(QUOTE);
      makeCombinedLog(sb, method);
      sb.append(SPACE);
      sb.append(url);
      sb.append(SPACE);
      sb.append("HTTP/1.1");
      sb.append(QUOTE);
      sb.append(SPACE);
      sb.append(returnCode);
      sb.append(SPACE);
      sb.append(sizeSent);
      sb.append(SPACE);
      makeCombinedLog(sb, referer);
      makeCombinedLog(sb, browser);
      sb.append(Utils.LINE_SEPARATOR);
      
      this.access = sb.toString();
    }
    
    return (this.access);
  }
  
  static void statistics() {
  }
  
  private void makeCombinedLog(StringBuffer sb, String msg) {
    if (msg == null) {
      sb.append(DASH);
    } else {
      sb.append(msg);
      sb.append(SPACE);
    }
  }
  
  /**
   * Describe variable <code>tracer</code> here.
   *
   */
  private Tracer tracer;
  
  /**
   * Describe variable <code>level</code> here.
   *
   */
  private int level;
  
  /**
   * Describe variable <code>userAddr</code> here.
   *
   */
  private String userAddr;
  
  /**
   * The logName identifies the user that is making the http request.
   *
   */
  private String logName;
  
  /**
   * Describe variable <code>userAuth</code> here.
   *
   */
  private String userAuth;
  
  /**
   * Describe variable <code>method</code> here.
   *
   */
  private String method;
  
  /**
   * Describe variable <code>url</code> here.
   *
   */
  private String url;
  
  /**
   * Describe variable <code>returnCode</code> here.
   *
   */
  private int returnCode;
  
  /**
   * Describe variable <code>sizeSent</code> here.
   *
   */
  private int sizeSent;
  
  /**
   * Describe variable <code>referer</code> here.
   *
   */
  private String referer;
  
  /**
   * Describe variable <code>browser</code> here.
   *
   */
  private String browser;
  
  /**
   * Describe variable <code>date</code> here.
   *
   */
  private long date;
  
  /**
   * Describe variable <code>access</code> here.
   *
   */
  private String access;
  
  /**
   * Describe constant <code>SPACE</code> here.
   *
   */
  private final static String SPACE = " ";
  
  /**
   * Describe constant <code>QUOTE</code> here.
   *
   */
  private final static String QUOTE = "\"";
  
  /**
   * Describe constant <code>DASH</code> here.
   *
   */
  private final static String DASH = "- ";
  
  /**
   * Date formater used by the logAccess method.
   *
   */
  private final static DateFormatter df = new DateFormatter(DateFormatter.NCSA_FORMAT);
}
