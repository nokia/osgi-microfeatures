// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.reporter.impl.standalone;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.log4j.Logger;

import com.alcatel.as.service.reporter.api.AlarmService;
import com.alcatel.as.service.reporter.api.AlarmLevels;
import com.alcatel.as.service.reporter.api.ExtendedInfo;

@Component
public class Log4jAlarmService implements AlarmService {
  public static Logger _logger = Logger.getLogger("com.alcatel_lucent.as.service.alarm");
  
  //
  // Alarm methods: we do not want to parse the MBDs at this level so always force default
  // severity and extendedInfo values if not provided by the user
  //
  @Override public void sendAlarm(String sourceId, int code, String message) {    
    sendAlarm (sourceId, code, message, null, null);
  }
  @Override public void sendAlarm(String sourceId, int code, String message, String user1) {    
    sendAlarm (sourceId, code, message, user1, null);
  }
  @Override public void sendAlarm(String sourceId, int code, String message, String user1, String user2) {    
    ExtendedInfo extendedInfo = new ExtendedInfo() ;
    extendedInfo.setUser1 (user1) ;
    extendedInfo.setUser2 (user2) ;
    sendAlarm (sourceId, code, AlarmLevels.ALARM, message, extendedInfo);
  }
  public void sendAlarm(String sourceId, int code, int severity, String message) {    
    sendAlarm (sourceId, code, severity, message, null) ;
  }
  @Override public void sendAlarm (String sourceId, int code, String message, ExtendedInfo extendedInfo) {
    sendAlarm (sourceId, code, AlarmLevels.ALARM, message, extendedInfo) ;
  }
  public void sendAlarm (String sourceId, int code, int severity, String message, ExtendedInfo extendedInfo) {
    if (_logger.isInfoEnabled()) {
      extendedInfo = (extendedInfo == null) ? ExtendedInfo.getDefault() : extendedInfo ;
      StringBuilder sb = new StringBuilder();
      sb.append("Alarm [sourceId=").append(sourceId);
      sb.append(", code=").append(code);
      sb.append(", severity=").append(severity);
      sb.append(", message=").append(message);
      sb.append(", ").append(extendedInfo.toString());
      _logger.info(sb.toString());
    }
  }
  
  @Override public void clearAlarm(String sourceId, int code, String message) {
    clearAlarm (sourceId, code, message, null, null) ;
  }
  @Override public void clearAlarm(String sourceId, int code, String message, String user1) {
    clearAlarm (sourceId, code, message, user1, null) ;
  }
  @Override public void clearAlarm(String sourceId, int code, String message, String user1, String user2) {
    ExtendedInfo extendedInfo = new ExtendedInfo() ;
    extendedInfo.setUser1 (user1) ;
    extendedInfo.setUser2 (user2) ;
    clearAlarm (sourceId, code, message, extendedInfo) ;
  }
  @Override public void clearAlarm (String sourceId, int code, String message, ExtendedInfo extendedInfo) {
    if (_logger.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Alarm Clear [sourceId=").append(sourceId);
      sb.append(", code=").append(code);
      sb.append(", message=").append(message);
      sb.append(", ").append(extendedInfo.toString());
      _logger.info(sb.toString());
    }
  }
  
  @Override public void commentAlarm(String sourceId, int code, String message) {
    commentAlarm (sourceId, code, message, null, null) ;
  }
  @Override public void commentAlarm(String sourceId, int code, String message, String user1) {
    commentAlarm (sourceId, code, message, null, null) ;
  }
  @Override public void commentAlarm(String sourceId, int code, String message, String user1, String user2) {
    ExtendedInfo extendedInfo = new ExtendedInfo() ;
    extendedInfo.setUser1 (user1) ;
    extendedInfo.setUser2 (user2) ;
    commentAlarm (sourceId, code, message, extendedInfo) ;
  }
  @Override public void commentAlarm (String sourceId, int code, String message, ExtendedInfo extendedInfo) {
    if (_logger.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Alarm Comment [sourceId=").append(sourceId);
      sb.append(", code=").append(code);
      sb.append(", message=").append(message);
      sb.append(", ").append(extendedInfo.toString());
      _logger.info(sb.toString());
    }
  }
}
