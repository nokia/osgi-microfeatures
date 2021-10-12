// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.discovery.impl.etcd;

import com.alcatel_lucent.as.management.annotation.config.*;

@Config(section = "Etcd")
public @interface EtcdConfiguration {
    
    public final static String DEF_SERVER_URL = "http://127.0.0.1:2379";
    public final static String DEF_PUBLISH_DIR = "/CASR";
    public final static int DEF_PUBLISH_TTL = 10;

    @StringProperty(title = "Etcd server url", defval = DEF_SERVER_URL, help = "Etcd server url.", dynamic = false, required=false)
    public static final String SERVER_URL = "server.url";

    @StringProperty(title = "publish dir", defval = DEF_PUBLISH_DIR, help = "Etcd publish directory.", dynamic = false, required=false)
    public static final String PUBLISH_DIR = "casr.publish.dir";

    @IntProperty(min = 1, max = 100, title = "Etcd publish ttl in seconds",
                 help = "Specifies the etcd time to leave in seconds.",
	         required = false, dynamic = false, defval = DEF_PUBLISH_TTL)
    public static final String PUBLISH_TTL = "publish.ttl.seconds";

    String getServerUrl() default DEF_SERVER_URL;

    String getCasrPublishDir() default DEF_PUBLISH_DIR;
	
    int getPublishTtlSeconds() default DEF_PUBLISH_TTL;
}
