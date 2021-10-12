// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http.proxy;

import java.util.Map;

public interface HttpProxyPluginFactory {
    
    public Object newPluginConfig (Map<String, Object> props);

    public HttpProxyPlugin newPlugin (Object config);
    
}
