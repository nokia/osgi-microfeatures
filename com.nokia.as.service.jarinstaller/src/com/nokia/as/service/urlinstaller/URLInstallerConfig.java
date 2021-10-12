// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

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
