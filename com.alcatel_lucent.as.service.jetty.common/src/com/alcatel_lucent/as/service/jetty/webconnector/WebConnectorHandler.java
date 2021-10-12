// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.jetty.webconnector;

import java.nio.ByteBuffer;


/**
 * Callback interface used by the WebConnector when notifying applications about
 * web container events.
 */
public interface WebConnectorHandler {
  /**
   * The web container channel has been closed. This event is used to detect
   * end of http response, which has no content-length, and a "Connection:Closed" header.
   */
  void webChannelClosed(Object clientContext);

  /**
   * Handle an http response.
   */
  void webResponse(Object clientContext, ByteBuffer ... buffers);
  
  /**
   * Ask for a new HTTP session
   * @param clientContext
   */
  String newSession(Object clientContext);

  /**
   * Ask for a new session ID
   * @param clientContext
   */
  String changeSessionId(Object clientContext);

}

