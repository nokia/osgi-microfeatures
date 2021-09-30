package com.alcatel.as.service.shutdown;

/**
 * An interface wrapping a shutdown sequence.
 * 
 */
public interface Shutdown {

    /**
     * Indicates the optional source of the shutdown (if provided).
     * @return the source
     */
    Object getSource ();
    
    /**
     * Indicates the maximum delay before the exit is forced.
     * @return the delay in milliseconds
     */
    long getExitDelay ();
    
    /**
     * Indicates the remaining delay until the exit is forced.
     * @return the remaining delay in milliseconds
     */
    long getRemainingExitDelay ();

    /**
     * Called by a Shutdownable to indicate that it completed its shutdown work.
     * <p/>When all Shutdownables are done, exit is called.
     * <p/>Exit is forced when the exit delay is reached.
     * @param shutdownable the shutdownable which is done
     */
    void done (Shutdownable shutdownable);
    
}
