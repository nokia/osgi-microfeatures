// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.bnd;

import java.io.PrintWriter;

import aQute.bnd.osgi.Annotation;

import com.alcatel_lucent.as.management.annotation.alarm.AlarmSeverity;
import com.alcatel_lucent.as.management.annotation.alarm.AlarmType ;
import com.alcatel_lucent.as.management.annotation.alarm.ProbableCause ;
import com.alcatel_lucent.as.management.annotation.alarm.DiscriminatingFields ;

class AlarmBean {
  private final int _code;
  private final String _name;
  private final int _severity;
  private final String _description;
  private String _message;
  private String _generatedBy;
  private String _clearCondition;
  private String _correctiveAction;
  private final String _fullClassName;
  private final String _className;
  private String _alarmType ;
  private String _probableCause ;
  private int _discriminatingFields ;
  
  /** 
   * the type param may be either the name string or the code int,
   * depending on where the annotation was applied
   */
  AlarmBean(Annotation annotation, String fullClassName, String className, String type) {
    Object code = annotation.get("code");
    _code = code != null ? (Integer)code : Integer.parseInt(type);
    String name = annotation.get("name");
    _name = (name != null && name.length() > 0) ? name : type;
    _severity = getSeverity(annotation);
    _alarmType = Utils.get (annotation, "alarmType", AlarmType.DEFAULT.name()) ;
    _probableCause = Utils.get (annotation, "probableCause", ProbableCause.DEFAULT.name()) ;
    _discriminatingFields = Utils.get (annotation, "discriminatingFields", DiscriminatingFields.DEFAULT) ;
    _description = annotation.get("description");
    _message = annotation.get("message");
    _generatedBy = annotation.get("generatedBy");
    _clearCondition = annotation.get("clearCondition");
    _correctiveAction = annotation.get("correctiveAction");
    _fullClassName = fullClassName;
    _className = className;
  }
  
  void validate() {
    if (_code < 0) {
      throw new IllegalArgumentException("Invalid code: " + _code);
    }
    if (_message == null) _message = "Not defined";
    if (_generatedBy == null) _generatedBy = "Not defined";
    if (_clearCondition == null) _clearCondition = "Not defined";
    if (_correctiveAction == null) _correctiveAction = "Not defined";
    if (_alarmType == null) {
      _alarmType = AlarmType.DEFAULT.name() ;
    }
    if (_probableCause == null) {
      _probableCause = ProbableCause.DEFAULT.name() ;
    }
    if (_discriminatingFields == 0 
        || (_discriminatingFields | DiscriminatingFields.MAXVAL) != DiscriminatingFields.MAXVAL) { 
      throw new IllegalArgumentException("DiscriminatingFields value " + _discriminatingFields + " out of range") ;
    }
  }
  
  void print(PrintWriter pw) {
    Utils.print(pw, 
        "      <notification description=\"", _description, "\" name=\"", _name, "\">\n",
        "          <descriptor>\n", 
        "             <field name=\"messageID\" value=\"", _code, "\"/>\n", 
        "             <field name=\"message\" value=\"", _message, "\"/>\n",
        "             <field name=\"severity\" value=\"", _severity, "\"/>\n",
        "             <field name=\"alarmType\" value=\"", _alarmType, "\"/>\n",
        "             <field name=\"probableCause\" value=\"", _probableCause, "\"/>\n",
        "             <field name=\"discriminatingFields\" value=\"", _discriminatingFields, "\"/>\n",
        "             <field name=\"generatedBy\" value=\"", _generatedBy, "\"/>\n",
        "             <field name=\"clearCondition\" value=\"", _clearCondition, "\"/>\n",
        "             <field name=\"correctiveAction\" value=\"", _correctiveAction, "\"/>\n",
        "          </descriptor>\n", 
        "      </notification>\n");
  }
  
  private int getSeverity(Annotation annotation) {
    AlarmSeverity severity = AlarmSeverity.valueOf((String) annotation.get("severity"));
    switch (severity) {
    case NON_RECOVERABLE:
      return 1;
    case CRITICAL:
      return 2;
    case MAJOR:
      return 3;
    case MINOR:
      return 4;
    case WARNING:
      return 5;
    case NORMAL:
      return 6;
    default:
      throw new IllegalArgumentException("Invalid severity value: " + severity);
    }
  }
}
