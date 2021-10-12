// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.httploader;

import java.util.Map;

import com.alcatel_lucent.as.management.annotation.config.BooleanProperty;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@SuppressWarnings("restriction")
@com.alcatel_lucent.as.management.annotation.config.Config(section="http loader")
public interface Config {
	
	@StringProperty(help="use GET POST PUT DELETE" , title="Http methode", defval="GET")
	String getMethodeType();
	
	@FileDataProperty(title = "My Body", dynamic = true, required = true, fileData = "body.txt", help = "body for you http request if it needs one.")
    String getHtmlBody();
	
	@StringProperty(help="target url path, like \"/hello\"", title="url path", defval="/hello?size=1024")
	String getUrl();
	
	@StringProperty(help="set content-type if it is needed", title="content-type", defval="application/x-www-form-urlencoded")
	String getContentType();
	
	@StringProperty(help="path of your Client KS", title="Client KS path", defval="/tmp/client.ks")
    String getClientKsPath();
	
	@StringProperty(title="request body variable name", help="name of a variable included in request body. The variable is then replaced by a counter that is incremented each time we send a new request", required=false, defval="")
	String getLoopVariable();

	@IntProperty(help="loop counter start", title="loop counter start", defval=1, min=0, max=1000000000)
	int getLoopStart();

	@IntProperty(min=0, max=1000000000, help="loop increment", title="loop increment", defval=1)
	int getLoopIncrement();

	@StringProperty(help="remote web server ip addr", title="remote address", defval="127.0.0.1")
	String getTo();

	@StringProperty(help="list of local addresses to bind to (comma separated)", title="local addresses", defval="127.0.0.1")
	default String[] getFrom() { return new String[] { "127.0.0.1" }; }
	
	@IntProperty(min=0, max=65535, help="remote web server port number", title="web server port", defval=8080)
	default int getPort() { return 8080; }

	@IntProperty(min=0, max=1000000, help="number of clients (one socket per client)", title="http clients", defval=1000)
	default int getClients() { return 1000; }

	@IntProperty(min=0, max=1000000, help="max requests to send for a given http client (0 = unlimitted)", title="max client requests", defval=100)
	default int getMaxClientRequests() { return 0; } 
	
	@BooleanProperty(help="add a Connection: close header in last request sent to the server before closing it", title="use connection close", defval=false)
	default boolean useConnectionClose() { return true; }
	
	@BooleanProperty(help="Using Proxy", title="Proxy", defval=false)
	default boolean proxy() { return false; }
	
	@BooleanProperty(help="Using Https", title="Https", defval=false)
	default boolean https() { return false; }
	
	@StringProperty(help="Http request headers", title="Http request headers", required=false, defval="")
	Map<String, String> getHeaders();
	
	@StringProperty(help="Test duration", title="Test duration", required=false, defval="15")
	int getTestDuration();
}