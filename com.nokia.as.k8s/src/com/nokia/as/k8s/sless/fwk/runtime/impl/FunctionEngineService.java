// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.k8s.sless.fwk.runtime.impl;

import com.nokia.as.k8s.sless.*;
import com.nokia.as.k8s.sless.fwk.*;
import com.nokia.as.k8s.sless.fwk.runtime.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.concurrent.Future;
import java.util.*;
import io.cloudevents.CloudEvent;
import org.apache.log4j.Logger;
import com.alcatel.as.service.concurrent.SerialExecutor;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.concurrent.*;

import org.osgi.framework.*;
import java.io.*;
import java.util.function.*;
import java.util.stream.Stream;
import org.osgi.service.component.annotations.*;

@Component(service={FunctionEngineService.class})
public class FunctionEngineService {

    public final static Logger LOGGER = Logger.getLogger("sless.runtime.function");
    public final static Logger LOGGER_EXEC = Logger.getLogger("sless.runtime.function.exec");

    private SerialExecutor _exec = new SerialExecutor ();

    private MeteringService _metering;
    private PlatformExecutors _execs;
    private BundleContext _osgi;
    private BundleDeployer _deployer;

    private Map<String, FunctionEngine> _engines = new HashMap<> ();

    @Reference
    public void setMetering (MeteringService ms){
	_metering = ms;
    }
    @Reference
    public void setPlatformExecs (PlatformExecutors execs){
	_execs = execs;
    }

    @Activate
    protected void activate(BundleContext osgi){
	_osgi = osgi;
	_deployer = new BundleDeployer (_osgi);
    }

    public void join (FunctionResource function, RouteResource route, BiConsumer<FunctionEngine, FunctionMeters.RouteMeters> callback){
	_exec.execute (() -> {
		String name = function.name;
		FunctionEngine engine = _engines.get (name);
		if (engine == null){
		    engine = new FunctionEngine (FunctionEngineService.this, function, _execs.createQueueExecutor (_execs.getProcessingThreadPoolExecutor ()));
		    engine.init (function, _osgi, _metering);
		    _engines.put (name, engine);
		}
		engine.join (FunctionEngineService.this, function, route, callback);
	    });
    }
    public void destroyed (FunctionEngine engine){
	_exec.execute (() -> {
		_engines.remove (engine.name ());
	    });
    }

    public BundleDeployer deployer (){ return _deployer;}
    
}
