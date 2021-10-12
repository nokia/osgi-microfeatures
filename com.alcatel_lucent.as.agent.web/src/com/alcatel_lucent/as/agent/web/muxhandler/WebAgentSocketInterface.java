// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.agent.web.muxhandler;

import com.alcatel_lucent.as.agent.web.container.Container;


public interface WebAgentSocketInterface {
  
  public void received(byte[] data, int off, int len);
  
  public void closed(Container container);

}
