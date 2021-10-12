// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb;

import java.util.Map;

public interface UnicastRouterFactory {

    public Object newUnicastRouterConfig (Map<String, Object> props);

    public UnicastRouter newUnicastRouter (Object routerConfig);
    
}
