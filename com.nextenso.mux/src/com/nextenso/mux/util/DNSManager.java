// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.util;

import com.nextenso.mux.MuxConnection;

/**
 * This class is used to manage DNS operations.
 */
public class DNSManager
{

    public final static long TIMEOUT = 30000;

    private static MuxClientManager dnsClientManager = new MuxClientManager();
    private static String[] VOID = new String[0];
    private static MuxConnectionManager connectionManager;

    private DNSManager()
    {
    }

    public static void setMuxConnectionManager(MuxConnectionManager _connectionManager)
    {
        connectionManager = _connectionManager;
    }

    public static String[] getByAddr(String addr, MuxConnection connection)
    {
        return get(true, addr, connection);
    }

    public static String[] getByName(String name, MuxConnection connection)
    {
        return get(false, name, connection);
    }

    private static String[] get(boolean byAddr, String query, MuxConnection connection)
    {
        MuxConnection myConnection = connection;
        if (myConnection == null)
        {
            myConnection = connectionManager.getRandomMuxConnection();
            if (myConnection == null)
            {
                return VOID;
            }
        }
        DNSClient client = new DNSClient(myConnection);
        return client.run(byAddr, query);
    }

    public static void notify(long id, String[] response, int errno)
    {
        DNSClient client = (DNSClient) dnsClientManager.getMuxClient(id);
        if (client == null)
            return;
        client.notify(response, errno);
    }

    /*********** Inner class that extends MuxClient **********/

    private static class DNSClient extends MuxClient
    {

        private MuxConnection _connection;
        private String _host;
        private boolean _byAddr;
        private String[] _responses = VOID;

        private DNSClient(MuxConnection connection)
        {
            super(TIMEOUT);
            _connection = connection;
        }

        private String[] run(boolean byAddr, String host)
        {
            _byAddr = byAddr;
            _host = host;
            dnsClientManager.runMuxClient(this);
            return _responses;
        }

        private void notify(String[] responses, int errno)
        {
            synchronized (getLock())
            {
                if (canceled)
                {
                    return;
                }
                if (errno == 0)
                    _responses = responses;
                resume();
            }
        }

        @Override
        public boolean sendMuxData()
        {
            return (_byAddr) ? _connection.sendDnsGetByAddr(getId(), _host)
                            : _connection.sendDnsGetByName(getId(), _host);
        }

    }

}
