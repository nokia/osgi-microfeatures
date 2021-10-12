// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.test.bundle.def;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@SuppressWarnings("restriction")
@Config(section = "HelloService configuration")
public interface HelloConfiguration {
    
    @StringProperty(title = "Greeting Message", dynamic = true, required = true, defval = "Hello", help = "My Help.")
    public String getGreetingMessage();

}