package com.nokia.as.thirdparty.jaeger.impl;

import java.util.Dictionary;

public class TracerConfiguration
{

    public final String serviceName;
    public final Boolean logSpans;
    public final String agentHost;
    public final Integer agentPort;
    public final Integer flushIntervalMs;
    public final Integer maxQueueSize;
    public final String type;
    public final Number param;
    public final String managerHostPort;
    public final String propagation;
    public final Boolean preciseClock;
    public final String collectorHost;

    public TracerConfiguration(Dictionary<String, String> conf)
    {
        serviceName = conf.get("service.name");
        logSpans = conf.get("log.spans") == null ? null :
                Boolean.valueOf(conf.get("log.spans"));
        agentHost = conf.get("agent.host");
        agentPort = conf.get("agent.port.") == null ? null :
                Integer.parseInt(conf.get("agent.port"));
        flushIntervalMs = conf.get("flush.interval.ms") == null ? null :
                Integer.parseInt(conf.get("flush.interval.ms"));
        maxQueueSize = conf.get("max.queue.size") == null ? null :
                Integer.parseInt(conf.get("max.queue.size"));
        type = conf.get("type");
        param = conf.get("param") == null ? null :
                Float.parseFloat(conf.get("param"));
        managerHostPort = conf.get("managerHostPort");
        String prop = conf.get("propagation");
        propagation = (prop == null || prop.isEmpty()) ? null : prop;
        preciseClock = conf.get("clock.microsPrecision") == null ? false :
        	Boolean.valueOf(conf.get("clock.microsPrecision"));
        collectorHost = conf.get("collector.host");
    }

}
