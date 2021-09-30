package com.nextenso.mux.socket;

import java.nio.ByteBuffer;
import java.util.Map;

import alcatel.tess.hometop.gateways.utils.IPAddr;

import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.util.MuxClient;
import com.nextenso.mux.util.MuxClientManager;

import com.alcatel.as.util.sctp.*;

public class SimpleSctpSocket extends SimpleSocket implements SctpSocket
{

    private static final MuxClientManager CLIENT_MANAGER = new MuxClientManager();

    private int _remotePort;
    private String[] _remoteIPs, _localIPs;
    private long _connectionId;
    private boolean _isSecure, _isClient;
    private byte[][] _buffer;
    private int[] _bufferOffset, _bufferLenght;
    private int _bufferCount;
    private int _maxInStreams, _maxOutStreams;
    private Map<SctpSocketOption, SctpSocketParam> _options;
    private Map<String, String> _params;
    private boolean _connected;

    /**
     * The Constructors to use when opening a socket to a remote peer.
     */
    public SimpleSctpSocket(MuxConnection connection, String remoteIP, int remotePort, String[] localIPs,
                            int localPort, boolean secure, long connectionId, int maxOutStreams,
                            int maxInStreams)
    {
        super(connection, TYPE_SCTP, 0, getPrimaryIp(localIPs), localPort);
        _remoteIPs = new String[] { remoteIP };
        _remotePort = remotePort;
        _isSecure = secure;
        _isClient = false;
        _connectionId = connectionId;
        _localIPs = localIPs;
        _maxOutStreams = maxOutStreams;
        _maxInStreams = maxInStreams;
    }

    /**
     * The Constructors to use when a client TcpSocket is connected.
     */
    public SimpleSctpSocket(MuxConnection connection, int socketId, long connectionId, String[] remoteIPs,
                            int remotePort, String[] localIPs, int localPort, int maxOutStreams,
                            int maxInStreams, boolean secure)
    {
        super(connection, TYPE_SCTP, socketId, getPrimaryIp(localIPs), localPort);
        set(remoteIPs, remotePort);
        _isSecure = secure;
        _isClient = true;
        _connectionId = connectionId;
        _localIPs = localIPs;
        _maxOutStreams = maxOutStreams;
        _maxInStreams = maxInStreams;
	_connected = true;
    }

    public void set(int sockId, String[] remoteIPs, String[] localIPs, int localport, int maxOutStreams,
                       int maxInStreams)
    {
        set(sockId, getPrimaryIp(localIPs), localport);
        _remoteIPs = remoteIPs;
        _localIPs = localIPs;
        _maxOutStreams = maxOutStreams;
        _maxInStreams = maxInStreams;
	_connected = true;
	if (_options != null){
	    // setOptions was called between connect() and connected() : we now set them
	    getMuxConnection ().sendSctpSocketOptions (getSockId (), _options);
	    _options = null;
	}
    }

    public void setOptions(Map<SctpSocketOption, SctpSocketParam> options){
	if (_connected) getMuxConnection ().sendSctpSocketOptions (getSockId (), options);
	else _options = options; // store for later connection
    }
    public void setParams(Map<String, String> params){
	if (_connected) getMuxConnection ().sendSctpSocketParams (getSockId (), params);
	else _params = params; // store for later connection
    }
    
    protected void set(String[] remoteIPs, int remotePort)
    {
        _remoteIPs = remoteIPs;
        _remotePort = remotePort;
    }

    @Override
    public long getConnectionId()
    {
        return _connectionId;
    }

    @Override
    public int getRemotePort()
    {
        return _remotePort;
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

    @Override
    public String[] getLocalIPs()
    {
        return _localIPs;
    }

    @Override
    public String[] getRemoteIPs()
    {
        return _remoteIPs;
    }

    @Override
    public int maxInStreams()
    {
        return _maxInStreams;
    }

    @Override
    public int maxOutStreams()
    {
        return _maxOutStreams;
    }

    public void setConnectionId(long connectionId)
    {
        _connectionId = connectionId;
    }

    public boolean sendData(byte[] data, int off, int len, boolean copy)
    {
		return getMuxConnection().sendSctpSocketData(getSockId(), _remoteIPs[0], true, true, 0, 0, 0, copy, ByteBuffer.wrap(data, off, len));
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

    public boolean flushBuffer(SimpleSctpSocket sock)
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
	Map<SctpSocketOption, SctpSocketParam> options = _options;
	_options = null; // unset them
	Map<String, String> params = _params;
	_params = null; // unset them
	return getMuxConnection().sendSctpSocketConnect(_connectionId, _remoteIPs[0], _remotePort, _localIPs,
							getLocalPort(), _maxOutStreams, _maxInStreams, _isSecure, options, params);
	
    }

    static String getPrimaryIp(String[] localIPs)
    {
        if (localIPs == null || localIPs.length == 0)
        {
            return ""; // TODO check if we must return an empty stream, or null ?
        }
        
        String ip = localIPs[0];
        if (ip ==null  || ip.trim().length() == 0) 
        {
          return ""; // TODO check if we must return an empty stream, or null ?
        }

        if (!IPAddr.isIPAddress(ip))
        {
            throw new IllegalArgumentException(ip + " is not a valid ip address");
        }
        return ip;
    }

    private class ConnectClient extends MuxClient
    {
        private SimpleSctpSocket socket = SimpleSctpSocket.this;

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

        private boolean resume(int sockId, String[] remoteIPs, String[] localIPs, int localPort,
                               int maxOutStreams, int maxInStreams)
        {
            synchronized (getLock())
            {
                if (canceled)
                    return false;
                if (sockId != 0)
                {
                    set(sockId, remoteIPs, localIPs, localPort, maxOutStreams, maxInStreams);
                }
                resume();
                return (sockId != 0);
            }
        }
    }

    public static SimpleSctpSocket notify(long connectionId, int sockId, String[] remoteIPs,
                                          String[] localIPs, int localPort, int maxOutStreams,
                                          int maxInStreams)
    {
        return notify(CLIENT_MANAGER, connectionId, sockId, remoteIPs, localIPs, localPort, maxOutStreams,
                      maxInStreams);
    }

    public static SimpleSctpSocket notify(MuxClientManager manager, long connectionId, int id,
                                          String[] remoteIPs, String[] localIPs, int localPort,
                                          int maxOutStreams, int maxInStreams)
    {
        ConnectClient client = (ConnectClient) manager.getMuxClient(connectionId);
        if (client == null)
            return null;
        if (client.resume(id, remoteIPs, localIPs, localPort, maxOutStreams, maxInStreams))
            return client.socket;

        return null;
    }

    @Override
    public String toString()
    {
        StringBuilder buff = new StringBuilder();
        buff.append("SimpleSctpSocket [ sockId=");
        buff.append(getSockId());
        buff.append(", connectionId=");
        buff.append(_connectionId);
        buff.append(", remotePort=");
        buff.append(_remotePort);
        buff.append(", localIPString=");
        buff.append(getLocalIPString());
        buff.append(", localHost=");
        buff.append(getLocalHost());
       buff.append(", localAddress=");
        buff.append(getLocalAddr());
        buff.append(", localPort=");
        buff.append(getLocalPort());
        buff.append(", ");
        buff.append((_isSecure) ? "secure" : "no-secure");
        buff.append(", ");
        buff.append((_isClient) ? "client" : "server");
        buff.append(" ]");
        return buff.toString();
    }
}
