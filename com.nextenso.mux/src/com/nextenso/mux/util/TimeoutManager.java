// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.util;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.nextenso.mux.MuxConnection;

/**
 * This class allows to detect sessions inactivity on a given mux connection.
 * @see MuxConnection#getTimeoutManager()
 */
public class TimeoutManager
{
    private final static Logger _logger = Logger.getLogger(TimeoutManager.class);
    private final Timer _timer = new Timer();
    private final AtomicLong _delay = new AtomicLong();
    private final MuxConnection _connection;
    private final Map<Long, Session> _sessions = new ConcurrentHashMap<Long, Session>();
    private final Listener _listener;
    private final boolean _threadSafe;

    /**
     * Listener interface used to notify about session creation and destruction.
     */
    public static interface Listener
    {
        public void sessionCreated(long sessionId);

        public void sessionDestroyed(long sessionId, long sessionDuration);
    }

    /**
     * Creates a timeout manager for a given mux connection. This constructor is reserved for the mux connection implementation.
     */
    public TimeoutManager(MuxConnection connection, long delay, Listener listener, boolean threadSafe)
    {
        _connection = connection;
        setDelay(delay);
        _listener = listener;
        _threadSafe = threadSafe;
    }

    public long getDelay()
    {
        return _delay.get();
    }

    public void setDelay(long delay)
    {
        _delay.set(delay);
    }

    public MuxConnection getMuxConnection()
    {
        return _connection;
    }

    public Listener getListener()
    {
        return _listener;
    }

    @Override
    public String toString()
    {
        StringBuilder buff = new StringBuilder("TimeoutManager [ connectionId=");
        buff.append(_connection.getId());
        buff.append(", delay=");
        buff.append(getDelay());
        buff.append(", sessions=");
        buff.append(_sessions.size());
        buff.append(" ]");
        return buff.toString();
    }

    /**
     * Called when data arrives for a given sessionId
     */
    public void update(long sessionId)
    {
        if (getDelay() > 0 && sessionId != 0L)
        {
            boolean created;
            if (_threadSafe)
            {
                synchronized (this)
                {
                    created = doUpdate(sessionId);
                }
            }
            else
            {
                created = doUpdate(sessionId);
            }

            if (created)
            {
                // always call listener outside a synchronized block
                _listener.sessionCreated(sessionId);
            }
        }
    }

    public void cancel()
    {
        if (_logger.isDebugEnabled())
        {
            _logger.debug("Cancelling TimeoutManager");
        }
        for (Map.Entry<Long, Session> entry : _sessions.entrySet())
        {
            long duration = entry.getValue().cancel();
            if (duration != -1)
            {
                _listener.sessionDestroyed(entry.getKey(), duration);
            }
        }
        _sessions.clear();
    }

    /**
     * Called when a Release Ack arrives for a given sessionId
     */
    public void ack(long sessionId)
    {
        long sessionDuration;

        if (getDelay() <= 0 || sessionId == 0L)
        {
            return;
        }

        if (_threadSafe)
        {
            synchronized (this)
            {
                sessionDuration = doAck(sessionId);
            }
        }
        else
        {
            sessionDuration = doAck(sessionId);
        }

        if (sessionDuration != -1)
        {
            _listener.sessionDestroyed(sessionId, sessionDuration);
        }
    }

    /********************* Private methods *********************/

    private boolean doUpdate(long sessionId)
    {
        Session session = _sessions.get(sessionId);
        if (session == null)
        {
            _sessions.put(sessionId, new Session(sessionId));
            return true;
        }
				session.updateLastAccessTime();
				return false;
    }

    /**
     * 
     * @return the session duration if the session has to be destroyed, or -1.
     */
    private long doAck(long sessionId)
    {
        long sessionDuration = -1;
        Session session = _sessions.remove(sessionId);
        if (session != null)
        {
            sessionDuration = session.ack();
            if (sessionDuration == -1)
            {
                // Maintain the session
                _sessions.put(sessionId, session);
            }
        }
        else
        {
            _logger.error("Received Release Ack for unknown id : " + sessionId);
        }
        return sessionDuration;
    }

    /********************* Inner Class *********************/

    private class Session
    {
        private static final long LAUNCH_DELAY = 20; // 20 milliseconds to launch
        private final long _sessionId;
        private final long _creationTime;
        private final AtomicLong _lastTime = new AtomicLong();
        private final AtomicBoolean _isAlive = new AtomicBoolean(true);
        private InnerTask _innerTask;

        private class InnerTask extends TimerTask
        {
            @Override
            public void run()
            {
                long elapsed = System.currentTimeMillis() - _lastTime.get();
                if (elapsed > getDelay())
                {
                    _isAlive.set(false);
                    if (_logger.isDebugEnabled())
                    {
                        _logger.debug("Sending Release for id : " + _sessionId);
                    }
                    _connection.sendRelease(_sessionId);
                }
                else
                {
                    launchTask(getDelay() - elapsed);
                }
            }
        }

        Session(long sessionId)
        {
            _sessionId = sessionId;
            _creationTime = System.currentTimeMillis();
            _lastTime.set(_creationTime);
            launchTask(getDelay());
        }

        private void launchTask(long delay)
        {
            _lastTime.addAndGet(-LAUNCH_DELAY);
            _timer.schedule((_innerTask = new InnerTask()), delay);
        }

        /**
         * @return the session duration if the session has been canceled, else -1
         */
        public long cancel()
        {
            if (_logger.isDebugEnabled())
            {
                _logger.debug("Cancelling Release for id : " + _sessionId);
            }
            if (_innerTask.cancel())
            {
                // The Ack will never come (expected or not)
                return System.currentTimeMillis() - _creationTime;
            }

            return -1;
        }

        public void updateLastAccessTime()
        {
            boolean doLaunchTask = false;
            // it cannot be canceled then accessed --> no check for canceled
            _lastTime.set(System.currentTimeMillis());
            if (_isAlive.compareAndSet(false, true))
            {
                doLaunchTask = true; // pending release Ack --> reset the timer
            }
            if (doLaunchTask)
            {
                launchTask(getDelay()); // out of synchronized block
            }
        }

        /**
         * 
         * @return the session duration if the session has to be destroyed, or -1.
         */
        public long ack()
        {
            // it cannot be canceled then acknowledged --> no check for canceled
            if (_isAlive.get())
            {
                // updateLastAccessTime was called --> maintain the session
                _connection.sendReleaseAck(_sessionId, false);
                return -1;
            }

            // updateLastAccessTime was not called --> OK to remove the session
            _connection.sendReleaseAck(_sessionId, true);
            return System.currentTimeMillis() - _creationTime;
        }
    }
}
