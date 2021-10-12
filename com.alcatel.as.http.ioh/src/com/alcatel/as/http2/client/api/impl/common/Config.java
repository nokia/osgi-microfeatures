// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api.impl.common;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;


public class Config {

    /* ----------------------------- CONFIG and overload of config */
    // System property prefix
    public static final String SYSTEM_PROPERTY_PREFIX      = "asr.http2.";
    // LOGGER_NAME
    public static final String LOGGER_NAME_DEFAULT         = "as.ioh.client.api";
    public static final String LOGGER_NAME_PROPS_KEY       = "logger.name";
    public static final String LOGGER_NAME_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + LOGGER_NAME_PROPS_KEY;
    public static final String LOGGER_NAME                 = System.getProperty(LOGGER_NAME_SYSTEM_PROPERTY, LOGGER_NAME_DEFAULT);

    public static Logger getLogger(long id) {

        String.format(LOGGER_NAME,id);

        return Logger.getLogger(LOGGER_NAME);
    }
    public static Logger loggerFromProps(HashMap<String, Object> dictionary, Supplier<Logger> alternative) {
        Objects.requireNonNull(dictionary);
        Objects.requireNonNull(alternative);
        return (Logger) dictionary.getOrDefault(LOGGER_NAME_PROPS_KEY, alternative.get());
    }

    // RETRY CONNECTION
    public static final String RETRY_CONNECTION_PROPS_KEY       = "client.retry";
    public static final String RETRY_CONNECTION_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + RETRY_CONNECTION_PROPS_KEY;
    public static final int    RETRY_CONNECTION_DEFAULT_VALUE   = Integer.parseInt(System.getProperty(RETRY_CONNECTION_SYSTEM_PROPERTY, "0"));

    // ON AVAILABLE TIMEOUT
    public static final String ON_AVAILABLE_TIMEOUT_PROPS_KEY       = "client.onavailable_timeout";
    public static final String ON_AVAILABLE_TIMEOUT_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + ON_AVAILABLE_TIMEOUT_PROPS_KEY;
    public static final long   ON_AVAILABLE_TIMEOUT_DEFAULT_VALUE   = Long.parseLong(System.getProperty(ON_AVAILABLE_TIMEOUT_SYSTEM_PROPERTY, "180000"));

    // USE SERIAL EXECUTOR
    public static final String  USE_SERIAL_EXECUTOR_PROPS_KEY       = "client.use_serial_executor";
    public static final String  USE_SERIAL_EXECUTOR_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + USE_SERIAL_EXECUTOR_PROPS_KEY;
    public static final boolean USE_SERIAL_EXECUTOR_DEFAULT_VALUE   = Boolean.parseBoolean(System.getProperty(USE_SERIAL_EXECUTOR_SYSTEM_PROPERTY, "false"));

    // SINGLE_PROXY_SOCKET_DEFAULT_VALUE
    public static final String  SINGLE_PROXY_SOCKET_PROPS_KEY       = "client.single_proxy_socket";
    public static final String  SINGLE_PROXY_SOCKET_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + SINGLE_PROXY_SOCKET_PROPS_KEY;
    public static final boolean SINGLE_PROXY_SOCKET_DEFAULT_VALUE   = Boolean.parseBoolean(System.getProperty(SINGLE_PROXY_SOCKET_SYSTEM_PROPERTY, "false"));
    public static final String  SINGLE_PROXY_SOCKET_ADDR_PROPS_KEY = "client.single_proxy_socket.proxy_address";
    public static final String  SINGLE_PROXY_SOCKET_PORT_PROPS_KEY = "client.single_proxy_socket.proxy_port";

    // SNI_DEFAULT_VALUE
    public static final String  SNI_PROPS_KEY       = "client.sni";
    public static final String  SNI_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + SNI_PROPS_KEY;
    public static final boolean SNI_DEFAULT_VALUE   = Boolean.parseBoolean(System.getProperty(SNI_SYSTEM_PROPERTY, "true"));

    // CLOSE GRACEFUL_TIMEOUT (in miliseconds)
    public static final String CLOSE_GRACEFUL_TIMEOUT_PROPS_KEY       = "client.close_graceful_timeout";
    public static final String CLOSE_GRACEFUL_TIMEOUT_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + CLOSE_GRACEFUL_TIMEOUT_PROPS_KEY;
    public static final long   CLOSE_GRACEFUL_TIMEOUT_DEFAULT_VALUE   = Long.parseLong(System.getProperty(CLOSE_GRACEFUL_TIMEOUT_SYSTEM_PROPERTY,
            "3600000"));

    // CLOSE GRACEFUL_DELAY (in miliseconds)
    public static final String CLOSE_GRACEFUL_DELAY_PROPS_KEY       = "client.close_graceful_delay";
    public static final String CLOSE_GRACEFUL_DELAY_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + CLOSE_GRACEFUL_DELAY_PROPS_KEY;
    public static final long   CLOSE_GRACEFUL_DELAY_DEFAULT_VALUE   = Long.parseLong(System.getProperty(CLOSE_GRACEFUL_DELAY_SYSTEM_PROPERTY,
            "-1"));

    // CONNECTION VERSION
    public static final String CONNECTION_VERSION_PROPS_KEY       = "client.connection.version";
    public static final String CONNECTION_VERSION_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + CONNECTION_VERSION_PROPS_KEY;
    public static final int    CONNECTION_VERSION_DEFAULT_VALUE   = Integer.parseInt(System.getProperty(CONNECTION_VERSION_SYSTEM_PROPERTY, "1"));

    // CYCLE_TRIGGER_THRESHOLD_ON_STREAM_ID
    public static final String CYCLE_TRIGGER_THRESHOLD_ON_STREAM_ID_PROPS_KEY       = "client.cycle_streamid_headroom";
    public static final String CYCLE_TRIGGER_THRESHOLD_ON_STREAM_ID_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + CYCLE_TRIGGER_THRESHOLD_ON_STREAM_ID_PROPS_KEY;
    public static final long   CYCLE_TRIGGER_THRESHOLD_ON_STREAM_ID_DEFAULT_VALUE   = Long.parseLong(System.getProperty(CYCLE_TRIGGER_THRESHOLD_ON_STREAM_ID_SYSTEM_PROPERTY, "100000"));

    /* ----------------------------- CONFIG and overload of config (the end)*/

    // EXECUTOR NAMES
    public static final String CLIENTAPI_EXECUTOR_NAME = "clientapi";
    public static final String POOL_EXECUTOR_NAME      = "pool";
    public static final String READ_EXECUTOR_NAME      = "read";
    public static final String WRITE_EXECUTOR_NAME     = "write";



}
