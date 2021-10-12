// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.features.admin;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;

@Config(section = "k8sadmin")
public interface KeycloakFilterConf {
	@FileDataProperty(title = "Keycloak Configuration file", dynamic = true, required = true, fileData = "keycloak.json", help = "enable keycloak authorization filter")
	String getKeycloakJson();

}
