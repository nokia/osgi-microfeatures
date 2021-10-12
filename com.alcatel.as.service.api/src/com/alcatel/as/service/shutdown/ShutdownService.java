// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.shutdown;

import com.alcatel.as.util.config.ConfigConstants;

/**
 * An interface wrapping the shutdown service.
 * 
 */
public interface ShutdownService {

    /**
     * The osgi topic to monitor shutdown events.
     */
    public static final String SHUTDOWN_TOPIC = "com/alcatel/as/service/shutdown/TOPIC";
    /**
     * The event property indicating the shutdown target instance id.
     */
    public static final String SHUTDOWN_TARGET_INSTANCE_ID = ConfigConstants.INSTANCE_ID;
    /**
     * The event property indicating the shutdown target instance name.
     */
    public static final String SHUTDOWN_TARGET_INSTANCE_NAME = ConfigConstants.INSTANCE_NAME;
    /**
     * The event property indicating the informative shutdown delay in seconds (if known).
     */
    public static final String SHUTDOWN_DELAY = "shutdown.delay";
    
    /**
     * Performs an immediate stop.
     * <p/>Shutdownables are not called.
     * @param exitStatus the exit status
     * @param dumpThreads indicates if a threads dump is requested
     */
    void halt(int exitStatus, boolean dumpThreads);
    
    /**
     * Registers a Shutdownable.
     * <br/>This is an alternative to registering it in the OSGi registry.
     * @param shutdownable the Shutdownable
     * @return true if the Shutdownable was registered (else false if a shutdown sequence was already fired).
     */
    boolean register (Shutdownable shutdownable);

    /**
     * Initiates a shutdown sequence.
     * @param src an optional informative Object identifying the shutdown origin
     * @param exitStatus the exit status
     * @return true if the sequence was launched, false otherswise (if another sequence was already initiated)
     */
    boolean shutdown (Object src, int exitStatus);
    
     /**
     * Initiates a shutdown sequence with defaut exit status.
     * @param src an optional informative Object identifying the shutdown origin
     * @return true if the sequence was launched, false otherswise (if another sequence was already initiated)
     */
    boolean shutdown (Object src);

    /**
     * Initiates a shutdown sequence.
     * The callback runnable 
     * @param src an optional informative Object identifying the shutdown origin
     * @param onCompleted a Runnable that will be called on completion. This runnable has the responsibility to perform the exit: this API call cannot be used as a repeatable mechanism.
     * @return true if the sequence was launched, false otherswise (if another sequence was already initiated)
     */
    boolean shutdown (Object src, Runnable onCompleted);
    
    
    /**
     * Triggers a shutdown notification event.
     * <br/>The goal is to inform about the planned shutdown of an instance (remote or local). This triggers an actual shutdown if the targeted instance is the local instance.
     * @param props the information regarding the shutdown (see SHUTDOWN_XXX constants above)
     */
    void sendShutdownEvent (java.util.Map<String, String> props);

    /**
     * Triggers a shutdown notification event.
     * <br/>This is a shortcut to the other method with the map.
     * @param instanceKey the key to identify the instance involved (SHUTDOWN_TARGET_INSTANCE_ID or SHUTDOWN_TARGET_INSTANCE_NAME usually)
     * @param instanceValue the corresponding value
     */
    void sendShutdownEvent (String instanceKey, String instanceValue);
}
