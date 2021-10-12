// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.bnd;

import java.io.PrintWriter;

import aQute.bnd.osgi.Annotation;
import aQute.service.reporter.Reporter;

class MBDBean {
  AlarmsCountersBean _alarmsCounters = new AlarmsCountersBean();
  ConfigBean _config = new ConfigBean();
  private String _fullClassName;
  private String _className;
  @SuppressWarnings("unused")
  private Reporter _reporter;
  
  public void reporter(Reporter reporter) {
    _reporter = reporter;
    _config.reporter(reporter);
  }
  
  void fullClassName(String fullClassName) {
    _fullClassName = fullClassName;
    _className = parseClassName(fullClassName);
    _config.fullClassName(fullClassName);
    _alarmsCounters.fullClassName(_fullClassName, _className);
  }
  
  public void config(Annotation annotation) {
    _config.config(annotation);
  }
  
  public void property(Annotation annotation, PropertyBean.Type type, String fieldName, String fieldValue) {
    _config.add(new PropertyBean(annotation, type, fieldName, fieldValue, _reporter));
  }
  
  public void stat(Annotation annotation) {
    _alarmsCounters.stat(annotation);
  }
  
  public void commands(Annotation annotation) {
    _alarmsCounters.commands(annotation);
  }
  
  void alarm(Annotation annotation, String id) {
    _alarmsCounters.add(new AlarmBean(annotation, _fullClassName, _className, id));
  }
  
  void counterField(Annotation annotation, CounterBean.Type type, String field, String fieldValue) {
    _alarmsCounters.add(new CounterBean(annotation, type, field, false /* field is the counter name */, fieldValue));
  }

  void counterMethod(Annotation annotation, CounterBean.Type type, String method) {
    _alarmsCounters.add(new CounterBean(annotation, type, method, true /* method getXXX is counter name */, null));
  }
  
  void command(Annotation annotation, String method) {
    _alarmsCounters.add(new CommandBean(annotation, method));
  }
  
  void validate() {
    _config.validate();
    _alarmsCounters.validate();
  }

  public boolean isPrintable() {
    return _config.isPrintable() || _alarmsCounters.isPrintable();
  }
  
  void print(PrintWriter pw) {
    Utils.print(pw, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", "<!DOCTYPE mbeans-descriptors PUBLIC ",
                "\"-//Apache Software Foundation//DTD Model MBeans Configuration File\" ",
                "\"http://jakarta.apache.org/commons/dtds/mbeans-descriptors.dtd\">\n",
                "<mbeans-descriptors>\n");
    _config.print(pw);
    _alarmsCounters.print(pw);
    Utils.print(pw, "</mbeans-descriptors>\n");
  }
  
  public void print(MonconfPropertiesBuilder monconf) {
    _config.print(monconf);
  }

  private String parseClassName(String fullClassName) {
    int dot = fullClassName.lastIndexOf(".");
    return dot != -1 ? fullClassName.substring(dot + 1) : fullClassName;
  }

}
