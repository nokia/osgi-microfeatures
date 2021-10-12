// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.parser;

import java.net.MalformedURLException;

/**
 * Interface used to notify about http request parsing.
 */
public interface HttpRequestHandler extends HttpHandler {
  // Request prolog
  public void setHttpRequestMethod(String method);
  
  public void setHttpRequestUri(String uri, boolean relativeUrl) throws MalformedURLException;
  
  public void setHttpRequestUrlAuthority(String host) throws MalformedURLException;
}
