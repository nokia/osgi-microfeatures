// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.jetty.common.connector;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface EndPointManager {
  
  public void messageReceived(Object clientContext, ByteBuffer ... bufs) throws IOException;

  public void connectionClosed(Object clientContext);

  public String newSession(Object clientContext);
  
  public String changeSessionId(Object clientContext);
  
}
