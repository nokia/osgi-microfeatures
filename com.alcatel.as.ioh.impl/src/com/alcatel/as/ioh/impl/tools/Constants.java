// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.impl.tools;

import java.text.*;
import java.util.*;

public class Constants {

    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat ("yyyy-MM-dd@HH:mm:ss");
    
    public static final String PROP_SERVER_PROCESSOR = "server.processor";
    public static final String PROP_CLIENT_PROCESSOR = "client.processor";
    public static final String PROP_SERVER_KEY = "server.key";
    public static final String PROP_SERVER_ID = "server.id";
    public static final String PROP_SERVER_LOGGER = "server.logger";
    public static final String PROP_SERVER_SINCE = "server.since";
    public static final String PROP_SERVER_SINCE_STRING = "server.since.string";
    public static final String PROP_CLIENT_ID = "client.id";
    public static final String PROP_CLIENT_SINCE = "client.since";
    public static final String PROP_CLIENT_SINCE_STRING = "client.since.string";
    public static final String PROP_CLIENT_EPHEMERAL = "client.ephemeral";

    public static Map<String, Object> setSince (boolean client, Map<String, Object> props){
	Date now = new Date (System.currentTimeMillis ());
	props.put (client ? PROP_CLIENT_SINCE : PROP_SERVER_SINCE, now);
	props.put (client ? PROP_CLIENT_SINCE_STRING : PROP_SERVER_SINCE_STRING, DATE_FORMAT.format (now));
	return props;
    }
}
