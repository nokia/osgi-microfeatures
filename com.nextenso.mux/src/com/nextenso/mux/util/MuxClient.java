package com.nextenso.mux.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This is a utility class used to implement synchronous clients.
 * <p/>
 * A client typically sends data over a MuxConnection and waits until a response
 * is received.<br/>
 * The only method to write is sendMuxData() which sends the request. The client
 * is notified when resume() is called.
 * <p/>
 * <b>All the synchronization is performed on the <code>lock</code>
 * attribute.</b>
 */
public abstract class MuxClient
{

    /**
     * An id that identifies the MuxClient.
     */
    protected long id;

    /**
     * The timeout when waiting for the response (in milliseconds).
     */
    protected long timeout;

    /**
     * A boolean indicating if the MuxClient was notified.
     */
    protected boolean notified = false;

    /**
     * A boolean indicating if the MuxClient stopped waiting for the response.
     */
    protected boolean canceled = false;

    /**
     * The Object used for synchronization (<code>this</code> by default).
     */
    protected Object lock = this;
    
    /**
     * Same as new MuxClient(MuxClient.getNextId (), timeout)
     */
    public MuxClient(long timeout)
    {
        this(getNextId(), timeout);
    }

    /**
     * Constructs a new MuxClient.
     */
    public MuxClient(long id, long timeout)
    {
        this.id = id;
        this.timeout = timeout;
    }

    /**
     * Returns the MuxClient id.
     */
    public long getId()
    {
        return id;
    }

    /**
     * Returns the MuxClient timeout.
     */
    public long getTimeout()
    {
        return timeout;
    }

    /**
     * Returns the MuxClient lock.
     */
    public Object getLock()
    {
        return lock;
    }

    protected void setLock(Object lock)
    {
        this.lock = lock;
    }

    /**
     * Notifies the Thread waiting for the response.
     */
    public boolean resume()
    {
        synchronized (lock)
        {
            if (canceled)
            {
                return false;
            }
            notified = true;
            lock.notifyAll();
            return true;
        }
    }

    /**
     * Sends the request (usually over a MuxConnection).
     * 
     * @return true if a response is expected, false if no response should be
     *         expected (usually the request was not successfully sent)
     */
    public abstract boolean sendMuxData();

    private final static AtomicLong SEED = new AtomicLong(1L);

    /**
     * Generates a unique positive id (incremental).
     */
    public static long getNextId()
    {
        return SEED.getAndIncrement();
    }

}
