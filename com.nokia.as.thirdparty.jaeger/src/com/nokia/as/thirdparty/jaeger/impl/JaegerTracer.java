// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.thirdparty.jaeger.impl;

import java.util.Dictionary;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.internal.JaegerTracer.Builder;
import io.jaegertracing.internal.clock.Clock;


@Component(propagate = true, factoryPid = "com.nokia.as.thirdparty.jaeger.impl.JaegerTracer")
public class JaegerTracer
{
    static final Clock precisionClock = new PreciseClock();

    volatile io.jaegertracing.internal.JaegerTracer tracer;

    @Inject
    BundleContext bc;

    ServiceRegistration<io.jaegertracing.internal.JaegerTracer> servReg = null;
    Logger log = Logger.getLogger(io.jaegertracing.internal.JaegerTracer.class);

    Dictionary<String, String> conf;

    boolean isStarted = false;

    void updated(Dictionary<String, String> conf)
    {
        TracerConfiguration tracerConfig = new TracerConfiguration(conf);
        String serviceName = tracerConfig.serviceName;
        SamplerConfiguration sampler =
                new SamplerConfiguration().withType(tracerConfig.type)
                        .withParam(tracerConfig.param)
                        .withManagerHostPort(tracerConfig.managerHostPort);
        SenderConfiguration sender =
                new SenderConfiguration();
        
        if (tracerConfig.collectorHost != null && ! tracerConfig.collectorHost.isEmpty()) {
        	sender = sender.withEndpoint(tracerConfig.collectorHost);
        } else {
        	sender = sender.withAgentHost(tracerConfig.agentHost).withAgentPort(tracerConfig.agentPort);
        }
        
        ReporterConfiguration reporter =
                new ReporterConfiguration().withLogSpans(tracerConfig.logSpans)
                        .withFlushInterval(tracerConfig.flushIntervalMs)
                        .withMaxQueueSize(tracerConfig.maxQueueSize)
                        .withSender(sender);
        Configuration.CodecConfiguration codecConfiguration =
                Configuration.CodecConfiguration
                        .fromString(tracerConfig.propagation);

        this.conf = conf;
        Builder b = new Configuration(serviceName).withSampler(sampler)
                .withReporter(reporter)
                .withCodec(codecConfiguration)
                .getTracerBuilder();
        
        if(tracerConfig.preciseClock) {
        	b.withClock(precisionClock);
        	log.info("Using microsecond-precision Clock implementation (JDK 9+ only)");
        } else {
        	log.info("Using default jaeger Clock implementation");
        }
        
        tracer = b.build();
        
        if (isStarted)
        {
            log.info("Updated tracer configuration : " + tracer.toString());
        }
        
        registerService();
    }

    @Start
    public void start()
    {
        isStarted = true;
    }

    public void registerService()
    {
    	ServiceRegistration<io.jaegertracing.internal.JaegerTracer> reg = 
    			bc.registerService(io.jaegertracing.internal.JaegerTracer.class, tracer, conf);
    	
        if (servReg != null)
        {
            try {
            	servReg.unregister();
            } catch (Exception e) {}            
        }
        servReg = reg;
    }

    @Stop
    public void stop()
    {
        servReg.unregister();
        servReg = null;
        isStarted = false;
    }

    public io.jaegertracing.internal.JaegerTracer getTracer()
    {
        return tracer;
    }

}
