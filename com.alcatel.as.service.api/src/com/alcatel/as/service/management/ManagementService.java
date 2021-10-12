// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.management;

import com.alcatel.as.service.reporter.api.ExtendedInfo ;

import java.io.IOException;

/**
 * This service is used for management purpose (sending commands, alarms, or
 * getting addressing advertisements)
 * 
 * @internal For internal use only.
 * @deprecated
 */
public interface ManagementService {

	/**
	 * Application generated low level alarm.
	 */
	public static final int ALRM_LEVEl_NOTIF = 0;

	/**
	 * Application generated medium level alarm.
	 */
	public static final int ALRM_LEVEL_MON_EVENT = 1;

	/**
	 * Application generated high level alarm.
	 */
	public static final int ALRM_LEVEL_ALARM = 2;

	/**
	 * The Clear alarm level.
	 */
	public static final int ALRM_LEVEL_CLEAR = 8;

	/**
	 * Sends an alarm to the alarm manager.
	 * @param appName
	 * @param prefixedInstName
	 * @param level (see constants ALRM_* in this class)
	 * @param alarmcode
	 * @param message
	 * @throws IOException
	 */
	void sendAlarm(String appName, String prefixedInstName, int level, int alarmcode, String message)
		throws IOException;

	/**
	 * Sends an alarm to the alarm manager.
	 * @param appName
	 * @param prefixedInstName
	 * @param level (see constants ALRM_* in this class)
	 * @param alarmcode
	 * @param message
   * @param extendedInfo Extended alarm information if any
	 * @throws IOException
	 */
	void sendAlarm(String appName, String prefixedInstName, int level, int alarmcode, 
      String message, ExtendedInfo extendedInfo) throws IOException;

	/**
	 * Sends a message to a peer manageable service. The peer will receive the
	 * message through the {@link ManagedService} interface. The recipient of the
	 * message needs to be encoded in the following manner:
	 * <ul>
	 * <li>"/appName/" to send a message to all instances of the application whose
	 * name is appName.
	 * <li>"appName/prefixedInstName/" to send a message to the instance
	 * prefixedInstName of the application appName.
	 * </ul>
	 * <p>
	 * The parameter deliverToAll is only significant when a message is addressed
	 * to an application name and not to a specific application instance. In this
	 * case, there may be several instances of this application active throughout
	 * the system. Setting deliverToAll to true then causes the message to be
	 * delivered to all instances of this application. Setting this parameter to
	 * false causes the message to only be delivered to one of the active
	 * application instances.
	 * 
	 * @param recipient Message recipient
	 * @param code Message code
	 * @param deliverToAll True to deliver the message to all recipients, false to
	 *          deliver it to one of potentially many recipients
	 * @param replyTo Who the recipient needs to reply to this message, if needed
	 * @param data The data.
	 * @param off The offset.
	 * @param len The length.
	 * @throws IOException
	 */
	void sendCommand(String recipient, int code, boolean deliverToAll, String replyTo, byte[] data, int off, int len)
		throws IOException;

	/**
	 * Discovers advertisements from remote peers
	 * 
	 * @return The list of discovered remote peer advertisements.
	 */
	Advertisement[] getAdvertisements()
		throws IOException;

}
