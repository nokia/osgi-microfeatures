// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.socket;

import alcatel.tess.hometop.gateways.utils.IPAddr;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.util.MuxClient;
import com.nextenso.mux.util.MuxClientManager;
import com.nextenso.mux.util.MuxUtils;

import java.util.Map;

public class SimpleTcpSocket extends SimpleSocket implements TcpSocket
{

    private static final MuxClientManager CLIENT_MANAGER = new MuxClientManager();

    private int _remotePort, _virtualPort;
    private String _remoteIP, _virtualIP;
    private String _remoteAddr, _virtualAddr;
    private String _remoteHost, _virtualHost;
    private long _connectionId;
    private boolean _isSecure, _isClient;
    private byte[][] _buffer;
    private int[] _bufferOffset, _bufferLenght;
    private int _bufferCount;
    private Map<String, String> _params;
    private boolean _connected;

    /**
     * The Constructors to use when opening a socket to a remote peer.
     */
    public SimpleTcpSocket(MuxConnection connection, String remotePeer, int remoteIP, int remotePort,
                           int localIP, int localPort, boolean secure, long connectionId)
    {
        this(connection,
             remotePeer,
             MuxUtils.getIPAsString(remoteIP),
             remotePort,
             MuxUtils.getIPAsString(localIP),
             localPort,
             secure,
             connectionId);
    }

    public SimpleTcpSocket(MuxConnection connection, String remotePeer, String remoteIP, int remotePort,
                           String localIP, int localPort, boolean secure, long connectionId)
    {
        super(connection, TYPE_TCP, 0, localIP, localPort);
        if (remotePeer != null)
        {
            if (!IPAddr.isIPAddress(remotePeer))
            {
                this._remoteHost = remotePeer; // host name
            }
            else
            {
                this._remoteAddr = remotePeer; // host IP
            }
        }
        this._remoteIP = remoteIP;
        if (MuxUtils.isNull(remoteIP) && _remoteAddr != null)
            this._remoteIP = _remoteAddr;
        this._remotePort = remotePort;
        this._isSecure = secure;
        this._isClient = false;
        this._connectionId = connectionId;
    }

    /**
     * The Constructors to use when a client TcpSocket is connected.
     */
    public SimpleTcpSocket(MuxConnection connection, int id, int remoteIP, int remotePort, int localIP,
                           int localPort, int virtualIP, int virtualPort, boolean secure,
                           boolean clientSocket, long connectionId)
    {
        this(connection,
             id,
             MuxUtils.getIPAsString(remoteIP),
             remotePort,
             MuxUtils.getIPAsString(localIP),
             localPort,
             MuxUtils.getIPAsString(virtualIP),
             virtualPort,
             secure,
             clientSocket,
             connectionId);
    }

    public SimpleTcpSocket(MuxConnection connection, int id, String remoteIP, int remotePort, String localIP,
                           int localPort, String virtualIP, int virtualPort, boolean secure,
                           boolean clientSocket, long connectionId)
    {
        super(connection, TYPE_TCP, id, localIP, localPort);
        this._remoteIP = remoteIP;
        this._remotePort = remotePort;
        this._virtualIP = virtualIP;
        this._virtualPort = virtualPort;
        this._isSecure = secure;
        this._isClient = clientSocket;
        this._connectionId = connectionId;
	_connected = true;
    }

    protected void set(int _id, int remoteIP, int localIp, int localport)
    {
        set(_id, MuxUtils.getIPAsString(remoteIP), MuxUtils.getIPAsString(localIp), localport);
    }

    public void set(int identifier, String remoteIP, String localIp, int localport)
    {
        set(identifier, localIp, localport);
        _remoteIP = remoteIP;
        _remoteAddr = null;
	_connected = true;
    }

    protected void set(int remoteIP, int remotePort)
    {
        set(MuxUtils.getIPAsString(remoteIP), remotePort);
    }

    protected void set(String remoteIP, int remotePort)
    {
        _remoteIP = remoteIP;
        _remotePort = remotePort;
    }
    public void setParams(Map<String, String> params){
	if (_connected) getMuxConnection ().sendTcpSocketParams (getSockId (), params);
	else _params = params; // store for later connection
    }

    @Override
    public long getConnectionId()
    {
        return _connectionId;
    }

    public String getRemoteHost()
    {
        return _remoteHost;
    }

    public String getRemoteAddr()
    {
        return (_remoteAddr != null) ? _remoteAddr : (_remoteAddr = _remoteIP);
    }

    @Override
    public int getRemoteIP()
    {
        return MuxUtils.getIPAsInt(_remoteIP);
    }

    public String getRemoteIPString()
    {
        return _remoteIP;
    }

    @Override
    public int getRemotePort()
    {
        return _remotePort;
    }

    public String getVirtualHost()
    {
        return _virtualHost;
    }

    public String getVirtualAddr()
    {
        return (_virtualAddr != null) ? _virtualAddr : (_virtualAddr = _virtualIP);
    }

    @Override
    public int getVirtualIP()
    {
        return MuxUtils.getIPAsInt(_virtualIP);
    }

    @Override
    public String getVirtualIPString()
    {
        return _virtualIP;
    }

    @Override
    public int getVirtualPort()
    {
        return _virtualPort;
    }

    @Override
    public boolean isSecure()
    {
        return _isSecure;
    }

    @Override
    public boolean isClientSocket()
    {
        return _isClient;
    }

    public void setConnectionId(long connectionId)
    {
        this._connectionId = connectionId;
    }

    public void setRemoteHost(String remoteHost)
    {
        this._remoteHost = remoteHost;
    }

    public void setVirtualHost(String virtualHost)
    {
        this._virtualHost = virtualHost;
    }

    public boolean sendData(byte[] data, int off, int len, boolean copy)
    {
        return getMuxConnection().sendTcpSocketData(getSockId(), data, off, len, copy);
    }

    private void allocateBuffer()
    {
        if (_buffer == null)
        {
            _buffer = new byte[8][];
            _bufferOffset = new int[8];
            _bufferLenght = new int[8];
        }
        else
        {
            byte[][] tmp = new byte[_buffer.length * 2][];
            int[] tmp_off = new int[_bufferOffset.length * 2];
            int[] tmp_len = new int[_bufferLenght.length * 2];
            System.arraycopy(_buffer, 0, tmp, 0, _bufferCount);
            System.arraycopy(_bufferOffset, 0, tmp_off, 0, _bufferCount);
            System.arraycopy(_bufferLenght, 0, tmp_len, 0, _bufferCount);
            _buffer = tmp;
            _bufferOffset = tmp_off;
            _bufferLenght = tmp_len;
        }
    }

    private void resetBuffer()
    {
        _bufferCount = 0;
        _buffer = null;
        _bufferOffset = null;
        _bufferLenght = null;
    }

    public void writeBuffer(byte[] data, int off, int len, boolean copy)
    {
        if (_buffer == null || _bufferCount >= _buffer.length)
        {
            allocateBuffer();
        }
        if (copy)
        {
            _buffer[_bufferCount] = new byte[len];
            System.arraycopy(data, off, _buffer[_bufferCount], 0, len);
            _bufferOffset[_bufferCount] = 0;
        }
        else
        {
            _buffer[_bufferCount] = data;
            _bufferOffset[_bufferCount] = off;
        }
        _bufferLenght[_bufferCount] = len;
        _bufferCount++;
    }

    public boolean flushBuffer()
    {
        try
        {
            boolean result = true;
            for (int i = 0; i < _bufferCount; i++)
            {
                result &= sendData(_buffer[i], _bufferOffset[i], _bufferLenght[i], false);
            }
            return result;
        }
        finally
        {
            resetBuffer();
        }
    }

    public boolean flushBuffer(SimpleTcpSocket sock)
    {
        try
        {
            boolean result = true;
            for (int i = 0; i < _bufferCount; i++)
            {
                result &= sock.sendData(_buffer[i], _bufferOffset[i], _bufferLenght[i], false);
            }
            return result;
        }
        finally
        {
            resetBuffer();
        }
    }

    @Override
    public boolean open(long timeout)
    {
        return open(CLIENT_MANAGER, timeout);
    }

    public boolean open(MuxClientManager manager, long timeout)
    {
        if (timeout == -1)
        {
            return sendConnect();
        }
        ConnectClient client = new ConnectClient(timeout);
        manager.runMuxClient(client);
        return (getSockId() != 0);
    }

    private boolean sendConnect()
    {
	Map<String, String> params = _params;
	_params = null; // unset them
        String remotePeer = (!MuxUtils.isNull(_remoteIP)) ? getRemoteAddr() : _remoteHost;
        return getMuxConnection().sendTcpSocketConnect(_connectionId, remotePeer, _remotePort,
                                                       getLocalIPString(), getLocalPort(), _isSecure, params);
    }

    private class ConnectClient extends MuxClient
    {

        private SimpleTcpSocket socket = SimpleTcpSocket.this;

        private ConnectClient(long timeOut)
        {
            super(_connectionId, timeOut);
            setLock(socket);
        }

        @Override
        public boolean sendMuxData()
        {
            return sendConnect();
        }

        //		private boolean resume(int _id, int _remoteIP, int _localIP, int _localPort) {
        //			return resume(_id, MuxUtils.getIPAsString(_remoteIP), MuxUtils.getIPAsString(_localIP), _localPort);
        //		}

        private boolean resume(int identifier, String remoteIP, String localIP, int localPort)
        {
            synchronized (getLock())
            {
                if (canceled)
                    return false;
                if (identifier != 0)
                {
                    set(identifier, remoteIP, localIP, localPort);
                }
                resume();
                return (identifier != 0);
            }
        }
    }

    public static SimpleTcpSocket notify(long connectionId, int id, int remoteIP, int localIP, int localPort)
    {
        return notify(connectionId, id, MuxUtils.getIPAsString(remoteIP), MuxUtils.getIPAsString(localIP),
                      localPort);
    }

    public static SimpleTcpSocket notify(long connectionId, int id, String remoteIP, String localIP,
                                         int localPort)
    {
        return notify(CLIENT_MANAGER, connectionId, id, remoteIP, localIP, localPort);
    }

    public static SimpleTcpSocket notify(MuxClientManager manager, long connectionId, int id, int remoteIP,
                                         int localIP, int localPort)
    {
        return notify(manager, connectionId, id, MuxUtils.getIPAsString(remoteIP),
                      MuxUtils.getIPAsString(localIP), localPort);
    }

    public static SimpleTcpSocket notify(MuxClientManager manager, long connectionId, int id,
                                         String remoteIP, String localIP, int localPort)
    {
        ConnectClient client = (ConnectClient) manager.getMuxClient(connectionId);
        if (client == null)
            return null;
        if (client.resume(id, remoteIP, localIP, localPort))
            return client.socket;

        return null;
    }

    @Override
    public String toString()
    {
        StringBuilder buff = new StringBuilder();
        buff.append("SimpleTcpSocket [ sockId=");
        buff.append(getSockId());
        buff.append(", connectionId=");
        buff.append(_connectionId);
        buff.append(", remoteHost=");
        buff.append(getRemoteHost());
        buff.append(", remoteAddress=");
        buff.append(getRemoteAddr());
        buff.append(", remotePort=");
        buff.append(_remotePort);
        buff.append(", localHost=");
        buff.append(getLocalHost());
        buff.append(", localAddress=");
        buff.append(getLocalAddr());
        buff.append(", localPort=");
        buff.append(getLocalPort());
        buff.append(", virtualHost=");
        buff.append(getVirtualHost());
        buff.append(", virtualAddress=");
        buff.append(getVirtualAddr());
        buff.append(", virtualPort=");
        buff.append(_virtualPort);
        buff.append(", ");
        buff.append((_isSecure) ? "secure" : "no-secure");
        buff.append(", ");
        buff.append((_isClient) ? "client" : "server");
        buff.append(" ]");
        return buff.toString();
    }

}
