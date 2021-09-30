package com.nokia.as.iohwebconsole.admin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.http.api.ExtHttpService;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;


@Component
public class IohWebConsoleServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
//	private static final String CMD_PATH = "/cmdiowebconsole";
	
	private static final String GUI_PATH = "/iohwebconsole";
	
	@ServiceDependency
	ExtHttpService _httpService;


	@Inject
	BundleContext _bc;

	@ServiceDependency
	LogServiceFactory _logFac;

	LogService _log;

	@Start
	void start() {
		try {
			_log = _logFac.getLogger(IohWebConsoleServlet.class);
			_log.warn("Registering IOH WEB Console Servlet ...");
//			_httpService.registerServlet(CMD_PATH, this, null, null);
			AtomicReference<String> guiPath = new AtomicReference<String>(_bc.getProperty("gui.url.path"));
			if(guiPath.get() == null || "".equals(guiPath.get().trim())){
				guiPath.set(GUI_PATH);
			}
			if(!guiPath.get().startsWith("/")){
				guiPath.set("/"+guiPath.get());
			}
			if(_log.isDebugEnabled()){
				_log.debug("Using gui path: "+guiPath.get());
			}
			_httpService.registerResources(guiPath.get().trim(), "admin", null);
			
			_httpService.registerFilter(new Filter(){
				@Override
				public void init(FilterConfig chain) throws ServletException {}
				
				@Override
				public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
						throws IOException, ServletException {
					String path = ((HttpServletRequest)req).getPathInfo();
					if(path == null || "/".equals(path) || GUI_PATH.equals(path) || (GUI_PATH + "/").equals(path)){
						((HttpServletResponse) resp).sendRedirect(guiPath.get()+"/index.html");
					} else {
						chain.doFilter(req, resp);
					}
				}
				
				@Override
				public void destroy() {}
			}, guiPath.get().trim()+".*",  null, 0, null);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {

			List<String> paths = getPathInfo(req);

			if (_log.isDebugEnabled()) {
				_log.debug("doGet: request paths=" + paths);
			}

			resp.setStatus(400);

		} catch (Exception e) {
			e.printStackTrace();
			try {
				respondError(resp, e);
			} catch (Exception ej) {}
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		List<String> paths = getPathInfo(req);
		try {
			if (_log.isDebugEnabled()) {
				_log.debug("doPost: request paths=" + paths+", content-type="+req.getContentType());
			}

			resp.setStatus(400);

		} catch (Exception e) {
			e.printStackTrace();
			try {
				respondError(resp, e);
			} catch (Exception ej) {}
		}
	}
	
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		List<String> paths = getPathInfo(req);
		try {
			if (_log.isDebugEnabled()) {
				_log.debug("doDelete: request paths=" + paths+", content-type="+req.getContentType());
			}

			resp.setStatus(400);

		} catch (Exception e) {
			e.printStackTrace();
			try {
				respondError(resp, e);
			} catch (Exception ej) {}
		}
	}


	private void respondError(HttpServletResponse resp, Exception e) throws Exception {
		respondError(resp,e.toString());
	}
	
	private void respondError(HttpServletResponse resp, String message) throws Exception {
		resp.setStatus(500);
		JSONObject result = new JSONObject();
		result.put("error", message);
		resp.setContentType("application/json;charset=UTF-8");
		result.write(resp.getWriter());
	}

	private List<String> getPathInfo(HttpServletRequest request) throws UnsupportedEncodingException {
		List<String> paths = new ArrayList<String>();
		String requestUri = request.getRequestURI().substring(request.getServletPath().length());
		while (requestUri.startsWith("/")) {
			requestUri = requestUri.substring(1);
		}
		int queryIndex = requestUri.indexOf("?");
		if (queryIndex == -1) {
			queryIndex = requestUri.indexOf("&");
		}
		if (queryIndex > -1) {
			requestUri = requestUri.substring(0, queryIndex);
		}
		for (String path : requestUri.split("/")) {
			paths.add(URLDecoder.decode(path.trim(),"UTF-8"));
		}
		return paths;
	}

}
