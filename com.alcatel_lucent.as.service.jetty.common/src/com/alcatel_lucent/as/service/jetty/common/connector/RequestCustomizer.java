// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.jetty.common.connector;

import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.Request;

public class RequestCustomizer implements Customizer {

  @Override
  public void customize(Connector connector, HttpConfiguration config, Request request) {
    EndPoint endPoint = request.getHttpChannel().getEndPoint();
    if (endPoint instanceof AbstractBufferEndPoint) {
      boolean secure = ((AbstractBufferEndPoint) endPoint).isSecure();
      request.setSecure(secure);
      if (secure) {
        request.setScheme(HttpScheme.HTTPS.asString());
      }
      else {
        request.setScheme(HttpScheme.HTTP.asString());
      }
    }
  }

}
