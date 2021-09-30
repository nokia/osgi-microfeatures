package com.alcatel.servlet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

public interface ServletLoader {
	Servlet loadServlet(boolean force) throws ServletException;
    Servlet initServlet(boolean force) throws ServletException;
}
