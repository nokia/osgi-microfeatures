package com.nokia.as.service.urlinstaller;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;

@SuppressWarnings("restriction")
@Config(section = "URLInstaller configuration")
public interface URLInstallerConfig {
    @SuppressWarnings("restriction")
	@FileDataProperty(title = "URL List", dynamic = true, required = true, fileData = "bundleURLs.txt", help = "List of bundle urls")
    public String getBundleUrls();
}
