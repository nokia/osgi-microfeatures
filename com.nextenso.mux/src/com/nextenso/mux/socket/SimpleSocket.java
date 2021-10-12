// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.socket;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.util.MuxUtils;

public abstract class SimpleSocket implements Socket
{
    private MuxConnection _connection;
    private int _type, _id, _localPort;
    private String _localHost, _localAddr, _localIP;
    
    protected SimpleSocket(MuxConnection connection, int type, int id, int localIP, int localPort)
    {
        this(connection, type, id, MuxUtils.getIPAsString(localIP), localPort);
    }

    protected SimpleSocket(MuxConnection connection, int type, int id, String localIP, int localPort)
    {
        _connection = connection;
        _type = type;
        set(id, localIP, localPort);
    }

    protected void set(int id, int localIP, int localPort)
    {
        set(id, MuxUtils.getIPAsString(localIP), localPort);
    }

    public void set(int id, String localIP, int localPort)
    {
        _id = id;
        _localIP = localIP;
        _localPort = localPort;
        _localAddr = null;
    }

    protected void setConnection(MuxConnection connection)
    {
        if (_connection == null)
        {
            _connection = connection;
        }
    }

    public MuxConnection getMuxConnection()
    {
        return _connection;
    }

    public int getSockId()
    {
        return _id;
    }

    public int getType()
    {
        return _type;
    }

    public int getLocalIP()
    {
        return MuxUtils.getIPAsInt(_localIP);
    }

    public String getLocalIPString()
    {
        return _localIP;
    }

    public int getLocalPort()
    {
        return _localPort;
    }

    public String getLocalHost()
    {
        return _localHost;
    }

    public String getLocalAddr()
    {
        return (_localAddr != null) ? _localAddr : (_localAddr = getLocalIPString());
    }

    public void setLocalHost(String localHost)
    {
        _localHost = localHost;
    }

    public boolean close()
    {
        switch (_type) {
        case TYPE_UDP:
            return getMuxConnection().sendUdpSocketClose(getSockId());
        case TYPE_TCP:
        case TYPE_TCP_SERVER:
            return getMuxConnection().sendTcpSocketClose(getSockId());
        case TYPE_SCTP:
        case TYPE_SCTP_SERVER:
            return getMuxConnection().sendSctpSocketClose(getSockId());
        }
        return false;
    }

    public boolean reset()
    {
        switch (_type) {
        case TYPE_UDP:
            return getMuxConnection().sendUdpSocketClose(getSockId());
        case TYPE_TCP:
        case TYPE_TCP_SERVER:
            return getMuxConnection().sendTcpSocketReset(getSockId());
        case TYPE_SCTP:
        case TYPE_SCTP_SERVER:
            return getMuxConnection().sendSctpSocketReset(getSockId());
        }
        return false;
    }

    public abstract boolean open(long timeout);
}
