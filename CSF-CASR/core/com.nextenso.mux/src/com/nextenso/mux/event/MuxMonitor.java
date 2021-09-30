package com.nextenso.mux.event;

public interface MuxMonitor
{

    public static final String INSTANCE_ALL = "*";
    public static final String INSTANCE_ANY = "?";

    /**
     * Sends an event that will be sent to the specified appName in the current JVM.
     */
    public boolean sendLocalEvent(String appName, String appInstance, int identifierI, String identifierS,
                                  Object data, boolean asynchronous);

    /**
     * Sends an event that will be sent to the specified appName/appInstance.
     */
    public boolean sendGlobalEvent(String appName, String appInstance, int identifierI, String identifierS,
                                   byte[] data, int off, int len);

    /**
     * Registers a Monitorable.
     * <br/>Returns an implementation-specific Object that wraps the monitoring.
     * @param instanceName the monitorable's instance name (null if not relevant)
     */
    public Object registerMonitorable(int appId, String appName, String instanceName,
                                      MuxMonitorable monitorable);
}
