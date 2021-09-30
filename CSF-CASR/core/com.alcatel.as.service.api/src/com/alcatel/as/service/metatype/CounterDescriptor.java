package com.alcatel.as.service.metatype;

public interface CounterDescriptor {
  MetaData getParent();
  String getSource();
  String getName();
  String getType();
  String getDescription();
}
