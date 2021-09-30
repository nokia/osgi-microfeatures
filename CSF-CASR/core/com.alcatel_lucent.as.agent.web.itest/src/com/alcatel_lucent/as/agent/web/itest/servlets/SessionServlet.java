package com.alcatel_lucent.as.agent.web.itest.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SessionServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2842249330825937910L;

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		HttpSession session = req.getSession(true);
		Integer counter = (Integer) session.getAttribute("counter");
		if (counter == null) {
			counter = new Integer(1);
		} else {
			counter = new Integer(counter.intValue() + 1);
		}
		session.setAttribute("counter", counter);

		PrintWriter out = res.getWriter();
		out.println(counter.intValue());
		out.close();
	}
}