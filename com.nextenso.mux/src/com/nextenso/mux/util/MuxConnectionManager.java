package com.nextenso.mux.util;

import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is used to manage MuxConnections.
 * <p/>
 * It helps a MuxHandler to keep track of the open MuxConnections.
 * <p/>
 * <b>This class is synchronized.</b>
 */
public class MuxConnectionManager
{

    private MuxConnection[] _connections = new MuxConnection[0];
    private int rnd = -1;

    /**
     * Constructor for this class.
     */
    public MuxConnectionManager()
    {
    }

    /**
     * Gets the number of connections.
     * 
     * @return The number of connections.
     */
    public synchronized int size()
    {
        return _connections.length;
    }

    /**
     * Gets the connections.
     * 
     * @return An enumeration on the connections.
     */
    public synchronized Enumeration getMuxConnections()
    {
        final MuxConnection[] list = _connections;
        return new Enumeration()
        {

            int index = 0;

            public boolean hasMoreElements()
            {
                return (index < list.length);
            }

            public Object nextElement()
            {
                if (!hasMoreElements())
                    throw new java.util.NoSuchElementException();
                return list[index++];
            }
        };
    }

    /**
     * Gets the connections according to criteria.
     * 
     * @param criteria The criteria.
     * @return An enumeration on the selected connections.
     */
    public synchronized Enumeration getMuxConnections(final Hashtable criteria)
    {
        Vector<MuxConnection> res = new Vector<MuxConnection>();
        for (MuxConnection connection : _connections)
        {
            if (match(connection, criteria))
            {
                res.add(connection);
            }
        }
        return res.elements();
    }

    /**
     * Adds a connection.
     * 
     * @param connection The connection to be added.
     */
    public synchronized void addMuxConnection(MuxConnection connection)
    {
        MuxConnection[] tmp = new MuxConnection[_connections.length + 1];
        System.arraycopy(_connections, 0, tmp, 0, _connections.length);
        tmp[_connections.length] = connection;
        _connections = tmp;
    }

    /**
     * Removes a connection.
     * 
     * @param connectionId The connection identifier.
     * @return The removed connection or null if not found.
     */
    public synchronized MuxConnection removeMuxConnection(int connectionId)
    {
        MuxConnection connection = null;
        int index = getMuxConnectionIndex(connectionId);
        if (index != -1)
        {
            connection = _connections[index];
            MuxConnection[] tmp = new MuxConnection[_connections.length - 1];
            if (index > 0)
                System.arraycopy(_connections, 0, tmp, 0, index);
            int left = tmp.length - index;
            if (left > 0)
                System.arraycopy(_connections, index + 1, tmp, index, left);
            _connections = tmp;
        }
        return connection;
    }

    /**
     * Removes a connection.
     * 
     * @param connection The connection to remove.
     * @return The removed connection or null if not found.
     */
    public MuxConnection removeMuxConnection(MuxConnection connection)
    {
        return removeMuxConnection(connection.getId());
    }

    /**
     * Gets a connection according to its identifier.
     * 
     * @param connectionId The connection identifier.
     * @return The connection or null if not found.
     */
    public synchronized MuxConnection getMuxConnection(int connectionId)
    {
        int index = getMuxConnectionIndex(connectionId);
        if (index == -1)
            return null;
        return _connections[index];
    }

    /**
     * Gets a connection randomly.
     * 
     * @return A connection or null if no connection is available.
     */
    public synchronized MuxConnection getRandomMuxConnection()
    {
        int nbConnections = size();
        if (nbConnections == 0)
        {
            return null;
        }
        rnd = (rnd + 1) % nbConnections;
        return _connections[rnd];
    }

    private int getMuxConnectionIndex(int connectionId)
    {
        for (int i = 0; i < _connections.length; i++)
        {
            if (_connections[i].getId() == connectionId)
            {
                return i;
            }
        }
        return -1;
    }

    private static boolean match(MuxConnection connection, Hashtable criteria)
    {
        boolean ok = true;
        Object o;

        // check the stakAppId
        int[] appIds = (int[]) criteria.get(MuxHandler.CONF_STACK_ID);
        if (appIds == null || appIds.length == 0)
            ok = true;
        else
        {
            int id = connection.getStackAppId();
            for (int j = 0; j < appIds.length; j++)
            {
                if (appIds[j] == id)
                {
                    ok = true;
                    break;
                }
            }
        }
        if (!ok)
        {
            return false;
        }

        // check the stackName
        o = criteria.get(MuxHandler.CONF_STACK_NAME);
        if (o == null)
        {
            ok = true;
        }
        else if (o instanceof String)
        {
            ok = (connection.getStackAppName().equalsIgnoreCase((String) o));
        }
        else
        {
            String[] stackNames = (String[]) o;
            ok = (stackNames.length == 0);
            String name = connection.getStackAppName();
            for (int j = 0; j < stackNames.length; j++)
            {
                if (stackNames[j].equalsIgnoreCase(name))
                {
                    ok = true;
                    break;
                }
            }
        }
        if (!ok)
        {
            return false;
        }

        // check the stackInstance
        o = criteria.get(MuxHandler.CONF_STACK_INSTANCE);
        if (o == null)
        {
            ok = true;
        }
        else if (o instanceof String)
        {
            ok = (connection.getStackInstance().equalsIgnoreCase((String) o));
        }
        else
        {
            String[] stackInstances = (String[]) o;
            ok = (stackInstances.length == 0);
            String instance = connection.getStackInstance();
            for (int j = 0; j < stackInstances.length; j++)
            {
                if ((stackInstances[j].equalsIgnoreCase(instance)))
                {
                    ok = true;
                    break;
                }
            }
        }
        if (!ok)
        {
            return false;
        }

        // check the stackHost
        o = criteria.get(MuxHandler.CONF_STACK_HOST);
        if (o == null)
        {
            ok = true;
        }
        else if (o instanceof String)
        {
            ok = (connection.getStackHost().equalsIgnoreCase((String) o));
        }
        else
        {
            String[] stackHosts = (String[]) o;
            ok = (stackHosts.length == 0);
            String host = connection.getStackHost();
            for (int j = 0; j < stackHosts.length; j++)
            {
                if (stackHosts[j].equalsIgnoreCase(host))
                {
                    ok = true;
                    break;
                }
            }
        }

        return ok;
    }
}
