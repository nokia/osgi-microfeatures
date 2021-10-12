// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.mgmt;

/**
 * This interface contains all the method to interact with the platform monitoring agent.
 * <p/>Only a Monitorable object can use the monitoring facilities, and it should be registered via <code>registerMonitorable(Monitorable monitorable)</code> prior to doing it.
 * <br/>An instance of Monitor can be retrieved from a ProxyletContext by calling <code>getMonitor()</code>.
 */
public interface Monitor {

    /**
     * The Standard Level.
     */
    public static final int LEVEL_INFO = 1;
    /**
     * The Alarm Level.
     */
    public static final int LEVEL_ALARM = 2;
    /**
     * The Error level (equivalent to LEVEL_ALARM).
     */
    public static final int LEVEL_ERROR = LEVEL_ALARM;
    /**
     * The Warning level.
     */
    public static final int LEVEL_WARNING = 3;
    /**
     * The Clear level.
     */
    public static final int LEVEL_CLEAR = 8;

    /**
     * Sends a message to the monitoring agent.
     * <br/>The level can be LEVEL_INFO or LEVEL_ALARM.
     * @param monitorable the Monitorable that generated the message.
     * @param level the message level, LEVEL_INFO or LEVEL_ALARM.
     * @param message the message to send.
     * @deprecated
     */
    @Deprecated
		public void sendMessage(Monitorable monitorable, int level, String message);

    /**
     * Sends an alarm to the monitoring agent.
     * <br/>The level can be LEVEL_INFO, LEVEL_WARNING, LEVEL_ERROR and
     * each of them can be ORed with LEVEL_CLEAR in order for a Monitorable to 'autoclear' a previous alarm.
     * @param monitorable the Monitorable that generated the message.
     * @param level the message level.
     * @param message the message to send.
     */
    public void sendAlarm(Monitorable monitorable, int alarmcode, int level, String message);

    /**
     * Registers a Monitorable.
     * <br/>The Monitorable will be monitored from then on.
     * @param monitorable the Monitorable to start monitoring.
     * @return true if the registration succeeded; false if it failed.
     */
    public boolean registerMonitorable(Monitorable monitorable);

    /**
     * Returns a Monitorable instance name.
     * <br/>When a Monitorable is registered, the Monitor assigns it a unique instance name.
     * This is the name that identifies the Monitorable in the monitoring agent.
     * @param monitorable the Monitorable whose instance name is requested.
     * @return the Monitorable instance name, or <code>null</code> if the Monitorable is not registered.
     */
    public String getInstanceName(Monitorable monitorable);

}
