package com.alcatel.as.service.metatype;

import com.alcatel_lucent.as.management.annotation.alarm.AlarmType ;
import com.alcatel_lucent.as.management.annotation.alarm.ProbableCause ;

public interface AlarmDescriptor {
  MetaData getParent();
  String getSource();
  String getName();
  String getMessage();
  int getCode();
  int getSeverity();
  String getDescription();
  AlarmType getAlarmType() ;
  ProbableCause getProbableCause() ;
  int getDiscriminatingFields() ;
}
