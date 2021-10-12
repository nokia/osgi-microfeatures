// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.client;

import java.util.*;
import java.net.*;
import java.nio.*;

import alcatel.tess.hometop.gateways.reactor.*;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface TcpClient extends Client {

    public static final String PROP_CONNECT_RETRY = "connect.retry";
    public static final String PROP_CONNECT_TIMEOUT = "connect.timeout";
    public static final String PROP_CONNECT_FROM = "connect.from";
    public static final String PROP_TCP_NO_DELAY = "tcp.nodelay";
    public static final String PROP_TRACK_MODULE_ID = "track.module.id";
    public static final String PROP_TRACK_INSTANCE_NAME = "track.instance.name";

    public TcpClient addDestinations (List<InetSocketAddress> destAddresses);

    public TcpClient addDestination (InetSocketAddress destAddress, Object attachment, Map<String, Object> props);
    
    public TcpClient open (TcpClientListener listener);
    
    public List<Destination> getDestinations ();

    public int sendAll (byte[] data, boolean availableOnly);

    public void disableConnect ();

    public void enableConnect ();

    public void execute (Runnable r);

    @ProviderType
    public interface Destination {

	public void execute (Runnable r);

	public TcpClient getTcpClient ();

	public InetSocketAddress getRemoteAddress ();

	public TcpChannel getChannel ();

	public boolean isOpen ();

	public void close ();
	
	public boolean isAvailable ();
	
	public int[] getHistory ();

	public Map<String, Object> getProperties ();

	public <T> T attach (Object attachment);
	
	public <T> T attachment ();
	
	public int send (byte[] data, boolean availableOnly);

	public void open ();
    }

}
