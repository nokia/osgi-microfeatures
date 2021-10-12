// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.agent.web.itest.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AllMethodsServlet extends HttpServlet {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private long value = 0;

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    PrintWriter out = res.getWriter();
    out.println(Long.toString(value));
    out.close();
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    String num = req.getParameter("number");
    value = Long.parseLong(num);
    PrintWriter out = res.getWriter();
    out.println("ok");
    out.close();
  }
  
  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    String num = req.getParameter("number");
    value = Long.parseLong(num);
    PrintWriter out = res.getWriter();
    out.println("ok");
    out.close();
  }
  
  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    value = -1;
    PrintWriter out = res.getWriter();
    out.println("ok");
    out.close();
  }
}
