// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.dns.impl;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import com.alcatel.as.service.reporter.api.AlarmService;
import com.alcatel_lucent.as.management.annotation.stat.Stat;
import com.alcatel_lucent.as.management.annotation.alarm.Alarm;
import com.alcatel_lucent.as.management.annotation.alarm.AlarmSeverity;

@Stat
public class DnsResolver extends SimpleResolver {
  /**
   * Our logger
   */
  private final static Logger _logger = Logger.getLogger(DnsResolver.class);
  
  /**
   * The Alarm code used when sending an alarm about a failing DNS service
   */
  @Alarm(name="DnsServerConnectionTimeout",
         severity=AlarmSeverity.MAJOR,
         message="Remote DNS Server does not respond",
         generatedBy="DNS",
         description="This alarm is generated when a DNS server does not reply timely.",
         correctiveAction="Check and Restart the DNS server.")
  private final static int ALARM_CODE = 54;
  
  /**
   * DNS server hostname.
   */
  String _hostname = null;
  
  /**
   * Our jvm instance name (group__instname)
   */
  private String _instanceName;
  
  /**
   * Max time in millis to wait before sending or clearing an alarm.
   * An alarm is sent or cleared only if some DNS requests fail (or are succesful) for at minimum _alarmWaterMark millis.
   * This allows to avoid sending/clearing an alarm too often. 0 Means alarm is sent when the first dns request is in timeout and cleared when the next dns request is succesful.
   */
  private long _alarmWaterMark;
  
  /**
   * Did we send an alarm.
   */
  protected boolean _alarmSent;
  
  /**
   * Internal state used to decide if an alarm must be sent or cleared
   */
  interface State {
    void requestSucceeded();
    
    void requestFailed(IOException e);
  }
  
  /**
   * Normal state, where DNS requests are all successful.
   */
  State NORMAL = new State() {    
    @Override
    public void requestSucceeded() {
      if (_alarmSent) {
        _alarmSent = false;
        clearAlarm();
      }
    }
    
    @Override
    public void requestFailed(IOException e) {
      _state = ALARM;
      _state.requestFailed(e);
    }
  };
  
  /**
   * Alarm state, where DNS requests are failing.
   */
  State ALARM = new State() {
    long _stateEnterTime = -1;
    
    @Override
    public void requestFailed(IOException e) {
      long now = System.currentTimeMillis();
      if (_stateEnterTime == -1) {
        _stateEnterTime = now;
      }
      
      if (!_alarmSent && ((now - _stateEnterTime) >= _alarmWaterMark)) {
        _alarmSent = true;
        sendAlarm(e);
      }
    }
    
    @Override
    public void requestSucceeded() {
      _state = NORMAL;
      _stateEnterTime = -1;
      _state.requestSucceeded();
    }
  };
  
  /**
   * Our current state (NORMAL by default).
   */
  State _state = NORMAL;
  
  /**
   * Our constructor
   * @param hostname the DNS server hostname
   * @throws UnknownHostException if the hostname could not be resolved.
   */
  public DnsResolver(String hostname) throws UnknownHostException {
    super(hostname);
    _hostname = hostname;
  }
  
  public void setAlarmWaterMark(long alarmSendWaterMark) {
    _alarmWaterMark = alarmSendWaterMark;
  }
  
  public void setInstanceName(String instanceName) {
    _instanceName = instanceName;
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder res = new StringBuilder("DNS Resolver: name=");
    res.append(_hostname).append(", ").append(super.toString());
    return res.toString();
  }
  
  public Message send(Message query) throws IOException {
    try {
      Message response = super.send(query);
      synchronized (this) {
        _state.requestSucceeded();
      }
      return response;
    } catch (IOException e) {
      synchronized (this) {
        _state.requestFailed(e);
      }
      throw e;
    }
  }
  
  private void sendAlarm(IOException e) {
    try {
      _logger.warn("DNS server not reachable from host " + _hostname + " (sending alarm)" + ": "
          + e.toString());
      DNSFactoryImpl.getAlarmService().sendAlarm("",
                                                   ALARM_CODE,
                                                   "Could not contact DNS server on host " + _hostname
                                                   + "(" + e.toString() + ")");
    } catch (Throwable t) {
      _logger.warn("Could not fire alarm on failing dns server: " + _hostname, t);
    }
  }
  
  private void clearAlarm() {
    try {
      _logger.warn("Clearing unreachable DNS server alarm from host " + _hostname);
      DNSFactoryImpl.getAlarmService().clearAlarm("", ALARM_CODE, 
                                                    "Could not contact DNS server on host " + _hostname);
    } catch (Throwable t) {
      _logger.warn("Could not fire alarm on failing dns server: " + _hostname, t);
    }
  }
}
