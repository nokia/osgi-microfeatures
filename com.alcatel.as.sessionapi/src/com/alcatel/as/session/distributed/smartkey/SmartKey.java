package com.alcatel.as.session.distributed.smartkey;

import java.io.Serializable;

public interface SmartKey extends Serializable
{
  public String getSessionId();

  public String getPlatformId();

  public String getGroupId();
  
  public long[] getAgentIds();
  
  public int getContainerId();
  
  public int getUid();
  
  public String getUserPart();
}
