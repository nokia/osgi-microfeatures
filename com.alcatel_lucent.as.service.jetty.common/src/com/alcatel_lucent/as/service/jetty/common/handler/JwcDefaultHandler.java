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
import org.eclipse.jetty.server.handler.AbstractHandler;

public class JwcDefaultHandler extends AbstractHandler {

  private HandlerHelper helper;

  public JwcDefaultHandler(HandlerHelper handlerHelper)
  {
    this.helper = handlerHelper;
  }

  @Override
  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response) throws IOException, ServletException {  
    if (response.isCommitted() || baseRequest.isHandled())
      return;

    baseRequest.setHandled(true);

    String method=request.getMethod();

    // little cheat for common request
    if (helper.hasFavicon() && method.equals(HttpMethod.GET.toString()) && request.getRequestURI().equals("/favicon.ico"))
    {
      if (request.getDateHeader(HttpHeader.IF_MODIFIED_SINCE.toString())==helper.getFaviconDate())
        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      else
      {
        helper.insertFavicon(response);
      }
      return;
    }


    if (!method.equals(HttpMethod.GET.toString()) || !request.getRequestURI().equals("/"))
    {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;   
    }

    helper.insertDeployedWebapps(response, request);

    return;
  }

}
