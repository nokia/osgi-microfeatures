package com.alcatel.as.ioh.client;

import java.util.*;
import java.net.*;
import java.nio.*;

import alcatel.tess.hometop.gateways.reactor.*;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface Client {

    public static final String PROP_CLIENT_REACTOR = "client.reactor";
    public static final String PROP_CLIENT_LOGGER = "client.logger";
    public static final String PROP_CLIENT_PRIORITY = "client.priority";
    public static final String PROP_READ_EXECUTOR = "read.executor";
    public static final String PROP_READ_TIMEOUT = "read.timeout";
    public static final String PROP_READ_PARALLEL = "read.parallel";

    public static final String PROP_CLIENT_TRANSPARENT = "client.transparent";

    public static final String PROP_TRACK_NAMESPACE = "track.namespace";
    public static final String PROP_TRACK_POD_NAME = "track.pod.name";
    public static final String PROP_TRACK_CONTAINER_NAME = "track.container.name";
    public static final String PROP_TRACK_CONTAINER_PORT_NAME = "track.container.port.name";
    public static final String PROP_TRACK_POD_LABEL = "track.pod.label";
    
    public <T> T attachment ();
    
    public <T> T attach (Object attachment);

    public Map<String, Object> getProperties ();

    public void close ();
}
