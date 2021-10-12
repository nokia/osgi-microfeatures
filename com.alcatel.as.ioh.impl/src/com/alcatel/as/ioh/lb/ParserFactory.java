// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb;

import java.util.Map;

public interface ParserFactory {

    public Object newParserConfig (Map<String, Object> props);

    public Parser newParser (Object parserConfig, int neededBuffer);

    public default Parser newClientParser (Object routerConfig, int neededBuffer){ return newParser (routerConfig, neededBuffer);}
    public default Parser newServerParser (Object routerConfig, int neededBuffer){ return newParser (routerConfig, neededBuffer);}
    
    
}
