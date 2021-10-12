// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.metering.prometheus.pull.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import io.prometheus.client.exporter.MetricsServlet;

@Component(provides = Servlet.class)
@Property(name = "alias", value = "/metrics")
public class PullMetricsServlet extends MetricsServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3251817308182428551L;
	
	@ServiceDependency(required = true)
	private ServletPullMetersTracker theTracker;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		PrintWriter writer = resp.getWriter();
		
		try {
				writer.println(theTracker.getMetricsForServlet());
				writer.flush();
		    } finally {
		    	writer.close();
		}
//		resp.getWriter().println(theTracker.getMetricsForServlet());
	}
}
