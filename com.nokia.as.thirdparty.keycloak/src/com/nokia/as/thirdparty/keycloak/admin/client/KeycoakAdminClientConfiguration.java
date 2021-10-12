// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.thirdparty.keycloak.admin.client;

import org.apache.felix.dm.annotation.api.PropertyType;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@SuppressWarnings("restriction")
@PropertyType
@Config(section = "keycloak admin client")
public @interface KeycoakAdminClientConfiguration {

	@StringProperty(help = "auth server url", title = "auth server url", defval = "http://localhost:8080/auth")
	String authUrl();

	@StringProperty(help = "default realm", title = "realm", defval = "master")
	String realm();

	@StringProperty(help = "admin's username", title = "username", defval = "admin")
	String username();

	@StringProperty(help = "admin's password", title = "password", defval = "admin")
	String password();

	@StringProperty(help = "default client id", title = "client id")
	String clientId();

	@StringProperty(help = "confidential clients require a client secret", title = "client secret")
	String getClientSecret();
}