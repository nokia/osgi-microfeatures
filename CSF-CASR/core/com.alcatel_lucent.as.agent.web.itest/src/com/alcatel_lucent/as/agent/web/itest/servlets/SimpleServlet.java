package com.alcatel_lucent.as.agent.web.itest.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SimpleServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 993135108447714324L;

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		PrintWriter out = res.getWriter();
		out.println("Hello, world!");
		out.close();
	}
}
