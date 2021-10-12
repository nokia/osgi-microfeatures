// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

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
