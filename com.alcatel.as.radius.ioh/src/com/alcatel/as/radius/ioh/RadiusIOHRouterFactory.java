// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.radius.ioh;

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
import com.alcatel.as.service.metering2.*;

import static com.alcatel.as.ioh.tools.ByteBufferUtils.getUTF8;
import com.alcatel.as.util.config.ConfigConstants;

public abstract class RadiusIOHRouterFactory {

    public static Logger LOGGER = Logger.getLogger ("as.ioh.radius.router");

    protected PlatformExecutors _execs;
    protected TimerService _timerService;
    protected MeteringService _metering;
    protected String _platformId;
    protected Map<String, String> _conf = new HashMap<> ();
    
    public void setSystemConfig(Dictionary<String, String> system){
	_platformId = system.get (ConfigConstants.PLATFORM_ID);
    }
    public void setMetering (MeteringService metering){
	_metering = metering;
    }
    public void setExecutors(PlatformExecutors executors){
	 _execs = executors;
    }
    public void setTimerService(TimerService timerService){
	_timerService = timerService;
    }
    public void setConf (Map<String, String> conf){
	_conf = conf;
    }
    
    public MeteringService getMeteringService (){ return _metering;}
    public PlatformExecutors getPlatformExecutors (){ return _execs;}
    public TimerService getTimerService (){ return _timerService;}
    public Map<String, String> getConf (){ return _conf;}
    
    public abstract RadiusIOHRouter newRadiusIOHRouter ();

}
