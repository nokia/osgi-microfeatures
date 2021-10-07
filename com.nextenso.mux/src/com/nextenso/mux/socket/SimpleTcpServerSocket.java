package com.nextenso.mux.socket;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.util.MuxClient;
import com.nextenso.mux.util.MuxClientManager;
import com.nextenso.mux.util.MuxUtils;

public class SimpleTcpServerSocket extends SimpleSocket implements TcpServerSocket
{

    private final static MuxClientManager CLIENT_MANAGER = new MuxClientManager();

    private long _listenId;
    private boolean _isSecure;

    /**
     * The Constructor to use when a TcpServerSocket is requested from the
     * MuxConnection.
     */
    public SimpleTcpServerSocket(MuxConnection connection, int localIP, int localPort, long listenId,
                                 boolean secure)
    {
        this(connection, 0, localIP, localPort, listenId, secure);
    }

    /**
     * .
     */
    public SimpleTcpServerSocket(MuxConnection connection, String localIP, int localPort, long listenId,
                                 boolean secure)
    {
        this(connection, 0, localIP, localPort, listenId, secure);
    }

    /**
     * The Constructor to use when a TcpServerSocket is opened.
     */
    public SimpleTcpServerSocket(MuxConnection connection, int id, int localIP, int localPort, long listenId,
                                 boolean secure)
    {
        this(connection, id, MuxUtils.getIPAsString(localIP), localPort, listenId, secure);
    }

    /**
     * IPV6 compatible constructor.
     */
    public SimpleTcpServerSocket(MuxConnection connection, int id, String localIP, int localPort,
                                 long listenId, boolean secure)
    {
        super(connection, TYPE_TCP_SERVER, id, localIP, localPort);
        _listenId = listenId;
        _isSecure = secure;
    }

    @Override
    public long getListenId()
    {
        return _listenId;
    }

    @Override
    public boolean isSecure()
    {
        return _isSecure;
    }

    public void setListenId(long listenId)
    {
        _listenId = listenId;
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
            return sendListen();
        }
        ConnectClient client = new ConnectClient(timeout);
        manager.runMuxClient(client);
        return (getSockId() != 0);
    }

    private boolean sendListen()
    {
        return getMuxConnection().sendTcpSocketListen(_listenId, getLocalIPString(), getLocalPort(),
                                                      _isSecure);
    }

    private class ConnectClient extends MuxClient
    {

        private SimpleTcpServerSocket _socket = SimpleTcpServerSocket.this;

        private ConnectClient(long timeOut)
        {
            super(_listenId, timeOut);
            setLock(_socket);
        }

        @Override
        public boolean sendMuxData()
        {
            return sendListen();
        }

        //		private boolean resume(int _id, int _localIP, int _localPort) {
        //			return resume(_id, MuxUtils.getIPAsString(_localIP), _localPort);
        //		}

        private boolean resume(int identifier, String localIP, int localPort)
        {
            synchronized (getLock())
            {
                if (canceled)
                    return false;
                if (identifier != 0)
                {
                    set(identifier, localIP, localPort);
                }
                resume();
                return (identifier != 0);
            }
        }
    }

    public static SimpleTcpServerSocket notify(long listenId, int id, int localIP, int localPort)
    {
        return notify(listenId, id, MuxUtils.getIPAsString(localIP), localPort);
    }

    public static SimpleTcpServerSocket notify(long listenId, int id, String localIP, int localPort)
    {
        return notify(CLIENT_MANAGER, listenId, id, localIP, localPort);
    }

    public static SimpleTcpServerSocket notify(MuxClientManager manager, long listenId, int id, int localIP,
                                               int localPort)
    {
        return notify(manager, listenId, id, MuxUtils.getIPAsString(localIP), localPort);
    }

    public static SimpleTcpServerSocket notify(MuxClientManager manager, long listenId, int id,
                                               String localIP, int localPort)
    {
        ConnectClient client = (ConnectClient) manager.getMuxClient(listenId);
        if (client == null)
            return null;
        if (client.resume(id, localIP, localPort))
            return client._socket;

        return null;
    }

    @Override
    public String toString()
    {
        StringBuilder buff = new StringBuilder();
        buff.append("SimpleTcpServerSocket [ sockId=");
        buff.append(getSockId());
        buff.append(", listenId=");
        buff.append(_listenId);
        buff.append(", localHost=");
        buff.append(getLocalHost());
        buff.append(", localAddress=");
        buff.append(getLocalAddr());
        buff.append(", localPort=");
        buff.append(getLocalPort());
        buff.append(", ");
        buff.append((_isSecure) ? "secure" : "not-secure");
        buff.append(" ]");
        return buff.toString();
    }
}
