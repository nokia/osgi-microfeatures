package com.alcatel.as.service.metatype;

public interface CommandDescriptor {
  MetaData getParent();
  String getSource();
  String getName();
  String getDescription();
}
