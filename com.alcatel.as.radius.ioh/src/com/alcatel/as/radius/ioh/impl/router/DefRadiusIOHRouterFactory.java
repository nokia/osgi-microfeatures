// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.radius.ioh.impl.router;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;

import java.util.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

import com.alcatel.as.radius.parser.*;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.alcatel.as.ioh.tools.ByteBufferUtils;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.radius.ioh.*;
import com.alcatel.as.service.metering2.*;

import static com.alcatel.as.ioh.tools.ByteBufferUtils.getUTF8;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel_lucent.as.management.annotation.config.*;

@Component(service={RadiusIOHRouterFactory.class}, property={"router.id=def"}, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class DefRadiusIOHRouterFactory extends RadiusIOHRouterFactory {

    public static Logger LOGGER = Logger.getLogger ("as.ioh.radius.router.def");
    public final static String CONF_ROUTE_BY_AUTHENTICATOR = "radius.ioh.route.by-authenticator";

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target = "(service.pid=system)")
    public void setSystemConfig(Dictionary<String, String> system){
	super.setSystemConfig (system);
    }
    public void unsetSystemConfig(Dictionary<String, String> system){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]			
    }
    
    @Reference
    public void setExecutors(PlatformExecutors executors){
	super.setExecutors (executors);
    }
    @Reference
    public void setMetering(MeteringService metering){
	super.setMetering (metering);
    }
    @Reference(target="(strict=false)")
    public void setTimerService(TimerService timerService){
	super.setTimerService (timerService);
    }
    @Modified
    public void updated(Map<String, String> conf) {
	setConf (conf);
    }
    @Activate
    public void activate(Map<String, String> conf) {
    }
    
    @Deactivate
    public void stop() {
    }

    @Override
    public String toString (){
	return "DefRadiusIOHRouterFactory";
    }

    public RadiusIOHRouter newRadiusIOHRouter (){
	return new DefRadiusIOHRouter (this, LOGGER);
    }
}
