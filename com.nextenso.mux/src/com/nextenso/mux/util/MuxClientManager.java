// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.util;

import java.util.Enumeration;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.tracer.Level;
import alcatel.tess.hometop.gateways.tracer.Tracer;
import alcatel.tess.hometop.gateways.utils.LongHashtable;

/**
 * This is the class to use to run a MuxClient.
 * <p/>
 */
public class MuxClientManager
{
    private final static Logger logger = Logger.getLogger("com.nextenso.mux.util.MuxClientManager");
    private LongHashtable clients = new LongHashtable();

    /**
     * Constructs a new MuxClientManager.
     * <p/>
     * One MuxClientManager should be instanciated per MuxClient type.
     */
    public MuxClientManager()
    {
    }

    /**
     * Runs a MuxClient.
     * <p/>
     * The process is:<br/>
     * - register the MuxClient with its id<br/>
     * - call sendMuxData on the MuxClient<br/>
     * - wait for the response (using the MuxClient timeout)<br/>
     * The wait is stopped when resume() is called on the MuxClient.
     * 
     * @return true if the response was received, false if the response was not
     *         received
     */
    public boolean runMuxClient(MuxClient client)
    {
        synchronized (clients)
        {
            clients.put(client.getId(), client);
        }
        Object lock = client.getLock();
        synchronized (lock)
        {
            if (client.sendMuxData())
            {
                try
                {
                    // Wait on the client lock, but do this in a while loop, in order to 
                    // avoid being unexpected interrupted by the JVM (see spurious wakeup in 
                    // Object.wait javadoc).

                    // If the client timeout is zero -> no timeout, else wait no more than specified
                    // timeout millis ...

                    if (client.getTimeout() == 0)
                    {
                        while (!client.notified && !client.canceled)
                        {
                            lock.wait();
                        }
                    }
                    else
                    {
                        long waitTime = client.getTimeout();
                        long start = System.currentTimeMillis();
                        while (!client.notified && !client.canceled)
                        {
                            if (waitTime <= 0)
                            {
                                break;
                            }

                            lock.wait(waitTime);
                            waitTime = client.getTimeout() - (System.currentTimeMillis() - start);
                        }
                    }
                }
                catch (InterruptedException ie)
                {
                    // same as timeout
                }
                if (client.notified)
                {
                    return true;
                }
                else if (client.canceled)
                {
                    if (logger.isInfoEnabled())
                        logger.info(getMuxClientInfo(client) + " : client canceled");
                    return false;
                }
                else
                {
                  if (logger.isInfoEnabled())
                    logger.info(getMuxClientInfo(client) + " : response not received - timeout");
                }
            }
            else
            {
              if (logger.isInfoEnabled())
                logger.info(getMuxClientInfo(client) + " : request not sent - no response expected");
            }
            client.canceled = true;
        }
        synchronized (clients)
        {
            clients.remove(client.getId());
        }
        return false;
    }

    /**
     * Removes and Returns the MuxClient identified by the specified id.
     */
    public MuxClient getMuxClient(long id)
    {
        synchronized (clients)
        {
            return (MuxClient) clients.remove(id);
        }
    }

    /**
     * Cancels all the MuxClients that are still pending.
     */
    public int cancelMuxClients()
    {
        int i = 0;
        synchronized (clients)
        {
            Enumeration enumer = clients.elements();
            while (enumer.hasMoreElements())
            {
                i++;
                MuxClient client = (MuxClient) enumer.nextElement();
                synchronized (client.getLock())
                {
                    // we know client.notified = false
                    client.canceled = true;
                    client.getLock().notifyAll();
                }
            }
            clients.clear();
        }
        return i;
    }

    private static String getMuxClientInfo(MuxClient client)
    {
        return client.getClass().getName() + "/id=" + client.getId();
    }

}
