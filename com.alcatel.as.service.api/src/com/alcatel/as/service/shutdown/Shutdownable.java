package com.alcatel.as.service.shutdown;

/**
 * An interface to any contributor to the graceful shutdown mechanism
 * 
 */
public interface Shutdownable {

    /**
     * Called by the shutdown service when a shutdown sequence was launched.
     * @param shutdown the shutdown sequence : call it back to notify that it may resume
     */
    void shutdown(Shutdown shutdown);
}
