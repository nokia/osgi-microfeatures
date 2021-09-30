package com.alcatel.as.service.uid;

/**
 * This interface is dedicated to ASR platform agents UID generation
 */
public interface UIDGenerator 
{
  String getPlatformUniqueName();
  long getAgentUniqueId(String group_name);
}


