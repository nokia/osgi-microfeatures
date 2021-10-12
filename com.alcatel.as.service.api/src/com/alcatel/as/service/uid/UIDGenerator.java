// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.uid;

/**
 * This interface is dedicated to ASR platform agents UID generation
 */
public interface UIDGenerator 
{
  String getPlatformUniqueName();
  long getAgentUniqueId(String group_name);
}


