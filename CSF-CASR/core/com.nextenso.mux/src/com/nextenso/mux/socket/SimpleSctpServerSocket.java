package com.nextenso.mux.socket;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.util.MuxClient;
import com.nextenso.mux.util.MuxClientManager;

public class SimpleSctpServerSocket extends SimpleSocket implements SctpServerSocket
{
    private final static MuxClientManager CLIENT_MANAGER = new MuxClientManager();

    private long _listenId;
    private String[] _localIPs;
    private int _maxOutStreams, _maxInStreams;
    private boolean _isSecure;

    /**
     * The Constructor to open a server SCTP socket (which listens to incoming client's sockets).
     */
    public SimpleSctpServerSocket(MuxConnection connection, String[] localIPs, int localPort, long listenId,
                                  int maxOutStreams, int maxInStreams, boolean secure)
    {
        this(connection, 0, localIPs, localPort, listenId, secure);
        _maxOutStreams = maxOutStreams;
        _maxInStreams = maxInStreams;
    }

    /**
     * The Constructor to use when a TcpServerSocket is opened and listening.
     */
    public SimpleSctpServerSocket(MuxConnection connection, int id, String[] localIPs, int localPort,
                                  long listenId, boolean secure)
    {
        super(connection, TYPE_TCP_SERVER, id, SimpleSctpSocket.getPrimaryIp(localIPs), localPort);
        _listenId = listenId;
        _localIPs = localIPs;
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
    public String[] getLocalIPs()
    {
        return _localIPs;
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
        ListenClient client = new ListenClient(timeout);
        manager.runMuxClient(client);
        return (getSockId() != 0);
    }

    private boolean sendListen()
    {
        return getMuxConnection().sendSctpSocketListen(_listenId, _localIPs, getLocalPort(), _maxOutStreams,
                                                       _maxInStreams, _isSecure);
    }

    private class ListenClient extends MuxClient
    {

        private SimpleSctpServerSocket _socket = SimpleSctpServerSocket.this;

        private ListenClient(long timeOut)
        {
            super(_listenId, timeOut);
            setLock(_socket);
        }

        @Override
        public boolean sendMuxData()
        {
            return sendListen();
        }

        private boolean resume(int identifier, String[] localIPs, int localPort)
        {
            synchronized (getLock())
            {
                if (canceled)
                    return false;
                if (identifier != 0)
                {
                    set(identifier, SimpleSctpSocket.getPrimaryIp(localIPs), localPort);
                    _localIPs = localIPs;
                }
                resume();
                return (identifier != 0);
            }
        }
    }

    public static SimpleSctpServerSocket notify(long listenId, int id, String[] localIP, int localPort)
    {
        return notify(CLIENT_MANAGER, listenId, id, localIP, localPort);
    }

    public static SimpleSctpServerSocket notify(MuxClientManager manager, long listenId, int id,
                                                String[] localIPs, int localPort)
    {
        ListenClient client = (ListenClient) manager.getMuxClient(listenId);
        if (client == null)
            return null;
        if (client.resume(id, localIPs, localPort))
            return client._socket;
        return null;
    }

    @Override
    public String toString()
    {
        StringBuilder buff = new StringBuilder();
        buff.append("SimpleSctpServerSocket [ sockId=");
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
