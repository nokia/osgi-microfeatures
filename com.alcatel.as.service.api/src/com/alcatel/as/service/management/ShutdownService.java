package com.alcatel.as.service.management;

/**
 * 
 * This is the interface to the graceful shutdown service.
 * <p>
 * The shutdown is triggered when invoking the shutdown method. Typically super
 * agent is in charge of initiating the shutdown when agent is stopped.
 * <p>
 * the shutdown will be propagated to any <code>shutdownable</code> subservice
 * that register to the service.
 * <p>
 * <b>Sample code</b>
 * 
 * <pre>
 *       // example subservice
 *       void bind (ShutdownService gs) { // be invoked by OSGI Service Layer
 *       	gs.register(new Shutdownable() {
 *       			public int shutdown() {
 *       				// check my condition
 *       				if condition==true return 0; else return 100; // recall me in 100ms
 *       			}
 *         	}
 *         );
 * </pre>
 */
public interface ShutdownService {

	/**
	 * The default value of the timeout. can be overridden when actor triggers the
	 * shutdown procedure
	 */
	public final int DELAY = 5000;

	/**
	 * Same as shutdown (callback,ShutdownService.DELAY).
	 * 
	 * @param callback
	 * @see #shutdown(Runnable, int)
	 */
	void shutdown(Runnable callback);

	/**
	 * Initiates the shutdown procedure. It will propagate the shutdown event to
	 * the shutdown participants.Once shutdown is completed or timeout fires, the
	 * callback is invoked.
	 * 
	 * @param callback
	 * @param timeout
	 */
	void shutdown(Runnable callback, int timeout);

	/**
	 * Halts the jvm. No participants are invoked.
	 * 
	 * @param status Termination status. By convention, a nonzero status code indicates abnormal termination.
	 * @param dumpThreads true if all thread stacktraces must be dumped before exiting, false if not
	 */
        void halt(int status, boolean dumpThreads);

	/**
	 * Registers a participant to the shutdown procedure. Same as
	 * register(service,5000);
	 * 
	 * @param subservice
	 */
	ShutdownService register(Shutdownable subservice);
}
