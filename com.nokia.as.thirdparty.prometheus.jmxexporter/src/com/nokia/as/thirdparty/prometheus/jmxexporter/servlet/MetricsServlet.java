// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.thirdparty.prometheus.jmxexporter.servlet;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nokia.as.thirdparty.prometheus.jmxexporter.Activator;

@Component(provides = Servlet.class)
@Property(name = "alias", value = "/jmxmetrics")
public class MetricsServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 669774964155023968L;
	@ServiceDependency(required = true)
	private Activator activator;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		resp.getWriter().println(activator.getMetrics());
	}
	
}
