package com.alcatel.as.ioh.server;

import java.util.*;
import java.net.*;
import java.nio.*;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface Server {

    public static final String PROP_PROCESSOR_ID = "processor.id";
    public static final String PROP_PROCESSOR_ADVERTIZE_ID = "processor.advertize.id";
    public static final String PROP_PROCESSOR_ADVERTIZE_NAME = "processor.advertize.name";
    public static final String PROP_SERVER_NAME = "server.name";
    public static final String PROP_SERVER_REACTOR = "server.reactor";
    public static final String PROP_SERVER_IP = "server.ip";
    public static final String PROP_SERVER_IF = "server.if";
    public static final String PROP_SERVER_PORT = "server.port";
    public static final String PROP_SERVER_PORT_RANGE = "server.port.range";
    public static final String PROP_SERVER_RETRY = "server.retry";
    public static final String PROP_SERVER_SECURE = "server.secure";
    public static final String PROP_SERVER_NAMESPACE = "server.namespace";
    public static final String PROP_SERVER_CHECK = "server.check";
    public static final String PROP_SERVER_STANDBY = "server.standby";
    public static final String PROP_SERVER_ACCEPT_THROTTLING = "server.accept.throttling";
    public static final String PROP_CLIENT_ALLOW_IP = "client.allow.ip";
    /**
     * @deprecated use PROP_CLIENT_ALLOW_SUBNET instead
     */
    public static final String PROP_CLIENT_ALLOW_MASK = "client.allow.mask";
    public static final String PROP_CLIENT_ALLOW_SUBNET = "client.allow.subnet";
    public static final String PROP_CLIENT_REJECT_IP = "client.reject.ip";
    /**
     * @deprecated use PROP_CLIENT_REJECT_SUBNET instead
     */
    public static final String PROP_CLIENT_REJECT_MASK = "client.reject.mask";
    public static final String PROP_CLIENT_REJECT_SUBNET = "client.reject.subnet";
    public static final String PROP_CLIENT_MAX = "client.max";
    public static final String PROP_CLIENT_PRIORITY = "client.priority";
    public static final String PROP_READ_TIMEOUT = "read.timeout";
    public static final String PROP_READ_EXECUTOR = "read.executor";
    public static final String PROP_READ_BUFFER = "read.buffer";
    public static final String PROP_READ_ENABLED = "read.enabled";
    public static final String PROP_WRITE_BUFFER = "write.buffer";
    public static final String PROP_READ_BUFFER_DIRECT = "read.buffer.direct";
    public static final String PROP_CLOSE_TIMEOUT = "close.timeout";
    
    public Map<String, Object> getProperties ();

    public boolean isOpen ();

    public void close ();

    public void closeAll ();

    public void stopListening (boolean closeAll);

    public void resumeListening ();
    
    public <T> T attachment ();

    public <T> T attach (Object attachment);
}
