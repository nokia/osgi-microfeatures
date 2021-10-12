// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.agent.web.itest.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DumpRequestBodyServlet extends HttpServlet {
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
		InputStream body = req.getInputStream();
		while (body.read() != -1);
		PrintWriter out = response.getWriter();
		out.println("Hello, world!");
		response.setStatus(HttpServletResponse.SC_OK);
		out.close();
	}
}
