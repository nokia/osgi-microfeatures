// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent;

import java.util.Dictionary;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.ConfigException;

import com.alcatel.as.util.config.ConfigHelper;
import com.nextenso.radius.agent.impl.AuthenticationUtils;
import com.nextenso.radius.agent.impl.RadiusServer;

public class RadiusProperties {

	private static final Logger LOGGER = Logger.getLogger("agent.radius.properties");

	private static int REQ_MAX_TRY = 3;
	private static long REQ_TIMEOUT = 3000L;
	private static long RESP_TIMEOUT = 3000L;

	public static int getRequestMaxTry() {
		return REQ_MAX_TRY;
	}

	/**
	 * Gets the request timeout.
	 * 
	 * @return The timeout in milliseconds.
	 */
	public static long getRequestTimeout() {
		return REQ_TIMEOUT;
	}

	public static void updateProperties(Dictionary cnf)
		throws ConfigException {
		REQ_MAX_TRY = ConfigHelper.getInt(cnf, PropertiesDeclaration.REQ_MAX, REQ_MAX_TRY);
		REQ_TIMEOUT = ConfigHelper.getLong(cnf, PropertiesDeclaration.REQ_TIMEOUT, REQ_TIMEOUT / 1000) * 1000;
		RESP_TIMEOUT = ConfigHelper.getLong(cnf, PropertiesDeclaration.RESP_TIMEOUT, RESP_TIMEOUT / 1000) * 1000;

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("updateProperties: Max Try Requests=" + getRequestMaxTry());
			LOGGER.debug("updateProperties: Request timeout=" + getRequestTimeout());
			LOGGER.debug("updateProperties: Response timeout=" + getResponseTimeout());
		}

		AuthenticationUtils.parseSecrets(ConfigHelper.getString(cnf, PropertiesDeclaration.SECRET));
		RadiusServer.setAccountingServers(ConfigHelper.getString(cnf, PropertiesDeclaration.NEXT_ACCT));
		RadiusServer.setAccessServers(ConfigHelper.getString(cnf, PropertiesDeclaration.NEXT_AUTH));

		RADIUS_CLIENT_SRC_IP = ConfigHelper.getString (cnf, RADIUS_CLIENT_SRC_IP_PROPERTY, RADIUS_CLIENT_SRC_IP);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("updateProperties: Radius Client Src IP = " + getClientSrcIP ());
		RADIUS_CLIENT_SRC_PORT = ConfigHelper.getInt (cnf, RADIUS_CLIENT_SRC_PORT_PROPERTY, RADIUS_CLIENT_SRC_PORT);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("updateProperties: Radius Client Src Port = " + getClientSrcPort ());
		RADIUS_CLIENT_SRC_PORT_RANGE = ConfigHelper.getInt (cnf, RADIUS_CLIENT_SRC_PORT_RANGE_PROPERTY, RADIUS_CLIENT_SRC_PORT_RANGE);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("updateProperties: Radius Client Src Port Range = " + getClientSrcPortRange ());
	}

	public static long getResponseTimeout() {
		return RESP_TIMEOUT;
	}

	/**************** The following are only used in JRadius ***************/
	
	public static final String RADIUS_CLIENT_SRC_IP_PROPERTY = "radiusagent.client.src.ip";
	private static String RADIUS_CLIENT_SRC_IP = "0.0.0.0";
	public static final String RADIUS_CLIENT_SRC_PORT_PROPERTY = "radiusagent.client.src.port";
	private static int RADIUS_CLIENT_SRC_PORT = 0;
	public static final String RADIUS_CLIENT_SRC_PORT_RANGE_PROPERTY = "radiusagent.client.src.port.range";
	private static int RADIUS_CLIENT_SRC_PORT_RANGE = 1;
    
	public static String getClientSrcIP (){ return RADIUS_CLIENT_SRC_IP;}
	public static int getClientSrcPort (){ return RADIUS_CLIENT_SRC_PORT;}
	public static int getClientSrcPortRange (){ return RADIUS_CLIENT_SRC_PORT_RANGE;}
}
