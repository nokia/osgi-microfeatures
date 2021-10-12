// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http.ioh;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;

import java.util.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

import com.alcatel.as.http.parser.*;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.alcatel.as.ioh.tools.ByteBufferUtils;
import com.alcatel.as.session.distributed.*;
import com.alcatel.as.service.concurrent.*;

import static com.alcatel.as.ioh.tools.ByteBufferUtils.getUTF8;

@Component(service={HttpIOHRouterFactory.class}, property={"router.id=def"}, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class HttpIOHRouterFactory {

    public static Logger LOGGER = Logger.getLogger ("as.ioh.http.router");

    protected SessionManager _sessionMgr;
    protected PlatformExecutors _execs;
    protected TimerService _timerService;

    @Reference
    public void setExecutors(PlatformExecutors executors){
	 _execs = executors;
    }
    @Reference(target="(strict=false)")
    public void setTimerService(TimerService timerService){
	_timerService = timerService;
    }
    @Reference
    public void setSessionManager (SessionManager mgr){
	_sessionMgr = mgr;
    }
    @Modified
    public void updated (Map<String, String> conf){
    }
    @Activate
    public void activate(Map<String, String> conf) {
    }
    
    @Deactivate
    public void stop() {	
    }
    
    @Override
    public String toString (){
	return "HttpIOHRouterFactory";
    }
    
    public SessionManager getSessionManager (){ return _sessionMgr;}
    public PlatformExecutors getPlatformExecutors (){ return _execs;}
    public TimerService getTimerService (){ return _timerService;}
    
    public HttpIOHRouter newHttpIOHRouter (){
	return new HttpIOHRouter (this, LOGGER);
    }
}
