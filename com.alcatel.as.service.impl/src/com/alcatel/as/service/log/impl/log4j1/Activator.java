// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.log.impl.log4j1;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyActivatorBase {

  @Override
  public void init(BundleContext ctx, DependencyManager dm) throws Exception {      
    //
    // Log4j service
    //
    boolean activateOSGiLogger = "debug".equals(ctx.getProperty("ds.loglevel"));
    String log4jProperty = ctx.getProperty("log4j.property");
    log4jProperty = (log4jProperty == null) ? "log4j.configuration" : log4jProperty;
    String log4jPID = ctx.getProperty("log4j.pid");
    if (log4jPID == null) log4jPID = "log4j";
    
    Log4jConfigurator logConfigurator = new Log4jConfigurator(activateOSGiLogger, log4jProperty);
    // 
    // If a log4j.properties file is found from classpath, initialize log4j right now.
    //
    try(InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream("log4j.properties")) {
    	if (in != null) {
    		Properties props = new Properties();
    		props.load(in);
    		logConfigurator.loadConfiguration(props);
    	}		  
    }
    
    Component log4jConfigurator = createComponent()
        .setImplementation(logConfigurator)
        .add(createServiceDependency().setService(Dictionary.class, "(service.pid=" + log4jPID + ")").setCallbacks("updated", "updated", null).setRequired(true));
    dm.add(log4jConfigurator);                
  }
  
}
