// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.parser;

import java.net.URL;
import java.util.List;
import java.util.Map;

import com.nokia.as.autoconfig.bnd.VersionedConfiguration;

public interface Parser {

	public List<VersionedConfiguration> parse(URL url, String version);
	public Map<String, Object> parseFile(URL url);
}
