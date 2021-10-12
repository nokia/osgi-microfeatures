// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.jarinstaller;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@SuppressWarnings("restriction")
@Config(section = "JarInstaller configuration")
public interface JarInstallerConfig {
	
    @SuppressWarnings("restriction")
    @StringProperty(title = "JAR search paths",
    dynamic = true,
    required = true, 
    defval = "jars", 
    help = "Search path were JARs/WARs will be searched for wrapping and deployment. \n "
    		+ "Multiple paths can be set, separated by a string ")
    public String getJarDirs();
}
