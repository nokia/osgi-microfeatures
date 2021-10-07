package com.alcatel.as.service.concurrent.impl;

import org.apache.log4j.Logger;

/**
 * Helper class to make sure that steps in a test happen in the correct order. Instantiate this
 * class and subsequently invoke <code>step(nr)</code> with steps starting at 1. You can also
 * have threads wait until you arrive at a certain step.
 */
public class Sequencer implements Runnable
{
    private final Logger _logger;
    private int _step = 0;
    private int _errorStep = -1;

    public Sequencer()
    {
        this(null);
    }

    public Sequencer(Logger logger)
    {
        _logger = logger;
    }

    /**
     * Mark this point as step <code>nr</code>.
     * 
     * @param nr the step we are in
     */
    public synchronized void step(int nr)
    {
        _step++;
        if (nr != _step)
        {
            _errorStep = nr;
            notifyAll();
            throw new IllegalArgumentException("Invalid step: " + nr + " (expected " + _step + ")");
        }
        if (_logger != null && _logger.isDebugEnabled())
        {
            _logger.debug("[Sequencer] step " + _step);
        }
        notifyAll();
    }

    /**
     * Mark this point as step <code>nr</code>.
     * 
     * @param nr the step we are in
     */
    public synchronized void step()
    {
        _step++;
        if (_logger != null && _logger.isDebugEnabled())
        {
            _logger.debug("[Sequencer] step " + _step);
        }
        notifyAll();
    }

    /**
     * Mark this point as an error step <code>nr</code>.
     * 
     * @param nr the step number which is in error.
     */
    public synchronized void error(int nr, String msg)
    {
        _errorStep = nr;
        if (_logger != null && _logger.isDebugEnabled())
        {
            _logger.debug("[Sequencer] step " + nr + " is in error: " + msg);
        }
        notifyAll();
    }

    /**
     * Wait until we arrive at least at step <code>nr</code> in the process, or fail if that takes
     * more than <code>timeout</code> milliseconds. If you invoke wait on a thread, you are
     * effectively assuming some other thread will invoke the <code>step(nr)</code> method.
     * 
     * @param nr the step to wait for
     * @param timeout the number of milliseconds to wait
     */
    public synchronized void waitForStep(int nr, long timeout)
    {
        if (_logger != null && _logger.isDebugEnabled())
        {
            _logger.debug("[Sequencer] waiting for step " + nr);
        }

        long start = System.currentTimeMillis();
        long waitTime = timeout;

        while (_step < nr && waitTime > 0 && _errorStep == -1)
        {
            try
            {
                wait(waitTime);
                waitTime = timeout - (System.currentTimeMillis() - start);
            }
            catch (InterruptedException e)
            {
            }
        }

        if (_errorStep != -1)
        {
            throw new IllegalStateException("Bad test sequence: was waiting for step " + nr
                    + ", but the test failed at sequence " + _errorStep);
        }

        if (_step < nr)
        {
            throw new IllegalStateException("Timed out waiting for " + timeout + " ms for step " + nr
                    + ", we are still at step " + _step);
        }
        if (_logger != null && _logger.isDebugEnabled())
        {
            _logger.debug("[Sequencer] arrived at step " + nr);
        }
    }

    public static Runnable createRunnableStep(final Sequencer ensure, final int nr)
    {
        return new Runnable()
        {
            public void run()
            {
                ensure.step(nr);
            }
        };
    }

    /**
     * Go to the next step
     */
    @Override
    public void run()
    {
        step();
    }

    public synchronized int getCurrentStep()
    {
       return _step;
    }
}
