package com.nextenso.proxylet.mgmt;

/**
 * The interface to implement to be monitored by the agent.
 * <p/>The main functionalities are command listening and counters providing.
 * <br/>A Monitorable must be registered into a Monitor via <code>registerMonitorable(Monitorable monitorable)</code> to start its monitoring.
 */
public interface Monitorable {

    /**
     * Returns the major version.
     * @return the major version.
     */
    public int getMajorVersion();

    /**
     * Returns the minor version.
     * @return the minor version.
     */
    public int getMinorVersion();

    /**
     * Returns the counters values.
     * <br/>These counters must be defined in the deployment and returned in the specified order.
     * @return the counters.
     */
    public int[] getCounters();

    /**
     * Called when a command is launched in the Monitoring agent.
     * @param event the CommandEvent that wraps the command.
     */
    public void commandEvent(CommandEvent event);

}
