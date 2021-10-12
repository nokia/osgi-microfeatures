// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb;

import java.util.Map;

public interface RouterFactory {

    public static final String PROP_WRITE_BUFFER_MAX = "write.buffer.max";

    public Object newRouterConfig (Map<String, Object> props);

    public Router newRouter (Object routerConfig);
    
}
