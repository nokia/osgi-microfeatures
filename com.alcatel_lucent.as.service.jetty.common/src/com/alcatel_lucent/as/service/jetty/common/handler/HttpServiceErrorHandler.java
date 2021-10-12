// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.jetty.common.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

public class HttpServiceErrorHandler extends ErrorHandler {
  
  private HandlerHelper helper;
  
  public HttpServiceErrorHandler(HandlerHelper helper) {
    this.helper = helper;
  }

  @Override
  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response) throws IOException, ServletException {
    String method = request.getMethod();
    if(!method.equals(HttpMethod.GET.toString()) && !method.equals(HttpMethod.POST.toString()) && !method.equals(HttpMethod.HEAD.toString())) {
      baseRequest.setHandled(true);
      return;
    }

    if (helper.hasFavicon() && method.equals(HttpMethod.GET.toString()) && request.getRequestURI().equals("/favicon.ico")) {
      if (request.getDateHeader(HttpHeader.IF_MODIFIED_SINCE.toString())==helper.getFaviconDate())
        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      else {
        helper.insertFavicon(response);
      }
      baseRequest.setHandled(true);
    }
    else if (method.equals(HttpMethod.GET.toString()) && request.getRequestURI().equals("/")) {
      helper.insertDeployedWebapps(response, request);
      baseRequest.setHandled(true);
    }
    else {
      super.handle(target, baseRequest, request, response);
    }
  }
  
}

