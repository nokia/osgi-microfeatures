// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.socket;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.util.*;

public class SimpleUdpSocket extends SimpleSocket implements UdpSocket
{

    private static MuxClientManager clientManager = new MuxClientManager();

    private long _bindId;
    private boolean _shared;

    /**
     * The Constructor to use when a UdpSocket is requested from the
     * MuxConnection.
     */
    public SimpleUdpSocket(MuxConnection connection, int localIP, int localPort, long bindId, boolean shared)
    {
        this(connection, MuxUtils.getIPAsString(localIP), localPort, bindId, shared);
    }

    public SimpleUdpSocket(MuxConnection connection, String localIP, int localPort, long bindId,
                           boolean shared)
    {
        super(connection, TYPE_UDP, 0, localIP, localPort);
        _bindId = bindId;
        _shared = shared;
    }

    /**
     * The Constructor to use when a shared UdpSocket is bound.
     */
    public SimpleUdpSocket(MuxConnection connection, int id, int localIP, int localPort, long bindId)
    {
        this(connection, id, MuxUtils.getIPAsString(localIP), localPort, bindId);
    }

    public SimpleUdpSocket(MuxConnection connection, int id, String localIP, int localPort, long bindId)
    {
        super(connection, TYPE_UDP, id, localIP, localPort);
        _bindId = bindId;
        _shared = true;
    }

    @Override
    public long getBindId()
    {
        return _bindId;
    }

    @Override
    public boolean isShared()
    {
        return _shared;
    }

    public void setBindId(long bindId)
    {
        _bindId = bindId;
    }

    public boolean sendData(int remoteIP, int remotePort, byte[] data, int off, int len, boolean copy)
    {
        return sendData(MuxUtils.getIPAsString(remoteIP), remotePort, data, off, len, copy);
    }

    public boolean sendData(String remoteIP, int remotePort, byte[] data, int off, int len, boolean copy)
    {
        return getMuxConnection().sendUdpSocketData(getSockId(), remoteIP, remotePort, null, 0, data, off,
                                                    len, copy);
    }

    @Override
    public boolean open(long timeout)
    {
        return open(clientManager, timeout);
    }

    public boolean open(MuxClientManager manager, long timeout)
    {
        if (timeout == -1)
        {
            return sendBind();
        }

        ConnectClient client = new ConnectClient(timeout);
        manager.runMuxClient(client);
        return (getSockId() != 0);
    }

    private boolean sendBind()
    {
        return getMuxConnection().sendUdpSocketBind(_bindId, getLocalIPString(), getLocalPort(), _shared);
    }

    private class ConnectClient extends MuxClient
    {

        private SimpleUdpSocket socket = SimpleUdpSocket.this;

        private ConnectClient(long timeout)
        {
            super(_bindId, timeout);
            setLock(socket);
        }

        @Override
        public boolean sendMuxData()
        {
            return sendBind();
        }

        private boolean resume(int id, String localIP, int localPort)
        {
            synchronized (getLock())
            {
                if (canceled)
                {
                    return false;
                }
                if (id != 0)
                {
                    set(id, localIP, localPort);
                }
                resume();
                return (id != 0);
            }
        }
    }

    public static SimpleUdpSocket notify(long bindId, int id, int localIP, int localPort)
    {
        return notify(bindId, id, MuxUtils.getIPAsString(localIP), localPort);
    }

    public static SimpleUdpSocket notify(long bindId, int id, String localIP, int localPort)
    {
        return notify(clientManager, bindId, id, localIP, localPort);
    }

    public static SimpleUdpSocket notify(MuxClientManager manager, long bindId, int id, int localIP,
                                         int localPort)
    {
        return notify(manager, bindId, id, MuxUtils.getIPAsString(localIP), localPort);
    }

    public static SimpleUdpSocket notify(MuxClientManager manager, long bindId, int id, String localIP,
                                         int localPort)
    {
        ConnectClient client = (ConnectClient) manager.getMuxClient(bindId);
        if (client == null)
            return null;
        if (client.resume(id, localIP, localPort))
            return client.socket;

        return null;
    }

    @Override
    public String toString()
    {
        StringBuilder buff = new StringBuilder();
        buff.append("SimpleUdpSocket [ sockId=");
        buff.append(getSockId());
        buff.append(", bindId=");
        buff.append(_bindId);
        buff.append(", localHost=");
        buff.append(getLocalHost());
        buff.append(", localAddress=");
        buff.append(getLocalAddr());
        buff.append(", localPort=");
        buff.append(getLocalPort());
        buff.append(", ");
        buff.append((_shared) ? "shared" : "not-shared");
        buff.append(" ]");
        return buff.toString();
    }
}
