
package com.nokia.as.agent.web.stest;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;

@Component
public class MyHttpServlet extends HttpServlet {
	@ServiceDependency
	LogServiceFactory _logFactory;

	@ServiceDependency
	HttpService _httpService;

	LogService _log;

	@Start
	void start() {
		_log = _logFactory.getLogger(MyHttpServlet.class);
		_log.warn("MyHttpServlet starting: " + this);

		try {
			// mmm , one day we'll have to clean this (why should we using higly TCCL ?)
			ClassLoader myCl = MyHttpServlet.class.getClassLoader();
			Thread.currentThread().setContextClassLoader(myCl);
			_httpService.registerServlet("/test", this, null, null);
			Thread.currentThread().setContextClassLoader(null);
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (NamespaceException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		_log.warn("MyHttpServlet doGet: " + this);

		String id = req.getSession().getId();
		PrintWriter out = resp.getWriter();
		out.println("<html>");
		out.println("<head><title>HttpServiceServlet</title></head>");
		out.println("<body>");
		out.println("HttpServiceServlet sessionId=" + id);
		out.println("</body>");
		out.println("</html>");
		out.close();
	}
}
