// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.log.impl.log4j1;

import static com.alcatel.as.util.config.ConfigConstants.COMPONENT_NAME;
import static com.alcatel.as.util.config.ConfigConstants.GROUP_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_NAME;
import static com.alcatel.as.util.config.ConfigConstants.PLATFORM_NAME;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.NDC;
import org.apache.log4j.spi.LoggingEvent;

/**
 * An appender which appends some useful information in all logging events.
 */
public class Decorator extends AppenderSkeleton
{
    /**
     * set the source information, which identified the logging application. By default, this
     * parameter is set to the jvm instance name.
     * Method not used anymore.
     *
     * @param sourceInfo The application identifier
     */
    public static void setSourceInfo(String sourceInfo)
    {
    }
    
    /**
     * set the source information, which identified the logging application. By default, this
     * parameter is set to the jvm instance name.
     * @param sourceInfo The application identifier
     */
   private static String _sourceInfo = getGroupInstName().replaceFirst("__", "/");

   private static String getGroupInstName() {    	
       String grpInstName = null;
       if (System.getProperty(GROUP_NAME) == null)
       {
           grpInstName = System.getProperty("platform.agent.instanceName");// deprecated
       }
       else
       {
           String group = System.getProperty(PLATFORM_NAME);
           group = group == null ? System.getProperty(GROUP_NAME) : group + "." + System.getProperty(GROUP_NAME);
           String instance = System.getProperty(COMPONENT_NAME);
           instance = instance == null ? System.getProperty(INSTANCE_NAME) : instance + "." + System.getProperty(INSTANCE_NAME);
           grpInstName = group + "__" + instance;
       }
       return grpInstName;
   }

   public void append(LoggingEvent event)
   {
        if (NDC.getDepth() == 0)
        {
            NDC.push(_sourceInfo);
        }
   }

    public boolean requiresLayout()
    {
        return false;
    }

    public void close()
    {
    }
}
