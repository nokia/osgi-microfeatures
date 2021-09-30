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
