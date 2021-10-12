// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.parser;


/**
 * Interface used to notify about http response parsing.
 */
public interface HttpResponseHandler extends HttpHandler {
  // Response prolog
  public void setHttpResponseStatus(int status);
  
  public void setHttpResponseReason(String reason);
}
