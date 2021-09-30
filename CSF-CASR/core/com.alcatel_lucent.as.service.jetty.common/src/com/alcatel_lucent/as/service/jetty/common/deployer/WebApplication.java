package com.alcatel_lucent.as.service.jetty.common.deployer;

import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

import org.osgi.framework.Bundle;

public interface WebApplication {

  Bundle getBundle() ;

  Map<String, HttpServlet> getServlets() ;

  Map<String, Filter> getFilters() ;

  Map<String, ServletContextListener> getListeners() ;

  String getName() ;

  void initDone() ; 

}
