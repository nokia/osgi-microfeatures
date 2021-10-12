// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb.mux;

import java.util.Map;

public interface IOHRouterFactory {

    public Object newIOHRouterConfig (Map<String, Object> props);

    public IOHRouter newIOHRouter (Object routerConfig);
    
}
